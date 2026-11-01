@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
node "%ROOT%\tools\smoke-org-contact-r5c4b.mjs"
exit /b %errorlevel%
