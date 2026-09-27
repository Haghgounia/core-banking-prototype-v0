# DPS2 0.11.0 — Phase 11N-K Servicing Approval Trace / Angular Compile Hotfix

## Reason
Final 11N-K production build reached Angular compilation after Oracle and backend gates passed, then failed because the Account Operations template renders `servicingHistory.approvalRequestId` while the TypeScript and backend servicing-history projections did not expose the canonical column.

## Canonical source
`DPS2.DEPOSIT_ACCOUNT_SERVICING_HISTORY.APPROVAL_REQUEST_ID` exists in the canonical Oracle DDL and references `DEPOSIT_OPERATION_APPROVAL_REQUEST`.

## Changes
- Expose `approvalRequestId` in the backend `ServicingHistory` projection.
- Read `APPROVAL_REQUEST_ID` from `DEPOSIT_ACCOUNT_SERVICING_HISTORY` in Account 360.
- Preserve the existing history API and add an overload accepting an approval request id.
- Persist approval trace for Condition Override history and Product Version history.
- Expose `approvalRequestId` in the Angular `DepositAccountServicingHistory` contract.

## Scope
No DDL. No migration. No business backfill. No fabricated approval evidence. Existing history rows may remain null when the originating action did not require maker/checker.

## Verification
- Phase 11N-D static: 64/64 PASS.
- Phase 11N Rapid static: 39/39 PASS.
- Full Angular/production build is qualified on the Windows target environment by `tools\\qualify-dps2-phase11nk-final.cmd`.
