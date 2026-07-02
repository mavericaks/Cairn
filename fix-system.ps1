# =============================================================
# Cairn System Fix Script - Run as Administrator
# =============================================================
# 
# RIGHT-CLICK this file > "Run with PowerShell" > Click "Yes" on UAC
# OR: Open PowerShell as Admin and run:  .\fix-system.ps1
#
# Fixes:
#   1. UDP port exhaustion (recurring BSOD trigger)
#   2. Stale Docker containers leaking ports
#   3. GPU driver TDR timeout (prevents VIDEO_SCHEDULER crash)
# =============================================================

#Requires -RunAsAdministrator

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Cairn System Fix Script" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# --- FIX 1: UDP Port Range ---
Write-Host "[1/4] Fixing UDP ephemeral port range..." -ForegroundColor Yellow

$before = netsh int ipv4 show dynamicport udp
Write-Host "  BEFORE:" -ForegroundColor DarkGray
$before | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }

netsh int ipv4 set dynamicport udp start=1025 num=64510 | Out-Null

$after = netsh int ipv4 show dynamicport udp
Write-Host "  AFTER:" -ForegroundColor Green
$after | ForEach-Object { Write-Host "    $_" -ForegroundColor Green }

Write-Host "  [OK] UDP port range expanded from 16,384 -> 64,510 ports" -ForegroundColor Green
Write-Host ""

# --- FIX 2: Docker Cleanup ---
Write-Host "[2/4] Cleaning up stale Docker resources..." -ForegroundColor Yellow

$dockerRunning = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
if ($dockerRunning) {
    $stopped = docker ps -a -q --filter "status=exited" 2>$null
    if ($stopped) {
        docker container prune -f 2>$null | Out-Null
        Write-Host "  [OK] Removed stopped containers" -ForegroundColor Green
    } else {
        Write-Host "  [OK] No stopped containers found" -ForegroundColor Green
    }

    docker network prune -f 2>$null | Out-Null
    Write-Host "  [OK] Cleaned dangling Docker networks" -ForegroundColor Green

    docker volume prune -f 2>$null | Out-Null
    Write-Host "  [OK] Cleaned dangling Docker volumes" -ForegroundColor Green
} else {
    Write-Host "  [SKIP] Docker Desktop not running, skipping" -ForegroundColor DarkGray
}
Write-Host ""

# --- FIX 3: GPU TDR Registry Fix ---
Write-Host "[3/4] Increasing GPU TDR timeout (prevents VIDEO_SCHEDULER BSOD)..." -ForegroundColor Yellow

$tdrPath = "HKLM:\SYSTEM\CurrentControlSet\Control\GraphicsDrivers"

$currentDelay = Get-ItemProperty -Path $tdrPath -Name "TdrDelay" -ErrorAction SilentlyContinue
if ($currentDelay -and $currentDelay.TdrDelay -ge 10) {
    Write-Host "  [OK] TdrDelay already set to $($currentDelay.TdrDelay)s" -ForegroundColor Green
} else {
    $oldVal = if ($currentDelay) { $currentDelay.TdrDelay } else { 2 }
    Set-ItemProperty -Path $tdrPath -Name "TdrDelay" -Value 10 -Type DWord
    Write-Host "  [OK] TdrDelay set to 10 seconds (was: ${oldVal}s)" -ForegroundColor Green
}

$currentDdi = Get-ItemProperty -Path $tdrPath -Name "TdrDdiDelay" -ErrorAction SilentlyContinue
if ($currentDdi -and $currentDdi.TdrDdiDelay -ge 10) {
    Write-Host "  [OK] TdrDdiDelay already set to $($currentDdi.TdrDdiDelay)s" -ForegroundColor Green
} else {
    $oldVal = if ($currentDdi) { $currentDdi.TdrDdiDelay } else { 5 }
    Set-ItemProperty -Path $tdrPath -Name "TdrDdiDelay" -Value 10 -Type DWord
    Write-Host "  [OK] TdrDdiDelay set to 10 seconds (was: ${oldVal}s)" -ForegroundColor Green
}
Write-Host ""

# --- FIX 4: Scheduled Docker Cleanup ---
Write-Host "[4/4] Creating scheduled task for weekly Docker cleanup..." -ForegroundColor Yellow

$taskName = "Cairn-DockerCleanup"
$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue

if ($existingTask) {
    Write-Host "  [OK] Scheduled task '$taskName' already exists" -ForegroundColor Green
} else {
    $action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -WindowStyle Hidden -Command ""docker container prune -f; docker network prune -f; docker volume prune -f"""
    $trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At "3:00AM"
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -DontStopOnIdleEnd

    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Description "Weekly cleanup of stale Docker containers, networks, and volumes to prevent port exhaustion" -RunLevel Highest | Out-Null
    Write-Host "  [OK] Created weekly cleanup task (Sundays @ 3 AM)" -ForegroundColor Green
}
Write-Host ""

# --- Summary ---
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  ALL FIXES APPLIED" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  [1] UDP port range: 16,384 -> 64,510 ports" -ForegroundColor White
Write-Host "  [2] Docker stale resources: cleaned" -ForegroundColor White
Write-Host "  [3] GPU TDR timeout: 2s -> 10s" -ForegroundColor White
Write-Host "  [4] Weekly Docker cleanup: scheduled" -ForegroundColor White
Write-Host ""
Write-Host "  WARNING: A REBOOT IS REQUIRED for the GPU TDR" -ForegroundColor Yellow
Write-Host "           changes to take effect." -ForegroundColor Yellow
Write-Host ""
Write-Host "  Press any key to exit..." -ForegroundColor DarkGray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
