#!/bin/bash
# 
# This script demonstrates how we would convert the exported HuggingFace format 
# merged model into a 4-bit quantized GGUF file for Ollama.
#
# Prerequisites:
# 1. Clone llama.cpp repository: git clone https://github.com/ggerganov/llama.cpp
# 2. Install requirements: pip install -r llama.cpp/requirements.txt
# 3. Build llama.cpp (make) to get the quantize tool

DOMAIN=$1

if [ -z "$DOMAIN" ]; then
    echo "Usage: ./export_to_gguf.sh <domain_name>"
    echo "Example: ./export_to_gguf.sh analytical"
    exit 1
fi

HF_MODEL_DIR="exported_models/${DOMAIN}"
GGUF_OUT="exported_models/${DOMAIN}.f16.gguf"
Q4_OUT="exported_models/${DOMAIN}.q4_k_m.gguf"

if [ ! -d "$HF_MODEL_DIR" ]; then
    echo "Error: Directory $HF_MODEL_DIR not found. Did you run export.py?"
    exit 1
fi

echo "1. Converting HuggingFace format to F16 GGUF..."
# Using python script from llama.cpp
python3 llama.cpp/convert_hf_to_gguf.py ${HF_MODEL_DIR} --outfile ${GGUF_OUT} --outtype f16

echo "2. Quantizing F16 GGUF to 4-bit (q4_k_m)..."
# Using compiled quantize binary from llama.cpp
./llama.cpp/llama-quantize ${GGUF_OUT} ${Q4_OUT} q4_K_M

echo "3. Cleaning up intermediate F16 file..."
rm ${GGUF_OUT}

echo "Success! Final model ready for Ollama: ${Q4_OUT}"
echo ""
echo "To add to Ollama, create a Modelfile:"
echo "FROM ./${Q4_OUT}"
echo "PARAMETER temperature 0"
echo "SYSTEM \"You are the Cairn ${DOMAIN} agent...\""
echo ""
echo "Then run: ollama create cairn-${DOMAIN} -f Modelfile"
