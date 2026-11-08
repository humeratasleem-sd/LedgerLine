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

## Entry 3 — Day 2 authentication implementation

### Date and request
2026-11-07. Asked to inspect the existing LedgerLine application, preserve its dashboard, finish and verify Day 1 where possible, and implement real registration, login, JWT authentication, role authorization, account permission checks, frontend authentication, automated tests, and current documentation. Money movement was explicitly excluded.

### Generated changes
- Added a profile/status-aware `User` entity; DTOs; BCrypt registration/login; HS256 access-token issuance; token expiry; a DB-backed role/status lookup on authenticated requests; safe profile responses; 401/403 handling; and route authorization.
- Added admin user/status, zero-balance account creation, and permission-management APIs; scoped account reads; read-only audit events; and same-transaction audit records for admin changes. No money movement was added.
- Added reusable account permission checks, a V2 migration for user profile/status fields, test-scoped H2 and MVC testing dependencies, and integration/unit tests.
- Added in-memory React auth context, sign-in/sign-up/profile pages, protected routes, role-filtered navigation, logout and Axios 401/403 handling. Connected account balances and administrative audit records to real APIs; balance paise are returned/displayed as integer strings. Statements and transactions remain unimplemented.
- Reworked root setup, threat, and AI-development documentation and added a backend environment-variable example with placeholders.

### Corrections made during verification
- First compilation of the new MVC test failed because the Spring Boot 4 modular MVC test starter was not on the classpath. Added `spring-boot-starter-webmvc-test`.
- Initial integration-test scaffolding depended on a test-slice security configuration that did not construct `HttpSecurity` and initially used unnecessary Mockito stubs. Replaced that test approach with a real Spring Boot context against H2 (Flyway explicitly disabled), removed unnecessary stubs, and fixed the account-permission varargs assertion.
- Frontend lint identified an HMR rule violation from exporting a hook alongside the auth provider. Split the context and hook exports into separate modules.
- Tightened default route behavior to deny unmatched API paths and limited account mutations to ADMIN, rather than relying on authentication alone.
- The first account-creation integration test found that H2 returned both the generated ID and created timestamp through a generic generated-keys request. Changed the insert to request only the `id` key; the admin API integration test then passed.
- Review found the account permission helper was not yet called by a real resource handler. Added account list/detail endpoints and tests proving users cannot read another user's account.

### Verification
- `.\mvnw.cmd test` — passed, 33 tests, 0 failures/errors/skips. Integration tests exercise actual registration, duplicate-email rejection, BCrypt storage, login, current profile, valid/expired/invalid JWT handling, role restrictions, admin account creation and audit, user status, account permission assignment/scope, registration role elevation rejection, and H2 health endpoint behavior. Flyway is disabled in this test context.
- `.\mvnw.cmd compile` — passed.
- `npm run lint` — passed.
- `npm run build` — passed.
- The database migration was not run on MySQL, and a live MySQL-backed startup/health check is not claimed: environment DB credentials are unset. The test H2 context does not verify the MySQL Flyway migration.
- No Git commit was created; therefore no commit link is available.

### Remaining
Provision and connect a MySQL test database, apply both migrations, validate Hibernate schema and live health, and add an admin UI plus transaction/statement APIs only after their financial invariants are specified. Authentication tests do not establish financial integrity or readiness for real funds.

## Entry 4 — Day 1/Day 2 complete audit and corrections

### Date and request
2026-11-07. Requested a full itemized audit of Day 1 and Day 2, correction of implementable gaps, actual reruns, an audit report, and no Day 3 or money movement.

### Initial findings
- Audited the full tracked project layout, current uncommitted changes, Maven/npm manifests, security configuration, schema/migrations, documentation, Java 25/Maven/Node versions, Git repository and origin.
- Found that the initial schema used `ledger_transactions` instead of the required `transactions` table name.
- There was no supported initial ADMIN provisioning process or protected role-change endpoint.
- There were no endpoint validation tests; the user/account/role and frontend documentation/test totals needed an updated checklist.
- MySQL80 was running and TCP 3306 accepted a connection, but DB username/password and JWT key environment variables were absent. This does not verify database authentication, Flyway, or live backend health.

### Changes produced
- Added forward-only Flyway V3 to rename `ledger_transactions` to `transactions`, retaining the existing V1 migration history.
- Added an opt-in one-time startup ADMIN bootstrap configured through environment variables only (no public bootstrap endpoint), BCrypt storage, validation, an audit record, and tests. Added ADMIN-only role change, audited and guarded against demoting the last active administrator.
- Added invalid registration-field tests and expanded admin/bootstrap tests. Updated dashboard state messaging so its data badge reflects authorized, absent, loading, or unavailable account data.
- Added this comprehensive 99-item audit report with initial and final statuses and evidence, refreshed setup/threat/prompt records, and kept all financial movement out of scope.

### Actual corrections during this audit
- The first `AdminBootstrapTest` run found a Mockito verification that mixed a matcher with a raw argument. Replaced the raw argument with `eq(...)`; the rerun passed.
- Tried to load the optional `agent-browser` CLI workflow; the executable is not installed. No browser CLI dependency was added. UI behavior is therefore recorded only to the level actually covered by frontend builds and backend integration tests; do not claim a browser E2E run.

### Verification
- `.\mvnw.cmd compile` — passed.
- `.\mvnw.cmd test` — passed, 40 tests, 0 failures/errors/skips. The test application uses H2 and disables Flyway; it does not verify MySQL migrations.
- `npm run lint` — passed.
- `npm run build` — passed.
- The final MySQL-backed startup/migration/health check remains blocked because DB credentials are not configured. The schema/migrations are inspectable source, not a successful migration result.
- Commit [`a87ed00ba99d68a50b3bf9250d003bb641ae392e`](https://github.com/humeratasleem-sd/LedgerLine/commit/a87ed00ba99d68a50b3bf9250d003bb641ae392e) was pushed to `main`; the remote branch was checked with `git ls-remote`.

### Remaining
Configure a disposable MySQL database and credentials, run Flyway V1–V3 and Hibernate validation, and obtain a live health response. Review/perform the protected first-admin bootstrap only in the intended environment and clear its bootstrap password immediately afterward. No money movement was added.

## Entry 5 — Closing verifiable Day 1/Day 2 partials

### Date and request
2026-11-07. Asked to verify MySQL migrations on a disposable DB, browser-test frontend authentication/authorization, validate the initial-admin bootstrap, review auth/security, update every audit status, and rerun checks without starting Day 3.

### Findings and corrections
- Confirmed MySQL Community Server 8.0.42 was installed/running and port 3306 answered, but DB username/password and JWT key were unset. Did not guess credentials or touch any database. Reviewed V1–V3 order, MySQL-oriented DDL, table rename, foreign keys, indexes, constraints, and BIGINT paise statically; explicitly did not call static review a migration run.
- Noticed the prior final audit counted an H2-backed health response as PASS for the requested MySQL health check. Corrected D1-21 to PARTIAL and updated totals and evidence.
- The prior audit grouped final statuses into ranges. Expanded the final matrix to one row per each of the 99 requirements and added exact manual MySQL/browser verification steps and MySQL DDL failure-recovery caveats.
- Inspected bootstrap conditions and added `AdminBootstrapConditionTest` loading the real `application.properties`; the default-disabled condition passed. Updated README with the operator shutdown steps and confirmation that bootstrap values are not logged.
- Opened the actual Vite app with integrated Playwright: login and registration pages rendered, an empty login submit made zero `/api/auth/login` requests, unauthenticated `/accounts`, `/audit`, `/transfers`, `/statements`, and `/profile` redirected to `/login`, and public `/register` remained after refresh. A real login/register request and role-specific authenticated flows could not be tested because the backend cannot connect to MySQL here; no API responses were mocked.
- Requested the dedicated security-review agent to inspect authentication and authorization. It reported no vulnerabilities; this is not a deployment certification.

### Verification
- Backend `.\mvnw.cmd compile` — passed.
- Backend `.\mvnw.cmd test` — passed, **41 tests, 0 failures/errors/skips**. Integration tests use H2 and disable Flyway; they do not prove MySQL compatibility.
- `npm run lint` and `npm run build` — passed.
- Playwright guest-page checks passed as listed above. Authenticated browser/API flows remain blocked pending MySQL credentials.
- Flyway migration execution, Hibernate validation against MySQL, MySQL-backed `/actuator/health`, and failure/repair simulation remain **not run**. No database was altered.
- Commit [`98a3bc2eefd6a2504ca08ff6789b7224b025af74`](https://github.com/humeratasleem-sd/LedgerLine/commit/98a3bc2eefd6a2504ca08ff6789b7224b025af74) was pushed to `main`; the remote head was verified with `git ls-remote`.

### Remaining
Provide a disposable MySQL database with scoped credentials, then run V1–V3, inspect the migrated schema/history, verify Hibernate and health, and simulate a failed migration only on a disposable copy. Run authenticated browser checks against that backend using actual test users for each role. No money movement or Day 3 work was added.

## Entry 6 — LedgerLine Days 1–7 completion request

### Date and request
2026-11-08. Requested completion of the existing seven-day LedgerLine project, implementation of secure ledger/account/statement flows, tests and documentation, safe local MySQL setup guidance, verification, and a reviewed commit/push to the existing `main` branch. Requested that unavailable MySQL/browser evidence be explicitly marked partial or blocked and that no credentials be shared.

### Work produced
- Inspected existing source, configuration, migration history, tests, audit and project documentation. No authoritative original seven-day schedule was found; documented a reconstructed schedule and its assumptions instead of claiming it was original.
- Added transaction posting, account statements, account lifecycle changes, scoped idempotency, external settlement references, balanced journal entries and append-only migration controls, with H2 integration coverage.
- Added API, architecture/database and demo documentation; updated README, Day 1/2 audit, progress, and threat records to distinguish source/H2 evidence from unverified MySQL behavior.
- No MySQL credentials were available to the process. No database was contacted or changed; no secret was requested or recorded.
- Backend/frontend run results and MySQL/browser limitations are recorded in the verification documents. Commit/push status is reported in the final response after Git operations. No AI history beyond this actual request and work is asserted here.

### Verification corrections and results
- The first Maven package run exposed that `permissionExists` had accidentally been nested in `lockAccount`; moved it to class scope and reran package successfully.
- The first frontend utility test expected western digit grouping for an `en-IN` INR formatter. Corrected its expected Indian grouping rather than changing the formatter.
- Review of retry behavior showed the transfer page generated a new idempotency key after an uncertain network/server failure. Added request-fingerprint-based key reuse for unchanged retries, with a frontend utility test; changed request details receive a new key.
- Final Maven/frontend, browser, MySQL and Git results are summarized in `DAY3_DAY7_PROGRESS.md` and the audit follow-up. MySQL remains unverified and no database was accessed.
