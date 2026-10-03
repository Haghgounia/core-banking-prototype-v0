@echo off
setlocal
set "ROOT=%~dp0..\"
cd /d "%ROOT%" || exit /b 1

echo ============================================================
echo DPS2 Account Operations UI qualification
echo ============================================================
node tools\verify-dps2-account-operations-ui-alignment.mjs || exit /b 1
call build-production.cmd || exit /b 1

echo ============================================================
echo DPS2_ACCOUNT_OPERATIONS_UI_QUALIFICATION_PASS
echo ============================================================
exit /b 0
