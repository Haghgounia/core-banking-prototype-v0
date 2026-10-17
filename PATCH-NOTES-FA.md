# Patch Notes | Posting R1-D5 Cash Deposit / Withdrawal

- Source baseline: `core-banking-prototype-v0-2026-10-17-1443.zip` only.
- Default status: INACTIVE (`core-banking.posting.cash.enabled=false` by absence of property).
- Two updated sources: `DepositTransactionService.java`, `DepositTransactionRepository.java`.
- Four new main Java classes in `posting/cash`: Policy, Bridge, PostingService, JournalReadController.
- New pure-Java and JUnit tests, Windows gated installer, SQL diagnostic scripts, UAT configuration template and acceptance documentation.
- No Oracle DDL migrations/seed; schema from `ACCOUNTING-2026-10-17.txt` is treated as an existing prerequisite.
- No Changes to Frontend, Kafka configuration, Product Builder, FEE, FEE2, user-defined GL accounts or Oracle existing data.

## Current validation from authoring sandbox

1. Policy unit self-test: 21/21 PASS.
2. New Java class syntax compile against minimal Spring API shims: PASS (not Maven).
3. ES1 static regression: 30/30 PASS.
4. DPS2 Phase11I Step05 static regression: 64/64 PASS.
5. DPS2 Phase11D Balance/Subledger static regression: 56/56 PASS.
6. Live Oracle and Spring Boot Maven Build: NOT EXECUTED; do not label the patch UAT accepted.

## Next boundary

Bank accounting owners must validate journal roles, GL classifications, chart, periods, source clearing accounts and published templates. Only then enable the feature in an isolated UAT. Verify with signed UAT evidence SQL/API in `docs/posting/R1D5-ACCEPTANCE-FA.md`.
