"""
End-to-end recommendation service API server
Provides complete recommendation service including recall, ranking and incremental updates
"""

import os
import sys
import asyncio
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime
import uvicorn
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import pandas as pd

sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from recommendation_service.core.data_connector import DataServiceManager
from recommendation_service.core.recall_strategy import MultiRecallStrategy
from recommendation_service.core.home_din_ranking import HoMEDINRankingService
from recommendation_service.core.enhanced_feature_engine import EnhancedFeatureEngine
from incremental_update_service import get_incremental_service

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s | %(levelname)s | %(name)s:%(funcName)s:%(lineno)d - %(message)s'
)
logger = logging.getLogger(__name__)

os.environ['OMP_NUM_THREADS'] = '1'

app = FastAPI(
    title="Recommendation System API",
    description="End-to-end recommendation service with recall, ranking and incremental updates",
    version="1.0.0"
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class RecommendationRequest(BaseModel):
    """Recommendation request model"""
    user_id: str = Field(..., description="User ID")
    num_recommendations: int = Field(default=10, ge=1, le=100, description="Number of recommendations")
    recall_strategies: Optional[List[str]] = Field(
        default=["collaborative_filtering", "popularity", "location_based"],
        description="Recall strategies list"
    )
    enable_ranking: bool = Field(default=True, description="Enable ranking")
    context: Optional[Dict[str, Any]] = Field(default=None, description="Context information")

class RecommendationResponse(BaseModel):
    """Recommendation response model"""
    user_id: str
    recommendations: List[Dict[str, Any]]
    metadata: Dict[str, Any]
    timestamp: datetime

class HealthResponse(BaseModel):
    """Health check response model"""
    status: str
    timestamp: datetime
    services: Dict[str, str]
recommendation_engine = None
data_connector = None
incremental_service = None

class RecommendationEngine:
    """Recommendation engine service class"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.data_connector = None
        self.recall_strategy = None
        self.ranking_service = None
        self.feature_engine = None
        self.incremental_service = None
        self.is_initialized = False
        
    async def initialize(self):
        try:
            
            self.incremental_service = get_incremental_service()
            self.incremental_service.start_background_updates()
            
            self.data_connector = DataServiceManager(
                db_path=self.config['db_path']
            )
            
            self.feature_engine = EnhancedFeatureEngine(
                data_service=self.data_connector
            )

            
            self.recall_strategy = MultiRecallStrategy(
                data_service=self.data_connector,
                enable_cf=self.config.get('enable_cf', True)
            )
            
            if self.config.get('enable_ranking', True):
                ranking_model_path = self.config.get('ranking_model_path')
                if ranking_model_path and not os.path.isabs(ranking_model_path):
                    ranking_model_path = os.path.abspath(ranking_model_path)
                if ranking_model_path and os.path.exists(ranking_model_path):
                    import torch
                    checkpoint = torch.load(ranking_model_path, map_location='cpu')
                    if 'model_config' in checkpoint:
                        model_config = checkpoint['model_config']
                    else:
                        model_config = {
                            'user_feature_dim': 780,
                            'item_feature_dim': 132,
                            'context_feature_dim': 9,
                            'sequence_feature_dim': 64,
                            'expert_dim': 128,
                            'num_experts': 4,
                            'task_groups': [[0], [1,2]],
                            'num_tasks': 3,
                            'hidden_dims': [256, 128, 64],
                            'dropout_rate': 0.3,
                        }
                else:
                    model_config = {
                        'user_feature_dim': 780,
                        'item_feature_dim': 132,
                        'context_feature_dim': 9,
                        'sequence_feature_dim': 64,
                        'expert_dim': 128,
                        'num_experts': 4,
                        'task_groups': [[0], [1,2]],
                        'num_tasks': 3,
                        'hidden_dims': [256, 128, 64],
                        'dropout_rate': 0.3,
                    }
                self.ranking_service = HoMEDINRankingService(
                    model_config=model_config,
                    feature_processor=self.feature_engine
                )
                
                if ranking_model_path and os.path.exists(ranking_model_path):
                    self.ranking_service.load_model(ranking_model_path)
            else:
                self.ranking_service = None
            
            self.is_initialized = True
            
        except Exception as e:
            raise
    
    async def recommend(self, request: RecommendationRequest) -> RecommendationResponse:
        """Execute recommendation"""
        if not self.is_initialized:
            raise HTTPException(status_code=503, detail="Recommendation engine not initialized")
        
        try:
            start_time = datetime.now()
            
            recall_candidates = await self._recall_candidates(
                user_id=request.user_id,
                strategies=request.recall_strategies
            )
            
            if request.enable_ranking and self.ranking_service and len(recall_candidates) > 0:
                ranked_candidates = await self._rank_candidates(
                    user_id=request.user_id,
                    candidates=recall_candidates,
                    num_recommendations=request.num_recommendations,
                    context=request.context
                )
            else:
                ranked_candidates = recall_candidates[:request.num_recommendations]
        
            recommendations = []
            for i, item_id in enumerate(ranked_candidates):
                recommendations.append({
                    "item_id": item_id,
                    "rank": i + 1,
                    "score": 1.0 - (i * 0.1) if i < 10 else 0.1
                })
            
            metadata = {
                "recall_candidates_count": len(recall_candidates),
                "final_recommendations_count": len(recommendations),
                "processing_time_ms": (datetime.now() - start_time).total_seconds() * 1000,
                "strategies_used": request.recall_strategies,
                "ranking_enabled": request.enable_ranking and self.ranking_service is not None
            }
            
            return RecommendationResponse(
                user_id=request.user_id,
                recommendations=recommendations,
                metadata=metadata,
                timestamp=datetime.now()
            )
            
        except Exception as e:
            logger.error(f"Recommendation execution failed: {e}")
            raise HTTPException(status_code=500, detail=f"Recommendation execution failed: {str(e)}")
    
    async def _recall_candidates(self, user_id: str, strategies: List[str]) -> List[str]:
        """Execute recall"""
        try:
            candidates = self.recall_strategy.recall(
                user_id=user_id,
                top_k=200
            )
            return candidates
        except Exception as e:
            logger.error(f"Recall failed: {e}")
            return []
    
    async def _rank_candidates(self, user_id: str, candidates: List[str], num_recommendations: int, context: Dict[str, Any] = None) -> List[str]:
        """Execute ranking"""
        try:
            if not self.ranking_service:
                return candidates[:num_recommendations]
            
            if context is None:
                context = {
                    'timestamp': datetime.now().timestamp(),
                    'device_type': 'mobile',
                    'scene': 'homepage',
                    'user_id': user_id,
                    'candidate_count': len(candidates)
                }
            
            ranked_results = self.ranking_service.rank_candidates(
                user_id=user_id,
                candidate_items=candidates,
                context=context,
                top_k=num_recommendations
            )
            
            ranked_items = [item_id for item_id, score, task_scores in ranked_results]
            return ranked_items
        except Exception as e:
            logger.error(f"Ranking failed: {e}")
            import traceback
            logger.error(f"Ranking failed details: {traceback.format_exc()}")
            return candidates[:num_recommendations]

CONFIG = {
    'db_path': 'small_data/recsys_data.db',
    'enable_cf': True,
    'enable_ranking': True,
    'ranking_model_path': 'recommendation_service/training_output/models/best_home_din_model.pth'
}

@app.on_event("startup")
async def startup_event():
    """Application startup event"""
    global recommendation_engine, incremental_service
    try:
        recommendation_engine = RecommendationEngine(CONFIG)
        await recommendation_engine.initialize()
        incremental_service = recommendation_engine.incremental_service
    except Exception as e:
        logger.error(f"Service startup failed: {e}")
        raise

@app.on_event("shutdown")
async def shutdown_event():
    """Application shutdown event"""
    global recommendation_engine, incremental_service
    if recommendation_engine and recommendation_engine.data_connector:
        recommendation_engine.data_connector.close()
    if incremental_service:
        incremental_service.shutdown()

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check"""
    services_status = {
        "recommendation_engine": "healthy" if recommendation_engine and recommendation_engine.is_initialized else "unhealthy",
        "data_connector": "healthy" if recommendation_engine and recommendation_engine.data_connector else "unhealthy",
        "recall_strategy": "healthy" if recommendation_engine and recommendation_engine.recall_strategy else "unhealthy",
        "ranking_service": "healthy" if recommendation_engine and recommendation_engine.ranking_service else "unhealthy"
    }
    
    return HealthResponse(
        status="healthy" if all(status == "healthy" for status in services_status.values()) else "unhealthy",
        timestamp=datetime.now(),
        services=services_status
    )

@app.post("/recommend", response_model=RecommendationResponse)
async def recommend(request: RecommendationRequest):
    """Recommendation endpoint"""
    if not recommendation_engine:
        raise HTTPException(status_code=503, detail="Recommendation engine not initialized")
    
    return await recommendation_engine.recommend(request)

@app.get("/cache/status")
async def get_cache_status():
    """Get cache status"""
    if not incremental_service:
        raise HTTPException(status_code=503, detail="Incremental update service not initialized")
    
    status = incremental_service.get_cache_status()
    return {
        "status": "success",
        "data": status,
        "timestamp": datetime.now().isoformat()
    }

@app.post("/cache/update/{update_type}")
async def force_cache_update(update_type: str):
    """Force update specified cache type"""
    if not incremental_service:
        raise HTTPException(status_code=503, detail="Incremental update service not initialized")
    
    valid_types = ['popularity', 'collaborative_filtering', 'location']
    if update_type not in valid_types:
        raise HTTPException(status_code=400, detail=f"Invalid update type: {update_type}")
    
    try:
        incremental_service.force_update(update_type)
        return {
            "status": "success",
            "message": f"Cache update completed: {update_type}",
            "timestamp": datetime.now().isoformat()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Cache update failed: {str(e)}")

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Recommendation System API Service",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
        "cache_status": "/cache/status",
        "force_update": "/cache/update/{update_type}"
    }

if __name__ == "__main__":
    uvicorn.run(
        "recommendation_api_server:app",
        host="0.0.0.0",
        port=8000,
        reload=False,
        log_level="info"
    )
