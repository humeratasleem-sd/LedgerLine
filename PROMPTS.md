# AI-assisted development record

## Entry 1 — LedgerLine Day 1 foundation

### Request
Establish a modest, modular Java 25 / Spring Boot 4.x and React/Vite foundation for LedgerLine, including environment-based MySQL configuration, Flyway schema, authentication foundations, a professional dashboard shell, initial tests, setup instructions, and explicit financial/security caveats. Do not implement the complete application or claim unverified guarantees.

### Existing state inspected
- Spring Boot parent was set to `4.2.0-RC2`; the application entry point and Java feature files were largely empty.
- `application.properties` contained hardcoded local database credentials.
- React was still the Vite starter; the API client and proposed routes/components were empty.
- Existing documentation described planned financial guarantees as if they were product features.

### Work produced
- Switched Maven to the stable Spring Boot `4.0.9` line and wired the Spring application.
- Added JWT resource-server validation, BCrypt configuration, a protected current-user endpoint, role enum, basic user persistence, and validation error mapping. There is deliberately no login/token issuance or user provisioning.
- Added Flyway V1 core schema and paise-based integer amount columns, scoped idempotency uniqueness, row constraints, and ordinary update/delete blocking triggers for ledger/audit rows.
- Replaced starter frontend with the design brief's routed, responsive dashboard foundation; added a Vite API URL example, ignored local env file, and removed empty, unused starter pages/components.
- Added root setup and threat documentation and replaced aspirational security claims with current limits.

### Verification
- Backend: `.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml package` — passed; 6 tests, 0 failures/errors.
- Maven logged a non-blocking HTTP 401 while probing optional Flyway community metadata; dependency resolution and packaging completed successfully.
- Frontend: `npm run lint` and `npm run build` — initially reported passed by the frontend implementation agent; also rerun successfully during final verification below.
- Independent dashboard design evaluation: PASS; desktop, tablet, and mobile screenshots were captured.
- The Flyway migration was not applied to the running local MySQL service; no database credentials were supplied for a migration check. No live endpoint check is claimed.

## Entry 2 — Day 1 final verification

### Findings and fixes
- Confirmed backend configuration requires externally supplied MySQL credentials and a valid JWT signing key; these secrets are intentionally not defaulted.
- Aligned the default JDBC URL with the requested `ledgerline_db` name.
- Set Axios's safe local development fallback to `http://localhost:8080`, retaining `VITE_API_BASE_URL` as the override. The health indicator calls the real public `/actuator/health` endpoint and does not synthesize success.
- Reviewed the migration: `users`, `accounts`, `account_permissions`, `ledger_transactions`, `ledger_entries`, `idempotency_keys`, and `audit_events` are present; paise amounts and balances use `BIGINT`.
- Confirmed role names, BCrypt bean, HS256 JWT decoder, required claim checks, and stateless authenticated API configuration. Login/token issuance, role-specific route authorization, and account-level permission enforcement remain unimplemented.

### Verification run
- `.\mvnw.cmd test` — passed; 6 tests, 0 failures/errors.
- `.\mvnw.cmd package` — passed; 6 tests, 0 failures/errors.
- From `frontend`: `npm run lint` and `npm run build` — both passed.
- Java 25.0.2 and Maven 3.10.0 were detected; the MySQL Windows service was running and port 3306 reachable.
- Ports 8080 and 5173 were not listening during the check. The health request failed to connect. Database credentials and `LEDGERLINE_JWT_SECRET_BASE64` were absent; a startup attempt failed during Flyway connection because the unresolved DB username placeholder was passed to MySQL. The migration was not applied. Migration validity is therefore a source review only, not a database execution result.

Day 1 is **not yet fully verified or ready to mark complete** until the configured database migration and live backend/frontend health connection are checked. None of these results certify the application for handling money.
