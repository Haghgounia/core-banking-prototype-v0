# DPS2 Wave A Runner / Packaging Hotfix

این Overlay دو نقص بسته‌بندی Wave A را اصلاح می‌کند:

- اضافه/جایگزین کردن helperهای qualification و resume با return-code صریح؛
- حذف READMEهای موقت از root پروژه تا Release Layout Guard عبور کند.

پس از Extract روی root پروژه اجرا شود:

```bat
tools\apply-wave-a-packaging-hotfix.cmd
```

انتظار:

```text
PHASE11J_STATIC_VERIFIER_PASS=73
PHASE11J_STATIC_VERIFIER_FAIL=0
PHASE11J_STATIC_BASELINE_PASS
DPS2_WAVE_A_PACKAGING_HOTFIX_PASS
```
