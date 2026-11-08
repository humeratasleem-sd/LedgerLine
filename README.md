# LedgerLine

LedgerLine is a development/demo account ledger built with Spring Boot, React, MySQL and Flyway. It is not a payment processor, bank, or production-ready financial system.

## Current status (2026-11-08)

Implemented in source and H2-backed automated tests:

- Java 25 / Spring Boot 4.0.9 API, Spring Security, JPA, BCrypt, signed JWTs, Actuator and Flyway.
- Environment-backed database and JWT configuration; Hibernate is `ddl-auto=validate`, Flyway clean is disabled.
- Flyway V1-V4 source: users/accounts/permissions, transactions, user-scoped idempotency, ledger entries, system clearing account, external settlement reservation, audit records and append-only triggers.
- Public registration is ACCOUNT_HOLDER-only; JWT signature, issuer and expiry are checked, and current role/status is reloaded from the database per request.
- Server-enforced role and account permission rules, safe 401/403 handling, ADMIN management and read-only AUDITOR views.
- Admin account provisioning, lifecycle and permission management; account-holder transfers; per-account statements.
- ADMIN-attested external settlement deposits/withdrawals only. These are manual ledger postings after independent verification outside LedgerLine; no bank/card/payment rail is called.
- Paise are positive integer `BIGINT` values. A successful operation changes customer balances and writes balanced paired ledger entries, idempotency state and audit record within a DB transaction. A refused business operation is stored as FAILED without ledger/balance changes.
- Responsive dashboard, account, transfer, statement and admin pages consume real API responses and show loading/error/empty states. No balances or success outcomes are fabricated.

**Database status: BLOCKED.** The current process has no MySQL credentials. Flyway V1-V4 have not been executed, and Hibernate validation or live MySQL health have not been verified. Tests use H2 with Flyway disabled and cannot certify MySQL DDL, row locking or trigger behavior.

**Browser status: PARTIAL.** Public pages and unauthenticated route protection can be tested locally. Authenticated browser/API flows require the backend and a verified disposable database. Do not report them as a pass until exercised with real API responses.

This project is not certified for real funds. External settlement attestation, environment/database security, MySQL migration verification, audit monitoring, backup/recovery, operational controls and independent accounting review remain risks.

## Technology

- Java 25, Spring Boot 4.0.9, Maven
- Spring Security, Spring Data JPA, BCrypt, Spring OAuth2 Resource Server/JWT
- MySQL 8.0.16+, Flyway
- React, Vite, React Router, Axios, Tailwind CSS
- JUnit 5, Spring test support, Mockito, H2 (test scope only)

## Prerequisites

- JDK 25
- MySQL 8.0.16 or later; MySQL must enforce `CHECK` constraints
- Node.js and npm
- A dedicated MySQL database and application user; do not use the MySQL root account

## Database safety and backend setup (Windows)

Never use an exposed root password. Do not paste credentials into chat or put them in Git. From Windows CMD, open the MySQL client; `-p` prompts for the current root password without putting it in the command:

```cmd
mysql -u root -p
```

In the interactive MySQL prompt, inspect the root host and rotate that account; the server generates a fresh password locally. Create a database user with privileges scoped to `ledgerline_db` (not `*.*`):

```sql
SELECT user, host FROM mysql.user WHERE user = 'root';
ALTER USER 'root'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
CREATE DATABASE IF NOT EXISTS ledgerline_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'ledgerline_app'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, TRIGGER
    ON ledgerline_db.* TO 'ledgerline_app'@'localhost';
SELECT COUNT(*) AS existing_objects
    FROM information_schema.tables WHERE table_schema = 'ledgerline_db';
```

`IDENTIFIED BY RANDOM PASSWORD` returns a generated secret in the local client. Store it in a password manager. If the `root` account host is not `localhost`, use the exact host returned by the first query. If the existing `ledgerline_db` schema contains any objects, stop: do not migrate it or drop/reset it. Proceed only when you have created/verified a newly disposable, empty target. No MySQL migration has been run by this session.

From CMD, use a separate PowerShell process to prompt locally for the dedicated DB credentials (the password prompt is hidden) and generate a random JWT key for this process. No secret is included as a command argument:

```powershell
powershell -NoProfile
```

In that PowerShell window:

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

The database, username/password and JWT key are required; there are no credential fallbacks. Flyway applies V1-V4 at startup and Hibernate validates the result without creating/updating tables. First confirm MySQL version and that the exact disposable `ledgerline_db` is empty. After startup, inspect `flyway_schema_history`, expected tables/triggers and `/actuator/health`. Stop if any target/schema ambiguity exists.

Alternatively, after independently verifying that the target is a newly created, empty disposable `ledgerline_db`, start the helper from the repository root. It prompts for the dedicated database user/password, asks you to confirm the schema name, generates a process-only JWT key, and clears its environment variables when Maven exits:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

The confirmation is an operator check, not an automatic database emptiness check. Do not run this helper against an existing or production schema.

MySQL DDL is not generally transactional across all statements in a migration. If a migration fails, do not assume Flyway rolled back every earlier DDL statement. Stop the app, inspect the disposable schema and `flyway_schema_history`, and restore/recreate only that disposable schema before retrying. Do not run `flyway clean` or destructive cleanup against production. A failed-migration recovery test has not yet been run in this environment.

For the first deployment only, enable the guarded administrator bootstrap before starting:

```powershell
$env:LEDGERLINE_ADMIN_BOOTSTRAP_ENABLED = "true"
$env:LEDGERLINE_ADMIN_BOOTSTRAP_FULL_NAME = "LedgerLine Administrator"
$env:LEDGERLINE_ADMIN_BOOTSTRAP_EMAIL = Read-Host "Initial administrator email"
$secureAdminPassword = Read-Host "Initial administrator password" -AsSecureString
$env:LEDGERLINE_ADMIN_BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new("", $secureAdminPassword).Password
```

The bootstrap bean is absent by default; it is activated only when `LEDGERLINE_ADMIN_BOOTSTRAP_ENABLED=true` is explicitly supplied. It requires a valid name/email and a password of at least 12 characters (at most 72 UTF-8 bytes), creates an administrator only when no ADMIN record exists, BCrypt-hashes the password, and records an `ADMIN_BOOTSTRAPPED` audit event. An existing ADMIN makes it a no-op; an invalid configuration or an email already assigned to a non-admin stops startup. It is not an HTTP endpoint, and the bootstrap code does not log the supplied name, email, or password.

After the first successful startup, stop the application and remove the bootstrap settings from the provisioning shell/service configuration. For the current PowerShell session:

```powershell
$env:LEDGERLINE_ADMIN_BOOTSTRAP_ENABLED = "false"
Remove-Item Env:LEDGERLINE_ADMIN_BOOTSTRAP_FULL_NAME -ErrorAction SilentlyContinue
Remove-Item Env:LEDGERLINE_ADMIN_BOOTSTRAP_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:LEDGERLINE_ADMIN_BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue
```

In the service manager or deployment secret store, likewise set the flag to `false` or remove it and delete the bootstrap credential values before restarting. The default is disabled if the flag is absent. Subsequent role changes require an authenticated ADMIN and are audited.

Check the health endpoint at `http://localhost:8080/actuator/health`. Health details are hidden. The endpoint is public; application API endpoints are not.

## Frontend setup

In another PowerShell terminal:

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
npm ci
npm run dev
```

`VITE_API_BASE_URL` defaults to `http://localhost:8080`; override it in `frontend/.env.local` if needed. Set `LEDGERLINE_CORS_ALLOWED_ORIGINS` on the backend to the exact frontend origin. The Vite `.env.local` file is ignored by Git.

The browser keeps the short-lived bearer token in memory only. Closing or reloading the page signs the user out; the client does not persist credentials or tokens in browser storage. Sign-out clears the local token but cannot revoke a copied token server-side; it remains usable until expiry. This avoids persistent browser token storage but does not protect an active session from successful same-origin script injection.

## Authentication API

All request/response bodies are JSON. Authentication failures return a generic `401`; missing/invalid bearer authentication returns `401`; role failures return `403`.

| Method | Path | Access | Behavior |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Validates full name, email, and password; creates an active `ACCOUNT_HOLDER`; returns a bearer token and safe profile. Supplied roles are not accepted. |
| `POST` | `/api/auth/login` | Public | Validates credentials and active status; returns a signed bearer token and safe profile. |
| `GET` | `/api/auth/me` | Bearer token | Returns the authenticated profile using the token subject, never a caller-supplied user ID. |
| `GET` | `/api/accounts` | Any authenticated role | Lists only accounts with an `account_permissions` row for the token's user. |
| `GET` | `/api/accounts/{accountId}` | Any authenticated role | Requires account permission before returning account details. |
| `GET` | `/api/accounts/{accountId}/transactions?limit=50` | Any authenticated role | Scoped statement including completed/refused transaction records; limit 1-100. |
| `GET` | `/api/admin/users` | ADMIN | Returns safe user profiles. |
| `GET` | `/api/admin/accounts` | ADMIN | Lists all account records for management. |
| `PATCH` | `/api/admin/users/{userId}/status` | ADMIN | Enables/disables users; blocks self-disable and disabling the last active admin. |
| `PATCH` | `/api/admin/users/{userId}/role` | ADMIN | Changes roles, prevents demoting the last active ADMIN, and writes an audit event. |
| `POST` | `/api/admin/accounts` | ADMIN | Creates an ACTIVE account with zero paise and assigns an active user as OWNER. |
| `PATCH` | `/api/admin/accounts/{accountId}/status` | ADMIN | Freezes/activates/closes accounts; closing requires zero balance. |
| `PUT` | `/api/admin/accounts/{accountId}/permissions` | ADMIN | Creates/updates an account permission for an active user. |
| `POST` | `/api/transactions/transfers` | ACCOUNT_HOLDER | Requires source OWNER/TRANSFER permission and `Idempotency-Key`; balanced journal, row locks and atomic update. |
| `POST` | `/api/admin/transactions/deposits` | ADMIN | Manual posting for externally verified settlement only; unique external reference required. |
| `POST` | `/api/admin/transactions/withdrawals` | ADMIN | Manual posting for externally verified settlement only; source balance enforced. |
| `GET` | `/api/audit/events?limit=50` | ADMIN, AUDITOR | Returns up to 100 newest audit events; read-only. |
| `GET` | `/actuator/health` | Public | Spring Actuator health; detailed component data remains hidden. |

`/api/v1/auth/{register,login,me}` remains as a compatibility alias for the original Day 1 identity path.

Passwords require at least 12 characters and are BCrypt-hashed. Because BCrypt processes at most 72 UTF-8 bytes, longer encoded passwords are rejected. User records and API responses never expose the password hash.

## Authorization behavior

- Only `ADMIN` can access `/api/admin/**` and mutate `/api/accounts/**`.
- `ADMIN` and `AUDITOR` can read `/api/audit/**`; all audit write methods are denied. Admins/auditors may view all account statements; account holders require matching account permission.
- `ACCOUNT_HOLDER` can transfer under `/api/transactions/transfers`; admin/auditor roles cannot. Transfers require OWNER/TRANSFER permission on the source account.
- ADMIN-only deposit/withdrawal routes record externally verified settlement and require an explicit boolean attestation plus external reference; they do not process external payments.
- Transfer requests require a printable ASCII `Idempotency-Key` (8-255 characters). Same key and request returns the prior outcome; a changed request conflicts. Successful external settlement references are reserved once.
- Account lifecycle and permission changes serialize on the account row to avoid racing a posting.
- V1/V4 triggers are intended to prevent ordinary ledger, audit, transaction and external-settlement history updates/deletes. Trigger execution still requires real MySQL verification.
- Unmatched API routes are denied by default.
- `AccountAuthorizationService` checks permissions against `account_permissions` using the authenticated subject. Financial writes lock account rows and check source permission on the server.
- Current user role and active status are looked up from the DB for each authenticated request, so disabling a user or changing their role takes effect without waiting for token expiry.
- Admin account/user-status/permission changes and financial completed/refused outcomes are written to `audit_events`; audit events are read-only through the API.
- Public signup always chooses `ACCOUNT_HOLDER`. Administrator roles cannot be assigned through public signup; initial provisioning is gated by explicit one-time environment configuration, and subsequent role changes require an authenticated ADMIN and are audited.

## Database schema

V1 creates `users`, `accounts`, `account_permissions`, `ledger_transactions`, `ledger_entries`, `idempotency_keys`, and `audit_events`. V3 renames the transaction table to `transactions`; V4 adds financial posting metadata and system settlement tracking. MySQL migration execution is still required to verify the resulting schema. Monetary values (`balance_paise`, `amount_paise`) are integer `BIGINT` paise.

Each successful operation creates two equal opposite journal rows; the service verifies two-row count and debit/credit totals before commit. User-scoped idempotency keys, ascending account row locks, transactional audit and balance updates, and append-only triggers are implemented in source. H2 tests do not prove MySQL-specific locking/triggers. Existing balances are not automatically reconciled to opening ledger entries; this is a demo ledger, not proof of complete financial integrity.

## Tests and verification

Run from the repository root:

```powershell
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml test
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml compile
Set-Location frontend
npm run lint
npm test
npm run build
```

Backend integration tests use H2 with Flyway disabled; migration verification requires a configured MySQL instance and a successful application startup. Do not treat test-database validation as proof the MySQL migration ran.

Latest verification evidence:

- Backend test/package counts and frontend build/lint/test results are recorded in [DAY3_DAY7_PROGRESS.md](./DAY3_DAY7_PROGRESS.md) after the latest run.
- H2 tests explicitly disable Flyway; they cover business/API behavior but not MySQL migration execution.
- MySQL V1-V4, Hibernate validation and MySQL health are **BLOCKED** until a verified empty disposable database and local credentials are available.
- Authenticated browser login/register and role-specific UI interactions are **PARTIAL** until a real backend is available; no mocked API success is claimed.

## Known limitations and next work

1. Verify and migrate a new empty disposable `ledgerline_db` with V1-V4, inspect triggers/history, then validate Hibernate and MySQL health.
2. Run authenticated browser workflows against that backend with disposable users for each role.
3. Before any real-fund use: independently review posting semantics, settlement operations, opening balance reconciliation, MySQL isolation, backup/recovery, database privileges, audit monitoring and regulatory requirements.
4. Add rate limiting, MFA, password recovery/email verification, key rotation, HTTPS/security headers, operational monitoring and external tamper evidence.

See [DAY3_DAY7_PROGRESS.md](./DAY3_DAY7_PROGRESS.md), [documentation/API.md](./documentation/API.md), [documentation/DATABASE_AND_ARCHITECTURE.md](./documentation/DATABASE_AND_ARCHITECTURE.md), [documentation/DEMO.md](./documentation/DEMO.md), [THREATS.md](./THREATS.md) and [PROMPTS.md](./PROMPTS.md).
