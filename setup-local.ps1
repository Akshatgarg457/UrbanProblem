$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$backendEnv = Join-Path $backendDir '.env'
$frontendEnv = Join-Path $frontendDir '.env.local'

function Convert-SecureStringToPlainText([Security.SecureString]$SecureValue) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

Write-Host 'CommonGround local setup' -ForegroundColor Cyan
Write-Host 'This creates ignored local environment files. Secrets are not added to source control.' -ForegroundColor Gray

if (-not (Test-Path $backendEnv)) {
    $dbUser = Read-Host 'MySQL username [root]'
    if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'root' }
    $dbPasswordSecure = Read-Host 'MySQL password for database upr' -AsSecureString
    $dbPassword = Convert-SecureStringToPlainText $dbPasswordSecure

    $adminEmail = Read-Host 'Initial SYSTEM_ADMIN email (leave blank if one already exists)'
    $adminPassword = ''
    if (-not [string]::IsNullOrWhiteSpace($adminEmail)) {
        $adminPasswordSecure = Read-Host 'Initial SYSTEM_ADMIN password (minimum 12 characters)' -AsSecureString
        $adminPassword = Convert-SecureStringToPlainText $adminPasswordSecure
        if ($adminPassword.Length -lt 12) { throw 'SYSTEM_ADMIN password must be at least 12 characters.' }
    }

    # Escape backslashes because Spring imports this file as Java-style .properties.
    $dbPassword = $dbPassword.Replace('\', '\\')
    $adminPassword = $adminPassword.Replace('\', '\\')
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $backendLines = @(
        'DB_URL=jdbc:mysql://localhost:3306/upr',
        "DB_USERNAME=$dbUser",
        "DB_PASSWORD=$dbPassword",
        "BOOTSTRAP_SYSTEM_ADMIN_EMAIL=$adminEmail",
        "BOOTSTRAP_SYSTEM_ADMIN_PASSWORD=$adminPassword",
        'CORS_ALLOWED_ORIGINS=http://localhost:5173',
        'SPRING_JPA_HIBERNATE_DDL_AUTO=validate'
    )
    [System.IO.File]::WriteAllLines($backendEnv, $backendLines, $utf8NoBom)
    Write-Host 'Created backend/.env.' -ForegroundColor Green
} else {
    Write-Host 'backend/.env already exists; leaving it unchanged.' -ForegroundColor Yellow
}

if (-not (Test-Path $frontendEnv)) {
    $contactEmail = Read-Host 'Contact email for Nominatim geocoding (optional, recommended for public server use)'
    $frontendLines = @(
        'VITE_API_BASE_URL=http://localhost:8080/api',
        'VITE_OSM_TILE_URL=https://tile.openstreetmap.org/{z}/{x}/{y}.png',
        'VITE_NOMINATIM_BASE_URL=https://nominatim.openstreetmap.org',
        "VITE_NOMINATIM_EMAIL=$contactEmail"
    )
    [System.IO.File]::WriteAllLines($frontendEnv, $frontendLines, $utf8NoBom)
    Write-Host 'Created frontend/.env.local.' -ForegroundColor Green
} else {
    Write-Host 'frontend/.env.local already exists; leaving it unchanged.' -ForegroundColor Yellow
}

if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Write-Host 'Installing frontend dependencies with npm ci...' -ForegroundColor Cyan
    Push-Location $frontendDir
    try { npm ci } finally { Pop-Location }
}

Write-Host ''
Write-Host 'Setup complete. Check that MySQL has the upr database and its schema, then run .\start-dev.ps1.' -ForegroundColor Green
