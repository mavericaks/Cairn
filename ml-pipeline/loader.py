import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from model import LlamaForCausalLM, LlamaConfig
import os

def load_llama_weights(custom_model, hf_model):
    """
    Map weights from HuggingFace's implementation to our custom Llama model.
    This demonstrates understanding of the exact tensor structure of Llama 3.
    """
    print("Mapping HuggingFace weights to custom architecture...")
    custom_state_dict = custom_model.state_dict()
    hf_state_dict = hf_model.state_dict()
    
    # Map token embeddings
    custom_state_dict['model.embed_tokens.weight'] = hf_state_dict['model.embed_tokens.weight']
    
    # Map layers
    num_layers = custom_model.model.config.num_hidden_layers
    for i in range(num_layers):
        prefix_c = f"model.layers.{i}."
        prefix_h = f"model.layers.{i}."
        
        # Attention
        custom_state_dict[f"{prefix_c}self_attn.q_proj.weight"] = hf_state_dict[f"{prefix_h}self_attn.q_proj.weight"]
        custom_state_dict[f"{prefix_c}self_attn.k_proj.weight"] = hf_state_dict[f"{prefix_h}self_attn.k_proj.weight"]
        custom_state_dict[f"{prefix_c}self_attn.v_proj.weight"] = hf_state_dict[f"{prefix_h}self_attn.v_proj.weight"]
        custom_state_dict[f"{prefix_c}self_attn.o_proj.weight"] = hf_state_dict[f"{prefix_h}self_attn.o_proj.weight"]
        
        # MLP
        custom_state_dict[f"{prefix_c}mlp.gate_proj.weight"] = hf_state_dict[f"{prefix_h}mlp.gate_proj.weight"]
        custom_state_dict[f"{prefix_c}mlp.up_proj.weight"] = hf_state_dict[f"{prefix_h}mlp.up_proj.weight"]
        custom_state_dict[f"{prefix_c}mlp.down_proj.weight"] = hf_state_dict[f"{prefix_h}mlp.down_proj.weight"]
        
        # LayerNorms
        custom_state_dict[f"{prefix_c}input_layernorm.weight"] = hf_state_dict[f"{prefix_h}input_layernorm.weight"]
        custom_state_dict[f"{prefix_c}post_attention_layernorm.weight"] = hf_state_dict[f"{prefix_h}post_attention_layernorm.weight"]
        
    # Map final layernorm
    custom_state_dict['model.norm.weight'] = hf_state_dict['model.norm.weight']
    
    # Map LM head
    custom_state_dict['lm_head.weight'] = hf_state_dict['lm_head.weight']
    
    custom_model.load_state_dict(custom_state_dict)
    print("Weights successfully loaded!")
    return custom_model

def get_pretrained_model(model_name="meta-llama/Llama-3.2-1B"):
    print("Initializing custom Llama-3.2-1B architecture...")
    config = LlamaConfig()
    custom_model = LlamaForCausalLM(config)
    
    hf_token = os.environ.get("HF_TOKEN")
    
    try:
        if not hf_token:
            print("Warning: HF_TOKEN not found. You must be authenticated to download Llama 3 weights.")
            print("To actually train, run: huggingface-cli login")
            print("For now, returning randomly initialized custom model for structural testing.")
            return custom_model, None
            
        print(f"Downloading/loading base weights for {model_name}...")
        hf_model = AutoModelForCausalLM.from_pretrained(model_name, token=hf_token)
        tokenizer = AutoTokenizer.from_pretrained(model_name, token=hf_token)
        
        # We need a padding token for batched training
        if tokenizer.pad_token is None:
            tokenizer.pad_token = tokenizer.eos_token
            
        custom_model = load_llama_weights(custom_model, hf_model)
        
        # Free up memory
        del hf_model
        import gc
        gc.collect()
        
        return custom_model, tokenizer
    except Exception as e:
        print(f"Failed to load HuggingFace weights: {e}")
        print("Returning randomly initialized custom model for structural testing.")
        return custom_model, None

if __name__ == "__main__":
    # Test loading
    model, tokenizer = get_pretrained_model()
    print(f"Model parameters: {sum(p.numel() for p in model.parameters()) / 1e9:.2f}B")
