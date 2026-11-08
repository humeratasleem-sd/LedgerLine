# LedgerLine: Days 3-7 progress

Date: 2026-11-08

## Plan source and assumptions

The repository does not contain an original seven-day schedule. This schedule is an explicit reconstruction from the project brief and existing architecture; it is not represented as the original author-provided plan.

| Day | Reconstructed scope |
|---|---|
| 1 | Spring Boot/React foundation, MySQL schema, environment configuration, base UI. |
| 2 | Registration/login, BCrypt, JWT, roles, account authorization, protected routes. |
| 3 | Account directory, account lifecycle, administrative provisioning and permissions. |
| 4 | Deposit/withdrawal posting only after an administrator attests that external settlement was actually verified. |
| 5 | Account-to-account transfer, double-entry journal, atomic balance changes, idempotency and row locking. |
| 6 | Scoped statements, immutable transaction and audit history, auditor read-only access. |
| 7 | Responsive user/admin surfaces, integration tests, API/schema/demo/security documentation and release checks. |

## Status

Day 1 and Day 2 source and H2 automated behavior are implemented. Their final status remains **PARTIAL** because Flyway V1-V4, Hibernate validation, live health and authenticated browser/API flows have not been verified against a confirmed disposable MySQL database.

| Day | Implementation status | Evidence / remaining verification |
|---|---|---|
| 1 | Implemented in source; MySQL deployment PARTIAL | Existing application configuration and V1-V4 migration sources; actual MySQL execution remains blocked pending local disposable DB credentials and emptiness verification. |
| 2 | Implemented in source and H2 integration tests; end-to-end PARTIAL | Existing auth/role/account authorization tests pass. Authenticated browser/API flows require the running application and verified DB. |
| 3 | Implemented in source | Admin account list/create, account lifecycle (ACTIVE/FROZEN/CLOSED), permission assignment, lock-serialized permission changes, account owner scoping and responsive account/admin interfaces. H2 endpoint coverage and full suite result are recorded after final verification below. |
| 4 | Implemented with a deliberately narrow settlement boundary | ADMIN-only deposit/withdrawal endpoints require a server-validated external-settlement attestation and unique external reference. These record postings only; LedgerLine does not initiate or confirm bank/card payments. |
| 5 | Implemented in source and H2 integration tests | Account-holder transfer endpoint; paired ledger rows; account row locks in ascending ID order; nonnegative paise balances; transaction, balance, ledger, idempotency and audit changes share one DB transaction. Concurrent withdrawal, duplicate request and rollback cases are tested. |
| 6 | Implemented in source and H2 integration tests | Account statements include completed and refused transactions and require account-view access; auditors can read but cannot post; transaction and audit table triggers are defined in V4. MySQL trigger execution remains unverified. |
| 7 | Implemented in source/docs; release verification PARTIAL | Dashboard, account, transfer, statement and admin pages consume real API endpoints and surface loading/empty/error results. Maven, frontend, browser and database evidence is summarized below; no production readiness is claimed. |

## Financial behavior and boundaries

- Amounts are positive integer paise (`BIGINT`), passed to the frontend as decimal strings. The UI parses rupees with integer arithmetic; it does not round binary floating-point money.
- Transfers debit the source account and credit the destination account with two equal positive ledger entries. Deposits and withdrawals use a seeded `EXTERNAL_CLEARING` system ledger account for the counter-entry.
- Deposit/withdrawal routes require ADMIN and an explicit, validated assertion that a real external settlement has already been checked. The external reference is reserved once on successful posting. There is no payment-rail integration.
- Business refusals such as insufficient funds are committed as immutable `FAILED` transaction and audit records with no balance or ledger mutation. Unauthorized or malformed requests are rejected and do not create financial records.
- Same-key/same-request retries return the saved outcome; a key reused with a different request conflicts. Concurrent balance-changing requests lock all affected account rows in ascending ID order.
- The database writes balances, immutable transactions, two ledger entries, external-settlement reservation (when applicable), idempotency linkage and audit event inside one transaction. A DB error rolls the full unit back.

## Verification evidence

Replace this section only with results from actual commands. Never convert database or browser PARTIAL results into PASS based on source review.

- Backend `.\backend\ledgerline\mvnw.cmd -f backend\ledgerline\pom.xml package` — **PASS**, `BUILD SUCCESS`; 49 tests, 0 failures, 0 errors, 0 skipped. The financial integration suite contributes 8 tests and uses H2 with Flyway disabled.
- Frontend `npm test` — **PASS**, 4 tests, 0 failures, including precise integer paise formatting and same-key transfer retry behavior. `npm run lint` — **PASS**. `npm run build` — **PASS**, Vite 8.3.6 production build.
- Browser smoke — **PASS for unauthenticated behavior only**: actual local Vite app rendered login and registration pages; unauthenticated `/accounts`, `/audit`, `/transfers`, `/statements`, and `/profile` redirected to `/login`; `/register` stayed public. No backend-authenticated browser/API action was run.
- MySQL V1-V4 / Hibernate / health — **BLOCKED / NOT RUN**. The process DB URL, username, password, and JWT-key variables were unset. No database was contacted or modified; no Flyway migration was run.
- Initial verification exposed one compile-problem path in `AdminService` when changing an existing account permission and an incorrect frontend large-INR formatting expectation. Moved `permissionExists` to class scope, corrected the test to match Indian digit grouping, and ensured the transfer UI reuses the idempotency key after an uncertain network/server failure. The corrected checks passed.
- Remaining blockers: verify V1-V4, Hibernate schema validation, triggers, MySQL locking/rollback and health on a confirmed empty disposable `ledgerline_db`; exercise authenticated browser flows against that backend. Pre-V4 balances still have no reconstructed opening ledger entries. No production-readiness claim.

## Operational and production limitations

H2 tests do not validate MySQL migration syntax, locking behavior, trigger enforcement or schema compatibility. Until V1-V4 run against a newly verified empty disposable `ledgerline_db` and the real Hibernate/health checks pass, the database release status is **BLOCKED**. Authenticated browser flows also remain **PARTIAL**. The project is a development/demo ledger, not a regulated core-banking or payment product; settlement attestation, key management, backup/recovery, operational monitoring, fraud controls and independent accounting review remain deployment responsibilities.