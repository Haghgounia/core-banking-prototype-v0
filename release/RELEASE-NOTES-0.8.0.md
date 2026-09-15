# Release Notes 0.8.0

First Deposit Account Operations release for Four Deposits.

- Adds a dedicated `/api/v1/deposit-accounts` read-only bounded-context API.
- Adds account search by account number, status, Opening Request, Party and four-deposit family.
- Adds Account 360 with Opening linkage, owner Parties and lifecycle timeline.
- Adds `/four-deposits/account-operations` workspace, menu, home card and breadcrumb.
- Uses the existing Phase 4 `DEPOSIT_ACCOUNT` and `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` contract only.
- Does not invent Hold, Reactivation, Closure, Dormancy, Posting or Balance tables without a supplied servicing model.
- No database migration is required.
