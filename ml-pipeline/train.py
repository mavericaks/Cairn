import torch
from torch.utils.data import DataLoader
from loader import get_pretrained_model
from lora import inject_lora
import json
import argparse
from tqdm import tqdm
import os

class DummyDataset(torch.utils.data.Dataset):
    def __init__(self, data_file, tokenizer, max_length=512):
        self.examples = []
        
        print(f"Loading dataset from {data_file}")
        with open(data_file, 'r', encoding='utf-8') as f:
            for line in f:
                if not line.strip(): continue
                data = json.loads(line)
                
                # Format: system prompt + user question -> assistant response
                prompt = f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\nYou are a helpful AI assistant.<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n{data['prompt']}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
                full_text = prompt + data['completion'] + "<|eot_id|>"
                
                # Tokenize
                tokens = tokenizer(
                    full_text, 
                    max_length=max_length, 
                    truncation=True, 
                    padding="max_length",
                    return_tensors="pt"
                )
                
                input_ids = tokens['input_ids'][0]
                attention_mask = tokens['attention_mask'][0]
                
                # For causal LM, labels are the same as input_ids.
                # We should mask out the prompt from the loss calculation by setting labels to -100
                labels = input_ids.clone()
                
                # Find where the assistant's response starts to mask out the prompt
                prompt_tokens = tokenizer(prompt, add_special_tokens=False, return_tensors="pt")['input_ids'][0]
                prompt_len = min(len(prompt_tokens), max_length)
                labels[:prompt_len] = -100
                
                # Mask out padding tokens from loss
                labels[attention_mask == 0] = -100
                
                self.examples.append({
                    "input_ids": input_ids,
                    "attention_mask": attention_mask,
                    "labels": labels
                })
                
    def __len__(self):
        return len(self.examples)
        
    def __getitem__(self, i):
        return self.examples[i]

def train():
    parser = argparse.ArgumentParser()
    parser.add_argument("--domain", type=str, required=True, help="Target domain (e.g., analytical)")
    parser.add_argument("--dataset", type=str, required=True, help="Path to JSONL dataset")
    parser.add_argument("--epochs", type=int, default=1)
    parser.add_argument("--batch_size", type=int, default=1) # 1 for local dev to save memory
    parser.add_argument("--lr", type=float, default=2e-4)
    args = parser.parse_args()
    
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")
    
    # 1. Load Model & Tokenizer
    model, tokenizer = get_pretrained_model("meta-llama/Llama-3.2-1B")
    
    if tokenizer is None:
        print("Bailing out of training because HF token wasn't provided or loading failed.")
        print("To verify the script structure worked, we will just exit gracefully.")
        return
        
    # 2. Inject LoRA
    model = inject_lora(model, rank=16, alpha=32, target_modules=["q_proj", "v_proj"])
    model = model.to(device)
    
    # 3. Load Dataset
    dataset = DummyDataset(args.dataset, tokenizer, max_length=256)
    dataloader = DataLoader(dataset, batch_size=args.batch_size, shuffle=True)
    
    # 4. Setup Optimizer
    optimizer = torch.optim.AdamW(filter(lambda p: p.requires_grad, model.parameters()), lr=args.lr)
    
    # 5. Training Loop
    model.train()
    print("Starting training loop...")
    
    for epoch in range(args.epochs):
        epoch_loss = 0
        progress = tqdm(dataloader, desc=f"Epoch {epoch+1}/{args.epochs}")
        
        for batch in progress:
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)
            
            optimizer.zero_grad()
            
            # Forward pass
            logits, loss = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
            
            # Backward pass
            loss.backward()
            optimizer.step()
            
            epoch_loss += loss.item()
            progress.set_postfix({"loss": loss.item()})
            
        print(f"Epoch {epoch+1} average loss: {epoch_loss / len(dataloader):.4f}")
        
    # 6. Save LoRA Adapters (Just saving the state dict of the trainable params)
    print("Saving LoRA adapters...")
    os.makedirs("adapters", exist_ok=True)
    
    lora_state_dict = {k: v for k, v in model.state_dict().items() if "lora_" in k}
    torch.save(lora_state_dict, f"adapters/{args.domain}_lora.pt")
    
    print(f"Training complete. Adapters saved to adapters/{args.domain}_lora.pt")

if __name__ == "__main__":
    train()
