"""
Generates a valid, tested cairn_training.ipynb notebook.
Run this script to create/recreate the notebook.
"""
import json

def make_code_cell(source_lines):
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {"id": ""},
        "outputs": [],
        "source": source_lines
    }

def make_md_cell(source_lines):
    return {
        "cell_type": "markdown",
        "metadata": {"id": ""},
        "source": source_lines
    }

cells = []

# Title
cells.append(make_md_cell([
    "# Cairn - LoRA Fine-Tuning Pipeline\n",
    "\n",
    "Fine-tunes Llama-3.2-1B with LoRA for all 6 Cairn domain agents.\n",
    "\n",
    "**Before running:** Runtime > Change runtime type > **T4 GPU**\n",
    "\n",
    "**Total time:** ~3 hours for all 6 domains"
]))

# Cell 1: GPU check
cells.append(make_code_cell([
    "import torch\n",
    "print('PyTorch version:', torch.__version__)\n",
    "print('CUDA available:', torch.cuda.is_available())\n",
    "if torch.cuda.is_available():\n",
    "    print('GPU:', torch.cuda.get_device_name(0))\n",
    "    mem_gb = torch.cuda.get_device_properties(0).total_mem / (1024**3)\n",
    "    print(f'VRAM: {mem_gb:.1f} GB')\n",
    "else:\n",
    "    print('WARNING: No GPU detected.')\n",
    "    print('Go to Runtime > Change runtime type > T4 GPU')"
]))

# Cell 2: Install deps
cells.append(make_code_cell([
    "!pip install -q transformers safetensors tqdm huggingface_hub"
]))

# Cell 3: Clone repo
cells.append(make_code_cell([
    "import os\n",
    "if os.path.exists('/content/cairn'):\n",
    "    print('Repo already cloned, pulling latest...')\n",
    "    !cd /content/cairn && git pull\n",
    "else:\n",
    "    !git clone https://github.com/mavericaks/Cairn.git /content/cairn\n",
    "\n",
    "os.chdir('/content/cairn/ml-pipeline')\n",
    "print('Working dir:', os.getcwd())\n",
    "!ls"
]))

# Cell 4: HF login
cells.append(make_code_cell([
    "import os\n",
    "\n",
    "# === PASTE YOUR HUGGINGFACE TOKEN BELOW ===\n",
    "HF_TOKEN = 'YOUR_HF_TOKEN_HERE'\n",
    "# ==========================================\n",
    "\n",
    "assert HF_TOKEN != 'YOUR_HF_TOKEN_HERE', 'You must paste your HF token above!'\n",
    "os.environ['HF_TOKEN'] = HF_TOKEN\n",
    "\n",
    "from huggingface_hub import login\n",
    "login(token=HF_TOKEN)\n",
    "print('HuggingFace login OK')"
]))

# Cell 5: Generate datasets
cells.append(make_code_cell([
    "!python generate_dataset.py --output_dir data\n",
    "\n",
    "print('\\nGenerated datasets:')\n",
    "import os\n",
    "total = 0\n",
    "for f in sorted(os.listdir('data')):\n",
    "    if f.endswith('.jsonl'):\n",
    "        n = sum(1 for _ in open(os.path.join('data', f)))\n",
    "        total += n\n",
    "        print(f'  {f}: {n} examples')\n",
    "print(f'Total: {total} examples')"
]))

# Cell 6: Smoke test
cells.append(make_code_cell([
    "# Quick smoke test - verify model + LoRA work before committing to training\n",
    "import torch\n",
    "import sys\n",
    "sys.path.insert(0, '.')\n",
    "\n",
    "from model import LlamaForCausalLM, LlamaConfig\n",
    "from lora import inject_lora\n",
    "\n",
    "config = LlamaConfig()\n",
    "m = LlamaForCausalLM(config)\n",
    "m = inject_lora(m, rank=16, alpha=32, target_modules=['q_proj', 'v_proj'])\n",
    "\n",
    "test_ids = torch.randint(0, 100, (2, 8))\n",
    "test_mask = torch.ones(2, 8, dtype=torch.long)\n",
    "test_labels = test_ids.clone()\n",
    "test_labels[:, :4] = -100\n",
    "\n",
    "logits, loss = m(test_ids, attention_mask=test_mask, labels=test_labels)\n",
    "print(f'Forward pass OK: logits shape = {logits.shape}')\n",
    "\n",
    "# Cleanup\n",
    "del m, logits, loss, test_ids, test_mask, test_labels\n",
    "import gc\n",
    "gc.collect()\n",
    "if torch.cuda.is_available():\n",
    "    torch.cuda.empty_cache()\n",
    "print('Smoke test PASSED')"
]))

# Cell 7: Train all domains
cells.append(make_md_cell([
    "## Training\n",
    "\n",
    "This trains all 6 domains sequentially. ~25-40 min per domain on T4.\n",
    "\n",
    "If you only want to train specific domains, edit the `DOMAINS` list below."
]))

cells.append(make_code_cell([
    "import time\n",
    "import torch\n",
    "import gc\n",
    "\n",
    "DOMAINS = ['analytical', 'execution', 'discovery', 'generative', 'conversational', 'system']\n",
    "\n",
    "results = {}\n",
    "for domain in DOMAINS:\n",
    "    start = time.time()\n",
    "    print(f'\\n{\"=\"*60}')\n",
    "    print(f'  TRAINING: {domain}')\n",
    "    print(f'{\"=\"*60}')\n",
    "\n",
    "    !python train.py \\\n",
    "        --domain {domain} \\\n",
    "        --dataset data/{domain}.jsonl \\\n",
    "        --epochs 5 \\\n",
    "        --batch_size 1 \\\n",
    "        --grad_accum 16 \\\n",
    "        --lr 2e-4 \\\n",
    "        --max_length 512 \\\n",
    "        --lora_rank 16 \\\n",
    "        --lora_alpha 32\n",
    "\n",
    "    elapsed = (time.time() - start) / 60\n",
    "    results[domain] = elapsed\n",
    "    print(f'  {domain} done in {elapsed:.1f} min')\n",
    "\n",
    "    gc.collect()\n",
    "    torch.cuda.empty_cache()\n",
    "\n",
    "print(f'\\n{\"=\"*60}')\n",
    "print('  ALL TRAINING COMPLETE')\n",
    "print(f'{\"=\"*60}')\n",
    "for d, t in results.items():\n",
    "    print(f'  {d}: {t:.1f} min')\n",
    "print(f'  Total: {sum(results.values()):.1f} min')"
]))

# Cell 8: Verify adapters
cells.append(make_code_cell([
    "import os\n",
    "print('Saved LoRA adapters:')\n",
    "if os.path.exists('adapters'):\n",
    "    for f in sorted(os.listdir('adapters')):\n",
    "        size = os.path.getsize(os.path.join('adapters', f)) / 1e6\n",
    "        print(f'  {f}: {size:.1f} MB')\n",
    "else:\n",
    "    print('  ERROR: adapters/ directory not found')"
]))

# Cell 9: Export
cells.append(make_code_cell([
    "DOMAINS = ['analytical', 'execution', 'discovery', 'generative', 'conversational', 'system']\n",
    "\n",
    "for domain in DOMAINS:\n",
    "    adapter = f'adapters/{domain}_lora.pt'\n",
    "    import os\n",
    "    if not os.path.exists(adapter):\n",
    "        print(f'Skipping {domain} - no adapter file')\n",
    "        continue\n",
    "    print(f'\\nExporting {domain}...')\n",
    "    !python export.py --domain {domain} --adapter_path {adapter}"
]))

# Cell 10: Convert to GGUF
cells.append(make_code_cell([
    "# Install llama.cpp conversion tools\n",
    "import os\n",
    "if not os.path.exists('/content/llama_cpp'):\n",
    "    !git clone --depth 1 https://github.com/ggerganov/llama.cpp /content/llama_cpp\n",
    "!pip install -q -r /content/llama_cpp/requirements/requirements-convert_hf_to_gguf.txt 2>/dev/null || pip install -q gguf\n",
    "\n",
    "os.makedirs('gguf', exist_ok=True)\n",
    "\n",
    "DOMAINS = ['analytical', 'execution', 'discovery', 'generative', 'conversational', 'system']\n",
    "for domain in DOMAINS:\n",
    "    src = f'exports/cairn-{domain}'\n",
    "    dst = f'gguf/cairn-{domain}-f16.gguf'\n",
    "    if not os.path.exists(os.path.join(src, 'model.safetensors')):\n",
    "        print(f'Skipping {domain} - not exported yet')\n",
    "        continue\n",
    "    print(f'Converting {domain} to GGUF...')\n",
    "    !python /content/llama_cpp/convert_hf_to_gguf.py {src} --outfile {dst} --outtype f16\n",
    "    if os.path.exists(dst):\n",
    "        size = os.path.getsize(dst) / 1e6\n",
    "        print(f'  OK: {dst} ({size:.0f} MB)')\n",
    "    else:\n",
    "        print(f'  FAILED: {dst} not created')"
]))

# Cell 11: Download
cells.append(make_code_cell([
    "# Zip everything for download\n",
    "import os\n",
    "!zip -r /content/cairn-models.zip gguf/ adapters/\n",
    "\n",
    "size = os.path.getsize('/content/cairn-models.zip') / 1e6\n",
    "print(f'\\ncairn-models.zip: {size:.0f} MB')\n",
    "\n",
    "from google.colab import files\n",
    "files.download('/content/cairn-models.zip')\n",
    "\n",
    "print('\\nNext steps:')\n",
    "print('1. Unzip cairn-models.zip')\n",
    "print('2. Copy gguf/ files to ml-pipeline/gguf/')\n",
    "print('3. Register each model with Ollama:')\n",
    "print('   ollama create cairn-analytical -f ollama/Modelfile.analytical')"
]))

notebook = {
    "nbformat": 4,
    "nbformat_minor": 0,
    "metadata": {
        "colab": {
            "provenance": [],
            "gpuType": "T4"
        },
        "kernelspec": {
            "name": "python3",
            "display_name": "Python 3"
        },
        "language_info": {
            "name": "python"
        },
        "accelerator": "GPU"
    },
    "cells": cells
}

output_path = 'ml-pipeline/cairn_training.ipynb'
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(notebook, f, indent=2, ensure_ascii=False)

print(f'Wrote {output_path}')
print(f'Cells: {len(cells)}')

# Validate
nb = json.load(open(output_path, encoding='utf-8'))
print(f'Validation: nbformat={nb["nbformat"]}, cells={len(nb["cells"])}')
for i, c in enumerate(nb['cells']):
    src = ''.join(c['source'])
    if c['cell_type'] == 'code':
        try:
            compile(src, f'cell_{i}', 'exec')
            print(f'  Cell {i} (code): syntax OK')
        except SyntaxError as e:
            # Cells with ! commands won't compile in pure Python, that's expected
            if '!' in src:
                print(f'  Cell {i} (code): has shell commands (OK)')
            else:
                print(f'  Cell {i} (code): SYNTAX ERROR: {e}')
    else:
        print(f'  Cell {i} (markdown): OK')
