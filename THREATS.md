# LedgerLine threat model and current gaps

This is a Day 1 threat register, not a security certification. The schema and starter configuration are not sufficient for real financial use.

## Verification state (2026-11-06)

Backend tests/package and frontend lint/build passed. The local MySQL service was reachable, but application database credentials and the required JWT key were not configured. A startup attempt failed during Flyway connection because the unresolved DB username placeholder was passed to MySQL. The migration was not executed, and the health endpoint was not available on port 8080. See the root [README](./README.md) for exact commands and results. Schema review is not a substitute for applying and validating the migration on the target MySQL version.

| Threat | Day 1 safeguard | Remaining work |
|---|---|---|
| Duplicate/replayed transfers | `idempotency_keys` has a unique `(user_id, idempotency_key)` constraint and stores a request hash. | No request handler, atomic claim/replay behavior, canonical request hashing, retention policy, or concurrency test exists. |
| Concurrent withdrawals / negative balances | Paise are stored as `BIGINT`; account balance has a database `CHECK (balance_paise >= 0)`. | No balance mutation or locking is implemented. A CHECK does not provide transaction isolation, prevent stale writes, or ensure balance matches ledger entries. |
| Unbalanced or partial internal transfer | Ledger entries require positive paise amounts and reference a transaction and account. | There is no transactional transfer service or cross-row invariant enforcing equal debit/credit totals. Do not execute transfers. |
| Ledger or audit row modification | MySQL triggers reject ordinary `UPDATE` and `DELETE` statements on ledger entries and audit events. | Database administrators can alter schema/triggers; privileges, backups, independent audit, tamper evidence, and operational monitoring are not configured. |
| Unauthorized account access | All non-health API paths require a valid JWT. Account permission table and backend role names are defined. | There are no account endpoints or permission service yet. JWT authentication alone does not enforce per-account ownership/permissions. |
| Weak/misissued JWT | HS256 decoder requires a Base64 secret of at least 32 bytes and validates issuer and timestamps; startup has no default secret. | There is no token issuer, key rotation, audience validation, revocation, user-active lookup, or verification that claims are assigned by a trusted identity service. |
| Password compromise | A BCrypt `PasswordEncoder` bean is configured. | There is no login, provisioning, password policy, MFA, reset flow, throttling, or credential breach response. |
| Secret exposure | Database credentials and JWT key are environment-supplied; no credentials are committed as defaults. | Production secret management, rotation, least-privilege separation for Flyway/runtime, and deployment controls remain unconfigured. |
| Browser abuse / CORS | CORS is restricted to configured origins, with a localhost development default and no credentialed cookies. | Production origins and deployment headers must be explicitly configured; CORS is not an authorization control. |
| Sensitive health details | Only Actuator health is exposed and component details are hidden. | Operational health behavior still needs review for production deployment. |
| AI-triggered money movement | The frontend has no financial action implementation. | Keep AI out of authorization and transaction execution paths. Any future AI integration must be advisory, auditable, and require explicit user-controlled approval through independently authorized backend flows. |

## Non-negotiable follow-up

Before any real or test-value transfer is enabled, implement and independently test transaction-bound balance updates, account permission checks, paired ledger postings, user-scoped idempotency semantics, failure rollback, concurrent withdrawal behavior, audit controls, and database privilege separation. Do not interpret successful compilation or migration as proof of financial safety.
