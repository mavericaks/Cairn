# Load environment variables from .env file and start Spring Boot
# Usage: .\run-local.ps1

Write-Host "Loading .env file..." -ForegroundColor Cyan
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        Write-Host "  Set $name" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "Starting Cairn..." -ForegroundColor Green
Write-Host "  Ollama:   $env:OLLAMA_BASE_URL" -ForegroundColor DarkGray
Write-Host "  Model:    $env:OLLAMA_MODEL" -ForegroundColor DarkGray
Write-Host "  GitHub:   OAuth App $($env:GITHUB_CLIENT_ID.Substring(0,8))..." -ForegroundColor DarkGray
Write-Host ""

./mvnw spring-boot:run
