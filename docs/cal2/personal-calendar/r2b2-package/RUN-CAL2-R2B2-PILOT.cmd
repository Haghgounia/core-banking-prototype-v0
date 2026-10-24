@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "ROOT=%~dp0"
cd /d "%ROOT%" || goto bad_folder

rem CAL2 R2-B2 local single-user PILOT launcher.
rem Builds fresh sources first, then starts the canonical backend + packaged Angular UI.
rem Does not execute SQL, change Oracle schema, or stop an existing process.

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

set "PORT=%SERVER_PORT%"
if not defined PORT set "PORT=8091"
set "PORT_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do set "PORT_PID=%%P"
if defined PORT_PID goto busy_port

echo ==============================================================
echo CAL2 R2-B2 PILOT - Build and Start (Windows)
echo Base directory: %ROOT%
echo ==============================================================
echo For local single-user UAT ONLY; NOT a production deployment.
echo Please confirm the CAL2 read-only schema check before UAT.
echo.

echo [1/3] Verifying CAL2 R2-B2 source contract (no Oracle writes)...
node "%ROOT%tools\verify-cal2-personal-r2b2.mjs"
if errorlevel 1 goto failed_verify

rem Explicitly opt in to the local demo principal; never use this in production.
set "CAL2_PERSONAL_R2B2_DEMO_ENABLED=true"
set "CAL2_PERSONAL_R2B2_SCHEDULER_ENABLED=true"
echo.
echo [2/3] Building fresh Java + Angular artifacts...
call "%ROOT%build-production.cmd"
if errorlevel 1 goto failed_build

if not exist "%ROOT%app\core-banking-prototype.jar" goto missing_jar
if exist "%ROOT%app\BUILD-DIRTY" goto failed_build

echo.
echo [3/3] Starting Core Banking Prototype on port %PORT%...
echo After startup open: http://localhost:%PORT%
echo Keep this CMD window open while using the application.
echo Demo principal is ENABLED only for this child process.
echo.
call "%ROOT%bin\start.cmd"
set "RESULT=%ERRORLEVEL%"
endlocal & exit /b %RESULT%

:bad_folder
echo ERROR: The launcher must be in the ROOT of the FULL project source.
echo For patch ZIP: extract OVER the existing source root, preserving paths.
goto failed
:bad_config
echo ERROR: config\application.yml not found. Configure local Oracle connection first.
goto failed
:missing_node
echo ERROR: Node.js and npm are required and must be on PATH.
goto failed
:missing_java
echo ERROR: Java 21 is required and must be on PATH.
goto failed
:busy_port
echo ERROR: Port %PORT% is already used, PID %PORT_PID%.
echo Stop that application manually before building or starting R2-B2.
goto failed
:failed_verify
echo ERROR: CAL2 R2-B2 static source verification failed; no build/start.
goto failed
:failed_build
echo ERROR: Build failed. See the output above. Application was NOT started.
echo Maven Wrapper might need access to its Maven repository.
goto failed
:missing_jar
echo ERROR: Build returned success but the executable JAR is missing.
goto failed
:failed
echo.
pause
endlocal & exit /b 1
