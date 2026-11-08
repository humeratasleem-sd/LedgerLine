# LedgerLine local demo

This is a development demo, not a production or real-money system. Database migrations must run only against a verified, newly created and empty `ledgerline_db`; do not use a production or populated database.

## Prerequisites

- JDK 25, MySQL 8.0.16+, Node.js/npm.
- A local `ledgerline_db` schema confirmed empty and a dedicated `ledgerline_app` user granted access only to that schema.
- A generated Base64 JWT key with at least 32 decoded random bytes.

## Windows CMD start

Start MySQL using the Windows Services app or an administrator-managed service. From CMD at the repository root, start a PowerShell process that prompts locally for DB credentials (the password prompt is hidden) and generates a signing key. Do not copy secrets into chat or a command-line literal:

```cmd
powershell -NoProfile
```

In the PowerShell window:

```powershell
$env:LEDGERLINE_DB_URL = "jdbc:mysql://localhost:3306/ledgerline_db?serverTimezone=UTC"
$env:LEDGERLINE_DB_USERNAME = Read-Host "Database username"
$secret = Read-Host "Database password" -AsSecureString
$env:LEDGERLINE_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $secret).Password
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:LEDGERLINE_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$env:LEDGERLINE_JWT_ISSUER = "ledgerline"
$env:LEDGERLINE_CORS_ALLOWED_ORIGINS = "http://localhost:5173"
Set-Location C:\Users\syedt\Documents\LedgerLine
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml spring-boot:run
```

Stop the backend and close this PowerShell window to clear these process-scoped values.

In a second CMD window:

```cmd
cd /d C:\Users\syedt\Documents\LedgerLine\frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Registration creates an ACCOUNT_HOLDER only. An ADMIN must be provisioned through the disabled-by-default operator bootstrap before admin pages are available. Never expose bootstrap credentials or leave bootstrap enabled after the first successful startup.

## Safe demo sequence

1. Register a disposable account-holder user.
2. Provision an ADMIN out of band; no public admin signup exists.
3. Admin creates a zero-balance INR customer account and assigns ownership.
4. If an actual external receipt has been checked outside LedgerLine, the admin may record that settlement with a unique external reference and attestation. This feature does not initiate a payment.
5. Account holder transfers integer-paise amounts between eligible authorized INR accounts.
6. Review transaction status and statement rows; refused attempts are present without fake balances.
7. Admin/AUDITOR inspect read-only audit events. AUDITOR cannot post or alter records.

## Verification commands

```cmd
cd /d C:\Users\syedt\Documents\LedgerLine\backend\ledgerline
mvnw.cmd test
mvnw.cmd package
cd /d C:\Users\syedt\Documents\LedgerLine\frontend
npm run lint
npm run build
```

Maven tests use H2 and do not prove MySQL migration correctness. Before a MySQL demo, verify exact schema emptiness locally, start the app, inspect all applied Flyway versions, observe Hibernate validation success and request `/actuator/health`. If credentials or proof of emptiness are missing, do not start the app against that schema.