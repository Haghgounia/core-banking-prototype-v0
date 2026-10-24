@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem CAL2 R2-B2 Pilot - Windows launcher inside tools to comply with root layout guard.
for %%I in ("%~dp0..") do set "ROOT=%%~fI\"
cd /d "%ROOT%" || goto bad_folder

if not exist "%ROOT%build-production.cmd" goto bad_folder
if not exist "%ROOT%bin\start.cmd" goto bad_folder
if not exist "%ROOT%tools\verify-cal2-personal-r2b2.mjs" goto bad_folder
if not exist "%ROOT%config\application.yml" goto bad_config

where node >nul 2>&1
if errorlevel 1 goto missing_node
where npm >nul 2>&1
if errorlevel 1 goto missing_node
where java >nul 2>&1
if errorlevel 1 goto missing_java

rem FIX2: Root only allows a fixed set of file names. Relocate ONLY prior
rem FIX1 package artifacts if present. Preserve all content; never overwrite.
set "ARCHIVE=%ROOT%docs\cal2\personal-calendar\r2b2-package"
if not exist "%ARCHIVE%" mkdir "%ARCHIVE%"
if errorlevel 1 goto failed_relocate
call :relocate "CAL2-R2B2-PILOT-MANIFEST.json"
if errorlevel 1 goto failed_relocate
call :relocate "CAL2-R2B2-PILOT-README-FA.txt"
if errorlevel 1 goto failed_relocate
call :relocate "CAL2-R2B2-START-HERE-FA.md"
if errorlevel 1 goto failed_relocate
call :relocate "RUN-CAL2-R2B2-PILOT.cmd"
if errorlevel 1 goto failed_relocate

echo ==============================================================
echo CAL2 R2-B2 PILOT - Windows Build and Start - LAYOUT FIX2
echo Project: %ROOT%
echo ==============================================================
echo Local single-user UAT ONLY; NOT for production.
echo Confirm the CAL2 read-only database contract before running UAT.
echo.

rem Fail before build marks a stale application dirty if root layout is wrong.
node "%ROOT%tools\verify-release-layout.mjs"
if errorlevel 1 goto failed_layout

set "PORT=%SERVER_PORT%"
if not defined PORT set "PORT=8091"
set "PORT_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do set "PORT_PID=%%P"
if defined PORT_PID goto busy_port

echo [1/3] Checking CAL2 R2-B2 source contract - no Oracle writes...
node "%ROOT%tools\verify-cal2-personal-r2b2.mjs"
if errorlevel 1 goto failed_verify

rem Environment scoped to this launcher process only.
set "CAL2_PERSONAL_R2B2_DEMO_ENABLED=true"
set "CAL2_PERSONAL_R2B2_SCHEDULER_ENABLED=true"
echo.
echo [2/3] Building a fresh Java and Angular application...
call "%ROOT%build-production.cmd"
if errorlevel 1 goto failed_build
if not exist "%ROOT%app\core-banking-prototype.jar" goto missing_jar
if exist "%ROOT%app\BUILD-DIRTY" goto failed_build

echo.
echo [3/3] Starting the application on port %PORT%...
echo Browser URL: http://localhost:%PORT%
echo Demo principal enabled for local UAT only.
echo.
call "%ROOT%bin\start.cmd"
set "RESULT=%ERRORLEVEL%"
endlocal & exit /b %RESULT%

:relocate
if not exist "%ROOT%%~1" exit /b 0
if exist "%ARCHIVE%\%~1" (
  echo ERROR: Both root and archive contain %~1. No files were overwritten.
  echo Resolve the duplicate manually and rerun this launcher.
  exit /b 1
)
echo Relocating old FIX1 file: %~1
move "%ROOT%%~1" "%ARCHIVE%\%~1" >nul
if errorlevel 1 exit /b 1
exit /b 0

:bad_folder
echo ERROR: Execute tools\RUN-CAL2-R2B2-PILOT.cmd inside the full project.
goto failed
:bad_config
echo ERROR: config\application.yml not found. Configure Oracle connection first.
goto failed
:missing_node
echo ERROR: Node.js and npm must be available on PATH.
goto failed
:missing_java
echo ERROR: Java 21 must be available on PATH.
goto failed
:failed_relocate
echo ERROR: Previous FIX1 root files could not be safely relocated.
goto failed
:failed_layout
echo ERROR: Project root layout is invalid; build and start were not attempted.
goto failed
:busy_port
echo ERROR: Port %PORT% is already used by PID %PORT_PID%.
echo Stop that application manually and rerun.
goto failed
:failed_verify
echo ERROR: R2-B2 static verification failed; no build or start.
goto failed
:failed_build
echo ERROR: Build failed; application was not started.
echo Examine the first detailed error above this line.
goto failed
:missing_jar
echo ERROR: Build returned success but the executable JAR is missing.
goto failed
:failed
echo.
pause
endlocal & exit /b 1
