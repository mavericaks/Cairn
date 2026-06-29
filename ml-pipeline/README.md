# Cairn ML Pipeline — Custom LLM Fine-Tuning

This directory contains the complete pipeline to fine-tune Meta's Llama-3.2-1B 
on Cairn's 6 domain-specific datasets using LoRA (Low-Rank Adaptation), then 
export to GGUF format for local inference via Ollama.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Training Pipeline                      │
│                                                          │
│  HuggingFace Hub          Custom Architecture            │
│  (Llama-3.2-1B)    ──→    (model.py)                    │
│       │                       │                          │
│       ▼                       ▼                          │
│  loader.py            lora.py (inject LoRA)              │
│       │                       │                          │
│       └──────────┬────────────┘                          │
│                  ▼                                        │
│            train.py                                       │
│    (Fine-tune on domain JSONL data)                      │
│                  │                                        │
│                  ▼                                        │
│     adapters/{domain}_lora.pt                            │
│                  │                                        │
│                  ▼                                        │
│            export.py                                      │
│    (Merge LoRA → HF save_pretrained)                     │
│                  │                                        │
│                  ▼                                        │
│       exported_models/{domain}/                          │
│    (model.safetensors + config.json)                     │
│                  │                                        │
│                  ▼                                        │
│         export_to_gguf.sh                                │
│    (llama.cpp → Q4_K_M quantization)                     │
│                  │                                        │
│                  ▼                                        │
│     cairn-{domain}.gguf → Ollama                         │
└──────────────────────────────────────────────────────────┘
```

## Prerequisites

### 1. Hardware
- **GPU**: NVIDIA GPU with ≥4GB VRAM (GTX 1650 works with batch_size=1)
- **RAM**: ≥16GB system RAM
- **Disk**: ≥10GB free (for model weights)

### 2. Software
```bash
# Python 3.10+
python --version

# CUDA toolkit (must match your PyTorch CUDA version)
nvcc --version

# Ollama (for inference)
ollama --version
```

### 3. HuggingFace Token
```bash
# 1. Create account at https://huggingface.co
# 2. Go to https://huggingface.co/settings/tokens → Create token (read access)
# 3. Accept the Llama 3.2 license at:
#    https://huggingface.co/meta-llama/Llama-3.2-1B
# 4. Set the token:
export HF_TOKEN=hf_your_token_here
```

## Step-by-Step Guide

### Step 1: Install Dependencies

```bash
cd ml-pipeline
pip install -r requirements.txt
```

### Step 2: Verify GPU Access

```bash
python -c "import torch; print(f'CUDA available: {torch.cuda.is_available()}'); print(f'GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"None\"}')"
```

Expected output:
```
CUDA available: True
GPU: NVIDIA GeForce GTX 1650
```

### Step 3: Train a Domain-Specific LoRA Adapter

Train one domain at a time. Each takes ~10-30 minutes on a GTX 1650.

```bash
# Train the analytical domain (SQL generation)
python train.py \
  --domain analytical \
  --dataset data/dummy_analytical.jsonl \
  --epochs 3 \
  --batch_size 1 \
  --lr 2e-4

# Train the execution domain (tool calling)
python train.py \
  --domain execution \
  --dataset data/execution.jsonl \
  --epochs 3 \
  --batch_size 1 \
  --lr 2e-4

# Train the discovery domain (document Q&A)
python train.py \
  --domain discovery \
  --dataset data/discovery.jsonl \
  --epochs 3 \
  --batch_size 1 \
  --lr 2e-4
```

Output: `adapters/{domain}_lora.pt`

### Step 4: Export to HuggingFace Format

```bash
python export.py \
  --domain analytical \
  --adapter_path adapters/analytical_lora.pt
```

Output: `exported_models/analytical/` (contains `model.safetensors`, `config.json`, `tokenizer.json`)

### Step 5: Convert to GGUF for Ollama

```bash
# Clone llama.cpp (one-time setup)
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp && pip install -r requirements.txt && cd ..

# Convert and quantize
bash export_to_gguf.sh exported_models/analytical cairn-analytical
```

### Step 6: Register with Ollama

Create a Modelfile for the domain:

```bash
cat > Modelfile.analytical << 'EOF'
FROM ./cairn-analytical.gguf

PARAMETER temperature 0.7
PARAMETER num_predict 1024

SYSTEM """You are an expert data analyst AI assistant. You specialize in:
1. Writing SQL queries for PostgreSQL databases
2. Analyzing data trends and patterns
3. Performing statistical calculations
4. Creating data summaries and reports

When asked to write SQL, always wrap it in a ```sql code block.
Use CTEs for complex queries. Mention performance considerations."""
EOF

ollama create cairn-analytical -f Modelfile.analytical
```

### Step 7: Test the Model

```bash
ollama run cairn-analytical "Write a SQL query to find the top 5 customers by lifetime value"
```

## Quick Start (Without Fine-Tuning)

If you want to skip training and just use the base model with custom system prompts:

```bash
# Pull the base model
ollama pull llama3.2:1b

# Create domain-specific Modelfiles (system prompts only, no fine-tuning)
# These are in the ollama/ directory
ollama create cairn-analytical -f ollama/Modelfile.analytical
ollama create cairn-execution -f ollama/Modelfile.execution
ollama create cairn-discovery -f ollama/Modelfile.discovery
ollama create cairn-generative -f ollama/Modelfile.generative
ollama create cairn-conversational -f ollama/Modelfile.conversational
ollama create cairn-system -f ollama/Modelfile.system
```

## Training Datasets

| Domain | File | Examples | Focus |
|--------|------|----------|-------|
| Analytical | `data/dummy_analytical.jsonl` | 8 | SQL generation, data analysis |
| Execution | `data/execution.jsonl` | 7 | Tool calling, math, time |
| Discovery | `data/discovery.jsonl` | 3 | Document Q&A, knowledge search |
| Generative | `data/generative.jsonl` | 4 | Email writing, code generation |
| Conversational | `data/conversational.jsonl` | 4 | General explanations |
| System | `data/system.jsonl` | 4 | Platform help, capabilities |

### Dataset Format (JSONL)
```json
{"prompt": "User's question or request", "completion": "The ideal assistant response"}
```

### Expanding Datasets
To improve model quality, add more examples to each JSONL file. Aim for:
- **50+ examples** per domain for noticeable behavioral differences
- **Diverse prompts** covering edge cases and variations
- **High-quality completions** that match the style you want the model to produce

## File Descriptions

| File | Purpose |
|------|---------|
| `model.py` | Custom Llama-3 architecture (RMSNorm, RoPE, GQA, SwiGLU) |
| `loader.py` | Downloads base weights from HuggingFace, maps to custom model |
| `lora.py` | LoRA linear layer implementation, injection, and weight merging |
| `train.py` | Training loop with proper prompt masking (labels=-100 for prompt tokens) |
| `export.py` | Merges LoRA adapters and exports to HF format for llama.cpp |
| `export_to_gguf.sh` | Converts HF model → GGUF with Q4_K_M quantization |

## Key Technical Decisions

### Why LoRA instead of Full Fine-Tuning?
- A 1B parameter model has ~2GB of weights
- Full fine-tuning requires ~8GB+ VRAM (optimizer states + gradients)
- LoRA freezes 99.8% of weights, only training ~2M parameters
- Fits on a 4GB VRAM GPU (GTX 1650)

### Why Custom Architecture (model.py) instead of just HuggingFace?
- Demonstrates deep understanding of transformer internals
- Shows you can implement RoPE, GQA, RMSNorm, and SwiGLU from scratch
- Required knowledge for any ML engineering role

### Why Q4_K_M Quantization?
- Reduces model size from ~2GB → ~700MB
- Minimal quality loss (4-bit with importance-weighted groups)
- Fast inference on CPU+GPU with Ollama
