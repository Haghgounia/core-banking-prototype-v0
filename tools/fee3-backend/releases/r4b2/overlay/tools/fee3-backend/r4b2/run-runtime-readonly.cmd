@echo off
setlocal
cd /d "%~dp0..\..\.."
python tools\fee3-backend\r4b2\runtime-readonly.py %*
exit /b %errorlevel%
