# R30-HF7-A3 — فهرست نصب و کنترل Patch

**Baseline مورد نیاز:** Core Banking Prototype 0.11.0 با Patchهای HF7-A1 / HF7-A2 / HF7-A2-HF1 (Build موفق در محیط کاربر).

**دامنه:** فقط سورس/آزمون/مستندات. **تغییر Oracle، Migration و DML:** هیچ.

**نصب:** تهیه Backup، بازکردن ZIP در ریشه `D:\Projects\core-banking-prototype-v0` و تأیید Overwrite فایل‌های اعلام‌شده. از پاک‌کردن سایر فایل‌ها خودداری کنید.

## فایل‌های جایگزین‌شده و افزوده

| نوع | مسیر | SHA-256 |
|---|---|---|
| M | `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleEvidenceEvaluator.java` | `2cc8913e414d03f6267b6710be0d2d119ca2485a25ef25af09348ee14f793e3e` |
| M | `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceService.java` | `433ef9f616f082b0a9f09259c7bec2eb62d0a1152a6aa6e6455b4d0d326c7ff8` |
| M | `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleSemanticContract.java` | `955c46352d9e9535620bcf4ba51169580586378ae2226f44331dac9cdba6a2be` |
| M | `build-production.cmd` | `865567300285a636839beda60e9e782c4056cf504fd26ca949ebbf32597eedbd` |
| M | `build-production.sh` | `74259a205f8376e0dd96385c6c9aeaa5e0fa9bc0e3b2f62c1e3527806e24c5bd` |
| M | `tools/verify-pdl-r30-hf7a1-semantic.mjs` | `7056e9ae4c5f7acb2bfb1784970f547ee6af057adcc304b783bf0eee29df65e3` |
| M | `tools/verify-pdl-r30-hf7a2-temporal.mjs` | `d3d0dc18c9c05c503f24b85a495366a03ea54f4ec6d7a87ff2e9683c5382e1bb` |
| A | `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleHierarchyAudit.java` | `a7db886197edf2ce37b772bc6a71221ac34345f16ffba7824b9134723b6c7046` |
| A | `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleHierarchyHF7A3Test.java` | `2c2f6bb75b1e429118cef589fa4e5dea946799c520656fe393498c02deb26d99` |
| A | `docs/PDL-2026-10-14-R30-HF7A3-FIELD-RISK-REGISTER-FA.md` | `3f4967697ff47496d786af0add26664fcac65d871e09f088754a69980e3e37e2` |
| A | `docs/PDL-2026-10-14-R30-HF7A3-REVIEW-DECISIONS-FA.md` | `396e5a475d31eda086b5769b6332b45e27022cb3cd063f72282fcca6a32c517b` |
| A | `tools/verify-pdl-r30-hf7a3-integrity.mjs` | `427ed073be0f69a6e4b6bb9a2e9fc1c0c7cd29868e1cd59e1e6720141307c4df` |

## کنترل محلی

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-release-layout.mjs
node tools\verify-pdl-r30-hf7a1-semantic.mjs
node tools\verify-pdl-r30-hf7a2-temporal.mjs
node tools\verify-pdl-r30-hf7a3-integrity.mjs
build-production.cmd
```

**توجه:** موفقیت verifierها جایگزین Maven/Surefire، Angular Build، Oracle UAT و آزمون عملیاتی نیست. محافظ `canOpen` همچنان فعال است.
