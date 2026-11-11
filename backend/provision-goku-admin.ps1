$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

if (-not (Test-Path '.env')) {
    throw 'backend\.env is missing. Run setup-local.ps1 first to configure the database.'
}

$adminEmail = Read-Host 'Admin login email [akshatgarg1260@gmail.com]'
if ([string]::IsNullOrWhiteSpace($adminEmail)) {
    $adminEmail = 'akshatgarg1260@gmail.com'
}
if ($adminEmail -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') {
    throw 'Enter a valid admin email. No accounts were changed.'
}

$confirmation = Read-Host 'This will activate Goku as the sole admin and deactivate all other admin accounts. Type PROVISION GOKU to continue'
if ($confirmation -cne 'PROVISION GOKU') {
    Write-Output 'Provisioning cancelled; no accounts were changed.'
    exit 1
}

$securePassword = Read-Host 'Enter Goku''s new admin password (12-128 characters)' -AsSecureString
if ($securePassword.Length -lt 12 -or $securePassword.Length -gt 128) {
    $securePassword.Dispose()
    throw 'Password must be between 12 and 128 characters. No accounts were changed.'
}

$passwordPointer = [IntPtr]::Zero
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $env:BOOTSTRAP_SYSTEM_ADMIN_EMAIL = $adminEmail.Trim().ToLowerInvariant()
    $env:BOOTSTRAP_SYSTEM_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $env:BOOTSTRAP_SYSTEM_ADMIN_FIRST_NAME = 'Goku'
    $env:BOOTSTRAP_SYSTEM_ADMIN_LAST_NAME = 'Admin'
    $env:BOOTSTRAP_SYSTEM_ADMIN_REPLACE_EXISTING = 'true'
    $env:APP_BOOTSTRAP_EXIT_AFTER = 'true'
    $env:SPRING_MAIN_WEB_APPLICATION_TYPE = 'none'

    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Admin provisioning failed (Maven exit code $LASTEXITCODE). Review the error above; no success is assumed."
    }

    Write-Output 'Provisioning completed. Goku is the active system administrator; other admin accounts are inactive.'
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $securePassword.Dispose()
    Remove-Item Env:BOOTSTRAP_SYSTEM_ADMIN_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:BOOTSTRAP_SYSTEM_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:BOOTSTRAP_SYSTEM_ADMIN_FIRST_NAME -ErrorAction SilentlyContinue
    Remove-Item Env:BOOTSTRAP_SYSTEM_ADMIN_LAST_NAME -ErrorAction SilentlyContinue
    Remove-Item Env:BOOTSTRAP_SYSTEM_ADMIN_REPLACE_EXISTING -ErrorAction SilentlyContinue
    Remove-Item Env:APP_BOOTSTRAP_EXIT_AFTER -ErrorAction SilentlyContinue
    Remove-Item Env:SPRING_MAIN_WEB_APPLICATION_TYPE -ErrorAction SilentlyContinue
}
