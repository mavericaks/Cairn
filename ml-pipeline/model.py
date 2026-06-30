"""
model.py — Hand-written Llama-3 Transformer architecture from scratch.

This file proves you understand the internals of a modern LLM:
- RoPE (Rotary Position Embeddings) with configurable theta
- GQA (Grouped Query Attention) with separate KV heads
- SwiGLU activation (gate_proj * up_proj with SiLU)
- RMSNorm (simpler, faster than LayerNorm)
- Causal masking for autoregressive generation

Architecture matches Llama-3.2-1B:
  - 16 layers, 2048 hidden, 8192 intermediate
  - 32 attention heads, 8 KV heads (4:1 GQA ratio)
  - 128256 vocab size, 8192 max positions
  - ~1.24B parameters

NOTE: For training, we load HuggingFace's pretrained weights into this architecture
via loader.py. For export, we merge LoRA back into HF format via export.py.
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
import math
from typing import Optional, Tuple


class LlamaRMSNorm(nn.Module):
    """Root Mean Square Layer Normalization (simpler than LayerNorm — no mean subtraction)."""

    def __init__(self, hidden_size: int, eps: float = 1e-5):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(hidden_size))
        self.eps = eps

    def forward(self, hidden_states: torch.Tensor) -> torch.Tensor:
        input_dtype = hidden_states.dtype
        hidden_states = hidden_states.to(torch.float32)
        variance = hidden_states.pow(2).mean(-1, keepdim=True)
        hidden_states = hidden_states * torch.rsqrt(variance + self.eps)
        return self.weight * hidden_states.to(input_dtype)


class LlamaRotaryEmbedding(nn.Module):
    """Rotary Position Embeddings — encodes position via rotation in complex plane."""

    def __init__(self, dim: int, max_position_embeddings: int = 8192, base: float = 500000.0):
        super().__init__()
        self.dim = dim
        self.max_position_embeddings = max_position_embeddings
        inv_freq = 1.0 / (base ** (torch.arange(0, dim, 2).float() / dim))
        self.register_buffer("inv_freq", inv_freq, persistent=False)

    def forward(self, x: torch.Tensor, seq_len: int) -> Tuple[torch.Tensor, torch.Tensor]:
        t = torch.arange(seq_len, device=x.device, dtype=self.inv_freq.dtype)
        freqs = torch.outer(t, self.inv_freq)
        emb = torch.cat((freqs, freqs), dim=-1)  # (seq_len, dim)
        return emb.cos(), emb.sin()


def apply_rotary_pos_emb(
    q: torch.Tensor, k: torch.Tensor, cos: torch.Tensor, sin: torch.Tensor
) -> Tuple[torch.Tensor, torch.Tensor]:
    """Apply RoPE to query and key tensors."""
    # q, k: (bsz, num_heads, seq_len, head_dim)
    # cos, sin: (seq_len, head_dim)
    cos = cos.unsqueeze(0).unsqueeze(0)  # (1, 1, seq_len, dim)
    sin = sin.unsqueeze(0).unsqueeze(0)

    def rotate_half(x):
        x1, x2 = x.chunk(2, dim=-1)
        return torch.cat((-x2, x1), dim=-1)

    q_out = (q * cos) + (rotate_half(q) * sin)
    k_out = (k * cos) + (rotate_half(k) * sin)
    return q_out, k_out


class LlamaMLP(nn.Module):
    """SwiGLU MLP: down_proj(SiLU(gate_proj(x)) * up_proj(x))"""

    def __init__(self, hidden_size: int, intermediate_size: int):
        super().__init__()
        self.gate_proj = nn.Linear(hidden_size, intermediate_size, bias=False)
        self.up_proj = nn.Linear(hidden_size, intermediate_size, bias=False)
        self.down_proj = nn.Linear(intermediate_size, hidden_size, bias=False)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.down_proj(F.silu(self.gate_proj(x)) * self.up_proj(x))


class LlamaAttention(nn.Module):
    """Multi-head attention with Grouped Query Attention (GQA) and RoPE."""

    def __init__(self, hidden_size: int, num_heads: int, num_kv_heads: int,
                 max_position_embeddings: int, rope_theta: float):
        super().__init__()
        self.hidden_size = hidden_size
        self.num_heads = num_heads
        self.num_kv_heads = num_kv_heads
        self.head_dim = hidden_size // num_heads
        self.num_kv_groups = num_heads // num_kv_heads

        self.q_proj = nn.Linear(hidden_size, num_heads * self.head_dim, bias=False)
        self.k_proj = nn.Linear(hidden_size, num_kv_heads * self.head_dim, bias=False)
        self.v_proj = nn.Linear(hidden_size, num_kv_heads * self.head_dim, bias=False)
        self.o_proj = nn.Linear(num_heads * self.head_dim, hidden_size, bias=False)

        self.rotary_emb = LlamaRotaryEmbedding(
            self.head_dim, max_position_embeddings=max_position_embeddings, base=rope_theta
        )

    def forward(self, hidden_states: torch.Tensor, attention_mask: Optional[torch.Tensor] = None) -> torch.Tensor:
        bsz, seq_len, _ = hidden_states.size()

        q = self.q_proj(hidden_states).view(bsz, seq_len, self.num_heads, self.head_dim).transpose(1, 2)
        k = self.k_proj(hidden_states).view(bsz, seq_len, self.num_kv_heads, self.head_dim).transpose(1, 2)
        v = self.v_proj(hidden_states).view(bsz, seq_len, self.num_kv_heads, self.head_dim).transpose(1, 2)

        cos, sin = self.rotary_emb(hidden_states, seq_len)
        q, k = apply_rotary_pos_emb(q, k, cos, sin)

        # GQA: repeat KV heads to match query heads
        k = k.repeat_interleave(self.num_kv_groups, dim=1)
        v = v.repeat_interleave(self.num_kv_groups, dim=1)

        # Scaled dot-product attention
        attn_weights = torch.matmul(q, k.transpose(2, 3)) / math.sqrt(self.head_dim)

        if attention_mask is not None:
            attn_weights = attn_weights + attention_mask

        attn_weights = F.softmax(attn_weights, dim=-1, dtype=torch.float32).to(q.dtype)
        attn_output = torch.matmul(attn_weights, v)

        attn_output = attn_output.transpose(1, 2).contiguous().view(bsz, seq_len, self.hidden_size)
        return self.o_proj(attn_output)


class LlamaDecoderLayer(nn.Module):
    """Single transformer decoder layer: attention + MLP with residual connections."""

    def __init__(self, hidden_size: int, intermediate_size: int, num_heads: int,
                 num_kv_heads: int, max_position_embeddings: int, rope_theta: float,
                 rms_norm_eps: float):
        super().__init__()
        self.self_attn = LlamaAttention(
            hidden_size, num_heads, num_kv_heads, max_position_embeddings, rope_theta
        )
        self.mlp = LlamaMLP(hidden_size, intermediate_size)
        self.input_layernorm = LlamaRMSNorm(hidden_size, eps=rms_norm_eps)
        self.post_attention_layernorm = LlamaRMSNorm(hidden_size, eps=rms_norm_eps)

    def forward(self, hidden_states: torch.Tensor, attention_mask: Optional[torch.Tensor] = None) -> torch.Tensor:
        # Pre-norm attention
        residual = hidden_states
        hidden_states = self.input_layernorm(hidden_states)
        hidden_states = self.self_attn(hidden_states, attention_mask=attention_mask)
        hidden_states = residual + hidden_states

        # Pre-norm MLP
        residual = hidden_states
        hidden_states = self.post_attention_layernorm(hidden_states)
        hidden_states = self.mlp(hidden_states)
        hidden_states = residual + hidden_states

        return hidden_states


class LlamaConfig:
    """Llama-3.2-1B configuration — matches Meta's published architecture."""

    def __init__(self):
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
    """The core Llama transformer (embeddings + decoder layers + final norm)."""

    def __init__(self, config: LlamaConfig):
        super().__init__()
        self.config = config
        self.embed_tokens = nn.Embedding(config.vocab_size, config.hidden_size)
        self.layers = nn.ModuleList([
            LlamaDecoderLayer(
                config.hidden_size, config.intermediate_size,
                config.num_attention_heads, config.num_key_value_heads,
                config.max_position_embeddings, config.rope_theta, config.rms_norm_eps,
            )
            for _ in range(config.num_hidden_layers)
        ])
        self.norm = LlamaRMSNorm(config.hidden_size, eps=config.rms_norm_eps)

    def forward(self, input_ids: torch.Tensor, attention_mask: Optional[torch.Tensor] = None) -> torch.Tensor:
        hidden_states = self.embed_tokens(input_ids)
        bsz, seq_len = input_ids.shape

        # Build causal mask: (1, 1, seq_len, seq_len)
        causal_mask = torch.full((seq_len, seq_len), float("-inf"), device=input_ids.device, dtype=hidden_states.dtype)
        causal_mask = torch.triu(causal_mask, diagonal=1)
        causal_mask = causal_mask.unsqueeze(0).unsqueeze(0)  # (1, 1, seq_len, seq_len)

        # If padding mask provided, combine with causal mask
        if attention_mask is not None and attention_mask.dim() == 2:
            # attention_mask: (bsz, seq_len) with 1=keep, 0=mask
            padding_mask = attention_mask[:, None, None, :]  # (bsz, 1, 1, seq_len)
            padding_mask = (1.0 - padding_mask.float()) * float("-inf")
            causal_mask = causal_mask + padding_mask

        for layer in self.layers:
            hidden_states = layer(hidden_states, attention_mask=causal_mask)

        return self.norm(hidden_states)


class LlamaForCausalLM(nn.Module):
    """Llama for causal language modeling (next token prediction)."""

    def __init__(self, config: LlamaConfig):
        super().__init__()
        self.config = config
        self.model = LlamaModel(config)
        self.lm_head = nn.Linear(config.hidden_size, config.vocab_size, bias=False)

    def forward(self, input_ids: torch.Tensor, attention_mask: Optional[torch.Tensor] = None,
                labels: Optional[torch.Tensor] = None) -> Tuple[torch.Tensor, Optional[torch.Tensor]]:
        hidden_states = self.model(input_ids, attention_mask)
        logits = self.lm_head(hidden_states)

        loss = None
        if labels is not None:
            # Shift: predict token i+1 from position i
            shift_logits = logits[..., :-1, :].contiguous()
            shift_labels = labels[..., 1:].contiguous()
            loss = F.cross_entropy(
                shift_logits.view(-1, shift_logits.size(-1)),
                shift_labels.view(-1),
                ignore_index=-100,  # Ignore masked prompt tokens
            )

        return logits, loss
