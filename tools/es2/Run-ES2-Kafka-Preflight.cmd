@echo off
setlocal EnableExtensions
rem Read-only; does not create topics, send messages or access Oracle.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0ES2-Kafka-Preflight.ps1" %*
exit /b %ERRORLEVEL%
