@echo off
setlocal
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"

set "SCHEMA_NAME=%~1"
if not defined SCHEMA_NAME set "SCHEMA_NAME=DPS"

set "TABLE_PREFIX=%~2"
if not defined TABLE_PREFIX set "TABLE_PREFIX=REF_"
if "%TABLE_PREFIX%"=="*" set "TABLE_PREFIX="

set "OUTPUT_DIR=%~3"
if not defined OUTPUT_DIR set "OUTPUT_DIR=%ROOT%\database\oracle\exports"
for %%I in ("%OUTPUT_DIR%") do set "OUTPUT_DIR=%%~fI"

set "JAR=%ROOT%\app\core-banking-prototype.jar"
if not exist "%JAR%" set "JAR=%ROOT%\backend\target\core-banking-prototype.jar"
if not exist "%JAR%" (
  echo ERROR: core-banking-prototype.jar was not found.
  echo Run build-production.cmd first.
  pause
  exit /b 1
)

set "CONFIG_PATH=%ROOT%\config\application.yml"
set "CONFIG_URI=%CONFIG_PATH:\=/%"
set "OUTPUT_URI=%OUTPUT_DIR:\=/%"

echo.
echo Oracle database export
echo ----------------------
echo Schema       : %SCHEMA_NAME%
echo Table prefix : %TABLE_PREFIX%
echo Output       : %OUTPUT_DIR%
echo.
echo The command will connect to Oracle using config\application.yml.
echo It will extract table DDL, sequences, indexes, PK, UK, CHECK, FK,
echo comments, triggers, grants, and table rows as INSERT statements.
echo.
choice /C YN /N /M "Continue? [Y/N]: "
if errorlevel 2 goto cancelled

echo.
echo Starting export...
java -jar "%JAR%" ^
  "--spring.config.additional-location=optional:file:/%CONFIG_URI%" ^
  "--spring.main.web-application-type=none" ^
  "--spring.main.banner-mode=off" ^
  "--core-banking.database-export.enabled=true" ^
  "--core-banking.database-export.schema=%SCHEMA_NAME%" ^
  "--core-banking.database-export.table-prefix=%TABLE_PREFIX%" ^
  "--core-banking.database-export.output-directory=%OUTPUT_URI%"
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if "%EXIT_CODE%"=="0" (
  echo Export finished successfully.
) else (
  echo ERROR: Export failed with code %EXIT_CODE%.
  echo Review the messages above.
)
pause
endlocal & exit /b %EXIT_CODE%

:cancelled
echo.
echo Export cancelled by user.
pause
endlocal & exit /b 0
