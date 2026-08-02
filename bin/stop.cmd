@echo off
setlocal
set "FOUND="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8091" ^| findstr "LISTENING"') do (
  set "FOUND=1"
  echo Stopping process %%P on port 8091...
  taskkill /PID %%P /F
)
if not defined FOUND echo No process is listening on port 8091.
endlocal
