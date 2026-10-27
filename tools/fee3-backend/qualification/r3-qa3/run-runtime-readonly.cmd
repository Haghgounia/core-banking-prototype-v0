@echo off
setlocal
cd /d "%~dp0\..\..\..\.."
python tools\fee3-backend\qualification\r3-qa3\qualify.py --mode runtime --base-url http://127.0.0.1:8091 --report docs\install\FEE3-R3-QA3-RUNTIME-RESULT.json
exit /b %errorlevel%
