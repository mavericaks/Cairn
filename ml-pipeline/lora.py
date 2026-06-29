import torch
import torch.nn as nn
import math

class LoRALinear(nn.Module):
    """
    Wraps a standard nn.Linear layer and injects LoRA A and B matrices.
    W_new = W_old + (B @ A) * (alpha / rank)
    """
    def __init__(self, linear_layer, rank=16, alpha=32, dropout=0.05):
        super().__init__()
        self.in_features = linear_layer.in_features
        self.out_features = linear_layer.out_features
        
        # Keep original weights frozen
        self.weight = linear_layer.weight
        self.weight.requires_grad = False
        
        # Bias (Llama generally uses bias=False, but we keep it generic)
        self.bias = linear_layer.bias
        if self.bias is not None:
            self.bias.requires_grad = False
            
        self.rank = rank
        self.alpha = alpha
        self.scaling = alpha / rank
        
        # LoRA matrices
        self.lora_A = nn.Parameter(torch.zeros((rank, self.in_features)))
        self.lora_B = nn.Parameter(torch.zeros((self.out_features, rank)))
        self.dropout = nn.Dropout(p=dropout) if dropout > 0. else nn.Identity()
        
        self.reset_parameters()
        
    def reset_parameters(self):
        # A is initialized with Kaiming uniform (like standard linear)
        nn.init.kaiming_uniform_(self.lora_A, a=math.sqrt(5))
        # B is initialized to zero, so initially B @ A is 0 and the model behaves exactly as base
        nn.init.zeros_(self.lora_B)
        
    def forward(self, x):
        # Standard forward
        result = nn.functional.linear(x, self.weight, self.bias)
        # LoRA forward: x @ A.T @ B.T
        lora_out = self.dropout(x)
        lora_out = nn.functional.linear(lora_out, self.lora_A)
        lora_out = nn.functional.linear(lora_out, self.lora_B)
        
        # Combine
        return result + (lora_out * self.scaling)
        
    def merge(self):
        """Merges B @ A into the frozen weight matrix for export."""
        if self.weight.requires_grad:
            return # Already merged or trainable base weights
            
        # W_merged = W + (B @ A) * scaling
        delta_w = (self.lora_B @ self.lora_A) * self.scaling
        self.weight.data += delta_w.to(self.weight.dtype)
        
        # Prevent double merging
        self.lora_A = None
        self.lora_B = None

def inject_lora(model, rank=16, alpha=32, target_modules=["q_proj", "v_proj"]):
    """
    Recursively replaces target linear layers with LoRALinear layers.
    Also ensures all other parameters in the model are frozen.
    """
    print(f"Injecting LoRA (rank={rank}, alpha={alpha}) into {target_modules}...")
    
    # First, freeze all parameters
    for param in model.parameters():
        param.requires_grad = False
        
    modules_replaced = 0
    
    # Helper to recursively replace
    def _replace_recursive(module, prefix=""):
        nonlocal modules_replaced
        for name, child in module.named_children():
            full_name = f"{prefix}.{name}" if prefix else name
            
            # If this is a leaf node we want to replace
            if isinstance(child, nn.Linear) and any(target in name for target in target_modules):
                setattr(module, name, LoRALinear(child, rank=rank, alpha=alpha))
                modules_replaced += 1
            else:
                # Recurse
                _replace_recursive(child, full_name)
                
    _replace_recursive(model)
    print(f"Injected LoRA into {modules_replaced} modules.")
    
    # Count trainable parameters
    trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)
    total = sum(p.numel() for p in model.parameters())
    print(f"Trainable Parameters: {trainable:,} ({100 * trainable / total:.2f}% of total)")
    
    return model

def merge_lora_weights(model):
    """Recursively merges all LoRALinear layers in the model back to base weights."""
    print("Merging LoRA weights back into base model...")
    modules_merged = 0
    
    def _merge_recursive(module):
        nonlocal modules_merged
        for name, child in module.named_children():
            if isinstance(child, LoRALinear):
                child.merge()
                # Replace the LoRALinear with a standard nn.Linear containing the merged weights
                standard_linear = nn.Linear(child.in_features, child.out_features, bias=child.bias is not None)
                standard_linear.weight = child.weight
                if child.bias is not None:
                    standard_linear.bias = child.bias
                setattr(module, name, standard_linear)
                modules_merged += 1
            else:
                _merge_recursive(child)
                
    _merge_recursive(model)
    print(f"Merged {modules_merged} LoRA modules.")
    return model
