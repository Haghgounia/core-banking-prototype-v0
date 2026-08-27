@echo off
setlocal
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"

for /f "usebackq delims=" %%V in ("%ROOT%\VERSION") do set "APP_VERSION=%%V"

set "RUNNING_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8091" ^| findstr "LISTENING"') do set "RUNNING_PID=%%P"
if defined RUNNING_PID (
  echo ERROR: Port 8091 is already used by PID %RUNNING_PID%.
  echo The browser may still be showing an older application version.
  echo Stop it with: bin\stop.cmd
  pause
  exit /b 1
)

rem FIX64: the executable name is always canonical; BUILD-VERSION prevents stale JAR reuse.
set "JAR=%ROOT%\app\core-banking-prototype.jar"
set "BUILD_VERSION_FILE=%ROOT%\app\BUILD-VERSION"
if not exist "%JAR%" (
  echo ERROR: Executable JAR was not found.
  echo Expected: %JAR%
  echo Run: build-production.cmd
  pause
  exit /b 1
)
if not exist "%BUILD_VERSION_FILE%" (
  echo ERROR: Build version marker was not found.
  echo Expected: %BUILD_VERSION_FILE%
  echo Run: build-production.cmd
  pause
  exit /b 1
)
set "BUILT_VERSION="
set /p BUILT_VERSION=<"%BUILD_VERSION_FILE%"
if /i not "%BUILT_VERSION%"=="%APP_VERSION%" (
  echo ERROR: Runtime JAR is stale or belongs to another source version.
  echo Source version : %APP_VERSION%
  echo Built version  : %BUILT_VERSION%
  echo Run: build-production.cmd
  pause
  exit /b 1
)

set "CONFIG_PATH=%ROOT%\config\application.yml"
set "CONFIG_URI=%CONFIG_PATH:\=/%"
if not exist "%ROOT%\logs" mkdir "%ROOT%\logs"
set "LOG_FILE=%ROOT%\logs\core-banking-prototype.log"

echo Starting Core Banking Prototype %APP_VERSION%...
echo JAR: %JAR%
echo Configuration: %CONFIG_PATH%
echo Log file: %LOG_FILE%
java -jar "%JAR%" "--spring.config.additional-location=optional:file:/%CONFIG_URI%"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
echo Core Banking Prototype process ended with code %EXIT_CODE%.
echo Review the messages above before closing this window.
pause
endlocal & exit /b %EXIT_CODE%
