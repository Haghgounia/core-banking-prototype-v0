@echo off
setlocal EnableExtensions
rem Requires operator invocation with -ApproveCreate; Kafka metadata write only.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0ES2-Create-Topic-Explicit.ps1" %*
exit /b %ERRORLEVEL%
