import json
import logging
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Union
from collections import defaultdict, Counter
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import PCA
from loguru import logger
from .data_connector import DataServiceManager

class TextEmbeddingEngine:
    """Text embedding engine"""
    def __init__(self, embedding_dim: int = 64):
        self.embedding_dim = embedding_dim
        self.tfidf_vectorizer = TfidfVectorizer(
            max_features=1000,
            stop_words='english',
            ngram_range=(1, 2)
        )
        self.pca = PCA(n_components=embedding_dim)
        self.is_fitted = False
        
    def fit(self, texts: List[str]):
        """Train text embedding model"""
        try:
            tfidf_matrix = self.tfidf_vectorizer.fit_transform(texts)
            
            if tfidf_matrix.shape[1] > self.embedding_dim:
                self.pca.fit(tfidf_matrix.toarray())
            
            self.is_fitted = True
            logger.info(f"Text embedding model fitted with {len(texts)} texts")
            
        except Exception as e:
            logger.error(f"Failed to fit text embedding model: {e}")
    
    def transform(self, texts: List[str]) -> np.ndarray:
        """Convert text to embedding vectors"""
        if not self.is_fitted:
            logger.warning("Text embedding model not fitted, using random embeddings")
            return np.random.normal(0, 0.1, (len(texts), self.embedding_dim))
        
        try:
            tfidf_matrix = self.tfidf_vectorizer.transform(texts)
            
            if hasattr(self.pca, 'components_'):
                embeddings = self.pca.transform(tfidf_matrix.toarray())
            else:
                dense_matrix = tfidf_matrix.toarray()
                if dense_matrix.shape[1] >= self.embedding_dim:
                    embeddings = dense_matrix[:, :self.embedding_dim]
                else:
                    embeddings = np.zeros((len(texts), self.embedding_dim))
                    embeddings[:, :dense_matrix.shape[1]] = dense_matrix
            
            return embeddings
            
        except Exception as e:
            logger.error(f"Failed to transform texts to embeddings: {e}")
            return np.random.normal(0, 0.1, (len(texts), self.embedding_dim))

class SequenceFeatureEngine:
    """Sequence feature engineering engine"""
    
    def __init__(self, max_sequence_length: int = 50, embedding_dim: int = 64):
        self.max_sequence_length = max_sequence_length
        self.embedding_dim = embedding_dim
        self.item_embeddings = {}  
        self.category_embeddings = {}  
        
    def build_item_embeddings(self, item_profiles: pd.DataFrame):
        """Build item embedding dictionary"""
        try:
            for _, item in item_profiles.iterrows():
                item_id = item['item_id']
                
                embedding = np.array([
                    float(item.get('quality_score', 0.5)),
                    np.log1p(float(item.get('view_count', 1))),
                    np.log1p(float(item.get('like_count', 1))),
                    float(item.get('content_length', 100)) / 1000.0,  
                ])
                
                if len(embedding) < self.embedding_dim:
                    padding = np.random.normal(0, 0.1, self.embedding_dim - len(embedding))
                    embedding = np.concatenate([embedding, padding])
                else:
                    embedding = embedding[:self.embedding_dim]
                
                self.item_embeddings[item_id] = embedding
                
            logger.info(f"Built embeddings for {len(self.item_embeddings)} items")
            
        except Exception as e:
            logger.error(f"Failed to build item embeddings: {e}")
    
    def build_category_embeddings(self, item_profiles: pd.DataFrame):
        """Build category embedding dictionary"""
        try:
            categories = item_profiles['category'].unique()
            
            for i, category in enumerate(categories):
                embedding = np.random.normal(0, 0.1, self.embedding_dim)
                embedding[i % self.embedding_dim] = 1.0  
                
                self.category_embeddings[category] = embedding
                
            logger.info(f"Built embeddings for {len(self.category_embeddings)} categories")
            
        except Exception as e:
            logger.error(f"Failed to build category embeddings: {e}")
    
    def extract_lastn_features(self, user_events: pd.DataFrame, n: int = 20) -> Dict:
        """Extract user LastN interaction sequence features"""
        features = {}
        
        try:
            if user_events.empty:
                return self._get_default_sequence_features()
            
            recent_events = user_events.sort_values('ts').tail(n)
            
            event_types = ['click', 'like', 'share', 'favorite']
            
            for event_type in event_types:
                type_events = recent_events[recent_events['event_type'] == event_type]
                
                if not type_events.empty:
                    item_embeddings = []
                    for item_id in type_events['item_id'].values:
                        if item_id in self.item_embeddings:
                            item_embeddings.append(self.item_embeddings[item_id])
                    
                    if item_embeddings:
                        item_embeddings = np.array(item_embeddings)
                        features[f'{event_type}_items_mean'] = np.mean(item_embeddings, axis=0)
                        features[f'{event_type}_items_max'] = np.max(item_embeddings, axis=0)
                        features[f'{event_type}_items_std'] = np.std(item_embeddings, axis=0)
                    else:
                        features[f'{event_type}_items_mean'] = np.zeros(self.embedding_dim)
                        features[f'{event_type}_items_max'] = np.zeros(self.embedding_dim)
                        features[f'{event_type}_items_std'] = np.zeros(self.embedding_dim)
                else:
                    features[f'{event_type}_items_mean'] = np.zeros(self.embedding_dim)
                    features[f'{event_type}_items_max'] = np.zeros(self.embedding_dim)
                    features[f'{event_type}_items_std'] = np.zeros(self.embedding_dim)
            
            features['sequence_length'] = len(recent_events)
            features['unique_items_count'] = recent_events['item_id'].nunique()
            features['avg_dwell_time'] = recent_events['dwell_time'].mean() if 'dwell_time' in recent_events.columns else 0
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract LastN features: {e}")
            return self._get_default_sequence_features()
    
    def _get_default_sequence_features(self) -> Dict:
        """Get default sequence features"""
        features = {}
        event_types = ['click', 'like', 'share', 'favorite']
        
        for event_type in event_types:
            features[f'{event_type}_items_mean'] = np.zeros(self.embedding_dim)
            features[f'{event_type}_items_max'] = np.zeros(self.embedding_dim)
            features[f'{event_type}_items_std'] = np.zeros(self.embedding_dim)
        
        features['sequence_length'] = 0
        features['unique_items_count'] = 0
        features['avg_dwell_time'] = 0
        
        return features

class SocialFeatureEngine:
    """Social relationship feature engineering engine"""
    
    def __init__(self):
        self.user_similarity_cache = {}
        self.social_graph = defaultdict(set)  
        
    def build_social_graph(self, user_profiles: pd.DataFrame):
        """Build social relationship graph"""
        try:
            for _, user in user_profiles.iterrows():
                user_id = user['user_id']
                followed_users = user.get('followed_users', [])
                
                if followed_users:
                    if isinstance(followed_users, str):
                        try:
                            followed_users = json.loads(followed_users)
                        except:
                            followed_users = []
                    
                    self.social_graph[user_id] = set(followed_users)
            
            logger.info(f"Built social graph for {len(self.social_graph)} users")
            
        except Exception as e:
            logger.error(f"Failed to build social graph: {e}")
    
    def extract_social_features(self, user_id: str, user_profiles: pd.DataFrame) -> Dict:
        """Extract social relationship features"""
        features = {}
        
        try:
            user_profile = user_profiles[user_profiles['user_id'] == user_id].iloc[0] if not user_profiles[user_profiles['user_id'] == user_id].empty else None
            
            if user_profile is None:
                return self._get_default_social_features()
            
            followed_users = self.social_graph.get(user_id, set())
            features['following_count'] = len(followed_users)
            
            similar_users = self._find_similar_users(user_id, user_profiles)
            features['similar_users_count'] = len(similar_users)
            
            features['social_centrality'] = self._calculate_centrality(user_id)
            
            user_interests = user_profile.get('interests', [])
            if isinstance(user_interests, str):
                try:
                    user_interests = json.loads(user_interests)
                except:
                    user_interests = []
            
            interest_overlap_scores = []
            for followed_user_id in followed_users:
                followed_profile = user_profiles[user_profiles['user_id'] == followed_user_id]
                if not followed_profile.empty:
                    followed_interests = followed_profile.iloc[0].get('interests', [])
                    if isinstance(followed_interests, str):
                        try:
                            followed_interests = json.loads(followed_interests)
                        except:
                            followed_interests = []
                    
                    if user_interests and followed_interests:
                        intersection = len(set(user_interests) & set(followed_interests))
                        union = len(set(user_interests) | set(followed_interests))
                        similarity = intersection / union if union > 0 else 0
                        interest_overlap_scores.append(similarity)
            
            features['avg_interest_similarity'] = np.mean(interest_overlap_scores) if interest_overlap_scores else 0
            features['max_interest_similarity'] = np.max(interest_overlap_scores) if interest_overlap_scores else 0
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract social features for user {user_id}: {e}")
            return self._get_default_social_features()
    
    def _find_similar_users(self, user_id: str, user_profiles: pd.DataFrame, top_k: int = 10) -> List[str]:
        """Find similar users based on user profile"""
        try:
            target_user = user_profiles[user_profiles['user_id'] == user_id]
            if target_user.empty:
                return []
            
            target_profile = target_user.iloc[0]
            similar_users = []
            
            similar_criteria = {
                'age_bucket': target_profile.get('age_bucket'),
                'gender': target_profile.get('gender'),
                'city': target_profile.get('city'),
                'activity_level': target_profile.get('activity_level')
            }
            
            for _, user in user_profiles.iterrows():
                if user['user_id'] == user_id:
                    continue
                
                similarity_score = 0
                for key, value in similar_criteria.items():
                    if user.get(key) == value:
                        similarity_score += 1
                
                if similarity_score >= 2:  
                    similar_users.append(user['user_id'])
                
                if len(similar_users) >= top_k:
                    break
            
            return similar_users
            
        except Exception as e:
            logger.error(f"Failed to find similar users: {e}")
            return []
    
    def _calculate_centrality(self, user_id: str) -> float:
        """Calculate user centrality in social network"""
        try:
            following_count = len(self.social_graph.get(user_id, set()))
            
            follower_count = sum(1 for followers in self.social_graph.values() if user_id in followers)
            
            centrality = np.log1p(follower_count) + np.log1p(following_count)
            
            return centrality
            
        except Exception as e:
            logger.error(f"Failed to calculate centrality: {e}")
            return 0.0
    
    def _get_default_social_features(self) -> Dict:
        """Get default social features"""
        return {
            'following_count': 0,
            'similar_users_count': 0,
            'social_centrality': 0.0,
            'avg_interest_similarity': 0.0,
            'max_interest_similarity': 0.0
        }

class EnhancedFeatureEngine:
    """Enhanced feature engineering engine - integrates all features"""
    
    def __init__(self, data_service: DataServiceManager = None):
        self.data_service = data_service or DataServiceManager()
        
        self.text_engine = TextEmbeddingEngine(embedding_dim=64)
        self.sequence_engine = SequenceFeatureEngine(max_sequence_length=50, embedding_dim=64)
        self.social_engine = SocialFeatureEngine()
        
        self.feature_cache = {}
        self.is_initialized = False
    
    def initialize(self):
        """Initialize all feature engines"""
        try:
            logger.info("Initializing enhanced feature engines...")
            
            try:
                conn = self.data_service.get_connection()
            except Exception as e:
                logger.error(f"Failed to get database connection: {e}")
                raise RuntimeError(f"Database connection failed, please check if the database path is correct: {e}")
            
            logger.info("Loading user profiles...")
            user_profiles = pd.read_sql_query("SELECT * FROM user_profile", conn)
            if user_profiles.empty:
                logger.warning("user_profile table is empty, social features will use default values")
            else:
                logger.info(f"Loaded {len(user_profiles)} user profiles")
            
            logger.info("Loading item profiles...")
            item_profiles = pd.read_sql_query("SELECT * FROM item_profile", conn)
            if item_profiles.empty:
                logger.warning("item_profile table is empty, item features will use default values")
            else:
                logger.info(f"Loaded {len(item_profiles)} item profiles")
            
            logger.info("Initializing text embedding engine...")
            if not item_profiles.empty:
                texts = []
                for _, item in item_profiles.iterrows():
                    tags = item.get('tags', '')
                    category = item.get('category', '')
                    subcategory = item.get('subcategory', '')
                    text = f"{tags} {category} {subcategory}".strip()
                    if text:
                        texts.append(text)
                if texts:
                    self.text_engine.fit(texts)
                    logger.info(f"Text embedding engine initialized, processed {len(texts)} texts")
                else:
                    logger.warning("No text data found, skipping text embedding training")
            
            logger.info("Initializing sequence feature engine...")
            self.sequence_engine.build_item_embeddings(item_profiles)
            self.sequence_engine.build_category_embeddings(item_profiles)
            logger.info("Sequence feature engine initialized")
            
            logger.info("Initializing social feature engine...")
            self.social_engine.build_social_graph(user_profiles)
            logger.info("Social feature engine initialized")
            
            self.is_initialized = True
            logger.info("Enhanced feature engines initialized successfully")
            
        except Exception as e:
            logger.error(f"Error type: {type(e).__name__}")
            import traceback
            logger.error(f"Error stack:\n{traceback.format_exc()}")
            self.is_initialized = False
            raise
    
    def extract_enhanced_user_features(self, user_id: str, days: int = 30) -> Dict:
        """Extract enhanced user features"""
        if not self.is_initialized:
            self.initialize()
        
        try:
            user_data = self.data_service.get_user_data(user_id, days)
            
            features = {}
            
            basic_features = self._extract_basic_features(user_data)
            features.update(basic_features)
            
            sequence_features = self.sequence_engine.extract_lastn_features(user_data['events'])
            features.update(sequence_features)
            
            conn = self.data_service.get_connection()
            user_profiles = pd.read_sql_query("SELECT * FROM user_profile", conn)
            social_features = self.social_engine.extract_social_features(user_id, user_profiles)
            features.update(social_features)
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract enhanced user features for {user_id}: {e}")
            return {}
    
    def extract_enhanced_item_features(self, item_id: str) -> Dict:
        """Extract item features"""
        if not self.is_initialized:
            self.initialize()
        
        try:
            conn = self.data_service.get_connection()
            
            item_query = "SELECT * FROM item_profile WHERE item_id = ?"
            item_data = pd.read_sql_query(item_query, conn, params=[item_id])
            
            if item_data.empty:
                return {}
            
            item = item_data.iloc[0]
            features = {}
            
            features['quality_score'] = float(item.get('quality_score', 0.5))
            features['view_count_log'] = np.log1p(float(item.get('view_count', 1)))
            features['like_count_log'] = np.log1p(float(item.get('like_count', 1)))
            features['content_length_norm'] = float(item.get('content_length', 100)) / 1000.0
            
            tags = item.get('tags', '')
            category = item.get('category', '')
            subcategory = item.get('subcategory', '')
            
            text = f"{tags} {category} {subcategory}".strip()
            if text:
                text_embedding = self.text_engine.transform([text])[0]
                for i, val in enumerate(text_embedding):
                    features[f'text_emb_{i}'] = val
            else:
                for i in range(64):
                    features[f'text_emb_{i}'] = 0.0
            
            if item_id in self.sequence_engine.item_embeddings:
                item_embedding = self.sequence_engine.item_embeddings[item_id]
                for i, val in enumerate(item_embedding):
                    features[f'item_emb_{i}'] = val
            else:
                for i in range(64):
                    features[f'item_emb_{i}'] = 0.0
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract enhanced item features for {item_id}: {e}")
            return {}
    
    def extract_enhanced_context_features(self, context: Dict) -> Dict:
        """Extract context features"""
        features = {}
        
        try:
            timestamp = context.get('timestamp', datetime.now().timestamp())
            dt = datetime.fromtimestamp(timestamp)
            
            features['hour'] = dt.hour
            features['day_of_week'] = dt.weekday()
            features['is_weekend'] = 1 if dt.weekday() >= 5 else 0
            features['is_peak_time'] = 1 if dt.hour in [8, 12, 18, 20] else 0
            
            device_type = context.get('device_type', 'unknown')
            device_mapping = {'iOS': 0, 'Android': 1, 'Web': 2, 'unknown': -1}
            features['device_type_encoded'] = device_mapping.get(device_type, -1)
            
            network_type = context.get('network_type', 'unknown')
            network_mapping = {'WiFi': 0, '4G': 1, '5G': 2, '3G': 3, 'unknown': -1}
            features['network_type_encoded'] = network_mapping.get(network_type, -1)
            
            location = context.get('location', '')
            if location:
                features['has_location'] = 1
                features['location_hash'] = hash(location) % 1000  
            else:
                features['has_location'] = 0
                features['location_hash'] = 0
            
            scene = context.get('scene', 'homepage')
            scene_mapping = {
                'homepage': 0, 'search': 1, 'category': 2, 
                'profile': 3, 'detail': 4, 'unknown': -1
            }
            features['scene_encoded'] = scene_mapping.get(scene, -1)
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract enhanced context features: {e}")
            return {}
    
    def _extract_basic_features(self, user_data: Dict) -> Dict:
        """Extract basic user features"""
        features = {}
        
        try:
            profile = user_data.get('profile', {})
            
            age_mapping = {'18-24': 0, '25-34': 1, '35-44': 2, '45-54': 3, '55+': 4}
            features['age_bucket_encoded'] = age_mapping.get(profile.get('age_bucket'), 1)
            
            gender_mapping = {'male': 1, 'female': 0, 'unknown': -1}
            features['gender_encoded'] = gender_mapping.get(profile.get('gender'), -1)
            
            events = user_data.get('events', pd.DataFrame())
            if not events.empty:
                features['total_events'] = len(events)
                features['unique_items'] = events['item_id'].nunique()
                features['avg_dwell_time'] = events['dwell_time'].mean() if 'dwell_time' in events.columns else 0
            else:
                features['total_events'] = 0
                features['unique_items'] = 0
                features['avg_dwell_time'] = 0
            
            return features
            
        except Exception as e:
            logger.error(f"Failed to extract basic features: {e}")
            return {}
        
class DailyActivityLevelCalculator:
    """Daily activity level calculator - 7 day time window"""
    def __init__(self, data_service, config=None):
        self.data_service = data_service
        self.config = config or {}
        self.window_days = 7
        self.thresholds = {
            'high': 50,     
            'medium': 15,   
            'low': 0        
        }
    
    def calculate_activity_level(self, user_id: str) -> str:
        """Calculate user 7 day activity level"""
        try:
            end_time = datetime.now()
            start_time = end_time - timedelta(days=self.window_days)
            
            conn = self.data_service.get_connection()
            query = """
            SELECT 
                COUNT(*) as event_count,
                COUNT(DISTINCT item_id) as unique_items,
                AVG(dwell_time) as avg_dwell_time,
                COUNT(DISTINCT DATE(ts)) as active_days
            FROM event_log 
            WHERE user_id = ? AND ts >= ? AND ts <= ?
            """
            
            result = pd.read_sql_query(query, conn, params=[user_id, start_time, end_time])
            if result.empty or result.iloc[0]['event_count'] == 0:
                return 'low'
            activity_score = self._calculate_activity_score(result.iloc[0])
            return self._score_to_level(activity_score)
            
        except Exception as e:
            logger.error(f"Failed to calculate activity level for {user_id}: {e}")
            return 'medium'  
    
    def _calculate_activity_score(self, stats: pd.Series) -> float:
        """Calculate 7 day activity score"""
        event_count = stats['event_count']
        unique_items = stats['unique_items']
        avg_dwell_time = stats['avg_dwell_time'] or 0
        active_days = stats['active_days']
        score = (
            event_count * 0.5 +        
            unique_items * 0.3 +       
            avg_dwell_time * 0.1 +     
            active_days * 0.1          
        )
        return score
    def _score_to_level(self, score: float) -> str:
        """Convert score to level"""
        if score >= self.thresholds['high']:
            return 'high'
        elif score >= self.thresholds['medium']:
            return 'medium'
        else:
            return 'low'
