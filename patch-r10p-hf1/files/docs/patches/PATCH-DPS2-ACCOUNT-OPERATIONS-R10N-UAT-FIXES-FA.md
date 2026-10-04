# DPS2 Account Operations R10N — UAT Fixes

Baseline: `core-banking-prototype-v0-2026-10-04-1409`

## Scope
- Persian business labels in account list, Account 360, balance/availability and subledger presentation.
- Owner/customer selection keeps the selected CIF Party ID and display name visible.
- Active OWNER/JOINT_OWNER ownership total is guarded at <= 100% in both Angular and backend servicing service.
- Transaction restriction authority/reference is a controlled selector derived from existing account evidence; free-text entry is removed.
- Controlled closure/reopening UX is hardened: closure reason choices are valid for each closure type, positive ledger requires a selected settlement account, settlement candidates are loaded independently of the current account search result, and local action/error feedback is shown for request/approval.

No DDL or data migration is included.
