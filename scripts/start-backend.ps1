$ErrorActionPreference = 'Stop'

$databaseName = 'ledgerline_db'
$confirmedDatabase = Read-Host "Type $databaseName only after verifying it is your newly created, empty disposable schema"
if ($confirmedDatabase -cne $databaseName) {
    throw 'Database confirmation did not match. The backend was not started.'
}

$username = Read-Host 'Database username'
if ([string]::IsNullOrWhiteSpace($username)) {
    throw 'A database username is required.'
}

$securePassword = Read-Host 'Database password' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $securePassword).Password
if ([string]::IsNullOrEmpty($password)) {
    throw 'A database password is required.'
}

$keyBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$env:LEDGERLINE_DB_URL = "jdbc:mysql://localhost:3306/$databaseName`?serverTimezone=UTC"
$env:LEDGERLINE_DB_USERNAME = $username
$env:LEDGERLINE_DB_PASSWORD = $password
$env:LEDGERLINE_JWT_SECRET_BASE64 = [Convert]::ToBase64String($keyBytes)
$env:LEDGERLINE_JWT_ISSUER = 'ledgerline'
$env:LEDGERLINE_CORS_ALLOWED_ORIGINS = 'http://localhost:5173'

try {
    Set-Location $repositoryRoot
    & '.\backend\ledgerline\mvnw.cmd' '-f' 'backend\ledgerline\pom.xml' 'spring-boot:run'
    if ($LASTEXITCODE -ne 0) {
        throw "Backend exited with code $LASTEXITCODE."
    }
}
finally {
    Remove-Item Env:LEDGERLINE_DB_URL -ErrorAction SilentlyContinue
    Remove-Item Env:LEDGERLINE_DB_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:LEDGERLINE_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:LEDGERLINE_JWT_SECRET_BASE64 -ErrorAction SilentlyContinue
    Remove-Item Env:LEDGERLINE_JWT_ISSUER -ErrorAction SilentlyContinue
    Remove-Item Env:LEDGERLINE_CORS_ALLOWED_ORIGINS -ErrorAction SilentlyContinue
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
    $password = $null
    $securePassword.Dispose()
}