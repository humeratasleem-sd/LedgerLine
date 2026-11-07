# LedgerLine frontend

The React/Vite application and backend setup are documented in the root [README](../README.md).

For local development, create `.env.local` from `.env.example`, configure the backend CORS origin, then run:

```powershell
npm ci
npm run dev
```

Authentication uses short-lived bearer tokens held in memory only. Reloading the page requires signing in again. Account, statement, audit, and transfer pages remain non-operational placeholders; the frontend does not generate sample financial data or submit money movement.
