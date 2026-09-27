@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
if "%~1"=="" (echo ERROR: SQL file path is required.& exit /b 2)
set "SQLFILE=%~f1"
if not exist "%SQLFILE%" (echo ERROR: SQL file not found: %SQLFILE%& exit /b 2)

rem Remote Oracle qualification is JDBC-first by design. SQL*Plus is opt-in only.
if /I "%CORE_BANKING_ORACLE_CLIENT%"=="SQLPLUS" (
  where sqlplus >nul 2>nul || (echo ERROR: CORE_BANKING_ORACLE_CLIENT=SQLPLUS but sqlplus.exe is not on PATH.& exit /b 20)
  echo INFO: Oracle client explicitly selected: SQL*Plus.
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%SQLFILE%"
  exit /b %errorlevel%
)

set "OJDBC=%USERPROFILE%\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.26.2.0.0\ojdbc11-23.26.2.0.0.jar"
if not exist "%OJDBC%" (
  echo ERROR: Oracle JDBC driver was not found at:
  echo   %OJDBC%
  echo Run the normal Maven build once to populate the local Maven repository, then retry.
  echo Optional fallback: set CORE_BANKING_ORACLE_CLIENT=SQLPLUS and put sqlplus.exe on PATH.
  exit /b 20
)
where java >nul 2>nul || (echo ERROR: Java is not available on PATH.& exit /b 20)
echo INFO: Executing Oracle SQL through project JDBC client against remote CORE_BANKING_ORACLE_CONNECT.
java -cp "%OJDBC%" "%ROOT%\tools\java\OracleJdbcSqlRunner.java" "%SQLFILE%"
exit /b %errorlevel%
