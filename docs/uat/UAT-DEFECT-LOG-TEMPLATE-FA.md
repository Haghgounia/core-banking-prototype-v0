# DPS2 0.11.0 — قالب ثبت Defect در UAT

برای هر Defect یک رکورد مستقل بسازید.

```text
Defect ID:
Step: 00..17
Title:
Severity: BLOCKER / HIGH / MEDIUM / LOW
Environment:
Account/Product/Test Data Reference:
Preconditions:

Steps to Reproduce:
1.
2.
3.

Expected Result:

Actual Result:

HTTP/API Error (if any):

Timestamp:

Relevant Log Extract:

Database Trace IDs / Transaction IDs / Approval IDs:

Screenshot/File Reference:

Reproducible: YES / NO / INTERMITTENT

Classification:
BUG / ENVIRONMENT / DATA_CONFIGURATION / REQUIREMENT_QUESTION / NEW_FEATURE

Notes:
```

## Severity راهنما

- `BLOCKER`: مانع ادامه یک Step یا UAT کلی
- `HIGH`: نتیجه مالی/وضعیت/Trace نادرست
- `MEDIUM`: بخشی از Flow کار می‌کند ولی Business behavior ناقص/غلط است
- `LOW`: UI/Label/UX بدون اثر بر Business outcome
