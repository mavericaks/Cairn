import torch
import torch.nn as nn
import torch.nn.functional as F
import math
from typing import Optional, Tuple

class LlamaRMSNorm(nn.Module):
    def __init__(self, hidden_size, eps=1e-5):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(hidden_size))
        self.eps = eps

    def forward(self, hidden_states):
        variance = hidden_states.to(torch.float32).pow(2).mean(-1, keepdim=True)
        hidden_states = hidden_states * torch.rsqrt(variance + self.eps)
        return self.weight * hidden_states.to(self.weight.dtype)

class LlamaRotaryEmbedding(nn.Module):
    def __init__(self, dim, max_position_embeddings=8192, base=500000.0, device=None):
        super().__init__()
        self.dim = dim
        self.max_position_embeddings = max_position_embeddings
        self.base = base
        
        inv_freq = 1.0 / (self.base ** (torch.arange(0, self.dim, 2).float().to(device) / self.dim))
        self.register_buffer("inv_freq", inv_freq, persistent=False)

    def forward(self, x, seq_len=None):
        if seq_len is None:
            seq_len = x.shape[1]
        t = torch.arange(seq_len, device=x.device, dtype=self.inv_freq.dtype)
        freqs = torch.einsum("i,j->ij", t, self.inv_freq)
        emb = torch.cat((freqs, freqs), dim=-1)
        return torch.cos(emb), torch.sin(emb)

def apply_rotary_pos_emb(q, k, cos, sin):
    # q, k: (bsz, seq_len, num_heads, head_dim)
    # cos, sin: (seq_len, dim)
    cos = cos.unsqueeze(0).unsqueeze(2) # (1, seq_len, 1, dim)
    sin = sin.unsqueeze(0).unsqueeze(2) # (1, seq_len, 1, dim)
    
    # Split q and k into two halves
    q1, q2 = q.chunk(2, dim=-1)
    k1, k2 = k.chunk(2, dim=-1)
    
    # Rotate half
    q_rot = torch.cat((-q2, q1), dim=-1)
    k_rot = torch.cat((-k2, k1), dim=-1)
    
    q_out = (q * cos) + (q_rot * sin)
    k_out = (k * cos) + (k_rot * sin)
    
    return q_out, k_out

class LlamaMLP(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.gate_proj = nn.Linear(config.hidden_size, config.intermediate_size, bias=False)
        self.up_proj = nn.Linear(config.hidden_size, config.intermediate_size, bias=False)
        self.down_proj = nn.Linear(config.intermediate_size, config.hidden_size, bias=False)
        self.act_fn = nn.SiLU()

    def forward(self, x):
        return self.down_proj(self.act_fn(self.gate_proj(x)) * self.up_proj(x))

class LlamaAttention(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.hidden_size = config.hidden_size
        self.num_heads = config.num_attention_heads
        self.head_dim = self.hidden_size // self.num_heads
        self.num_key_value_heads = config.num_key_value_heads
        self.num_key_value_groups = self.num_heads // self.num_key_value_heads
        
        self.q_proj = nn.Linear(self.hidden_size, self.num_heads * self.head_dim, bias=False)
        self.k_proj = nn.Linear(self.hidden_size, self.num_key_value_heads * self.head_dim, bias=False)
        self.v_proj = nn.Linear(self.hidden_size, self.num_key_value_heads * self.head_dim, bias=False)
        self.o_proj = nn.Linear(self.num_heads * self.head_dim, self.hidden_size, bias=False)
        
        self.rotary_emb = LlamaRotaryEmbedding(self.head_dim, max_position_embeddings=config.max_position_embeddings, base=config.rope_theta)

    def forward(self, hidden_states, attention_mask=None, position_ids=None):
        bsz, q_len, _ = hidden_states.size()

        query_states = self.q_proj(hidden_states)
        key_states = self.k_proj(hidden_states)
        value_states = self.v_proj(hidden_states)

        query_states = query_states.view(bsz, q_len, self.num_heads, self.head_dim)
        key_states = key_states.view(bsz, q_len, self.num_key_value_heads, self.head_dim)
        value_states = value_states.view(bsz, q_len, self.num_key_value_heads, self.head_dim)

        cos, sin = self.rotary_emb(value_states, seq_len=q_len)
        query_states, key_states = apply_rotary_pos_emb(query_states, key_states, cos, sin)

        # GQA: repeat K and V heads
        key_states = torch.repeat_interleave(key_states, self.num_key_value_groups, dim=2)
        value_states = torch.repeat_interleave(value_states, self.num_key_value_groups, dim=2)
        
        query_states = query_states.transpose(1, 2) # (bsz, num_heads, q_len, head_dim)
        key_states = key_states.transpose(1, 2)
        value_states = value_states.transpose(1, 2)

        attn_weights = torch.matmul(query_states, key_states.transpose(2, 3)) / math.sqrt(self.head_dim)
        
        if attention_mask is not None:
            attn_weights = attn_weights + attention_mask

        attn_weights = nn.functional.softmax(attn_weights, dim=-1, dtype=torch.float32).to(query_states.dtype)
        attn_output = torch.matmul(attn_weights, value_states)

        attn_output = attn_output.transpose(1, 2).contiguous()
        attn_output = attn_output.view(bsz, q_len, self.hidden_size)

        attn_output = self.o_proj(attn_output)
        return attn_output

class LlamaDecoderLayer(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.self_attn = LlamaAttention(config)
        self.mlp = LlamaMLP(config)
        self.input_layernorm = LlamaRMSNorm(config.hidden_size, eps=config.rms_norm_eps)
        self.post_attention_layernorm = LlamaRMSNorm(config.hidden_size, eps=config.rms_norm_eps)

    def forward(self, hidden_states, attention_mask=None, position_ids=None):
        residual = hidden_states
        hidden_states = self.input_layernorm(hidden_states)
        hidden_states = self.self_attn(hidden_states, attention_mask=attention_mask, position_ids=position_ids)
        hidden_states = residual + hidden_states

        residual = hidden_states
        hidden_states = self.post_attention_layernorm(hidden_states)
        hidden_states = self.mlp(hidden_states)
        hidden_states = residual + hidden_states

        return hidden_states

class LlamaConfig:
    def __init__(self):
        # Llama-3.2-1B defaults
        self.vocab_size = 128256
        self.hidden_size = 2048
        self.intermediate_size = 8192
        self.num_hidden_layers = 16
        self.num_attention_heads = 32
        self.num_key_value_heads = 8
        self.max_position_embeddings = 8192
        self.rms_norm_eps = 1e-5
        self.rope_theta = 500000.0

class LlamaModel(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.config = config
        self.embed_tokens = nn.Embedding(config.vocab_size, config.hidden_size)
        self.layers = nn.ModuleList([LlamaDecoderLayer(config) for _ in range(config.num_hidden_layers)])
        self.norm = LlamaRMSNorm(config.hidden_size, eps=config.rms_norm_eps)

    def forward(self, input_ids, attention_mask=None, position_ids=None):
        hidden_states = self.embed_tokens(input_ids)
        
        # Causal mask for decoder
        if attention_mask is None:
            bsz, seq_len = input_ids.shape
            mask = torch.full((seq_len, seq_len), float("-inf"), device=input_ids.device)
            mask = torch.triu(mask, diagonal=1)
            attention_mask = mask.unsqueeze(0).unsqueeze(0) # (1, 1, seq_len, seq_len)
            
        for layer in self.layers:
            hidden_states = layer(hidden_states, attention_mask=attention_mask, position_ids=position_ids)
            
        hidden_states = self.norm(hidden_states)
        return hidden_states

class LlamaForCausalLM(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.model = LlamaModel(config)
        self.lm_head = nn.Linear(config.hidden_size, config.vocab_size, bias=False)

    def forward(self, input_ids, attention_mask=None, position_ids=None, labels=None):
        hidden_states = self.model(input_ids, attention_mask, position_ids)
        logits = self.lm_head(hidden_states)
        
        loss = None
        if labels is not None:
            shift_logits = logits[..., :-1, :].contiguous()
            shift_labels = labels[..., 1:].contiguous()
            loss_fct = nn.CrossEntropyLoss()
            loss = loss_fct(shift_logits.view(-1, shift_logits.size(-1)), shift_labels.view(-1))
            
        return logits, loss
