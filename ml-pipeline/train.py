"""
train.py — Production LoRA fine-tuning for Cairn domain models.

Designed for Google Colab T4 GPU (16GB VRAM). With Llama-3.2-1B + LoRA (rank 16),
this uses ~8-10GB VRAM with batch_size=4 + gradient accumulation.

Usage:
    python train.py --domain analytical --dataset data/analytical.jsonl --epochs 5
"""

import torch
from torch.utils.data import DataLoader
from loader import get_pretrained_model
from lora import inject_lora
import json
import argparse
from tqdm import tqdm
import os
import math


class InstructionDataset(torch.utils.data.Dataset):
    """
    Loads JSONL and formats as Llama-3 instruction pairs.
    Masks prompt tokens from loss (labels=-100) so the model only learns completions.
    """

    def __init__(self, data_file, tokenizer, max_length=512):
        self.examples = []

        print(f"Loading dataset: {data_file}")
        with open(data_file, "r", encoding="utf-8") as f:
            raw_lines = [line.strip() for line in f if line.strip()]

        print(f"  Tokenizing {len(raw_lines)} examples...")
        skipped = 0

        for line in raw_lines:
            try:
                data = json.loads(line)
            except json.JSONDecodeError:
                skipped += 1
                continue

            prompt_text = data.get("prompt", "")
            completion_text = data.get("completion", "")
            if not prompt_text or not completion_text:
                skipped += 1
                continue

            # Format as Llama-3 chat
            prompt = (
                "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
                "You are a helpful AI assistant.<|eot_id|>"
                "<|start_header_id|>user<|end_header_id|>\n\n"
                f"{prompt_text}<|eot_id|>"
                "<|start_header_id|>assistant<|end_header_id|>\n\n"
            )
            full_text = prompt + completion_text + "<|eot_id|>"

            # Tokenize full text
            full_tokens = tokenizer(
                full_text,
                max_length=max_length,
                truncation=True,
                padding="max_length",
                return_tensors="pt",
            )

            # Tokenize prompt only (to find where completion starts)
            prompt_tokens = tokenizer(
                prompt,
                max_length=max_length,
                truncation=True,
                return_tensors="pt",
                add_special_tokens=False,
            )

            input_ids = full_tokens["input_ids"].squeeze(0)
            attention_mask = full_tokens["attention_mask"].squeeze(0)

            # Create labels: mask prompt tokens with -100
            labels = input_ids.clone()
            prompt_len = min(prompt_tokens["input_ids"].shape[1], max_length)
            labels[:prompt_len] = -100
            labels[attention_mask == 0] = -100  # Also mask padding

            self.examples.append({
                "input_ids": input_ids,
                "attention_mask": attention_mask,
                "labels": labels,
            })

        if skipped:
            print(f"  Skipped {skipped} invalid examples")
        print(f"  Loaded {len(self.examples)} examples (max_length={max_length})")

    def __len__(self):
        return len(self.examples)

    def __getitem__(self, i):
        return self.examples[i]


def cosine_lr_lambda(current_step, num_warmup_steps, num_training_steps):
    """Cosine schedule with linear warmup."""
    if current_step < num_warmup_steps:
        return float(current_step) / float(max(1, num_warmup_steps))
    progress = float(current_step - num_warmup_steps) / float(
        max(1, num_training_steps - num_warmup_steps)
    )
    return max(0.0, 0.5 * (1.0 + math.cos(math.pi * progress)))


def train():
    parser = argparse.ArgumentParser(description="LoRA fine-tune Cairn domain model")
    parser.add_argument("--domain", type=str, required=True)
    parser.add_argument("--dataset", type=str, required=True)
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch_size", type=int, default=4)
    parser.add_argument("--grad_accum", type=int, default=4)
    parser.add_argument("--lr", type=float, default=2e-4)
    parser.add_argument("--max_length", type=int, default=512)
    parser.add_argument("--lora_rank", type=int, default=16)
    parser.add_argument("--lora_alpha", type=int, default=32)
    parser.add_argument("--warmup_ratio", type=float, default=0.1)
    args = parser.parse_args()

    # Device setup
    if torch.cuda.is_available():
        device = torch.device("cuda")
        gpu_name = torch.cuda.get_device_name(0)
        gpu_mem = torch.cuda.get_device_properties(0).total_mem / 1e9
    else:
        device = torch.device("cpu")
        gpu_name = "CPU"
        gpu_mem = 0

    print(f"\n{'=' * 60}")
    print(f"  Cairn LoRA Training - Domain: {args.domain}")
    print(f"{'=' * 60}")
    print(f"  Device: {device} ({gpu_name})")
    if gpu_mem > 0:
        print(f"  VRAM: {gpu_mem:.1f} GB")
    print(f"  Batch: {args.batch_size} x {args.grad_accum} = {args.batch_size * args.grad_accum} effective")
    print(f"  LoRA: rank={args.lora_rank}, alpha={args.lora_alpha}")
    print(f"  LR: {args.lr} (cosine + warmup)")
    print(f"{'=' * 60}\n")

    # 1. Load model + tokenizer
    model, tokenizer = get_pretrained_model("meta-llama/Llama-3.2-1B")

    if tokenizer is None:
        print("FATAL: No tokenizer. Set HF_TOKEN environment variable.")
        print("  export HF_TOKEN=hf_your_token")
        return

    # 2. Inject LoRA
    model = inject_lora(
        model, rank=args.lora_rank, alpha=args.lora_alpha,
        target_modules=["q_proj", "v_proj"],
    )
    model = model.to(device)

    trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)
    total = sum(p.numel() for p in model.parameters())
    print(f"Trainable: {trainable:,} / {total:,} ({100 * trainable / total:.2f}%)\n")

    # 3. Dataset
    dataset = InstructionDataset(args.dataset, tokenizer, max_length=args.max_length)
    if len(dataset) == 0:
        print("FATAL: Dataset is empty!")
        return

    # Ensure batch_size doesn't exceed dataset size
    effective_batch = min(args.batch_size, len(dataset))
    dataloader = DataLoader(dataset, batch_size=effective_batch, shuffle=True, drop_last=True)

    if len(dataloader) == 0:
        print(f"FATAL: Not enough examples ({len(dataset)}) for batch_size={effective_batch}")
        return

    # 4. Optimizer + Scheduler
    optimizer = torch.optim.AdamW(
        [p for p in model.parameters() if p.requires_grad],
        lr=args.lr,
        weight_decay=0.01,
    )

    total_steps = (len(dataloader) * args.epochs) // args.grad_accum
    warmup_steps = int(total_steps * args.warmup_ratio)

    scheduler = torch.optim.lr_scheduler.LambdaLR(
        optimizer,
        lr_lambda=lambda step: cosine_lr_lambda(step, warmup_steps, total_steps),
    )

    # Mixed precision (use new API that works on all PyTorch versions)
    use_amp = device.type == "cuda"
    scaler = torch.amp.GradScaler("cuda", enabled=use_amp) if use_amp else None

    print(f"Training: {total_steps} steps ({warmup_steps} warmup)")
    print(f"Dataset: {len(dataset)} examples, {len(dataloader)} batches/epoch")
    print(f"Mixed precision: {use_amp}\n")

    # 5. Training loop
    model.train()
    best_loss = float("inf")
    global_step = 0
    optimizer.zero_grad()

    for epoch in range(args.epochs):
        epoch_loss = 0.0
        num_batches = 0
        progress = tqdm(dataloader, desc=f"Epoch {epoch + 1}/{args.epochs}")

        for batch_idx, batch in enumerate(progress):
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)

            # Forward
            if use_amp:
                with torch.amp.autocast("cuda"):
                    _logits, loss = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
            else:
                _logits, loss = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)

            loss = loss / args.grad_accum

            # Backward
            if scaler is not None:
                scaler.scale(loss).backward()
            else:
                loss.backward()

            # Step every grad_accum batches
            if (batch_idx + 1) % args.grad_accum == 0:
                if scaler is not None:
                    scaler.unscale_(optimizer)
                    torch.nn.utils.clip_grad_norm_(
                        [p for p in model.parameters() if p.requires_grad], max_norm=1.0
                    )
                    scaler.step(optimizer)
                    scaler.update()
                else:
                    torch.nn.utils.clip_grad_norm_(
                        [p for p in model.parameters() if p.requires_grad], max_norm=1.0
                    )
                    optimizer.step()

                optimizer.zero_grad()
                scheduler.step()
                global_step += 1

            actual_loss = loss.item() * args.grad_accum
            epoch_loss += actual_loss
            num_batches += 1
            progress.set_postfix(loss=f"{actual_loss:.4f}", lr=f"{scheduler.get_last_lr()[0]:.2e}")

        avg_loss = epoch_loss / max(num_batches, 1)
        print(f"Epoch {epoch + 1} -- avg loss: {avg_loss:.4f}")
        if avg_loss < best_loss:
            best_loss = avg_loss

    # 6. Save adapters
    print("\nSaving LoRA adapters...")
    os.makedirs("adapters", exist_ok=True)
    lora_state = {k: v.cpu() for k, v in model.state_dict().items() if "lora_" in k}
    adapter_path = f"adapters/{args.domain}_lora.pt"
    torch.save(lora_state, adapter_path)

    size_mb = os.path.getsize(adapter_path) / 1e6

    print(f"\n{'=' * 60}")
    print(f"  Training Complete!")
    print(f"{'=' * 60}")
    print(f"  Domain: {args.domain}")
    print(f"  Best loss: {best_loss:.4f}")
    print(f"  Adapter: {adapter_path} ({size_mb:.1f} MB)")
    print(f"  Next: python export.py --domain {args.domain} --adapter_path {adapter_path}")
    print(f"{'=' * 60}")

    if torch.cuda.is_available():
        print(f"\n  Peak GPU: {torch.cuda.max_memory_allocated() / 1e9:.1f} GB")


if __name__ == "__main__":
    train()
