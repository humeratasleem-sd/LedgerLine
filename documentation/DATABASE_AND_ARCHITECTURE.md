# Database and architecture

## Runtime shape

React/Vite communicates with Spring MVC over JSON and bearer JWTs. Spring Security is stateless, validates the HMAC token, then reloads the user's active status and role from MySQL on each request. Services perform account-level permission checks. JDBC template operations use Spring-managed transactions.

Flyway owns schema creation and evolution. Hibernate is configured with `ddl-auto=validate`; it must never create or update the production schema. No database migration or target inspection should run until the operator confirms the exact `ledgerline_db` is newly created, disposable and empty.

## Migration history

| Version | Purpose |
|---|---|
| V1 | Users, accounts, permissions, transaction foundation, idempotency, ledger entries, audit events, constraints, indexes and append-only ledger/audit triggers. |
| V2 | User profile and explicit account status. |
| V3 | Rename `ledger_transactions` to `transactions`. |
| V4 | Transaction type/account references/failure metadata, external clearing account, exactly-one ledger account constraint, idempotent external settlement reservation, and append-only transaction/settlement triggers. |

The V4 migration is source-reviewed only until Flyway applies it to MySQL. MySQL DDL may not roll back as a whole; inspect Flyway history and schema after any failed run and only repair a confirmed disposable database.

## Financial posting model

All business amounts and customer balances are signed SQL `BIGINT` storing positive integer paise. API responses serialize amounts as strings to preserve integer precision. The frontend converts rupees to paise with `BigInt`, then sends a safe integer JSON value.

Each successful posting creates exactly two positive ledger rows:

| Operation | DEBIT | CREDIT |
|---|---|---|
| Transfer | Source customer account | Destination customer account |
| Deposit | `EXTERNAL_CLEARING` system account | Destination customer account |
| Withdrawal | Source customer account | `EXTERNAL_CLEARING` system account |

The transaction, idempotency record, locked customer balance updates, two ledger entries and audit event use one DB transaction. The service checks that the transaction has two entries and equal debit/credit totals before commit. Insufficient funds, frozen/closed accounts, unsupported currency, self-transfer and overflow create a FAILED transaction/audit outcome but no ledger entries or balance mutation.

The application selects affected account rows `FOR UPDATE` in ascending ID order to serialize conflicting operations and reduce deadlocks. Permissions are checked after account locks. Permission assignment also locks the account before changing rows, so a transfer and revocation are ordered.

`idempotency_keys` is unique by actor and key and stores a SHA-256 request fingerprint. A successful external settlement also reserves a unique external reference in `external_settlements`. Completed and refused financial transactions are immutable and append-only. Ledger and audit rows use DB triggers to reject ordinary updates/deletes.

The H2 suite checks application-level transaction behavior, locking under the H2 MySQL compatibility mode, replay, refusals, ledger totals and rollback. It does not execute V1-V4, prove MySQL locking/isolation behavior or verify MySQL trigger syntax. Existing pre-V4 account balances are not retroactively reconstructed as journal entries; this project must not claim historical reconciliation.

## Security boundaries

- `ACCOUNT_HOLDER` may transfer only from an OWNER/TRANSFER account.
- ADMIN operational deposits/withdrawals are manual bookkeeping after an explicit settlement attestation. The application does not send money to or query a bank.
- AUDITOR can read audit/account/statement data but cannot create or mutate financial records.
- Account owner permissions restrict holder reads. Auditor/admin broad read access is intentional and read-only.
- External settlement references are unique only for successful settlements; refused attempts remain in transaction history and can be retried under a new idempotency key after review.
- No real secrets belong in Git, this documentation, frontend environment variables or chat.