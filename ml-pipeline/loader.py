"""
loader.py — Loads Llama-3.2-1B pretrained weights into our custom architecture.

Two modes:
  1. Training mode: Uses HuggingFace's AutoModel to download weights, then maps
     them into our custom LlamaForCausalLM. Our custom model is used for training
     so we can demonstrate we understand the architecture.
  2. Structural test: If HF_TOKEN is not set, returns a randomly initialized model
     so you can verify the code runs without needing to download 2.5GB.
"""

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from model import LlamaForCausalLM, LlamaConfig
import os
import gc


def load_weights_from_hf(custom_model, hf_model_name, hf_token):
    """
    Downloads HuggingFace model, copies weights into our custom architecture,
    then immediately deletes the HF model to free memory.
    """
    print(f"  Downloading {hf_model_name} from HuggingFace...")
    hf_model = AutoModelForCausalLM.from_pretrained(
        hf_model_name,
        token=hf_token,
        torch_dtype=torch.float16,  # Use float16 to prevent System RAM swap freezing
        low_cpu_mem_usage=True,     # Load weights sequentially to reduce peak RAM
    )

    hf_sd = hf_model.state_dict()
    custom_sd = custom_model.state_dict()

    print("  Mapping HuggingFace weights → custom architecture...")

    # Token embeddings
    custom_sd["model.embed_tokens.weight"] = hf_sd["model.embed_tokens.weight"]

    # Per-layer weights
    num_layers = custom_model.config.num_hidden_layers
    for i in range(num_layers):
        p = f"model.layers.{i}."
        # Attention projections
        for proj in ["q_proj", "k_proj", "v_proj", "o_proj"]:
            custom_sd[f"{p}self_attn.{proj}.weight"] = hf_sd[f"{p}self_attn.{proj}.weight"]
        # MLP projections
        for proj in ["gate_proj", "up_proj", "down_proj"]:
            custom_sd[f"{p}mlp.{proj}.weight"] = hf_sd[f"{p}mlp.{proj}.weight"]
        # LayerNorms
        custom_sd[f"{p}input_layernorm.weight"] = hf_sd[f"{p}input_layernorm.weight"]
        custom_sd[f"{p}post_attention_layernorm.weight"] = hf_sd[f"{p}post_attention_layernorm.weight"]

    # Final norm + LM head
    custom_sd["model.norm.weight"] = hf_sd["model.norm.weight"]
    custom_sd["lm_head.weight"] = hf_sd["lm_head.weight"]

    custom_model.load_state_dict(custom_sd)
    custom_model = custom_model.to(torch.float16)
    print(f"  Loaded {len(custom_sd)} weight tensors successfully (float16).")

    # Free HF model memory immediately
    del hf_model, hf_sd
    gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()

    return custom_model


def get_pretrained_model(model_name="meta-llama/Llama-3.2-1B"):
    """
    Returns (model, tokenizer). If HF_TOKEN is not set, returns (model, None)
    with random weights for structural testing only.
    """
    print("Initializing custom Llama-3.2-1B architecture...")
    config = LlamaConfig()
    custom_model = LlamaForCausalLM(config)

    total_params = sum(p.numel() for p in custom_model.parameters())
    print(f"  Parameters: {total_params / 1e9:.2f}B")

    hf_token = os.environ.get("HF_TOKEN")

    if not hf_token:
        print("WARNING: HF_TOKEN not set. Returning randomly initialized model.")
        print("  Set HF_TOKEN to download real Llama-3.2-1B weights.")
        return custom_model, None

    try:
        custom_model = load_weights_from_hf(custom_model, model_name, hf_token)

        print(f"  Loading tokenizer from {model_name}...")
        tokenizer = AutoTokenizer.from_pretrained(model_name, token=hf_token)
        if tokenizer.pad_token is None:
            tokenizer.pad_token = tokenizer.eos_token
            tokenizer.pad_token_id = tokenizer.eos_token_id

        return custom_model, tokenizer

    except Exception as e:
        print(f"ERROR loading weights: {e}")
        print("Returning randomly initialized model.")
        return custom_model, None


if __name__ == "__main__":
    model, tokenizer = get_pretrained_model()
    print(f"\nModel loaded: {sum(p.numel() for p in model.parameters()) / 1e9:.2f}B parameters")
    if tokenizer:
        print(f"Tokenizer vocab: {tokenizer.vocab_size}")
        test = tokenizer("Hello world", return_tensors="pt")
        print(f"Test tokenization: {test['input_ids'].shape}")
    else:
        print("No tokenizer (HF_TOKEN not set)")
