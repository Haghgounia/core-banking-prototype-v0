@echo off
setlocal

for /f "usebackq delims=" %%V in ("VERSION") do set VERSION=%%V
if "%VERSION%"=="" set VERSION=source
set OUT=..\core-banking-prototype-v0-%VERSION%.zip

if exist "%OUT%" del /q "%OUT%"

rem Normalize legacy/overlay source layout before packaging.
node tools\sync-system-specification.mjs || exit /b 1
node tools\migrate-release-layout.mjs || exit /b 1
node tools\migrate-root-layout.mjs || exit /b 1
node tools\migrate-source-layout.mjs || exit /b 1
node tools\verify-release-layout.mjs || exit /b 1
node tools\verify-dps2-account-schema-reconciliation-092.mjs || exit /b 1
node tools\verify-dps2-reference-fk-reconciliation-093.mjs || exit /b 1
node tools\verify-dps2-opening-operational-v5-phase10.mjs || exit /b 1
node tools\verify-dps2-opening-operational-v5-phase10b.mjs || exit /b 1
node tools\verify-dps2-opening-operational-v5-phase10d.mjs || exit /b 1
node tools\verify-dps2-opening-v5-phase10e10f.mjs || exit /b 1
node tools\verify-dps2-account-operations-schema-reconciliation-11a.mjs || exit /b 1
node tools\verify-dps2-account-servicing-core-11b.mjs || exit /b 1

echo Creating clean source package: %OUT%
tar.exe -a -c -f "%OUT%" ^
  --exclude=frontend/node_modules ^
  --exclude=frontend/dist ^
  --exclude=frontend/.angular ^
  --exclude=backend/target ^
  --exclude=backend/src/main/resources/static ^
  --exclude=app/*.jar ^
  --exclude=app/*.jar.original ^
  --exclude=app/BUILD-VERSION ^
  --exclude=logs ^
  --exclude=data/document-storage ^
  --exclude=backend/data/document-storage ^
  --exclude=database/oracle/exports ^
  --exclude=.git ^
  --exclude=.idea ^
  --exclude=.vscode ^
  --exclude=.upgrade-backup ^
  --exclude=*.log ^
  --exclude=*.tmp ^
  --exclude=*.class ^
  .

if errorlevel 1 (
  echo Packaging failed.
  exit /b 1
)

echo Package created: %OUT%
endlocal
