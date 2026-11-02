# IAM Canonical Roadmap — Oracle Snapshot Addendum R1.5.1

**Parent:** `docs/iam/IAM-CANONICAL-ROADMAP-FA.md` (R1.5)  
**Status:** Proposed addendum, to be recorded in repository with next IAM-S1A consolidated patch.  
**Date:** 2026-11-02

## عدم تغییر فازها
تمام فازهای `IAM-00` تا `IAM-08`، نام و ترتیب و اهداف نسخه R1.5 بدون تغییر باقی می‌مانند. هیچ فاز جدیدی ایجاد، حذف، ادغام یا بازتعریف نشده است.

## شواهد جدید
- `IAM-2026-11-02.txt`: DDL Export شامل 37 جدول، 300 ستون، 54 FK، 22 Sequence؛ انطباق ساختار با R0.9.
- R1.3 Target: 38 جدول، 319 ستون، 58 FK؛ هشت ستون پیشنهادی در سه جدول و یک جدول کاندید هنوز در Snapshot نیست.
- بخش Runtime Oracle تأیید مستقل نشده است؛ گزارش قبلی `IAM_TABLES_VISIBLE=0` صرفاً متعلق به Snapshot روز قبل است.

## اثر در اجرای IAM-00 و IAM-01
| Track | Current | Next action | Acceptance |
|---|---|---|---|
| IAM-00: Architecture/Data | Current physical snapshot reconciled; open ADR-IAM-02/04/09, IAM-REF-01 | Finalize schema delta and migration scope, confirm Oracle metadata | Approved ADR, no direct use of DDL dump as migration |
| IAM-01: User Management | Physical core exists in export; Username normalization nullable | Design and validate normalization/uniqueness, Seed, UUID and API security; build DEV vertical slice | Negative tests on duplicate username, cross-context, untrusted headers |
| IAM-02: Federation | OIDC/LDAP providers and account linking base present; history UQs unresolved | Approve secure Link/Unlink/Re-link, Test IdP integration | Auditable proof and valid local/federated account mapping |
| IAM-05: IGA | TARGET_SYSTEM_ACCOUNT_LINKS candidate absent | Decide IAM versus IGA ownership before adding | ADR-IAM-09 approved or explicitly deferred |

## Readiness gate treatment
- `G1`: Evidence adequate for decision, formal sign-off outstanding.
- `G2`: Not automatically passed by physical DDL existence; backend authorization, migration plan, Threat Model and acceptance tests remain required.
- `G3`: Not started.

## Evidence and delivery discipline
No Oracle write, no application changes, no Git commit in this documentation snapshot. The next consolidated IAM-S1A code delivery must include a precise implemented/pending/blocked register and reproducible Developer QA evidence.
