@echo off
setlocal EnableExtensions
rem Launch an isolated development-only Chrome profile on local 127.0.0.1.
rem Keeps the server fail-closed; does not disable browser or TLS security.
set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" (
  echo CLT_R6L_CHROME_NOT_FOUND
  exit /b 1
)
start "" "%CHROME%" --no-proxy-server --disable-extensions --no-first-run --user-data-dir="%LOCALAPPDATA%\CLT-UAT-Chrome" "http://127.0.0.1:8091/#/collateral/reference-data/COLLATERAL_GROUP"
exit /b %ERRORLEVEL%
