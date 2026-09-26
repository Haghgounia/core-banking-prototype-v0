@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
if "%~1"=="" (echo ERROR: SQL file path is required.& exit /b 2)
set "SQLFILE=%~f1"
if not exist "%SQLFILE%" (echo ERROR: SQL file not found: %SQLFILE%& exit /b 2)
where sqlplus >nul 2>nul
if not errorlevel 1 (
  echo INFO: Executing Oracle SQL through local SQL*Plus against CORE_BANKING_ORACLE_CONNECT.
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%SQLFILE%"
  exit /b %errorlevel%
)
set "OJDBC=%USERPROFILE%\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.26.2.0.0\ojdbc11-23.26.2.0.0.jar"
if not exist "%OJDBC%" (
  echo ERROR: Local SQL*Plus is not installed and Oracle JDBC driver was not found at:
  echo   %OJDBC%
  echo Run the normal Maven build once to populate the local Maven repository, then retry.
  exit /b 20
)
where java >nul 2>nul || (echo ERROR: Java is not available on PATH.& exit /b 20)
echo INFO: Local SQL*Plus not found; using Oracle JDBC client for remote database.
java -cp "%OJDBC%" "%ROOT%\tools\java\OracleJdbcSqlRunner.java" "%SQLFILE%"
exit /b %errorlevel%
