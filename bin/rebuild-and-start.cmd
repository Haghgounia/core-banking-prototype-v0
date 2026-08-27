@echo off
setlocal
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"

echo Stopping any currently running prototype on port 8091...
call "%ROOT%\bin\stop.cmd"

echo.
echo Rebuilding frontend and backend for the VERSION in this folder...
call "%ROOT%\build-production.cmd"
if errorlevel 1 (
  echo.
  echo ERROR: Build failed. The application was not started.
  pause
  exit /b 1
)

echo.
echo Starting the freshly built version...
call "%ROOT%\bin\start.cmd"
endlocal
