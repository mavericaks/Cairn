"""
export.py — Merges LoRA adapters into base model and exports to HuggingFace format.

Pipeline: train.py saves LoRA adapters (.pt) -> this script merges them into base 
weights -> saves as HF safetensors -> ready for GGUF conversion.

Usage:
    python export.py --domain analytical --adapter_path adapters/analytical_lora.pt
"""

import torch
from loader import get_pretrained_model
from lora import inject_lora, merge_lora_weights
from transformers import AutoTokenizer
import argparse
import os


def export():
    parser = argparse.ArgumentParser(description="Merge LoRA and export to HF format")
    parser.add_argument("--domain", type=str, required=True)
    parser.add_argument("--adapter_path", type=str, required=True)
    parser.add_argument("--output_dir", type=str, default="exports")
    parser.add_argument("--base_model", type=str, default="meta-llama/Llama-3.2-1B")
    args = parser.parse_args()

    print(f"\n{'=' * 60}")
    print(f"  Exporting domain: {args.domain}")
    print(f"{'=' * 60}\n")

    if not os.path.exists(args.adapter_path):
        print(f"FATAL: Adapter file not found: {args.adapter_path}")
        print("Run train.py first.")
        return

    hf_token = os.environ.get("HF_TOKEN")
    if not hf_token:
        print("FATAL: HF_TOKEN not set.")
        return

    # 1. Load our custom model with pretrained weights
    print("Step 1: Loading base model with pretrained weights...")
    model, tokenizer = get_pretrained_model(args.base_model)
    if tokenizer is None:
        print("FATAL: Could not load tokenizer.")
        return

    # 2. Inject LoRA (same config as training)
    print("\nStep 2: Injecting LoRA structure...")
    model = inject_lora(model, rank=16, alpha=32, target_modules=["q_proj", "v_proj"])

    # 3. Load trained adapter weights
    print(f"\nStep 3: Loading adapter weights from {args.adapter_path}...")
    adapter_weights = torch.load(args.adapter_path, map_location="cpu", weights_only=True)
    print(f"  Adapter contains {len(adapter_weights)} tensors")

    # Load into model (strict=False to allow missing non-LoRA keys)
    model_sd = model.state_dict()
    loaded = 0
    for key, value in adapter_weights.items():
        if key in model_sd:
            model_sd[key] = value
            loaded += 1
        else:
            print(f"  WARNING: Key not found in model: {key}")
    model.load_state_dict(model_sd)
    print(f"  Loaded {loaded}/{len(adapter_weights)} adapter tensors")

    # 4. Merge LoRA: W' = W + (B @ A) * scaling
    print("\nStep 4: Merging LoRA weights into base model...")
    model = merge_lora_weights(model)

    # 5. Now we need to save in HuggingFace format for GGUF conversion.
    # Our custom model doesn't have save_pretrained(), so we save the state dict
    # and config manually in a format llama.cpp understands.
    out_path = os.path.join(args.output_dir, f"cairn-{args.domain}")
    os.makedirs(out_path, exist_ok=True)

    print(f"\nStep 5: Saving to {out_path}...")

    # Save state dict as safetensors
    from safetensors.torch import save_file
    state_dict = model.state_dict()

    # The llama.cpp converter expects HuggingFace key names, so we save as-is
    # since our key names already match HF's naming convention
    save_file(state_dict, os.path.join(out_path, "model.safetensors"))

    # Save tokenizer
    tokenizer.save_pretrained(out_path)

    # Save config.json that llama.cpp needs
    import json
    config = {
        "architectures": ["LlamaForCausalLM"],
        "model_type": "llama",
        "hidden_size": model.config.hidden_size,
        "intermediate_size": model.config.intermediate_size,
        "num_hidden_layers": model.config.num_hidden_layers,
        "num_attention_heads": model.config.num_attention_heads,
        "num_key_value_heads": model.config.num_key_value_heads,
        "vocab_size": model.config.vocab_size,
        "max_position_embeddings": model.config.max_position_embeddings,
        "rms_norm_eps": model.config.rms_norm_eps,
        "rope_theta": model.config.rope_theta,
        "torch_dtype": "float32",
        "bos_token_id": tokenizer.bos_token_id,
        "eos_token_id": tokenizer.eos_token_id,
    }
    with open(os.path.join(out_path, "config.json"), "w") as f:
        json.dump(config, f, indent=2)

    total_params = sum(p.numel() for p in model.parameters()) / 1e9
    model_size_mb = os.path.getsize(os.path.join(out_path, "model.safetensors")) / 1e6

    print(f"\n{'=' * 60}")
    print(f"  Export Complete!")
    print(f"{'=' * 60}")
    print(f"  Domain: {args.domain}")
    print(f"  Parameters: {total_params:.2f}B")
    print(f"  Size: {model_size_mb:.0f} MB")
    print(f"  Output: {out_path}/")
    print(f"\n  Next: Convert to GGUF:")
    print(f"    python llama.cpp/convert_hf_to_gguf.py {out_path} --outfile gguf/cairn-{args.domain}.gguf --outtype f16")
    print(f"{'=' * 60}")


if __name__ == "__main__":
    export()
