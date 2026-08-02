@echo off
setlocal
set "ROOT=%~dp0"

for /f "usebackq delims=" %%V in ("%ROOT%VERSION") do set "APP_VERSION=%%V"
echo Building Core Banking Prototype %APP_VERSION%...

rem Remove stale packages first. A failed build must never leave an older JAR looking current.
if exist "%ROOT%app\core-banking-prototype.jar" del /q "%ROOT%app\core-banking-prototype.jar"
if exist "%ROOT%backend\target\core-banking-prototype.jar" del /q "%ROOT%backend\target\core-banking-prototype.jar"

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
copy /y "%ROOT%backend\target\core-banking-prototype.jar" "%ROOT%app\core-banking-prototype.jar" >nul
if not exist "%ROOT%app\core-banking-prototype.jar" (
  echo ERROR: Packaged JAR was not created.
  exit /b 1
)

echo Built version: %APP_VERSION%
echo JAR: %ROOT%app\core-banking-prototype.jar
endlocal
