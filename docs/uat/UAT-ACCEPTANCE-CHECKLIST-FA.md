# DPS2 0.11.0 — Business Acceptance Checklist — Steps 00–17

> وضعیت هر ردیف: `PASS` / `FAIL` / `BLOCKED_BY_ENV_OR_DATA`

| Step | سناریوی پذیرش اصلی | انتظار کسب‌وکاری | نتیجه |
|---:|---|---|---|
| 00 | Dashboard | Account/Hold/Transaction/Balance/Availability فقط خواندنی نمایش داده شود؛ هیچ Business Write از Dashboard انجام نشود | ☐ |
| 01 | Account Maintenance | Basic Info، Party/Contact، Condition Override، Product Version Change و Signatory با History/Audit مناسب انجام شود؛ Opened Product Version immutable بماند | ☐ |
| 02 | Lifecycle/Hold | READY→ACTIVE، Suspend/Dormant/Reactivate، Hold/Release و Bulk با Guard و Idempotency صحیح؛ Hold به‌تنهایی Status حساب را تغییر ندهد | ☐ |
| 03 | Term Operations | Maturity Instruction، Renewal/Settlement، Conversion، Partial Withdrawal و Early Termination مطابق Term Contract؛ عملیات مالی از Step 05 عبور کند | ☐ |
| 04 | Profit | Profile/Period/Accrual/Detail/Adjustment/Payment قابل Trace باشد؛ Maker/Checker Adjustment؛ مقصد خارجی از Step 05 | ☐ |
| 05 | Transactions | Initiate→Validate→Authorize→Post؛ Cash/Transfer؛ Legs/Subledger trace؛ Reversal غیرمخرب؛ Hold/Limit/Regulatory gates اعمال شوند | ☐ |
| 06 | Statement | Statement از Subledger تولید شود؛ Header/Items/Running Balance و Delivery قابل Trace باشند | ☐ |
| 07 | Limits/Restrictions | Limit/Restriction ایجاد و اعمال شود؛ Usage از POSTED transaction مشتق شود؛ Reversal Usage را کاهش دهد | ☐ |
| 08 | Closure/Maturity | Closure کنترل‌شده با Gateهای Hold/Reservation/Balance؛ Reopen کنترل‌شده؛ Maturity batch/event بدون Fabrication مالی | ☐ |
| 09 | Account Services | Inquiry/Confirmation/Notification Preference/Event و API Access روی Account واقعی ثبت/خوانده شوند | ☐ |
| 10 | Party/Access | Signature Rule، Delegation، Authorized User، Beneficiary و Payment Instrument فقط روی Partyهای مجاز Account انجام شوند | ☐ |
| 11 | Regulatory | Restriction/Evaluation و Reserve Requirement/Position و Regulatory Report/Items با Rule/Requirement Governed ایجاد و قابل Trace باشند | ☐ |
| 12 | Pricing/Fee | Fee Rule/Tier، Pricing Override Maker/Checker، Fee Assessment، Package/Benefit/Enrollment و Profitability روی داده Governed کار کنند | ☐ |
| 13 | Tax | Exemption/Calculation/Certificate، Adjustment، Liability، Payment و Reconciliation فقط از Rule/Event قابل Trace استفاده کنند | ☐ |
| 14 | Reconciliation | Run/Item ایجاد شود؛ mismatch → Discrepancy؛ **Suspense فقط با Action مستقل «ثبت قلم باز»** ایجاد شود؛ Exception handoff مستقل باشد | ☐ |
| 15 | Exception/Correction | Case/Assignment/RCA؛ چهار نوع `REVERSAL`, `CORRECTION`, `BACKDATED_CORRECTION`, `DUPLICATE_CANCEL`؛ Maker/Checker و history کامل؛ مالی جعلی ایجاد نشود | ☐ |
| 16 | NOSTRO/VOSTRO | Account Master مستقل + Extension + Profile؛ Product Profile Governed؛ Reconciliation account-scoped؛ هر دو Type قابل ایجاد باشند | ☐ |
| 17 | Rewards/Lottery | Program/Eligibility/Enrollment؛ Entry Freeze؛ Weighted Draw؛ Winner؛ Prize Payment از Step 05 و با Trace مالی | ☐ |

## Cross-Step Acceptance

| کنترل | انتظار | نتیجه |
|---|---|---|
| Auditability | هر Mutation مهم قابل Trace به Actor/Approval/Reference باشد | ☐ |
| Idempotency | Retry همان درخواست Business outcome تکراری تولید نکند | ☐ |
| Maker/Checker | Maker و Checker در Flowهای Approval یک نفر نباشند | ☐ |
| No Fabrication | Evidence/Rule/Posting مالی ساختگی برای عبور از Gate ایجاد نشود | ☐ |
| Canonical Balance | Ledger/Available/Blocked از Package 17 و Postingهای معتبر مشتق شوند | ☐ |
| Product Governance | Product-specific behavior از Product Builder/Governed Product Version بیاید | ☐ |
| Error UX | Business validation به خطای قابل فهم 4xx تبدیل شود؛ خطای غیرمنتظره با Trace در Log قابل پیگیری باشد | ☐ |

## Sign-off

```text
UAT Environment:
Build/Version:
Database/Schema Baseline:
Business Tester:
Designer:
Date:
Result: PASS / CONDITIONAL PASS / FAIL
Blocking Defects:
Notes:
```
