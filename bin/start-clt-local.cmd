@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
call "%ROOT%\tools\clt\r6h\RUN-CLT-LOCAL-UAT.cmd" "%ROOT%"
exit /b %ERRORLEVEL%
