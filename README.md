# LedgerLine

LedgerLine is a Day 1 foundation for a financial transaction management system. It establishes the project structure, database schema, security boundary, and frontend shell; it is **not ready to process or safeguard real money**.

## Status and scope

Implemented:

- Java 25, Spring Boot 4.0.9, Maven, Spring Security, Spring Data JPA, MySQL 8, and Flyway.
- Environment-based database credentials and required JWT verification key.
- Flyway V1 schema for users, accounts, account permissions, transactions, ledger entries, idempotency keys, and audit events.
- BCrypt password-encoder bean; HS256 JWT validation with issuer and timestamp validation; authenticated `GET /api/v1/auth/me`.
- Public Spring Actuator `GET /actuator/health`.
- React/Vite dashboard shell and API client (see frontend setup below).
- Starter unit tests for JWT validation/role mapping, authentication response mapping, and BCrypt hashing.

Not implemented: login/token issuance, registration or user provisioning, account APIs, permission enforcement on account operations, transaction execution, balanced double-entry validation, idempotent request processing, balance locking, or production auditing. The UI displays no invented account data and does not execute financial actions.

## Final verification status

Verified on 2026-11-06:

- Backend `test` and `package` both succeeded with 6 tests, 0 failures, and 0 errors. Java 25.0.2 and Maven 3.10.0 are installed; the Maven project targets Spring Boot 4.0.9.
- Frontend `npm run lint` and `npm run build` succeeded.
- MySQL is listening on port 3306, but the LedgerLine DB username/password and JWT key are not configured in this environment. A startup attempt failed when Flyway tried to connect using the unresolved username placeholder; Flyway V1 was **not** applied or runtime-validated.
- At verification time, ports 8080 and 5173 were not listening, and `GET http://localhost:8080/actuator/health` could not connect. The frontend correctly reports this unavailable backend; no successful health response is claimed.

The migration defines `users`, `accounts`, `account_permissions`, `ledger_transactions`, `ledger_entries`, `idempotency_keys`, and `audit_events`. All monetary columns (`balance_paise`, `amount_paise`) are `BIGINT` integer paise. Migration syntax and constraints were inspected but must still be applied to a configured MySQL 8.0.16+ database before database verification is complete.

## Architecture

Backend source is under `backend/ledgerline/src/main/java/com/ledgerline/`:

- `config`: stateless API security, JWT validation, BCrypt, and CORS.
- `controller`: authenticated identity endpoint.
- `service`: current-user response mapping.
- `repository`, `entity`, `enums`: initial user persistence model and role names.
- `dto`, `exception`: API response and validation-error foundations.

The other financial tables are defined in `backend/ledgerline/src/main/resources/db/migration/V1__create_ledgerline_core_tables.sql`. Hibernate schema generation is disabled; Hibernate validates the schema after Flyway runs.

## Prerequisites

- JDK 25
- MySQL 8.0.16 or later (CHECK constraints must be enforced)
- Node.js and npm

Create the `ledgerline_db` database and a dedicated development database user in MySQL. The user must have the privileges needed to run Flyway's initial DDL migration. Do not use the MySQL root account for the application.

## Run the backend (PowerShell)

From the project root, set credentials and a random 32-byte JWT verification key in the current PowerShell session:

```powershell
$env:LEDGERLINE_DB_URL = "jdbc:mysql://localhost:3306/ledgerline_db?serverTimezone=UTC"
$env:LEDGERLINE_DB_USERNAME = Read-Host "MySQL username"
$securePassword = Read-Host "MySQL password" -AsSecureString
$env:LEDGERLINE_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$keyBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:LEDGERLINE_JWT_SECRET_BASE64 = [Convert]::ToBase64String($keyBytes)
$env:LEDGERLINE_JWT_ISSUER = "ledgerline"
```

Then run:

```powershell
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml spring-boot:run
```

Flyway applies V1 on startup. The database, username, password, and JWT secret are required; startup intentionally fails rather than substituting credentials or a weak secret. The JWT key must be shared securely with the trusted token issuer. Tokens accepted by this service must use HS256, have the configured `iss`, a required valid `exp`, valid `nbf` when present, a `sub`, and a `roles` array containing role names (`ADMIN`, `ACCOUNT_HOLDER`, or `AUDITOR`).

Check health at `http://localhost:8080/actuator/health`. `GET /api/v1/auth/me` requires a valid bearer token.

## Run the frontend

In another PowerShell terminal:

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

The default `VITE_API_BASE_URL` points to `http://localhost:8080`, including when `.env.local` has not been created. Change it in `frontend/.env.local` if the backend uses another origin. Set `LEDGERLINE_CORS_ALLOWED_ORIGINS` on the backend if the frontend is served from a different origin.

## Build and tests

```powershell
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml test
.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml package
Set-Location frontend
npm run lint
npm run build
```

Backend tests are unit tests and do not require a running database. A successful build or test run does not demonstrate financial correctness, complete authorization, or production readiness. See [THREATS.md](./THREATS.md) for known gaps and [PROMPTS.md](./PROMPTS.md) for the implementation record.
