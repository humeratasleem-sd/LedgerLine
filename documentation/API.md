# LedgerLine API

Base URL: `http://localhost:8080`. JSON is used for request and response bodies. Send `Authorization: Bearer <access-token>` to protected endpoints. Access tokens are short-lived and held in browser memory only.

## Authentication and account access

| Method | Path | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Creates `ACCOUNT_HOLDER`; returns a bearer token. Role input is ignored/not accepted. |
| POST | `/api/auth/login` | Public | Generic invalid-credentials response. |
| GET | `/api/auth/me` | Authenticated | Resolves profile from verified token subject. |
| GET | `/api/accounts` | Account holder / admin / auditor | Account holders see assigned accounts; admin and auditor views are read-only and broad. |
| GET | `/api/accounts/{accountId}` | Account holder / admin / auditor | Account holders need an account permission; admin and auditor can read. |
| GET | `/api/accounts/{accountId}/transactions?limit=50` | Account holder / admin / auditor | Requires account view access; `limit` is 1-100; includes refused attempts. |

The compatibility paths `/api/v1/auth/register`, `/api/v1/auth/login`, and `/api/v1/auth/me` remain available.

## Administration

All paths below require `ADMIN`.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/users` | Safe user profiles. |
| PATCH | `/api/admin/users/{userId}/status` | Enable/disable; prevents self-disable and removing the last active admin. |
| PATCH | `/api/admin/users/{userId}/role` | Audited role changes; prevents demoting the last active admin. |
| GET | `/api/admin/accounts` | Administrative account directory. |
| POST | `/api/admin/accounts` | Creates a zero-balance INR account and OWNER permission. |
| PATCH | `/api/admin/accounts/{accountId}/status` | ACTIVE/FROZEN/CLOSED; closure requires zero balance and is terminal. |
| PUT | `/api/admin/accounts/{accountId}/permissions` | Adds/updates access; serialized with account postings. |
| POST | `/api/admin/transactions/deposits` | Manual posting for externally verified settlement only. |
| POST | `/api/admin/transactions/withdrawals` | Manual posting for externally verified settlement only. |

## Financial posting

### Transfer

`POST /api/transactions/transfers` requires `ACCOUNT_HOLDER` and a printable ASCII `Idempotency-Key` header of 8-255 characters.

```json
{
  "sourceAccountId": 10,
  "destinationAccountId": 11,
  "amountPaise": 1250
}
```

Amount values must be positive integer paise. The source requires OWNER or TRANSFER permission. The service verifies both accounts are ACTIVE, INR, distinct, and that the source has sufficient funds. It creates one DEBIT and one CREDIT of the same amount.

### External settlement deposit/withdrawal

Both operations require `Idempotency-Key`, positive integer `amountPaise`, `externalReference` (1-128 restricted ASCII characters) and `externalSettlementVerified: true`. Deposits use `destinationAccountId`; withdrawals use `sourceAccountId`.

```json
{
  "destinationAccountId": 10,
  "amountPaise": 1250,
  "externalReference": "bank-ref-2026-001",
  "externalSettlementVerified": true
}
```

These are not payment instructions. An ADMIN attests that the external receipt or payout was independently verified before recording it. A successful external reference can be posted only once.

Successful requests return `200` with a `COMPLETED` transaction response. Business refusals return `422` with a persisted `FAILED` response and `failureCode`. A repeated key with the same canonical request returns the prior outcome; a key reused for different content returns `409`. Invalid requests return `400`; missing/invalid authentication is `401`; role/account denial is `403`; missing accounts are `404`.

Example transaction response:

```json
{
  "id": 123,
  "transactionType": "TRANSFER",
  "status": "COMPLETED",
  "amountPaise": "1250",
  "currency": "INR",
  "sourceAccountId": 10,
  "destinationAccountId": 11,
  "failureCode": null,
  "externalReference": null,
  "createdAt": "2026-11-08T12:00:00Z"
}
```

`GET /api/audit/events?limit=50` is ADMIN/AUDITOR read-only access, limited to 1-100 events. No financial endpoint is available to AUDITOR.

## Health

`GET /actuator/health` is public and hides component details. It is not proof of MySQL health until the application is running against the verified target database.