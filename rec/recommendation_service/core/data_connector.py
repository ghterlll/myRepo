import json
import logging
import sqlite3
import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
import numpy as np
import pandas as pd
from loguru import logger


class DatabaseConnector:
    """Database connector - supports SQLite database"""
    def __init__(self, db_path: str = None):
        self.db_path = db_path
        self.connection = None
        
    def connect(self):
        """Establish database connection"""
        try:
            if self.db_path is None:
                error_msg = "Database path is not set"
                logger.error(error_msg)
                raise ValueError(error_msg)
            
            if not os.path.exists(self.db_path):
                error_msg = f"Database file not found: {self.db_path}"
                logger.error(error_msg)
                raise FileNotFoundError(error_msg)
            
            self.connection = sqlite3.connect(self.db_path)
            logger.info(f"Database connection established: {self.db_path}")
            return True
        except Exception as e:
            logger.error(f"Failed to connect to database: {e}")
            raise
        
    def get_connection(self):
        """Get database connection"""
        if self.connection is None:
            self.connect()
        return self.connection
        
    def execute_query(self, query: str, params: tuple = None) -> pd.DataFrame:
        """Execute SQL query"""
        try:
            if self.connection is None:
                self.connect()
            if params:
                return pd.read_sql_query(query, self.connection, params=params)
            else:
                return pd.read_sql_query(query, self.connection)
        except Exception as e:
            logger.error(f"Failed to execute query: {e}")
            return pd.DataFrame()
    
    def close(self):
        """Close database connection"""
        if self.connection:
            self.connection.close()
            self.connection = None
            logger.info("Database connection closed")


class EventLogService:
    """Event log service - based on event_log table"""
    def __init__(self, db_connector: DatabaseConnector):
        self.db = db_connector
        
    def get_user_events(self, user_id: str, days: int = 30) -> pd.DataFrame:
        """
        获取用户事件日志
        Args:
            user_id: 用户ID
            days: 查询天数
        Returns:
            用户事件DataFrame
        """
        query = """
        SELECT user_id, item_id, event_type, ts, session_id, dwell_time,
               device_type, network_type, geo_lat, geo_lon, city,
               refer_item_id, position
        FROM event_log 
        WHERE user_id = ? 
          AND ts >= ?
        ORDER BY ts DESC
        """
        
        start_date = datetime.now() - timedelta(days=days)
        params = (user_id, start_date)
        try:
            return self.db.execute_query(query, params)
        except Exception as e:
            logger.error(f"Failed to get user events: {e}")
            return self._get_mock_user_events(user_id, days)
    
    def get_item_events(self, item_id: str, days: int = 30) -> pd.DataFrame:
        """Get item event logs"""
        query = """
        SELECT user_id, item_id, event_type, ts, session_id, dwell_time,
               position
        FROM event_log 
        WHERE item_id = ? 
          AND ts >= ?
        ORDER BY ts DESC
        """
        start_date = datetime.now() - timedelta(days=days)
        params = (item_id, start_date)
        try:
            return self.db.execute_query(query, params)
        except Exception as e:
            logger.error(f"Failed to get item events: {e}")
            return self._get_mock_item_events(item_id, days)
    
    def get_user_behavior_sequence(self, user_id: str, days: int = 30) -> List[Dict]:
        """Get user behavior sequence"""
        events_df = self.get_user_events(user_id, days)
        if events_df.empty:
            return []
        behavior_sequence = []
        for _, row in events_df.iterrows():
            behavior_sequence.append({
                'action_type': row['event_type'],
                'item_id': row['item_id'],
                'timestamp': row['ts'],
                'session_id': row['session_id'],
                'dwell_time': row.get('dwell_time', 0),
                'position': row.get('position', 0)
            })
        
        return behavior_sequence
    
    def _get_mock_user_events(self, user_id: str, days: int) -> pd.DataFrame:
        """Simulate user event data"""
        np.random.seed(hash(user_id) % 2147483647)
        events = []
        for i in range(np.random.randint(50, 200)):
            events.append({
                'user_id': user_id,
                'item_id': f'i{np.random.randint(1, 1000)}',
                'event_type': np.random.choice(['expose', 'click', 'like', 'fav', 'share', 'comment']),
                'ts': datetime.now() - timedelta(minutes=np.random.randint(0, days * 24 * 60)),
                'session_id': f's{np.random.randint(1, 20)}',
                'dwell_time': np.random.exponential(5.0),
                'device_type': np.random.choice(['iOS', 'Android', 'Web']),
                'network_type': np.random.choice(['WiFi', '4G', '5G']),
                'geo_lat': np.random.uniform(39.0, 40.0),
                'geo_lon': np.random.uniform(116.0, 117.0),
                'city': np.random.choice(['Beijing', 'Shanghai', 'Shenzhen']),
                'position': np.random.randint(1, 20)
            })
        return pd.DataFrame(events)
    def _get_mock_item_events(self, item_id: str, days: int) -> pd.DataFrame:
        """Simulate item event data"""
        np.random.seed(hash(item_id) % 2147483647)
        events = []
        for i in range(np.random.randint(10, 100)):
            events.append({
                'user_id': f'u{np.random.randint(1, 1000)}',
                'item_id': item_id,
                'event_type': np.random.choice(['expose', 'click', 'like', 'fav', 'share']),
                'ts': datetime.now() - timedelta(minutes=np.random.randint(0, days * 24 * 60)),
                'session_id': f's{np.random.randint(1, 50)}',
                'dwell_time': np.random.exponential(5.0),
                'position': np.random.randint(1, 20)
            })
        return pd.DataFrame(events)

class UserProfileService:
    """User profile service - based on user_profile table"""
    
    def __init__(self, db_connector: DatabaseConnector):
        self.db = db_connector
        
    def get_user_profile(self, user_id: str) -> Dict:
        """Get user profile"""
        query = """
        SELECT user_id, age_bucket, gender, location, device_preference,
               interests, followed_users, recent_geos, activity_level, subscription_type
        FROM user_profile 
        WHERE user_id = ?
        """
        try:
            result = self.db.execute_query(query, (user_id,))
            if not result.empty:
                return result.iloc[0].to_dict()
        except Exception as e:
            logger.error(f"Failed to get user profile: {e}")
            
        return self._get_mock_user_profile(user_id)
    
    def _get_mock_user_profile(self, user_id: str) -> Dict:
        """Simulate user profile data"""
        np.random.seed(hash(user_id) % 2147483647) 
        return {
            'user_id': user_id,
            'age_bucket': np.random.choice(['18-24', '25-34', '35-44', '45-54', '55+']),
            'gender': np.random.choice(['male', 'female', 'unknown']),
            'location': np.random.choice(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen']),
            'device_preference': np.random.choice(['iOS', 'Android', 'Web']),
            'interests': np.random.choice([
                ['tech', 'travel'], ['food', 'lifestyle'], ['sports', 'music'],
                ['fashion', 'beauty'], ['education', 'career']
            ]),
            'followed_users': [f'u{np.random.randint(1, 1000)}' for _ in range(np.random.randint(5, 50))],
            'recent_geos': [f'geohash_{i}' for i in range(np.random.randint(2, 8))],
            'activity_level': np.random.choice(['low', 'medium', 'high']),
            'subscription_type': np.random.choice(['free', 'premium'])
        }

class ItemProfileService:
    """Content profile service - based on item_profile table"""
    
    def __init__(self, db_connector: DatabaseConnector):
        self.db = db_connector
        
    def get_item_profile(self, item_id: str) -> Dict:
        """Get content profile"""
        query = """
        SELECT item_id, title, tags, author_id, publish_ts, category,
               text_embed, image_embed, video_length, geo_lat, geo_lon, popularity_score
        FROM item_profile 
        WHERE item_id = ?
        """
        try:
            result = self.db.execute_query(query, (item_id,))
            if not result.empty:
                return result.iloc[0].to_dict()
        except Exception as e:
            logger.error(f"Failed to get item profile: {e}")
            
        return self._get_mock_item_profile(item_id)
    
    def _get_mock_item_profile(self, item_id: str) -> Dict:
        """Simulate content profile data"""
        np.random.seed(hash(item_id) % 2147483647)
        return {
            'item_id': item_id,
            'title': f'Content Title {item_id}',
            'tags': np.random.choice([
                ['food', 'restaurant'], ['travel', 'scenic'], ['tech', 'review'],
                ['fashion', 'style'], ['lifestyle', 'daily']
            ]),
            'author_id': f'u{np.random.randint(1, 1000)}',
            'publish_ts': datetime.now() - timedelta(days=np.random.randint(1, 30)),
            'category': np.random.choice(['lifestyle', 'tech', 'food', 'travel', 'fashion']),
            'text_embed': np.random.randn(768).tolist(),  
            'image_embed': np.random.randn(512).tolist(), 
            'video_length': np.random.randint(10, 300) if np.random.random() > 0.5 else 0,
            'geo_lat': np.random.uniform(39.0, 40.0) if np.random.random() > 0.3 else None,
            'geo_lon': np.random.uniform(116.0, 117.0) if np.random.random() > 0.3 else None,
            'popularity_score': np.random.uniform(0.0, 1.0)
        }

class ImpressionLogService:
    """Impression log service - based on impression_log table"""
    
    def __init__(self, db_connector: DatabaseConnector):
        self.db = db_connector
        
    def get_user_impressions(self, user_id: str, days: int = 7) -> pd.DataFrame:
        """Get user impression records"""
        query = """
        SELECT impression_id, user_id, item_id, ts, position, scene,
               clicked, liked, favorited, shared
        FROM impression_log 
        WHERE user_id = ? 
          AND ts >= ?
        ORDER BY ts DESC
        """
        
        start_date = datetime.now() - timedelta(days=days)
        params = (user_id, start_date)
        
        try:
            return self.db.execute_query(query, params)
        except Exception as e:
            logger.error(f"Failed to get user impressions: {e}")
            return self._get_mock_impressions(user_id, days)
    
    def _get_mock_impressions(self, user_id: str, days: int) -> pd.DataFrame:
        """Simulate impression data"""
        np.random.seed(hash(user_id) % 2147483647)
        
        impressions = []
        for i in range(np.random.randint(100, 500)):
            clicked = np.random.random() < 0.1  # 10% CTR
            liked = clicked and (np.random.random() < 0.3)  # 30% like rate if clicked
            
            impressions.append({
                'impression_id': f'imp_{i}',
                'user_id': user_id,
                'item_id': f'i{np.random.randint(1, 1000)}',
                'ts': datetime.now() - timedelta(minutes=np.random.randint(0, days * 24 * 60)),
                'position': np.random.randint(1, 20),
                'scene': np.random.choice(['homepage', 'detail_page', 'search']),
                'clicked': 1 if clicked else 0,
                'liked': 1 if liked else 0,
                'favorited': 1 if (liked and np.random.random() < 0.2) else 0,
                'shared': 1 if (liked and np.random.random() < 0.1) else 0
            })
        
        return pd.DataFrame(impressions)

class DataServiceManager:
    """Data service manager - unified data access interface"""
    def __init__(self, db_path: str = None):
        self.db_connector = DatabaseConnector(db_path)
        self.db_connector.connect()
        
        self.event_service = EventLogService(self.db_connector)
        self.user_service = UserProfileService(self.db_connector)
        self.item_service = ItemProfileService(self.db_connector)
        self.impression_service = ImpressionLogService(self.db_connector)
    
    def get_connection(self):
        """Get database connection"""
        return self.db_connector.get_connection()
    
    def get_user_data(self, user_id: str, days: int = 30) -> Dict:
        """Get all user-related data"""
        return {
            'profile': self.user_service.get_user_profile(user_id),
            'events': self.event_service.get_user_events(user_id, days),
            'behavior_sequence': self.event_service.get_user_behavior_sequence(user_id, days),
            'impressions': self.impression_service.get_user_impressions(user_id, min(days, 7))
        }
    
    def get_item_data(self, item_id: str, days: int = 30) -> Dict:
        """Get all item-related data"""
        return {
            'profile': self.item_service.get_item_profile(item_id),
            'events': self.event_service.get_item_events(item_id, days)
        }
    
    def get_user_sequence_features(self, user_id: str, seq_length: int = 20) -> Dict:
        try:
            conn = self.get_connection()
            
            query = """
            SELECT 
                e.item_id,
                e.event_type,
                e.timestamp,
                e.dwell_time,
                i.category,
                i.subcategory,
                i.tags
            FROM event_log e
            LEFT JOIN item_profile i ON e.item_id = i.item_id
            WHERE e.user_id = ?
            ORDER BY e.timestamp DESC
            LIMIT ?
            """
            
            sequence_data = pd.read_sql_query(query, conn, params=[user_id, seq_length])
            
            sequence_features = {
                'click_items': [],
                'like_items': [],
                'share_items': [],
                'favorite_items': [],
                'sequence_length': len(sequence_data),
                'categories': [],
                'avg_dwell_time': 0.0
            }
            
            if not sequence_data.empty:
                for _, row in sequence_data.iterrows():
                    event_type = row['event_type']
                    item_id = row['item_id']
                    category = row.get('category', '')
                    
                    if event_type == 'click':
                        sequence_features['click_items'].append(item_id)
                    elif event_type == 'like':
                        sequence_features['like_items'].append(item_id)
                    elif event_type == 'share':
                        sequence_features['share_items'].append(item_id)
                    elif event_type == 'favorite':
                        sequence_features['favorite_items'].append(item_id)
                    
                    if category:
                        sequence_features['categories'].append(category)
                
                sequence_features['avg_dwell_time'] = sequence_data['dwell_time'].mean()
            
            return sequence_features
            
        except Exception as e:
            logger.error(f"Failed to get user sequence features for {user_id}: {e}")
            return {
                'click_items': [],
                'like_items': [],
                'share_items': [],
                'favorite_items': [],
                'sequence_length': 0,
                'categories': [],
                'avg_dwell_time': 0.0
            }
    
    def close(self):
        """Close database connection"""
        self.db_connector.close()