@echo off
setlocal
cd /d "%~dp0\..\..\..\.."
python tools\fee3-backend\qualification\r3\qualify.py --mode source --report docs\install\FEE3-R3-SOURCE-RESULT.json
exit /b %errorlevel%
