@echo off
setlocal EnableExtensions
if "%~1"=="" (echo CLT_R5C_SMOKE_TARGET_REQUIRED & exit /b 2)
set "ROOT=%~f1"
set "SRC=%ROOT%\backend\src\main\java\com\behsazan\corebanking\collateral\release\CollateralFinalReleasePolicy.java"
set "TEST=%ROOT%\tools\clt\r5c\FinalReleasePolicySmoke.java"
set "OUT=%TEMP%\clt-r5c-smoke-%RANDOM%-%RANDOM%"
mkdir "%OUT%" >nul 2>&1
javac -encoding UTF-8 -d "%OUT%" "%SRC%" "%TEST%"
if errorlevel 1 (rmdir /s /q "%OUT%" >nul 2>&1 & exit /b 1)
java -cp "%OUT%" FinalReleasePolicySmoke
set "RESULT=%ERRORLEVEL%"
rmdir /s /q "%OUT%" >nul 2>&1
exit /b %RESULT%
