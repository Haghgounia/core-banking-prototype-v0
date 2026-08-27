@echo off
setlocal
set "ROOT=%~dp0"

for /f "usebackq delims=" %%V in ("%ROOT%VERSION") do set "APP_VERSION=%%V"
echo Building Core Banking Prototype %APP_VERSION%...

rem Synchronize generated system specification before any verifier reads it.
node "%ROOT%tools\sync-system-specification.mjs" || exit /b 1

rem Fail fast when the source package is incomplete.
if not exist "%ROOT%frontend\src\app\app.component.ts" (
  echo ERROR: frontend\src\app is incomplete. app.component.ts was not found.
  exit /b 1
)
if not exist "%ROOT%frontend\src\app\features\cif\party-list.component.ts" (
  echo ERROR: CIF frontend source is missing.
  exit /b 1
)
if not exist "%ROOT%frontend\src\app\features\cif\party-360.component.ts" (
  echo ERROR: CIF Customer 360 frontend source is missing.
  exit /b 1
)
if not exist "%ROOT%frontend\src\app\features\cif-reference\party-reference-page.component.ts" (
  echo ERROR: CIF Party Reference frontend source is missing.
  exit /b 1
)
if not exist "%ROOT%backend\src\main\resources\cif\party-reference\party-reference-model.json" (
  echo ERROR: CIF Party Reference metadata is missing.
  exit /b 1
)

rem Persisted-grid static UI regression guard: CIF records remain column grids and sidebar docking stays wired.
node "%ROOT%tools\verify-cif-persisted-grids.mjs" || exit /b 1

rem Fix30 static guard: EA/XMI to configured Oracle schema comparison must remain fully wired.
node "%ROOT%tools\verify-ea-oracle-comparison.mjs" || exit /b 1

rem FIX46 static guard: CAL enterprise calendar forms and separate Reference Data menu.
node "%ROOT%tools\verify-calendar-reference.mjs" || exit /b 1

rem FIX47 static guard: transactional CAL dataset CSV import over JDBC; no SQL*Loader dependency.
node "%ROOT%tools\verify-calendar-dataset-import.mjs" || exit /b 1

rem CAL2 static guard: independent forms/DDL, recurring events and ZIP JDBC import.
node "%ROOT%tools\verify-calendar2-reference.mjs" || exit /b 1

rem FIX65 static guard: CAL2 monthly calendar read model and Angular month view.
node "%ROOT%tools\verify-calendar2-month-view.mjs" || exit /b 1

rem FIX69 static guard: BUSINESS_CALENDAR country/time-zone lookups; organization remains free text.
node "%ROOT%tools\verify-calendar2-business-calendar-lookups.mjs" || exit /b 1

rem FIX70 static guard: PDL unified product builder menu, metadata CRUD and product workspace.
node "%ROOT%tools\verify-pdl-product-builder.mjs" || exit /b 1

rem FIX55 static guard: user-facing calendar labels must stay simple and distinct.
node "%ROOT%tools\verify-calendar-display-labels.mjs" || exit /b 1

rem FIX64 guard: all runtime scripts use one canonical executable JAR name plus a build-version marker.
node "%ROOT%tools\verify-runtime-artifact-contract.mjs" || exit /b 1

rem FIX62 fail-fast: compile Java before the Angular production build so type errors are caught early.
cd /d "%ROOT%backend"
call mvnw.cmd -DskipTests compile || exit /b 1
cd /d "%ROOT%"

rem Remove stale runtime artifacts first. A failed build must never leave an older JAR looking current.
if exist "%ROOT%app\*.jar" del /q "%ROOT%app\*.jar"
if exist "%ROOT%app\BUILD-VERSION" del /q "%ROOT%app\BUILD-VERSION"
if exist "%ROOT%backend\target\core-banking-prototype.jar" del /q "%ROOT%backend\target\core-banking-prototype.jar"

if exist "%ROOT%frontend\dist" rmdir /s /q "%ROOT%frontend\dist"

cd /d "%ROOT%frontend"
call npm install || exit /b 1
call npm run build || exit /b 1

if exist "%ROOT%backend\src\main\resources\static" rmdir /s /q "%ROOT%backend\src\main\resources\static"
mkdir "%ROOT%backend\src\main\resources\static"
if exist "%ROOT%frontend\dist\core-banking-ui\browser" (
  xcopy /e /i /y "%ROOT%frontend\dist\core-banking-ui\browser\*" "%ROOT%backend\src\main\resources\static\" >nul
) else (
  xcopy /e /i /y "%ROOT%frontend\dist\core-banking-ui\*" "%ROOT%backend\src\main\resources\static\" >nul
)

cd /d "%ROOT%backend"
call mvnw.cmd clean package || exit /b 1
if not exist "%ROOT%app" mkdir "%ROOT%app"
set "JAR=%ROOT%app\core-banking-prototype.jar"
copy /y "%ROOT%backend\target\core-banking-prototype.jar" "%JAR%" >nul
>"%ROOT%app\BUILD-VERSION" echo %APP_VERSION%
if not exist "%JAR%" (
  echo ERROR: Packaged JAR was not created.
  echo Expected: %JAR%
  exit /b 1
)
if not exist "%ROOT%app\BUILD-VERSION" (
  echo ERROR: Build version marker was not created.
  exit /b 1
)

echo Built version: %APP_VERSION%
echo JAR: %JAR%
endlocal
