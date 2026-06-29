"""
export.py — Merges LoRA adapters into the base model and exports to HuggingFace format.

This is the bridge between training (train.py) and GGUF conversion (export_to_gguf.sh).
After this step, the model is in a standard HF format that llama.cpp can convert.

Usage:
    python export.py --domain analytical --adapter_path adapters/analytical_lora.pt
"""

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from lora import inject_lora, merge_lora_weights
from loader import get_pretrained_model
import argparse
import os


def export():
    parser = argparse.ArgumentParser(description="Merge LoRA adapters and export to HF format")
    parser.add_argument("--domain", type=str, required=True, help="Target domain (e.g., analytical)")
    parser.add_argument("--adapter_path", type=str, required=True, help="Path to saved LoRA adapters (.pt file)")
    parser.add_argument("--output_dir", type=str, default="exported_models")
    parser.add_argument("--base_model", type=str, default="meta-llama/Llama-3.2-1B")
    args = parser.parse_args()

    print(f"═══ Exporting model for domain: {args.domain} ═══")

    # 1. Load the BASE HuggingFace model directly (not our custom architecture)
    # WHY: For export, we need the HF model class so we can use save_pretrained()
    # which outputs the format that llama.cpp expects.
    hf_token = os.environ.get("HF_TOKEN")
    if not hf_token:
        print("ERROR: HF_TOKEN environment variable required for export.")
        print("Set it with: export HF_TOKEN=your_token_here")
        return

    print(f"Loading base model: {args.base_model}")
    hf_model = AutoModelForCausalLM.from_pretrained(args.base_model, token=hf_token)
    tokenizer = AutoTokenizer.from_pretrained(args.base_model, token=hf_token)

    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    # 2. Inject LoRA structure into the HF model
    print("Injecting LoRA layers...")
    hf_model = inject_lora(hf_model, rank=16, alpha=32, target_modules=["q_proj", "v_proj"])

    # 3. Load trained adapter weights
    print(f"Loading adapter weights from {args.adapter_path}...")
    if not os.path.exists(args.adapter_path):
        print(f"ERROR: Adapter file not found: {args.adapter_path}")
        print("Did you run train.py first?")
        return

    adapter_weights = torch.load(args.adapter_path, map_location="cpu", weights_only=True)
    model_dict = hf_model.state_dict()

    # Only load keys that exist in both
    matched = {k: v for k, v in adapter_weights.items() if k in model_dict}
    print(f"  Loaded {len(matched)}/{len(adapter_weights)} adapter weight tensors")
    model_dict.update(matched)
    hf_model.load_state_dict(model_dict)

    # 4. Merge LoRA weights back into the base weights (W' = W + alpha/rank * B @ A)
    print("Merging LoRA weights into base model...")
    hf_model = merge_lora_weights(hf_model)

    # 5. Save in HuggingFace format (this is what llama.cpp's convert script expects)
    out_path = os.path.join(args.output_dir, args.domain)
    os.makedirs(out_path, exist_ok=True)

    print(f"Saving merged model to {out_path}...")
    hf_model.save_pretrained(out_path, safe_serialization=True)
    tokenizer.save_pretrained(out_path)

    total_params = sum(p.numel() for p in hf_model.parameters()) / 1e9
    model_size_mb = os.path.getsize(os.path.join(out_path, "model.safetensors")) / 1e6

    print(f"\n═══ Export Complete ═══")
    print(f"  Domain: {args.domain}")
    print(f"  Parameters: {total_params:.2f}B")
    print(f"  Model size: {model_size_mb:.0f} MB")
    print(f"  Output: {out_path}/")
    print(f"\nNext step: Convert to GGUF for Ollama:")
    print(f"  bash export_to_gguf.sh {out_path} cairn-{args.domain}")


if __name__ == "__main__":
    export()
