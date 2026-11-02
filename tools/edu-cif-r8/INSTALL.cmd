@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "ROOT=%~dp0..\.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "TARGET=%ROOT%\backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java"
set "SOURCE=%~dp0EduLookupController.java"
set "BACKUP=%~dp0EduLookupController.before-R8.java.bak"
if not exist "%TARGET%" (echo R8_FAIL_TARGET_MISSING & exit /b 2)
if not exist "%SOURCE%" (echo R8_FAIL_PAYLOAD_MISSING & exit /b 3)
fc /b "%TARGET%" "%SOURCE%" >nul 2>&1
if not errorlevel 1 (echo EDU_CIF_R8_ALREADY_APPLIED_PASS & exit /b 0)
findstr /c:"EDU.V_EDUCATION" "%TARGET%" >nul
if errorlevel 1 (echo R8_FAIL_UNEXPECTED_SOURCE_NO_EDU_VIEW; NO_CHANGES & exit /b 4)
findstr /c:"CIF.EDUCATION" "%TARGET%" >nul
if not errorlevel 1 (echo R8_FAIL_MIXED_SOURCE; NO_CHANGES & exit /b 5)
if exist "%BACKUP%" (echo R8_FAIL_BACKUP_EXISTS; NO_CHANGES & exit /b 6)
copy /y "%TARGET%" "%BACKUP%" >nul
if errorlevel 1 (echo R8_FAIL_BACKUP & exit /b 7)
copy /y "%SOURCE%" "%TARGET%" >nul
if errorlevel 1 (echo R8_FAIL_COPY; RESTORE_BACKUP & copy /y "%BACKUP%" "%TARGET%" >nul & exit /b 8)
fc /b "%TARGET%" "%SOURCE%" >nul 2>&1
if errorlevel 1 (echo R8_FAIL_VERIFY; RESTORE_BACKUP & copy /y "%BACKUP%" "%TARGET%" >nul & exit /b 9)
findstr /c:"EDU.V_EDUCATION" "%TARGET%" >nul
if not errorlevel 1 (echo R8_FAIL_OLD_QUERY_REMAINS; RESTORE_BACKUP & copy /y "%BACKUP%" "%TARGET%" >nul & exit /b 10)
findstr /c:"CIF.EDUCATION" "%TARGET%" >nul
if errorlevel 1 (echo R8_FAIL_CIF_QUERY_MISSING; RESTORE_BACKUP & copy /y "%BACKUP%" "%TARGET%" >nul & exit /b 11)
echo EDU_CIF_R8_CONTROLLER_PASS; FOUR_QUERIES_TARGET_CIF
echo BACKUP=%BACKUP%
exit /b 0
