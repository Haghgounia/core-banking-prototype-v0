# QA Report 0.5.2

Targeted defect: Java compiler error in `DepositOpeningAuditService` caused by capturing a reassigned local variable inside a validation lambda.

## Result
- Defect source fixed with immutable/effectively-final `entity` normalization.
- Dedicated 0.5.2 verifier: PASS.
- Full static regression: 36/36 PASS.
- Incremental patch overlay from 0.5.1: byte-identical to target 0.5.2 (0 missing / 0 extra / 0 different).
- Full ZIP integrity: PASS.
- Patch ZIP integrity: PASS.
- Oracle DDL changes: none.
- Maven compile in the packaging environment: not executable because Maven wrapper download from Maven Central is blocked; final compile must be confirmed on the Windows build environment.
