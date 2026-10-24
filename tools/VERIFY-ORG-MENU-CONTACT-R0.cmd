@echo off
setlocal
pushd "%~dp0.."
node "tools\verify-org-menu-contact-review.mjs"
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%
