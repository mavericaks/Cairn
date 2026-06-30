"""
train.py — Production-grade LoRA fine-tuning for Cairn domain models.

Designed for Google Colab T4 GPU (16GB VRAM). With Llama-3.2-1B + LoRA (rank 16),
this uses ~8-10GB VRAM with batch_size=4 + gradient accumulation.

Usage:
    python train.py --domain analytical --dataset data/analytical.jsonl --epochs 5

Full training (all domains):
    for domain in analytical execution discovery generative conversational system; do
        python train.py --domain $domain --dataset data/$domain.jsonl --epochs 5
    done
"""

import torch
from torch.utils.data import DataLoader
from torch.cuda.amp import autocast, GradScaler
from loader import get_pretrained_model
from lora import inject_lora
import json
import argparse
from tqdm import tqdm
import os
import math


class InstructionDataset(torch.utils.data.Dataset):
    """
    Formats training examples as Llama-3 instruction-following conversations.
    
    Key detail: We mask the prompt tokens from the loss (labels=-100) so the model
    only learns to predict the completion, not memorize the prompt.
    """

    def __init__(self, data_file, tokenizer, max_length=512):
        self.examples = []

        print(f"Loading dataset from {data_file}")
        with open(data_file, "r", encoding="utf-8") as f:
            for line in f:
                if not line.strip():
                    continue
                data = json.loads(line)

                # Format as Llama-3 chat template
                prompt = (
                    f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
                    f"You are a helpful AI assistant.<|eot_id|>"
                    f"<|start_header_id|>user<|end_header_id|>\n\n"
                    f"{data['prompt']}<|eot_id|>"
                    f"<|start_header_id|>assistant<|end_header_id|>\n\n"
                )
                full_text = prompt + data["completion"] + "<|eot_id|>"

                # Tokenize
                tokens = tokenizer(
                    full_text,
                    max_length=max_length,
                    truncation=True,
                    padding="max_length",
                    return_tensors="pt",
                )

                input_ids = tokens["input_ids"][0]
                attention_mask = tokens["attention_mask"][0]

                # Labels: mask out the prompt so loss only applies to the completion
                labels = input_ids.clone()
                prompt_tokens = tokenizer(
                    prompt, add_special_tokens=False, return_tensors="pt"
                )["input_ids"][0]
                prompt_len = min(len(prompt_tokens), max_length)
                labels[:prompt_len] = -100
                labels[attention_mask == 0] = -100  # Also mask padding

                self.examples.append(
                    {
                        "input_ids": input_ids,
                        "attention_mask": attention_mask,
                        "labels": labels,
                    }
                )

        print(f"Loaded {len(self.examples)} examples (max_length={max_length})")

    def __len__(self):
        return len(self.examples)

    def __getitem__(self, i):
        return self.examples[i]


def get_cosine_schedule_with_warmup(optimizer, num_warmup_steps, num_training_steps):
    """Cosine learning rate schedule with linear warmup."""
    def lr_lambda(current_step):
        if current_step < num_warmup_steps:
            return float(current_step) / float(max(1, num_warmup_steps))
        progress = float(current_step - num_warmup_steps) / float(
            max(1, num_training_steps - num_warmup_steps)
        )
        return max(0.0, 0.5 * (1.0 + math.cos(math.pi * progress)))

    return torch.optim.lr_scheduler.LambdaLR(optimizer, lr_lambda)


def train():
    parser = argparse.ArgumentParser(description="LoRA fine-tune Cairn domain model")
    parser.add_argument(
        "--domain", type=str, required=True, help="Target domain (analytical, execution, etc.)"
    )
    parser.add_argument(
        "--dataset", type=str, required=True, help="Path to JSONL training dataset"
    )
    parser.add_argument("--epochs", type=int, default=5, help="Number of training epochs")
    parser.add_argument(
        "--batch_size", type=int, default=4, help="Batch size (4 fits on T4 16GB)"
    )
    parser.add_argument(
        "--grad_accum", type=int, default=4, help="Gradient accumulation steps (effective batch = batch_size * grad_accum)"
    )
    parser.add_argument("--lr", type=float, default=2e-4, help="Peak learning rate")
    parser.add_argument(
        "--max_length", type=int, default=512, help="Max sequence length"
    )
    parser.add_argument(
        "--lora_rank", type=int, default=16, help="LoRA rank (higher = more params, better quality)"
    )
    parser.add_argument(
        "--lora_alpha", type=int, default=32, help="LoRA scaling factor"
    )
    parser.add_argument(
        "--warmup_ratio", type=float, default=0.1, help="Fraction of steps for LR warmup"
    )
    parser.add_argument(
        "--save_every", type=int, default=0, help="Save checkpoint every N epochs (0 = only final)"
    )
    parser.add_argument(
        "--fp16", action="store_true", default=True, help="Use mixed precision (default: True)"
    )
    args = parser.parse_args()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\n{'='*60}")
    print(f"  Cairn LoRA Training — Domain: {args.domain}")
    print(f"{'='*60}")
    print(f"  Device: {device}")
    if torch.cuda.is_available():
        print(f"  GPU: {torch.cuda.get_device_name(0)}")
        print(f"  VRAM: {torch.cuda.get_device_properties(0).total_mem / 1e9:.1f} GB")
    print(f"  Batch size: {args.batch_size} x {args.grad_accum} = {args.batch_size * args.grad_accum} effective")
    print(f"  Max length: {args.max_length}")
    print(f"  LoRA rank: {args.lora_rank}, alpha: {args.lora_alpha}")
    print(f"  Learning rate: {args.lr} (cosine schedule with warmup)")
    print(f"  Mixed precision: {args.fp16}")
    print(f"{'='*60}\n")

    # 1. Load Model & Tokenizer
    model, tokenizer = get_pretrained_model("meta-llama/Llama-3.2-1B")

    if tokenizer is None:
        print("ERROR: HF_TOKEN not set or model download failed.")
        print("Set HF_TOKEN environment variable and ensure you've accepted the Llama 3.2 license.")
        return

    # 2. Inject LoRA adapters
    model = inject_lora(
        model,
        rank=args.lora_rank,
        alpha=args.lora_alpha,
        target_modules=["q_proj", "v_proj"],
    )
    model = model.to(device)

    # Count trainable vs frozen parameters
    trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)
    total = sum(p.numel() for p in model.parameters())
    print(f"Trainable parameters: {trainable:,} / {total:,} ({100 * trainable / total:.2f}%)")

    # 3. Load Dataset
    dataset = InstructionDataset(args.dataset, tokenizer, max_length=args.max_length)
    dataloader = DataLoader(dataset, batch_size=args.batch_size, shuffle=True, drop_last=True)

    # 4. Setup Optimizer + Scheduler
    optimizer = torch.optim.AdamW(
        filter(lambda p: p.requires_grad, model.parameters()),
        lr=args.lr,
        weight_decay=0.01,
    )

    num_training_steps = len(dataloader) * args.epochs // args.grad_accum
    num_warmup_steps = int(num_training_steps * args.warmup_ratio)
    scheduler = get_cosine_schedule_with_warmup(optimizer, num_warmup_steps, num_training_steps)

    # Mixed precision scaler
    scaler = GradScaler(enabled=args.fp16)

    print(f"\nTraining steps: {num_training_steps} ({num_warmup_steps} warmup)")
    print(f"Dataset size: {len(dataset)} examples")
    print(f"Batches per epoch: {len(dataloader)}\n")

    # 5. Training Loop
    model.train()
    best_loss = float("inf")
    global_step = 0

    for epoch in range(args.epochs):
        epoch_loss = 0
        num_batches = 0
        progress = tqdm(dataloader, desc=f"Epoch {epoch + 1}/{args.epochs}")

        for batch_idx, batch in enumerate(progress):
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)

            # Forward pass with mixed precision
            with autocast(enabled=args.fp16):
                logits, loss = model(
                    input_ids=input_ids,
                    attention_mask=attention_mask,
                    labels=labels,
                )
                loss = loss / args.grad_accum  # Scale loss for gradient accumulation

            # Backward pass with gradient scaling
            scaler.scale(loss).backward()

            # Gradient accumulation: step every grad_accum batches
            if (batch_idx + 1) % args.grad_accum == 0:
                scaler.unscale_(optimizer)
                torch.nn.utils.clip_grad_norm_(
                    filter(lambda p: p.requires_grad, model.parameters()), max_norm=1.0
                )
                scaler.step(optimizer)
                scaler.update()
                optimizer.zero_grad()
                scheduler.step()
                global_step += 1

            epoch_loss += loss.item() * args.grad_accum  # Un-scale for logging
            num_batches += 1
            progress.set_postfix(
                {
                    "loss": f"{loss.item() * args.grad_accum:.4f}",
                    "lr": f"{scheduler.get_last_lr()[0]:.2e}",
                }
            )

        avg_loss = epoch_loss / num_batches
        print(f"Epoch {epoch + 1} — avg loss: {avg_loss:.4f}, lr: {scheduler.get_last_lr()[0]:.2e}")

        # Track best loss
        if avg_loss < best_loss:
            best_loss = avg_loss

        # Checkpoint saving
        if args.save_every > 0 and (epoch + 1) % args.save_every == 0:
            ckpt_path = f"adapters/{args.domain}_lora_epoch{epoch + 1}.pt"
            os.makedirs("adapters", exist_ok=True)
            lora_state = {k: v for k, v in model.state_dict().items() if "lora_" in k}
            torch.save(lora_state, ckpt_path)
            print(f"  Checkpoint saved: {ckpt_path}")

    # 6. Save final LoRA adapters
    print("\nSaving final LoRA adapters...")
    os.makedirs("adapters", exist_ok=True)
    lora_state_dict = {k: v for k, v in model.state_dict().items() if "lora_" in k}
    adapter_path = f"adapters/{args.domain}_lora.pt"
    torch.save(lora_state_dict, adapter_path)

    adapter_size_mb = os.path.getsize(adapter_path) / 1e6
    print(f"\n{'='*60}")
    print(f"  Training Complete!")
    print(f"{'='*60}")
    print(f"  Domain: {args.domain}")
    print(f"  Best loss: {best_loss:.4f}")
    print(f"  Adapter params: {trainable:,}")
    print(f"  Adapter size: {adapter_size_mb:.1f} MB")
    print(f"  Saved to: {adapter_path}")
    print(f"\n  Next step:")
    print(f"    python export.py --domain {args.domain} --adapter_path {adapter_path}")
    print(f"{'='*60}")

    # Clean up GPU memory
    if torch.cuda.is_available():
        peak_mem = torch.cuda.max_memory_allocated() / 1e9
        print(f"\n  Peak GPU memory: {peak_mem:.1f} GB")


if __name__ == "__main__":
    train()
