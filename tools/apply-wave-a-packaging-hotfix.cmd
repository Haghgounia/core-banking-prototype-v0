@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1

echo ============================================================
echo DPS2 Wave A - Runner / Packaging Hotfix
echo ============================================================

for %%F in (README-HOTFIX-FA.md README-RUNNER-HOTFIX-FA.md) do (
  if exist "%ROOT%\%%F" (
    del /q "%ROOT%\%%F" || exit /b 2
    echo REMOVE ^| %%F
  )
)

if not exist "%ROOT%\tools\resume-dps2-wave-a-after-11i.cmd" (
  echo ERROR: resume helper missing after overlay extraction.
  exit /b 3
)

node "%ROOT%\tools\verify-node-tool-path-portability.mjs" || exit /b 10
node "%ROOT%\tools\verify-dps2-wave-a-servicing-lifecycle-11j.mjs" || exit /b 11

echo ============================================================
echo DPS2_WAVE_A_PACKAGING_HOTFIX_PASS
echo ============================================================
endlocal & exit /b 0
