$models = @("system", "generative", "execution", "discovery", "analytical", "conversational")
$baseDir = "a:\temp_models\Cairn\ml-pipeline\gguf"

foreach ($model in $models) {
    $modelfile = "a:\temp_models\Modelfile.$model"
    $ggufPath = "$baseDir\cairn-$model-f16.gguf"
    
    # Create the Modelfile
    "FROM $ggufPath" | Out-File -FilePath $modelfile -Encoding utf8
    
    Write-Host "Loading cairn-$model into Ollama..."
    # Import into Ollama
    ollama create "cairn-$model" -f $modelfile
}

Write-Host "All models loaded successfully!"
