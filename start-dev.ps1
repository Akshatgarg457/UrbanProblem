$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'

if (-not (Test-Path (Join-Path $backend '.env'))) {
    throw 'backend/.env is missing. Run .\setup-local.ps1 first.'
}
if (-not (Test-Path (Join-Path $frontend '.env.local'))) {
    throw 'frontend/.env.local is missing. Run .\setup-local.ps1 first.'
}
if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
    throw 'Frontend dependencies are missing. Run .\setup-local.ps1 or cd frontend; npm ci.'
}

$mysql = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if ($mysql -and $mysql.Status -ne 'Running') {
    Start-Service -Name 'MySQL80'
    Write-Host 'Started MySQL80.' -ForegroundColor Green
} elseif ($mysql) {
    Write-Host 'MySQL80 is already running.' -ForegroundColor Green
} else {
    Write-Warning 'MySQL80 service was not found. Start your configured MySQL service manually.'
}

$backendCommand = "Set-Location -LiteralPath '$backend'; .\mvnw.cmd spring-boot:run"
$frontendCommand = "Set-Location -LiteralPath '$frontend'; npm run dev"
Start-Process powershell -ArgumentList @('-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $backendCommand)
Start-Process powershell -ArgumentList @('-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand)
Write-Host 'Backend: http://localhost:8080' -ForegroundColor Cyan
Write-Host 'Frontend: http://localhost:5173' -ForegroundColor Cyan
