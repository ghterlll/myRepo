from math import inf
import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
from typing import Dict, List, Tuple, Optional
from loguru import logger

class AttentionLayer(nn.Module):

    def __init__(self, embedding_dim: int, hidden_dim: int = 64, use_nohist_embed: bool = False):
        super(AttentionLayer, self).__init__()
        self.embedding_dim = embedding_dim
        self.hidden_dim = hidden_dim

        self.attention_net = nn.Sequential(
            nn.Linear(embedding_dim * 4, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, 1)
        )

        self.dropout = nn.Dropout(0.2)

        self.use_nohist_embed = use_nohist_embed
        if use_nohist_embed:
            self.no_hist_embedding = nn.Parameter(torch.zeros(embedding_dim))
            nn.init.normal_(self.no_hist_embedding, std=1e-3)

    def forward(self, query: torch.Tensor, keys: torch.Tensor, mask: torch.Tensor = None) -> torch.Tensor:

        B, L, E = keys.shape

        q = query.unsqueeze(1).expand(B, L, E)

        attn_in = torch.cat([q, keys, q * keys, q - keys], dim=-1)  
        scores = self.attention_net(attn_in).squeeze(-1)  

        if mask is not None:
            mask = mask.to(dtype=scores.dtype)
            scores = scores.masked_fill(mask == 0, float('-inf'))

            attn = F.softmax(scores, dim=1) * mask
            denom = attn.sum(dim=1, keepdim=True)  
            attn = attn / (denom + 1e-12)
        else:
            attn = F.softmax(scores, dim=1)

        user_interest = torch.bmm(attn.unsqueeze(1), keys).squeeze(1)  

        if mask is not None:
            all_pad = (mask.sum(dim=1) == 0)
            if all_pad.any():
                if self.use_nohist_embed:
                    user_interest[all_pad] = self.no_hist_embedding
                else:
                    user_interest[all_pad] = 0.0

        return user_interest

class ExpertNetwork(nn.Module):
    
    def __init__(self, input_dim: int, expert_dim: int, dropout_rate: float = 0.3):
        super(ExpertNetwork, self).__init__()
        
        self.network = nn.Sequential(
            nn.Linear(input_dim, expert_dim),
            nn.BatchNorm1d(expert_dim),  
            nn.SiLU(),  
            nn.Dropout(dropout_rate),
            
            nn.Linear(expert_dim, expert_dim),
            nn.BatchNorm1d(expert_dim),  
            nn.SiLU(),  
            nn.Dropout(dropout_rate)
        )
        
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.network(x)

class HierarchicalGate(nn.Module):
    
    def __init__(
        self, 
        input_dim: int, 
        num_experts: int, 
        task_groups: List[List[int]],  
        hidden_dim: int = 64
    ):
        super(HierarchicalGate, self).__init__()
        self.num_experts = num_experts
        self.task_groups = task_groups
        self.num_groups = len(task_groups)
        
        self.global_gate = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, num_experts),
            nn.Softmax(dim=-1)
        )
        
        self.group_gates = nn.ModuleList([
            nn.Sequential(
                nn.Linear(input_dim, hidden_dim // 2),
                nn.ReLU(),
                nn.Linear(hidden_dim // 2, num_experts),
                nn.Softmax(dim=-1)
            ) for _ in range(self.num_groups)
        ])
        
        self.task_gates = nn.ModuleList([
            nn.Sequential(
                nn.Linear(input_dim, hidden_dim // 4),
                nn.ReLU(),
                nn.Linear(hidden_dim // 4, num_experts),
                nn.Softmax(dim=-1)
            ) for group in task_groups for _ in group
        ])
        
    def forward(self, x: torch.Tensor, task_id: int) -> torch.Tensor:

        global_weights = self.global_gate(x)
        
        group_id = None
        task_idx_in_group = None
        current_task_idx = 0
        
        for gid, group in enumerate(self.task_groups):
            if task_id in group:
                group_id = gid
                task_idx_in_group = group.index(task_id)
                break
            current_task_idx += len(group)
        
        if group_id is None:
            return global_weights
        
        group_weights = self.group_gates[group_id](x)
        
        actual_task_idx = sum(len(self.task_groups[i]) for i in range(group_id)) + task_idx_in_group
        task_weights = self.task_gates[actual_task_idx](x)
        
        final_weights = 0.4 * global_weights + 0.3 * group_weights + 0.3 * task_weights
        
        final_weights = F.softmax(final_weights, dim=-1)
        
        return final_weights

class FeatureGate(nn.Module):
    
    def __init__(self, input_dim: int, num_tasks: int, gate_dim: int = 64):
        super(FeatureGate, self).__init__()
        self.num_tasks = num_tasks
        
        self.feature_gates = nn.ModuleList([
            nn.Sequential(
                nn.Linear(input_dim, gate_dim),
                nn.ReLU(),
                nn.Linear(gate_dim, input_dim),
                nn.Sigmoid()  
            ) for _ in range(num_tasks)
        ])
        
    def forward(self, x: torch.Tensor, task_id: int) -> torch.Tensor:

        gate_weights = self.feature_gates[task_id](x)
        return x * gate_weights

class HoMEDINRankingModel(nn.Module):
    
    def __init__(
        self,
        user_feature_dim: int,
        item_feature_dim: int,
        context_feature_dim: int,
        sequence_feature_dim: int = 64,  
        expert_dim: int = 128,
        num_experts: int = 4,            
        num_tasks: int = 3,              
        task_groups: List[List[int]] = None,  
        hidden_dims: List[int] = [256, 128, 64],
        dropout_rate: float = 0.3
    ):
        super(HoMEDINRankingModel, self).__init__()
        
        self.user_feature_dim = user_feature_dim
        self.item_feature_dim = item_feature_dim
        self.context_feature_dim = context_feature_dim
        self.sequence_feature_dim = sequence_feature_dim
        self.expert_dim = expert_dim
        self.num_experts = num_experts
        self.num_tasks = num_tasks
        
        if task_groups is None:
            task_groups = [[0], [1,2]]  
        self.task_groups = task_groups
        
        base_feature_dim = user_feature_dim + item_feature_dim + context_feature_dim
        
        self.din_attention = AttentionLayer(sequence_feature_dim, hidden_dim=64)
        
        total_feature_dim = base_feature_dim + sequence_feature_dim
        self.feature_fusion = nn.Sequential(
            nn.Linear(total_feature_dim, hidden_dims[0]),
            nn.BatchNorm1d(hidden_dims[0]),
            nn.ReLU(),
            nn.Dropout(dropout_rate)
        )
        
        self.feature_gate = FeatureGate(hidden_dims[0], num_tasks, gate_dim=64)
        
        self.experts = nn.ModuleList([
            ExpertNetwork(hidden_dims[0], expert_dim, dropout_rate)
            for _ in range(num_experts)
        ])
        
        self.hierarchical_gate = HierarchicalGate(
            hidden_dims[0], num_experts, task_groups, hidden_dim=64
        )
        
        self.task_towers = nn.ModuleList([
            nn.Sequential(
                nn.Linear(expert_dim, hidden_dims[1]),
                nn.BatchNorm1d(hidden_dims[1]),
                nn.SiLU(),  
                nn.Dropout(dropout_rate),
                
                nn.Linear(hidden_dims[1], hidden_dims[2]),
                nn.BatchNorm1d(hidden_dims[2]),
                nn.SiLU(),  
                nn.Dropout(dropout_rate),
                
                nn.Linear(hidden_dims[2], 1)
            ) for _ in range(num_tasks)
        ])
        
        self.task_names = ['click', 'like', 'favorite']
        
        self.apply(self._init_weights)
        
    def _init_weights(self, module):
        
        if isinstance(module, nn.Linear):
            nn.init.xavier_uniform_(module.weight)
            if module.bias is not None:
                nn.init.zeros_(module.bias)
    
    def forward(
        self,
        user_features: torch.Tensor,      
        item_features: torch.Tensor,      
        context_features: torch.Tensor,   
        sequence_features: torch.Tensor,  
        candidate_embedding: torch.Tensor, 
        sequence_mask: torch.Tensor = None 
    ) -> Dict[str, torch.Tensor]:
        """Forward pass"""
        batch_size = user_features.shape[0]
        
        user_interest = self.din_attention(
            query=candidate_embedding,
            keys=sequence_features,
            mask=sequence_mask
        )  
        
        combined_features = torch.cat([
            user_features,      
            item_features,      
            context_features,   
            user_interest       
        ], dim=1)
        
        fused_features = self.feature_fusion(combined_features)
        
        expert_outputs = []
        for expert in self.experts:
            expert_output = expert(fused_features)
            expert_outputs.append(expert_output)
        
        expert_outputs = torch.stack(expert_outputs, dim=1)  
        
        task_outputs = {}
        
        for task_id in range(self.num_tasks):
            task_name = self.task_names[task_id]
            
            gated_features = self.feature_gate(fused_features, task_id)
            
            expert_weights = self.hierarchical_gate(gated_features, task_id)  
            
            expert_weights_expanded = expert_weights.unsqueeze(-1)  
            task_expert_output = torch.sum(
                expert_outputs * expert_weights_expanded, dim=1
            )  
            
            task_prediction = self.task_towers[task_id](task_expert_output)
            task_outputs[task_name] = task_prediction
        
        return task_outputs
    
    def get_model_info(self) -> Dict:

        total_params = sum(p.numel() for p in self.parameters())
        trainable_params = sum(p.numel() for p in self.parameters() if p.requires_grad)
        
        return {
            'model_name': 'Ranking Model',
            'user_feature_dim': self.user_feature_dim,
            'item_feature_dim': self.item_feature_dim,
            'context_feature_dim': self.context_feature_dim,
            'sequence_feature_dim': self.sequence_feature_dim,
            'expert_dim': self.expert_dim,
            'num_experts': self.num_experts,
            'num_tasks': self.num_tasks,
            'task_groups': self.task_groups,
            'total_parameters': total_params,
            'trainable_parameters': trainable_params,
            'key_features': [
                'DIN Attention Mechanism',
                'Expert Normalization with Swish Activation',
                'Hierarchical Gate with Task Grouping',
                'Feature Gate for Task-specific Representations',
                'Multi-task Learning with Shared Experts'
            ]
        }

class HoMEDINRankingService:
    
    def __init__(self, model_config: Dict = None, feature_processor=None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = None
        self.feature_processor = feature_processor
        self.model_config = model_config 
    
    def build_model(self) -> HoMEDINRankingModel:

        model = HoMEDINRankingModel(**self.model_config)
        model = model.to(self.device)
        
        self.model = model
        
        return model
    
    def predict(
        self,
        user_features: np.ndarray,
        item_features: np.ndarray,
        context_features: np.ndarray,
        sequence_features: np.ndarray,
        candidate_embeddings: np.ndarray,
        sequence_masks: np.ndarray = None
    ) -> Dict[str, np.ndarray]:
    
        if self.model is None:
            raise ValueError("Model not built. Call build_model() first.")
        
        self.model.eval()
        
        with torch.no_grad():
            user_tensor = torch.FloatTensor(user_features).to(self.device)
            item_tensor = torch.FloatTensor(item_features).to(self.device)
            context_tensor = torch.FloatTensor(context_features).to(self.device)
            sequence_tensor = torch.FloatTensor(sequence_features).to(self.device)
            candidate_tensor = torch.FloatTensor(candidate_embeddings).to(self.device)
            
            mask_tensor = None
            if sequence_masks is not None:
                mask_tensor = torch.FloatTensor(sequence_masks).to(self.device)
            
            outputs = self.model(
                user_features=user_tensor,
                item_features=item_tensor,
                context_features=context_tensor,
                sequence_features=sequence_tensor,
                candidate_embedding=candidate_tensor,
                sequence_mask=mask_tensor
            )
            
            results = {}
            for task_name, output in outputs.items():
                results[task_name] = torch.sigmoid(output).cpu().numpy()
            
            return results
    
    def _extract_user_sequence(self, user_id: str, batch_size: int, seq_len: int = 20) -> Tuple[np.ndarray, np.ndarray]:

        try:
            import pandas as pd
            
            conn = self.feature_processor.data_service.get_connection()
            user_events = pd.read_sql_query(f"""
                SELECT item_id, event_type, ts
                FROM event_log 
                WHERE user_id = '{user_id}' 
                ORDER BY ts DESC 
                LIMIT {seq_len}
            """, conn)
            
            feature_dim = 64
            sequence = np.zeros((seq_len, feature_dim), dtype=np.float32)
            mask = np.zeros(seq_len, dtype=np.float32)
            
            if len(user_events) > 0:
                for i, (_, row) in enumerate(user_events.iterrows()):
                    if i >= seq_len:
                        break
                    
                    item_id = row['item_id']
                    
                    item_features = self.feature_processor.extract_enhanced_item_features(item_id)
                    
                    item_feature_values = []
                    for key in sorted(item_features.keys()):
                        value = item_features[key]
                        if isinstance(value, (int, float)):
                            item_feature_values.append(float(value))
                        elif isinstance(value, bool):
                            item_feature_values.append(1.0 if value else 0.0)
                        elif isinstance(value, np.ndarray):
                            item_feature_values.extend(value.flatten().tolist())
                        elif isinstance(value, (list, tuple)):
                            item_feature_values.extend([float(x) for x in value])
                        else:
                            item_feature_values.append(0.0)
                    
                    item_feature_array = np.array(item_feature_values, dtype=np.float32)
                    
                    if len(item_feature_array) >= feature_dim:
                        sequence[i] = item_feature_array[-feature_dim:]
                    else:
                        sequence[i, :len(item_feature_array)] = item_feature_array
                    
                    mask[i] = 1.0
            
            sequence_features = np.tile(sequence, (batch_size, 1, 1))
            sequence_masks = np.tile(mask, (batch_size, 1))
            
            return sequence_features, sequence_masks
            
        except Exception as e:
            logger.warning(f"Failed to extract user sequence for {user_id}: {e}")
            sequence_features = np.zeros((batch_size, seq_len, 64), dtype=np.float32)
            sequence_masks = np.zeros((batch_size, seq_len), dtype=np.float32)
            return sequence_features, sequence_masks
    
    def rank_candidates(
        self,
        user_id: str,
        candidate_items: List[str],
        context: Dict,
        top_k: int = 100
    ) -> List[Tuple[str, float, Dict]]:
    
        if not candidate_items:
            return []
        
        if self.feature_processor is None:
            logger.error("Feature processor not provided")
            return []
        
        try:
            batch_size = len(candidate_items)
            
            user_features = self.feature_processor.extract_enhanced_user_features(user_id)
            user_feature_values = []
            for key in sorted(user_features.keys()):
                value = user_features[key]
                if isinstance(value, (int, float)):
                    user_feature_values.append(float(value))
                elif isinstance(value, bool):
                    user_feature_values.append(1.0 if value else 0.0)
                elif isinstance(value, np.ndarray):
                    user_feature_values.extend(value.flatten().tolist())
                elif isinstance(value, (list, tuple)):
                    user_feature_values.extend([float(x) for x in value])
                else:
                    user_feature_values.append(0.0)
            user_feature_array = np.array([user_feature_values] * batch_size)
            
            item_feature_arrays = []
            candidate_embeddings = []
            
            for item_id in candidate_items:
                item_features = self.feature_processor.extract_enhanced_item_features(item_id)
                item_feature_values = []
                for key in sorted(item_features.keys()):
                    value = item_features[key]
                    if isinstance(value, (int, float)):
                        item_feature_values.append(float(value))
                    elif isinstance(value, bool):
                        item_feature_values.append(1.0 if value else 0.0)
                    elif isinstance(value, np.ndarray):
                        item_feature_values.extend(value.flatten().tolist())
                    elif isinstance(value, (list, tuple)):
                        item_feature_values.extend([float(x) for x in value])
                    else:
                        item_feature_values.append(0.0)
                item_feature_array = np.array(item_feature_values)
                item_feature_arrays.append(item_feature_array)
                
                candidate_emb = item_feature_array[-64:]  
                candidate_embeddings.append(candidate_emb)
            
            item_feature_arrays = np.array(item_feature_arrays)
            candidate_embeddings = np.array(candidate_embeddings)
            
            context_features = self.feature_processor.extract_enhanced_context_features(context)
            context_feature_values = []
            for key in sorted(context_features.keys()):
                value = context_features[key]
                if isinstance(value, (int, float)):
                    context_feature_values.append(float(value))
                elif isinstance(value, bool):
                    context_feature_values.append(1.0 if value else 0.0)
                elif isinstance(value, np.ndarray):
                    context_feature_values.extend(value.flatten().tolist())
                elif isinstance(value, (list, tuple)):
                    context_feature_values.extend([float(x) for x in value])
                else:
                    context_feature_values.append(0.0)
            context_feature_array = np.array([context_feature_values] * batch_size)
            
            seq_len = 20
            sequence_features, sequence_masks = self._extract_user_sequence(user_id, batch_size, seq_len)
            
            predictions = self.predict(
                user_features=user_feature_array,
                item_features=item_feature_arrays,
                context_features=context_feature_array,
                sequence_features=sequence_features,
                candidate_embeddings=candidate_embeddings,
                sequence_masks=sequence_masks
            )
            
            results = []
            for i, item_id in enumerate(candidate_items):
                task_scores = {
                    task_name: float(scores[i][0])
                    for task_name, scores in predictions.items()
                }
                
                final_score = (
                    0.6 * task_scores['click'] +
                    0.3 * task_scores['like'] +
                    0.1 * task_scores['favorite']
                )
                
                results.append((item_id, final_score, task_scores))
            
            results.sort(key=lambda x: x[1], reverse=True)
            
            return results[:top_k]
            
        except Exception as e:
            logger.error(f"Failed to rank candidates: {e}")
            return []
    
    def save_model(self, save_path: str):
        
        if self.model is None:
            raise ValueError("No model to save")
        
        torch.save({
            'model_state_dict': self.model.state_dict(),
            'model_config': self.model_config,
            'model_info': self.model.get_model_info()
        }, save_path)
        
        logger.info(f"Model saved to {save_path}")
    
    def load_model(self, load_path: str):
        
        checkpoint = torch.load(load_path, map_location=self.device)
        
        current_feature_processor = self.feature_processor
        
        if 'model_config' in checkpoint:
            self.model_config = checkpoint['model_config']
            logger.info("Loaded model_config from checkpoint")
        elif self.model_config is None:
            logger.warning("No model_config in checkpoint, using default config")
            raise ValueError("model_config must be provided when loading old models")
        else:
            logger.info("Using existing model_config")
        
        self.model = HoMEDINRankingModel(**self.model_config)
        
        if 'model_state_dict' in checkpoint:
            self.model.load_state_dict(checkpoint['model_state_dict'])
        else:
            self.model.load_state_dict(checkpoint)
        
        self.model = self.model.to(self.device)
        self.model.eval()  
        
        self.feature_processor = current_feature_processor
        
        logger.info(f"Model loaded from {load_path}")
        if 'model_info' in checkpoint:
            logger.info(f"Model info: {checkpoint['model_info']}")
