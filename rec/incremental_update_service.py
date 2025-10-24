import os
import sys
import json
import time
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
import pandas as pd
import numpy as np
from concurrent.futures import ThreadPoolExecutor
import threading
from dataclasses import dataclass
import sqlite3

sys.path.append('recommendation_service')

from recommendation_service.core.data_connector import DataServiceManager
from recommendation_service.core.recall_strategy import MultiRecallStrategy
from recommendation_service.core.home_din_ranking import HoMEDINRankingService
from recommendation_service.core.enhanced_feature_engine import EnhancedFeatureEngine

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s | %(levelname)s | %(name)s:%(funcName)s:%(lineno)d - %(message)s'
)
logger = logging.getLogger(__name__)

@dataclass
class UpdateConfig:
    """Incremental update configuration"""
    popularity_update_interval: int = 300
    cf_update_interval: int = 3600
    location_update_interval: int = 1800
    
    cache_size_limit: int = 10000
    cache_ttl: int = 3600
    
    db_path: str = 'small_data/recsys_data.db'
    
    ranking_model_path: str = 'recommendation_service/training_output/models/best_home_din_model.pth'

class IncrementalUpdateService:
    """Incremental update service"""
    
    def __init__(self, config: UpdateConfig):
        self.config = config
        self.data_service = DataServiceManager(db_path=config.db_path)
        
        self.cache = {
            'popularity_scores': {},
            'user_similarity': {},
            'item_similarity': {},
            'location_cache': {},
            'last_update_times': {}
        }
        
        self.update_status = {
            'popularity': 'idle',
            'collaborative_filtering': 'idle',
            'location': 'idle'
        }
        
        self.executor = ThreadPoolExecutor(max_workers=4)
        
        self.cache_lock = threading.RLock()
        
        self._initialize_cache()

    def _threadsafe_conn(self) -> sqlite3.Connection:
        """Create a new SQLite connection usable in background threads."""
        return sqlite3.connect(self.config.db_path, check_same_thread=False)
    
    def _initialize_cache(self):
        """Initialize cache"""
        try:
            self._load_cache_from_disk()
            self._precompute_initial_data()
            
        except Exception as e:
            logger.error(f"Cache initialization failed: {e}")
            raise

    def _json_safe(self, obj):
        """Convert object to JSON serializable structure:
        - Convert tuple keys of dictionary to strings
        - Convert numpy numbers to Python built-in numbers
        - Recursively process list/tuple/dict
        """
        import numpy as np
        if isinstance(obj, dict):
            new_dict = {}
            for k, v in obj.items():
                if isinstance(k, tuple):
                    k = ":".join(map(str, k))
                elif not isinstance(k, (str, int, float, bool)) and k is not None:
                    k = str(k)
                new_dict[k] = self._json_safe(v)
            return new_dict
        elif isinstance(obj, (list, tuple)):
            return [self._json_safe(x) for x in obj]
        elif isinstance(obj, (np.integer,)):
            return int(obj)
        elif isinstance(obj, (np.floating,)):
            return float(obj)
        elif isinstance(obj, (np.bool_,)):
            return bool(obj)
        elif obj is None:
            return None
        else:
            return obj
    
    def _load_cache_from_disk(self):
        """Load cache from disk"""
        cache_file = 'cache/incremental_cache.json'
        if os.path.exists(cache_file):
            try:
                with open(cache_file, 'r', encoding='utf-8') as f:
                    cached_data = json.load(f)
                    self.cache.update(cached_data)
            except Exception as e:
                logger.warning(f"Failed to load cache: {e}")
    
    def _save_cache_to_disk(self):
        """Save cache to disk"""
        cache_dir = 'cache'
        os.makedirs(cache_dir, exist_ok=True)
        
        cache_file = os.path.join(cache_dir, 'incremental_cache.json')
        try:
            safe_cache = self._json_safe(self.cache)
            with open(cache_file, 'w', encoding='utf-8') as f:
                json.dump(safe_cache, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"Failed to save cache: {e}")
    
    def _precompute_initial_data(self):
        """Precompute initial data"""
        try:
            
            self._update_popularity_scores()
            self._update_collaborative_filtering()
            self._update_location_cache()
            
            
        except Exception as e:
            logger.error(f"Precomputation failed: {e}")
    
    def _update_popularity_scores(self):
        """Update popularity scores"""
        try:
            conn = self._threadsafe_conn()
            
            query = """
                WITH max_ts AS (
                    SELECT MAX(ts) AS latest FROM event_log
                )
                SELECT e.item_id, e.event_type, COUNT(*) as count,
                       AVG(e.dwell_time) as avg_dwell_time
                FROM event_log e, max_ts
                WHERE e.ts <= max_ts.latest
                  AND e.ts >= datetime(max_ts.latest, '-7 days')
                GROUP BY e.item_id, e.event_type
            """
            
            df = pd.read_sql_query(query, conn)
            
            if df.empty:
                logger.warning("No event data available for popularity calculation")
                return
            
            popularity_scores = {}
            weights = {'click': 1.0, 'like': 2.0, 'fav': 3.0}
            
            for _, row in df.iterrows():
                item_id = row['item_id']
                event_type = row['event_type']
                count = row['count']
                avg_dwell = row['avg_dwell_time'] or 0
                
                if event_type in weights:
                    base_score = count * weights[event_type]
                    
                    dwell_bonus = min(avg_dwell / 60, 2.0)
                    time_decay = 1.0
                    
                    final_score = base_score * dwell_bonus * time_decay
                    
                    if item_id not in popularity_scores:
                        popularity_scores[item_id] = 0
                    popularity_scores[item_id] += final_score
            
            with self.cache_lock:
                self.cache['popularity_scores'] = popularity_scores
                self.cache['last_update_times']['popularity'] = time.time()
            
            
        except Exception as e:
            logger.error(f"Failed to update popularity scores: {e}")
    
    def _update_collaborative_filtering(self):
        """Update collaborative filtering similarity"""
        try:
            conn = self._threadsafe_conn()
            
            query = """
                SELECT user_id, item_id, 
                       SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) as clicks,
                       SUM(CASE WHEN event_type = 'like' THEN 1 ELSE 0 END) as likes,
                       SUM(CASE WHEN event_type = 'fav' THEN 1 ELSE 0 END) as favs
                FROM event_log 
                GROUP BY user_id, item_id
            """
            
            df = pd.read_sql_query(query, conn)
            
            if df.empty:
                logger.warning("No interaction data available for collaborative filtering")
                return
            
            df['rating'] = df['clicks'] * 1.0 + df['likes'] * 2.0 + df['favs'] * 3.0
            
            user_item_matrix = df.pivot_table(
                index='user_id', 
                columns='item_id', 
                values='rating', 
                fill_value=0
            )
            
            user_similarity = {}
            users = user_item_matrix.index.tolist()
            
            for i, user1 in enumerate(users):
                
                
                user_similarity[user1] = {}
                for user2 in users:
                    if user1 != user2:
                        vec1 = user_item_matrix.loc[user1].values
                        vec2 = user_item_matrix.loc[user2].values
                        
                        if np.linalg.norm(vec1) > 0 and np.linalg.norm(vec2) > 0:
                            similarity = np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2))
                            if similarity > 0.1:
                                user_similarity[user1][user2] = similarity
            
            item_similarity = {}
            items = user_item_matrix.columns.tolist()
            
            for i, item1 in enumerate(items):
                
                item_similarity[item1] = {}
                for item2 in items:
                    if item1 != item2:
                        vec1 = user_item_matrix[item1].values
                        vec2 = user_item_matrix[item2].values
                        
                        if np.linalg.norm(vec1) > 0 and np.linalg.norm(vec2) > 0:
                            similarity = np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2))
                            if similarity > 0.1:
                                item_similarity[item1][item2] = similarity
            
            with self.cache_lock:
                self.cache['user_similarity'] = user_similarity
                self.cache['item_similarity'] = item_similarity
                self.cache['last_update_times']['collaborative_filtering'] = time.time()
            
            
        except Exception as e:
            logger.error(f"Failed to update collaborative filtering similarity: {e}")
    
    def _update_location_cache(self):
        """Update location cache"""
        try:
            
            conn = self._threadsafe_conn()
            
            query = """
                SELECT item_id, geo_lat, geo_lon
                FROM item_profile
            """
            
            df = pd.read_sql_query(query, conn)
            
            if df.empty:
                logger.warning("No geographic data available")
                return
            
            if 'geo_lat' in df.columns and 'geo_lon' in df.columns:
                df = df[df['geo_lat'].notna() & df['geo_lon'].notna()]
            
            def bucket(lat, lon, step=0.1):
                lat_b = round(float(lat) / step, 1)
                lon_b = round(float(lon) / step, 1)
                return f"{lat_b}:{lon_b}"

            location_cache = {}
            
            for _, row in df.iterrows():
                item_id = row['item_id']
                lat = row['geo_lat']
                lon = row['geo_lon']

                bkey = bucket(lat, lon)
                if bkey not in location_cache:
                    location_cache[bkey] = []
                location_cache[bkey].append({
                    'item_id': item_id,
                    'geo_lat': lat,
                    'geo_lon': lon
                })
            
            with self.cache_lock:
                self.cache['location_cache'] = location_cache
                self.cache['last_update_times']['location'] = time.time()
            
            
        except Exception as e:
            logger.error(f"Failed to update location cache: {e}")
    
    def start_background_updates(self):
        """Start background update tasks"""
        try:
            
            self.executor.submit(self._background_popularity_update)
            self.executor.submit(self._background_cf_update)
            self.executor.submit(self._background_location_update)
            
            logger.info("Background incremental update tasks started")
            
        except Exception as e:
            logger.error(f"Failed to start background update: {e}")
    
    def _background_popularity_update(self):
        """Background popularity update"""
        while True:
            try:
                logger.info("Background incremental update tasks started")
                time.sleep(self.config.popularity_update_interval)
                
                if self.update_status['popularity'] == 'idle':
                    self.update_status['popularity'] = 'updating'
                    self._update_popularity_scores()
                    self.update_status['popularity'] = 'idle'
                    
                    self._save_cache_to_disk()
                    
            except Exception as e:
                logger.error(f"Failed to update popularity scores: {e}")
                self.update_status['popularity'] = 'error'
    
    def _background_cf_update(self):
        """Background collaborative filtering update"""
        while True:
            try:
                time.sleep(self.config.cf_update_interval)
                
                if self.update_status['collaborative_filtering'] == 'idle':
                    self.update_status['collaborative_filtering'] = 'updating'
                    self._update_collaborative_filtering()
                    self.update_status['collaborative_filtering'] = 'idle'
                    
                    self._save_cache_to_disk()
                    
            except Exception as e:
                logger.error(f"Failed to update collaborative filtering similarity: {e}")
                self.update_status['collaborative_filtering'] = 'error'
    
    def _background_location_update(self):
        """Background location update"""
        while True:
            try:
                time.sleep(self.config.location_update_interval)
                
                if self.update_status['location'] == 'idle':
                    self.update_status['location'] = 'updating'
                    self._update_location_cache()
                    self.update_status['location'] = 'idle'
                    
                    self._save_cache_to_disk()
                    
            except Exception as e:
                logger.error(f"Failed to update location cache: {e}")
                self.update_status['location'] = 'error'
    
    def get_cache_status(self) -> Dict[str, Any]:
        """Get cache status"""
        with self.cache_lock:
            return {
                'cache_sizes': {
                    'popularity_scores': len(self.cache.get('popularity_scores', {})),
                    'user_similarity': len(self.cache.get('user_similarity', {})),
                    'item_similarity': len(self.cache.get('item_similarity', {})),
                    'location_cache': len(self.cache.get('location_cache', {}))
                },
                'last_update_times': self.cache.get('last_update_times', {}),
                'update_status': self.update_status
            }
    
    def force_update(self, update_type: str):
        """Force update specified type"""
        try:
            logger.info(f"Force update: {update_type}")
            
            if update_type == 'popularity':
                self._update_popularity_scores()
            elif update_type == 'collaborative_filtering':
                self._update_collaborative_filtering()
            elif update_type == 'location':
                self._update_location_cache()
            else:
                logger.warning(f"Unknown update type: {update_type}")
                return
            
            self._save_cache_to_disk()
            
            logger.info(f"Force update completed: {update_type}")
            
        except Exception as e:
            logger.error(f"Failed to force update: {update_type}: {e}")
    
    def shutdown(self):
        """Shutdown service"""
        try:
            logger.info("Shutdown incremental update service...")
            
            self._save_cache_to_disk()
            
            self.executor.shutdown(wait=True)
            
            logger.info("Incremental update service closed")
            
        except Exception as e:
            logger.error(f"Failed to shutdown service: {e}")

incremental_service = None

def get_incremental_service() -> IncrementalUpdateService:
    """Get incremental update service instance"""
    global incremental_service
    if incremental_service is None:
        config = UpdateConfig()
        incremental_service = IncrementalUpdateService(config)
    return incremental_service

if __name__ == "__main__":
    config = UpdateConfig()
    service = IncrementalUpdateService(config)
    
    service.start_background_updates()
    
    try:
        while True:
            time.sleep(60)
            status = service.get_cache_status()
            logger.info(f"Cache status: {status}")
            
    except KeyboardInterrupt:
        logger.info("Received interrupt signal, shutting down service...")
        service.shutdown()
