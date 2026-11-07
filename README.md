# LedgerLine

LedgerLine is an early financial transaction management application foundation. It includes a React dashboard, a Spring Boot API, a MySQL/Flyway schema, and Day 2 authentication. It is not ready to process or safeguard real money.

## Current status (2026-11-07)

Implemented and verified in code/tests:

- Java 25 and Spring Boot 4.0.9 backend with Maven, Spring Security, Spring Data JPA, BCrypt, JWT, Actuator, and Flyway dependencies.
- Environment-based MySQL/JWT configuration and Hibernate schema validation (`ddl-auto=validate`); Hibernate does not create or update the production schema.
- Flyway V1 core schema, V2 user-profile/status extension, and V3 transaction-table rename. Monetary amounts and balances use integer `BIGINT` paise.
- Public registration is restricted to `ACCOUNT_HOLDER`; login issues signed HS256 access tokens with configured issuer and expiry.
- Authenticated requests reload the user role and status from the database. `/api/auth/me` derives identity from the verified token subject.
- Stateless security rules, role restrictions, safe 401/403 responses, account-level authorization, and fail-closed unmatched API routes.
- Admin APIs for user listing/status management, zero-balance account creation, and permission assignment; admin changes are appended to audit events.
- Authorized account reads and read-only audit-event retrieval. The dashboard and Accounts/Audit pages use actual backend responses; account balances are kept as integer paise strings at the frontend boundary.
- Existing dashboard retained, with login, registration, profile, logout, role-filtered navigation, and an Axios bearer interceptor. Tokens are held only in JavaScript memory, never local/session storage.
- No invented transactions, balances, or API success responses. Statements and transfers remain non-operational; there is no money-movement API.

**Still not verified:** the MySQL migrations have not been applied to the configured MySQL server; no database credentials are set in this environment, and the backend has not been started against MySQL. Maven integration tests use an isolated H2 database and explicitly disable Flyway, so they do not prove MySQL compatibility or migration execution. The live health endpoint was separately exercised against a temporary H2-backed backend, not MySQL.

**Not implemented:** account-management UI, statements, transaction history APIs/execution, balanced double-entry validation, idempotency processing, locking/concurrency handling, transaction audit events, MFA, password reset, rate limiting, refresh tokens, and production secret/DB privilege management.

Successful compilation or authentication tests do not guarantee financial integrity. No money movement is enabled.

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

## Database and backend setup (PowerShell)

Create the `ledgerline_db` database and an application database user. Configure the database user with only the required application and Flyway privileges. Then, from the repository root, set the connection values and generate a random JWT key in the current PowerShell session:

```powershell
$env:LEDGERLINE_DB_URL = "jdbc:mysql://localhost:3306/ledgerline_db?serverTimezone=UTC"
$env:LEDGERLINE_DB_USERNAME = Read-Host "MySQL username"
$securePassword = Read-Host "MySQL password" -AsSecureString
$env:LEDGERLINE_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$keyBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:LEDGERLINE_JWT_SECRET_BASE64 = [Convert]::ToBase64String($keyBytes)
$env:LEDGERLINE_JWT_ISSUER = "ledgerline"
$env:LEDGERLINE_JWT_ACCESS_TOKEN_TTL = "PT15M"
$env:LEDGERLINE_CORS_ALLOWED_ORIGINS = "http://localhost:5173"
```

`backend/ledgerline/.env.example` lists the variable names but is not automatically loaded by Spring. Its credential and key values are placeholders; generate real local values as above and never commit them.

Start the backend from the repository root:

```powershell
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml spring-boot:run
```

Flyway applies V1 through V3 during startup. The database, DB username/password, and a valid Base64 JWT secret (at least 32 decoded bytes) are required. Startup intentionally does not fall back to default credentials or a weak signing key. Hibernate validates the migrated schema and does not update it.

For the first deployment only, enable the guarded administrator bootstrap before starting:

```powershell
$env:LEDGERLINE_ADMIN_BOOTSTRAP_ENABLED = "true"
$env:LEDGERLINE_ADMIN_BOOTSTRAP_FULL_NAME = "LedgerLine Administrator"
$env:LEDGERLINE_ADMIN_BOOTSTRAP_EMAIL = Read-Host "Initial administrator email"
$secureAdminPassword = Read-Host "Initial administrator password" -AsSecureString
$env:LEDGERLINE_ADMIN_BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new("", $secureAdminPassword).Password
```

The bootstrap requires a valid name/email and a password of at least 12 characters (at most 72 UTF-8 bytes), runs only when no ADMIN record exists, BCrypt-hashes the password, and records an `ADMIN_BOOTSTRAPPED` audit event. It is not an HTTP endpoint. After the first successful startup, disable it and clear the bootstrap password from the environment. Subsequent role changes require an authenticated ADMIN and are audited.

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
| `GET` | `/api/admin/users` | ADMIN | Returns safe user profiles. |
| `PATCH` | `/api/admin/users/{userId}/status` | ADMIN | Enables/disables users; blocks self-disable and disabling the last active admin. |
| `PATCH` | `/api/admin/users/{userId}/role` | ADMIN | Changes roles, prevents demoting the last active ADMIN, and writes an audit event. |
| `POST` | `/api/admin/accounts` | ADMIN | Creates an ACTIVE account with zero paise and assigns an active user as OWNER. |
| `PUT` | `/api/admin/accounts/{accountId}/permissions` | ADMIN | Creates/updates an account permission for an active user. |
| `GET` | `/api/audit/events?limit=50` | ADMIN, AUDITOR | Returns up to 100 newest audit events; read-only. |
| `GET` | `/actuator/health` | Public | Spring Actuator health; detailed component data remains hidden. |

`/api/v1/auth/{register,login,me}` remains as a compatibility alias for the original Day 1 identity path.

Passwords require at least 12 characters and are BCrypt-hashed. Because BCrypt processes at most 72 UTF-8 bytes, longer encoded passwords are rejected. User records and API responses never expose the password hash.

## Authorization behavior

- Only `ADMIN` can access `/api/admin/**` and mutate `/api/accounts/**`.
- `ADMIN` and `AUDITOR` can read `/api/audit/**`; all audit write methods are denied.
- `ACCOUNT_HOLDER` can write under `/api/transfers/**` and `/api/transactions/**`; auditors and admins cannot. No such controller or money movement operation is currently implemented.
- `ADMIN`, `ACCOUNT_HOLDER`, and `AUDITOR` may read transaction/ledger route prefixes, but those data controllers are not implemented. Account reads are scoped by `account_permissions`.
- Ledger mutation routes are denied. V1 includes database triggers preventing ordinary ledger-entry and audit-row updates/deletes.
- Unmatched API routes are denied by default.
- `AccountAuthorizationService` checks permissions against `account_permissions` using the authenticated subject. The account read endpoints invoke it. Any future account/transaction write service must use the same permission checks.
- Current user role and active status are looked up from the DB for each authenticated request, so disabling a user or changing their role takes effect without waiting for token expiry.
- Admin account/user-status/permission changes are written to `audit_events`; audit events are read-only through the API. Financial transaction events are not generated because transaction APIs do not exist.
- Public signup always chooses `ACCOUNT_HOLDER`. Administrator roles cannot be assigned through public signup; initial provisioning is gated by explicit one-time environment configuration, and subsequent role changes require an authenticated ADMIN and are audited.

## Database schema

V1 creates `users`, `accounts`, `account_permissions`, `ledger_transactions`, `ledger_entries`, `idempotency_keys`, and `audit_events`. V3 renames the transaction table to `transactions`; MySQL migration execution is still required to verify the resulting database schema. All monetary values (`balance_paise`, `amount_paise`) are signed integer `BIGINT` paise with constraints where applicable. V2 adds `full_name` and constrained `account_status` to `users`, preserving the existing `active` column for migration compatibility.

The schema includes primary/unique/foreign-key constraints, indexes, non-negative balance and positive posting checks, user-scoped idempotency uniqueness, and append-only triggers for ledger/audit rows. This schema alone does not guarantee balanced postings, safe concurrent balances, or end-to-end financial integrity.

## Tests and verification

Run from the repository root:

```powershell
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml test
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml compile
Set-Location frontend
npm run lint
npm run build
```

Backend integration tests use H2 with Flyway disabled; migration verification requires a configured MySQL instance and a successful application startup. Do not treat test-database validation as proof the MySQL migration ran.

Verification run on 2026-11-07:

- `.\mvnw.cmd test` — passed, 40 tests, 0 failures/errors/skips.
- `.\mvnw.cmd compile` — passed.
- `npm run lint` — passed.
- `npm run build` — passed.
- `GET /actuator/health` — HTTP 200 with `{"status":"UP"}` from a running H2-backed Spring Boot process; this is not a MySQL health check.
- Flyway V1–V3 on MySQL — not run. MySQL port reachability alone is not a connection or migration test.

## Known limitations and next work

1. Apply V1 through V3 on MySQL 8.0.16+ and confirm Hibernate validation plus a live health request.
2. Provision the first ADMIN through the gated bootstrap only in the intended environment; disable and clear its credentials afterward. Add a role-aware admin UI.
3. Implement transaction/statement APIs only after their service, ledger, concurrency, idempotency, and audit invariants are specified and tested.
4. Add account and transaction service integration tests against MySQL; retain account permission checks on every new account-scoped operation.
5. Before any money movement: design and independently test transaction-bound balance updates, paired ledger postings, user-scoped idempotency, rollback, concurrency, and audit controls. Do not enable transfers before these checks pass.
6. Add rate limiting/lockout controls, MFA, password reset/email verification, secret rotation/management, HTTPS/security headers, and operational monitoring.

See [THREATS.md](./THREATS.md) for current risks and [PROMPTS.md](./PROMPTS.md) for the dated AI-assisted development record.
