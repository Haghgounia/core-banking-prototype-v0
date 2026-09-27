@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"

echo ============================================================
echo Core Banking Prototype - UAT Non-Destructive Preflight
echo ============================================================
set "FAIL=0"
set "BUILD_REQUIRED=0"

if not exist "%ROOT%\VERSION" (
  echo FAIL ^| VERSION file missing
  set "FAIL=1"
) else (
  set /p APP_VERSION=<"%ROOT%\VERSION"
  echo PASS ^| source VERSION=!APP_VERSION!
)

if not exist "%ROOT%\config\application.yml" (
  echo FAIL ^| config\application.yml missing
  set "FAIL=1"
) else (
  echo PASS ^| config\application.yml exists
)

where java >nul 2>nul
if errorlevel 1 (
  echo FAIL ^| java not found on PATH
  set "FAIL=1"
) else (
  for /f "tokens=*" %%J in ('java -version 2^>^&1 ^| findstr /i "version"') do echo PASS ^| %%J
)

if not exist "%ROOT%\app\core-banking-prototype.jar" (
  echo INFO ^| runtime JAR is not packaged in Source RC
  echo INFO ^| run build-production.cmd before first UAT start
  set "BUILD_REQUIRED=1"
) else (
  echo PASS ^| runtime JAR exists
  if not exist "%ROOT%\app\BUILD-VERSION" (
    echo FAIL ^| app\BUILD-VERSION missing
    set "FAIL=1"
  ) else (
    set /p BUILT_VERSION=<"%ROOT%\app\BUILD-VERSION"
    echo PASS ^| BUILD-VERSION=!BUILT_VERSION!
    if defined APP_VERSION if /i not "!BUILT_VERSION!"=="!APP_VERSION!" (
      echo FAIL ^| BUILD-VERSION does not match source VERSION
      set "FAIL=1"
    )
  )
)

set "RUNNING_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8091" ^| findstr "LISTENING"') do set "RUNNING_PID=%%P"
if defined RUNNING_PID (
  echo INFO ^| port 8091 already LISTENING on PID !RUNNING_PID!
) else (
  echo PASS ^| port 8091 is free
)

echo.
if not "%FAIL%"=="0" (
  echo UAT_PREFLIGHT_FAIL
  endlocal & exit /b 1
)
if "%BUILD_REQUIRED%"=="1" (
  echo UAT_PREFLIGHT_BUILD_REQUIRED
  endlocal & exit /b 0
)
echo UAT_PREFLIGHT_PASS
endlocal & exit /b 0
