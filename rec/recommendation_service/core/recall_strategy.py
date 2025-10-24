import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional
import numpy as np
import pandas as pd
from loguru import logger
import faiss
from sklearn.metrics.pairwise import cosine_similarity
from scipy.sparse import csr_matrix

class MultiRecallStrategy:
    """Multi-recall strategy"""
    def __init__(self, data_service=None, enable_cf=True):
        self.data_service = data_service
        
        self.strategies = {
            "collaborative_filtering": CollaborativeFilteringRecall(data_service) if enable_cf else None,
            "popularity": PopularityRecall(data_service),
            "location_based": LocationBasedRecall(data_service),
        }
        self.weights = {
            "collaborative_filtering": 0.5,    
            "popularity": 0.25,                
            "location_based": 0.25,            
        }
        
        self.recall_stats = {
            "coverage": {},           
            "click_rate": {},         
            "dedup_ratio": {},        
            "strategy_performance": {} 
        }
        self.total_items = 1000      
        self.performance_window = 1000  
        

    def recall(self, user_id: int, candidate_size: int = 1000, top_k: int = None) -> List[int]:
        """多路召回"""
        if top_k is not None:
            candidate_size = top_k
        self._validate_weights()
            
        all_candidates = {}
        strategy_results = {}
        
        strategy_candidate_sizes = {
            'collaborative_filtering': int(candidate_size * 0.5),  
            'popularity': int(candidate_size * 0.25),             
            'location_based': int(candidate_size * 0.25),         
        }
        
        traditional_strategies = ['collaborative_filtering', 'popularity', 'location_based']
        
        for strategy_name in traditional_strategies:
            strategy = self.strategies.get(strategy_name)
            if strategy is None:
                continue
                
            try:
                strategy_size = strategy_candidate_sizes.get(strategy_name, candidate_size // 4)
                candidates = strategy.recall(user_id, top_k=strategy_size)
                weight = self.weights.get(strategy_name, 0.0)
                
                strategy_results[strategy_name] = candidates
                logger.info(f"Traditional recall strategy {strategy_name}: recalled {len(candidates)} candidates")

                for note_id, score in candidates:
                    if note_id in all_candidates:
                        all_candidates[note_id] += score * weight
                    else:
                        all_candidates[note_id] = score * weight

            except Exception as e:
                logger.error(f"Traditional recall strategy {strategy_name} failed: {e}")
                strategy_results[strategy_name] = []
                continue
        

        sorted_candidates = sorted(
            all_candidates.items(), key=lambda x: x[1], reverse=True
        )
        final_candidates = [note_id for note_id, _ in sorted_candidates[:candidate_size]]
        
        self._update_recall_stats(strategy_results, final_candidates)
        
        try:
            self._log_recall_summary(strategy_results, final_candidates)
        except AttributeError:
            traditional_count = sum(len(candidates) for strategy_name, candidates in strategy_results.items())
            logger.info(f"Recall completed: traditional recall {traditional_count} candidates, cold start recall 0 candidates, final candidates {len(final_candidates)} candidates")
        
        return final_candidates

    def _validate_weights(self):
        """Validate weight configuration"""
        try:
            strategy_names = set(self.strategies.keys())
            weight_names = set(self.weights.keys())
            
            missing_weights = strategy_names - weight_names
            if missing_weights:
                logger.error(f"Missing weights for strategies: {missing_weights}")
            
            extra_weights = weight_names - strategy_names
            if extra_weights:
                logger.warning(f"Extra weights without strategies: {extra_weights}")
            
            total_weight = sum(self.weights.values())
            if abs(total_weight - 1.0) > 0.01:
                logger.warning(f"Total weights sum to {total_weight}, not 1.0")
                
        except Exception as e:
            logger.error(f"Failed to validate weights: {e}")
    
    def _update_recall_stats(self, strategy_results: Dict, final_candidates: List[int]):
        """Update recall statistics"""
        try:
            for strategy_name, candidates in strategy_results.items():
                if strategy_name not in self.recall_stats["coverage"]:
                    self.recall_stats["coverage"][strategy_name] = []
                
                coverage = len(candidates) / self.total_items
                self.recall_stats["coverage"][strategy_name].append(coverage)
                
                if len(self.recall_stats["coverage"][strategy_name]) > self.performance_window:
                    self.recall_stats["coverage"][strategy_name] = self.recall_stats["coverage"][strategy_name][-self.performance_window:]
            
            total_recalled = sum(len(candidates) for candidates in strategy_results.values())
            unique_recalled = len(set().union(*[set(candidates) for candidates in strategy_results.values()]))
            dedup_ratio = unique_recalled / total_recalled if total_recalled > 0 else 1.0
            
            if "overall" not in self.recall_stats["dedup_ratio"]:
                self.recall_stats["dedup_ratio"]["overall"] = []
            self.recall_stats["dedup_ratio"]["overall"].append(dedup_ratio)
            
            if len(self.recall_stats["dedup_ratio"]["overall"]) > self.performance_window:
                self.recall_stats["dedup_ratio"]["overall"] = self.recall_stats["dedup_ratio"]["overall"][-self.performance_window:]
                
        except Exception as e:
            logger.error(f"Failed to update recall stats: {e}")

    def get_coverage_stats(self) -> Dict:
        """Get coverage statistics"""
        try:
            coverage_stats = {}
            for strategy_name, coverage_history in self.recall_stats["coverage"].items():
                if coverage_history:
                    coverage_stats[strategy_name] = {
                        "current": coverage_history[-1],
                        "avg": sum(coverage_history) / len(coverage_history),
                        "min": min(coverage_history),
                        "max": max(coverage_history)
                    }
            return coverage_stats
        except Exception as e:
            logger.error(f"Failed to get coverage stats: {e}")
            return {}
        """Get recall strategy summary report"""
        try:
            return {
                "coverage": self.get_coverage_stats(),
                "click_rate": self.get_click_rate_stats(),
                "dedup_ratio": self.get_dedup_stats(),
                "current_weights": self.weights.copy(),
                "total_strategies": len(self.strategies)
            }
        except Exception as e:
            logger.error(f"Failed to get recall summary: {e}")
            return {}

class CollaborativeFilteringRecall:
    """Collaborative filtering recall - using real interaction data from event_log table"""

    def __init__(self, data_service=None):
        self.data_service = data_service
        self.user_item_matrix = None
        self.item_similarity_index = None
        self.user_similarity_index = None
        self.user_to_idx = {}
        self.item_to_idx = {}
        self.idx_to_item = {}
        
        self.interaction_weights = {
            'expose': 0.0,    
            'click': 3.0,     
            'like': 5.0,      
            'fav': 7.0,       
        }

    def build_user_item_matrix(self):
        """Build user-item interaction matrix based on event_log table"""
        try:
            if not self.data_service:
                logger.error("Data service not available for collaborative filtering")
                return
            
            conn = self.data_service.get_connection()
            event_data = pd.read_sql_query("SELECT * FROM event_log", conn)
            
            if event_data.empty:
                logger.warning("No event data found in event_log table")
                return
                
            logger.info(f"Loading {len(event_data)} interaction events from event_log")
            
            valid_events = event_data[event_data['event_type'].isin(self.interaction_weights.keys())]
            
            if valid_events.empty:
                logger.warning("No valid interaction events found")
                return
            
            user_ids = sorted(valid_events['user_id'].unique())
            item_ids = sorted(valid_events['item_id'].unique())
            
            self.user_to_idx = {uid: idx for idx, uid in enumerate(user_ids)}
            self.item_to_idx = {iid: idx for idx, iid in enumerate(item_ids)}
            self.idx_to_item = {v: k for k, v in self.item_to_idx.items()}
            
            logger.info(f"Found {len(user_ids)} users and {len(item_ids)} items")
            
            rows, cols, data = [], [], []
            
            user_item_scores = {}
            
            for _, event in valid_events.iterrows():
                user_id = event['user_id']
                item_id = event['item_id']
                event_type = event['event_type']
                
                weight = self.interaction_weights.get(event_type, 1.0)
                
                """
                dwell_time = event.get('dwell_time', 1.0)
                if pd.notna(dwell_time) and dwell_time > 0:
                    time_weight = min(np.log1p(dwell_time), 3.0)
                    weight *= time_weight"""
                
                key = (user_id, item_id)
                if key not in user_item_scores:
                    user_item_scores[key] = 0.0
                user_item_scores[key] += weight
            
            for (user_id, item_id), score in user_item_scores.items():
                if user_id in self.user_to_idx and item_id in self.item_to_idx:
                    rows.append(self.user_to_idx[user_id])
                    cols.append(self.item_to_idx[item_id])
                    data.append(score)
            
            self.user_item_matrix = csr_matrix(
                (data, (rows, cols)), 
                shape=(len(user_ids), len(item_ids))
            )
            
            logger.info(f"Built user-item matrix: {self.user_item_matrix.shape}, "
                       f"density: {self.user_item_matrix.nnz / (self.user_item_matrix.shape[0] * self.user_item_matrix.shape[1]):.6f}")
            
        except Exception as e:
            logger.error(f"Failed to build user-item matrix from event_log: {e}")
            self.user_item_matrix = csr_matrix((100, 1000))
            self.user_to_idx = {}
            self.item_to_idx = {}
            self.idx_to_item = {}
            
    def build_item_similarity_index(self, top_k: int = 100):

        try:
            if self.user_item_matrix is None or self.user_item_matrix.shape[1] == 0:
                return

            X = self.user_item_matrix.tocsc().astype(np.float32)  

            col_sq = X.power(2).sum(axis=0).A1  
            col_norm = np.sqrt(np.maximum(col_sq, 1e-12))        
            inv_col_norm = 1.0 / col_norm

            X_norm = X.multiply(inv_col_norm)

            S = (X_norm.T @ X_norm).tocsr()

            S.setdiag(0.0)
            S.eliminate_zeros()

            num_items = S.shape[0]
            neighbors = {}
            for i in range(num_items):
                row_start, row_end = S.indptr[i], S.indptr[i+1]
                cols = S.indices[row_start:row_end]
                vals = S.data[row_start:row_end]
                if len(vals) > top_k:
                    top_idx = np.argpartition(-vals, top_k)[:top_k]
                    cols, vals = cols[top_idx], vals[top_idx]
                    order = np.argsort(-vals)
                    cols, vals = cols[order], vals[order]
                else:
                    order = np.argsort(-vals)
                    cols, vals = cols[order], vals[order]
                neighbors[i] = list(zip(cols.tolist(), vals.tolist()))

            self.item_cf_neighbors = neighbors  
            self.item_similarity_index = None    
            logger.info(f"Built ItemCF neighbors for {num_items} items with top_k={top_k}")
        except Exception as e:
            logger.error(f"Failed to build ItemCF neighbors: {e}")
    
    def build_user_similarity_index(self):
        """Build user similarity index"""    
        try:
            if self.user_item_matrix is None or self.user_item_matrix.shape[0] == 0:
                return
            
            user_similarity = cosine_similarity(self.user_item_matrix)
            
            d = user_similarity.shape[1]
            self.user_similarity_index = faiss.IndexFlatIP(d)
            self.user_similarity_index.add(user_similarity.astype('float32'))
            
            logger.info(f"Built user similarity index with {d} dimensions")
            
        except Exception as e:
            logger.error(f"Failed to build user similarity index: {e}")

    def recall(self, user_id: str, top_k: int = 200) -> List[Tuple[str, float]]:
        """Collaborative filtering recall - using real interaction data from event_log table"""
        try:
            if self.user_item_matrix is None:
                self.build_user_item_matrix()
                self.build_item_similarity_index()
                self.build_user_similarity_index()
            
            if user_id not in self.user_to_idx:
                logger.warning(f"User {user_id} not found in collaborative filtering data")
                return []
                
            user_idx = self.user_to_idx[user_id]
            user_vector = self.user_item_matrix[user_idx].toarray()[0]
            
            interacted_items_idx = np.where(user_vector > 0)[0]
            
            if len(interacted_items_idx) == 0:
                logger.warning(f"User {user_id} has no interaction history")
                return []
            
            item_based_candidates = {}
            user_based_candidates = {}
            
            for item_idx in interacted_items_idx:
                neighbors = self.item_cf_neighbors.get(item_idx, [])
                if not neighbors:
                    continue
                
                user_rating = user_vector[item_idx]
                
                per_item_k = max(10, min(top_k // max(1, len(interacted_items_idx)) + 10, len(neighbors)))
                top_neighbors = neighbors[:per_item_k]  
                
                for nbr_idx, sim_score in top_neighbors:
                    if nbr_idx == item_idx or nbr_idx in interacted_items_idx:
                        continue
                    
                    if nbr_idx in self.idx_to_item:
                        item_id = self.idx_to_item[nbr_idx]
                        score = float(sim_score) * float(user_rating)
                        
                        if item_id in item_based_candidates:
                            item_based_candidates[item_id] += score  
                        else:
                            item_based_candidates[item_id] = score
            
            if self.user_similarity_index is not None:
                if not hasattr(self, 'user_similarity_matrix'):
                    from sklearn.metrics.pairwise import cosine_similarity
                    self.user_similarity_matrix = cosine_similarity(self.user_item_matrix)
                
                user_similarities = self.user_similarity_matrix[user_idx]
                similar_users_idx = np.argsort(-user_similarities)[1:min(31, self.user_item_matrix.shape[0])]
                
                for sim_user_idx in similar_users_idx:
                    sim_score = user_similarities[sim_user_idx]
                    
                    if sim_score <= 0.1:
                        break  
                    
                    sim_user_vector = self.user_item_matrix[sim_user_idx].toarray()[0]
                    sim_user_items_idx = np.where(sim_user_vector > 0)[0]
                    
                    for item_idx in sim_user_items_idx:
                        if item_idx in interacted_items_idx:
                            continue
                        
                        if item_idx in self.idx_to_item:
                            item_id = self.idx_to_item[item_idx]
                            score = float(sim_score) * float(sim_user_vector[item_idx])
                            
                            if item_id in user_based_candidates:
                                user_based_candidates[item_id] += score  
                            else:
                                user_based_candidates[item_id] = score
            
            final_candidates = {}
            itemcf_weight = 0.7
            usercf_weight = 0.3
            
            for item_id, score in item_based_candidates.items():
                final_candidates[item_id] = score * itemcf_weight
            
            for item_id, score in user_based_candidates.items():
                if item_id in final_candidates:
                    final_candidates[item_id] += score * usercf_weight
                else:
                    final_candidates[item_id] = score * usercf_weight
            
            sorted_candidates = sorted(final_candidates.items(), key=lambda x: x[1], reverse=True)
            result = sorted_candidates[:top_k]
            
            logger.info(f"Collaborative filtering recalled {len(result)} candidates for user {user_id} "
                       f"(ItemCF: {len(item_based_candidates)}, UserCF: {len(user_based_candidates)})")
            return result
            
        except Exception as e:
            logger.error(f"Collaborative filtering recall failed for user {user_id}: {e}")
            import traceback
            logger.error(f"Traceback: {traceback.format_exc()}")
            return []

class PopularityRecall:
    """Popularity-based recall - using real interaction data from event_log and impression_log tables"""

    def __init__(self, data_service=None):
        self.data_service = data_service
        self.popularity_cache = {}
        self.cache_ttl = 3600  
        
        self.popularity_weights = {
            'click': 1.0,      
            'like': 2.0,       
            'fav': 3.0,        
        }

    def recall(self, user_id: str, top_k: int = 200) -> List[Tuple[str, float]]:
        """Popularity-based recall - using real user behavior data"""
        try:
            current_hour = datetime.now().strftime('%Y%m%d_%H')
            cache_key = f"popularity_recall_{current_hour}"
            
            if cache_key in self.popularity_cache:
                cached_result = self.popularity_cache[cache_key][:top_k]
                logger.info(f"Using cached popularity results for user {user_id}")
                return cached_result
            
            if not self.data_service:
                logger.error("Data service not available for popularity recall")
                return []
            
            popularity_scores = self._calculate_popularity_scores()
            
            if not popularity_scores:
                logger.warning("No popularity data available")
                return []
            
            sorted_candidates = sorted(popularity_scores.items(), key=lambda x: x[1], reverse=True)
            self.popularity_cache[cache_key] = sorted_candidates
            
            self._cleanup_cache()
            
            result = sorted_candidates[:top_k]
            logger.info(f"Popularity recall returned {len(result)} candidates for user {user_id}")
            return result
            
        except Exception as e:
            logger.error(f"Popularity recall failed for user {user_id}: {e}")
            return []
    
    def _calculate_popularity_scores(self) -> Dict[str, float]:
        """Calculate item popularity scores"""
        try:
            conn = self.data_service.get_connection()
            event_data = pd.read_sql_query("SELECT * FROM event_log", conn)
            impression_data = pd.read_sql_query("SELECT * FROM impression_log", conn)
            item_profile = pd.read_sql_query("SELECT * FROM item_profile", conn)
            
            if event_data.empty:
                logger.warning("No event data available for popularity calculation")
                return {}
            
            if 'ts' in event_data.columns:
                event_data['ts'] = pd.to_datetime(event_data['ts'])
                latest_time = event_data['ts'].max()
                current_time = latest_time + timedelta(days=1)
                seven_days_ago = current_time - timedelta(days=7)
                
                recent_events = event_data[event_data['ts'] >= seven_days_ago]
                logger.info(f"Using dataset latest time {latest_time} as reference, current_time: {current_time}")
                logger.info(f"7-day window: {seven_days_ago} to {latest_time}, found {len(recent_events)} events")
            else:
                recent_events = event_data
            
            popularity_scores = {}
            
            for _, event in recent_events.iterrows():
                item_id = event['item_id']
                event_type = event['event_type']
                
                if event_type in self.popularity_weights:
                    weight = self.popularity_weights[event_type]
                    
                    if 'ts' in event and pd.notna(event['ts']):
                        hours_ago = (current_time - event['ts']).total_seconds() / 3600
                        time_decay = np.exp(-hours_ago / (24 * 7))  
                    else:
                        time_decay = 0.5  
                    
                    dwell_bonus = 1.0
                    if 'dwell_time' in event and pd.notna(event['dwell_time']):
                        dwell_bonus = min(1.0 + np.log1p(event['dwell_time']) / 10, 2.0)
                    
                    score = weight * time_decay * dwell_bonus

                    if item_id not in popularity_scores:
                        popularity_scores[item_id] = 0.0
                    popularity_scores[item_id] += score
                    
            if not item_profile.empty and 'publish_ts' in item_profile.columns:
                item_profile['publish_ts'] = pd.to_datetime(item_profile['publish_ts'])
                
                for _, item in item_profile.iterrows():
                    item_id = item['item_id']
                    
                    if item_id in popularity_scores:
                        if pd.notna(item['publish_ts']):
                            days_since_publish = (current_time - item['publish_ts']).days
                            if days_since_publish <= 1:  
                                freshness_bonus = 1.5
                            elif days_since_publish <= 7:  
                                freshness_bonus = 1.2
                            else:
                                freshness_bonus = 1.0
                            
                            popularity_scores[item_id] *= freshness_bonus
            
            logger.info(f"Calculated popularity scores for {len(popularity_scores)} items")
            
            if len(popularity_scores) > 1000:
                sorted_items = sorted(popularity_scores.items(), key=lambda x: x[1], reverse=True)
                popularity_scores = dict(sorted_items[:1000])  
                logger.info(f"Memory optimization: only keep top1000 popular item scores")
            return popularity_scores
            
        except Exception as e:
            logger.error(f"Failed to calculate popularity scores: {e}")
            return {}

    def _cleanup_cache(self):
        """Clean up expired cache"""
        try:
            current_time = datetime.now()
            expired_keys = []
            
            for key in self.popularity_cache.keys():
                if '_' in key:
                    time_str = key.split('_')[-1]
                    try:
                        cache_time = datetime.strptime(time_str, '%Y%m%d_%H')
                        if (current_time - cache_time).total_seconds() > self.cache_ttl:
                            expired_keys.append(key)
                    except ValueError:
                        expired_keys.append(key)  
            
            for key in expired_keys:
                del self.popularity_cache[key]
                
            if expired_keys:
                logger.info(f"Cleaned up {len(expired_keys)} expired cache entries")
            
        except Exception as e:
            logger.error(f"Failed to cleanup cache: {e}")

class LocationBasedRecall:
    """Location-based recall - using real geographic data from user_profile and item_profile tables"""

    def __init__(self, data_service=None):
        self.data_service = data_service
        self.location_cache = {}
        
        self.max_distance_km = 100  
        self.distance_decay_factor = 0.1  

    def recall(self, user_id: str, top_k: int = 200) -> List[Tuple[str, float]]:
        """Location-based recall - using real user and item geographic data"""
        try:
            if not self.data_service:
                logger.error("Data service not available for location-based recall")
                return []
            
            user_location = self._get_user_location(user_id)
            if not user_location:
                logger.warning(f"No location data found for user {user_id}")
                return []
            
            nearby_items = self._find_nearby_items(user_location, top_k * 2)
            
            if not nearby_items:
                logger.warning(f"No nearby items found for user {user_id}")
                return []
            
            scored_candidates = self._score_location_candidates(nearby_items, user_location)
            
            result = scored_candidates[:top_k]
            logger.info(f"Location-based recall returned {len(result)} candidates for user {user_id}")
            return result
            
        except Exception as e:
            logger.error(f"Location-based recall failed for user {user_id}: {e}")
            return []
    
    def _get_user_location(self, user_id: str) -> Optional[Dict]:
        """Get user location from user_profile table"""
        try:
            if user_id in self.location_cache:
                return self.location_cache[user_id]
            
            conn = self.data_service.get_connection()
            user_profile = pd.read_sql_query("SELECT * FROM user_profile WHERE user_id = ?", conn, params=[user_id])
            
            if user_profile.empty:
                return None
            
            user_data = user_profile.iloc[0]

            if 'recent_geos' in user_data and pd.notna(user_data['recent_geos']):
                try:
                    recent_geos = json.loads(user_data['recent_geos'])
                    if recent_geos:
                        geo_hash = recent_geos[0]
                        location = {
                            'lat': 39.9 + hash(geo_hash) % 100 / 1000,
                            'lon': 116.4 + hash(geo_hash) % 100 / 1000,
                            'source': 'geohash',
                            'geohash': geo_hash
                        }
                        self.location_cache[user_id] = location
                        return location
                except json.JSONDecodeError:
                    pass
            elif 'location' in user_data and pd.notna(user_data['location']):
                city_location = self._get_city_coordinates(user_data['location'])
                if city_location:
                    lat_offset = np.random.uniform(-0.1, 0.1)  
                    lon_offset = np.random.uniform(-0.1, 0.1)
                    
                    location = {
                        'lat': city_location['lat'] + lat_offset,
                        'lon': city_location['lon'] + lon_offset,
                        'source': 'city',
                        'city': user_data['location']
                    }
                    self.location_cache[user_id] = location
                    return location
            
            return None
            
        except Exception as e:
            logger.error(f"Failed to get user location for {user_id}: {e}")
            return None
    
    def _get_city_coordinates(self, city_name: str) -> Optional[Dict]:
        """Get city coordinates"""
        city_coordinates = {
            '北京': {'lat': 39.9042, 'lon': 116.4074},
            '上海': {'lat': 31.2304, 'lon': 121.4737},
            '广州': {'lat': 23.1291, 'lon': 113.2644},
            '深圳': {'lat': 22.5431, 'lon': 114.0579},
            '杭州': {'lat': 30.2741, 'lon': 120.1551},
            '成都': {'lat': 30.5728, 'lon': 104.0668},
            '重庆': {'lat': 29.5647, 'lon': 106.5507},
            '西安': {'lat': 34.3416, 'lon': 108.9398},
            '南京': {'lat': 32.0603, 'lon': 118.7969},
            '武汉': {'lat': 30.5928, 'lon': 114.3055}
        }
        
        return city_coordinates.get(city_name)

    def _find_nearby_items(self, user_location: Dict, limit: int) -> List[Dict]:
        """Find nearby items"""
        try:
            conn = self.data_service.get_connection()
            item_profile = pd.read_sql_query("SELECT * FROM item_profile", conn)
            
            if item_profile.empty:
                return []
            
            items_with_location = item_profile[
                (item_profile['geo_lat'].notna()) & 
                (item_profile['geo_lon'].notna())
            ]
            
            if items_with_location.empty:
                logger.warning("No items with location data found")
                return []
            
            nearby_items = []
            user_lat = user_location['lat']
            user_lon = user_location['lon']
            
            for _, item in items_with_location.iterrows():
                item_lat = float(item['geo_lat'])
                item_lon = float(item['geo_lon'])
                
                distance_km = self._calculate_distance(user_lat, user_lon, item_lat, item_lon)
                
                if distance_km <= self.max_distance_km:
                    nearby_items.append({
                        'item_id': item['item_id'],
                        'distance_km': distance_km,
                        'lat': item_lat,
                        'lon': item_lon,
                        'title': item.get('title', ''),
                        'category': item.get('category', ''),
                        'publish_ts': item.get('publish_ts', ''),
                        'popularity_score': item.get('popularity_score', 0.0)
                    })
            
            nearby_items.sort(key=lambda x: x['distance_km'])
            
            logger.info(f"Found {len(nearby_items)} items within {self.max_distance_km}km")
            return nearby_items[:limit]
            
        except Exception as e:
            logger.error(f"Failed to find nearby items: {e}")
            return []
    
    def _calculate_distance(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Calculate distance between two points in km"""
        from math import radians, cos, sin, asin, sqrt
        
        lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
        
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
        c = 2 * asin(sqrt(a))
        
        r = 6371
        
        return c * r

    def _score_location_candidates(self, nearby_items: List[Dict], user_location: Dict) -> List[Tuple[str, float]]:
        """Score location candidates"""
        try:
            scored_candidates = []
            
            for item in nearby_items:
                distance_km = item['distance_km']
                distance_score = max(0, 1 - distance_km / self.max_distance_km)
                
                distance_decay = np.exp(-distance_km * self.distance_decay_factor)
                
                popularity_score = item.get('popularity_score', 0.0)
                normalized_popularity = min(popularity_score, 1.0)  
                
                freshness_score = 1.0
                if item.get('publish_ts'):
                    try:
                        publish_time = pd.to_datetime(item['publish_ts'])
                        days_ago = (datetime.now() - publish_time).days
                        if days_ago <= 1:
                            freshness_score = 1.5  
                        elif days_ago <= 7:
                            freshness_score = 1.2  
                        else:
                            freshness_score = 1.0
                    except:
                        freshness_score = 1.0
                
                final_score = (
                    distance_score * 0.4 +           
                    distance_decay * 0.3 +           
                    normalized_popularity * 0.2 +    
                    (freshness_score - 1) * 0.1      
                )
                
                scored_candidates.append((item['item_id'], final_score))
            
            scored_candidates.sort(key=lambda x: x[1], reverse=True)
            return scored_candidates
            
        except Exception as e:
            logger.error(f"Failed to score location candidates: {e}")
            return []
    
    def _log_recall_summary(self, strategy_results: dict, final_candidates: list):
        
        traditional_count = 0
        for strategy_name in ['collaborative_filtering', 'popularity', 'location_based']:
            candidates = strategy_results.get(strategy_name, [])
            count = len(candidates)
            traditional_count += count
            logger.info(f"  {strategy_name}: {count} candidates")

        logger.info(f"  Final candidates: {len(final_candidates)} candidates")
        logger.info("=" * 50)
    
