@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "MIG=%ROOT%\database\oracle\dps2\migrations\0.11.0-opening-tax-source-cif-financial-profile-hotfix.sql"
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-opening-tax-source-cif-financial-profile-hotfix-verifier.sql"
echo ============================================================
echo DPS2 Opening Tax Source - CIF Financial Profile Hotfix
echo ============================================================
echo [1/2] Applying Oracle reference-data hotfix...
call "%ROOT%\tools\run-oracle-sql.cmd" "%MIG%" || exit /b 21
echo [2/2] Verifying Oracle reference-data hotfix...
call "%ROOT%\tools\run-oracle-sql.cmd" "%VER%" || exit /b 22
echo ============================================================
echo DPS2_OPEN_TAX_CIF_SOURCE_HOTFIX_PASS
echo ============================================================
endlocal & exit /b 0
