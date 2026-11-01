@echo off
setlocal EnableExtensions
cd /d "%~dp0..\.."
if not exist "backend\pom.xml" (echo EDU_R3_ERROR=PROJECT_ROOT_NOT_FOUND & exit /b 1)
set "SRC=tools\edu-lookup-r3\files\backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java"
set "DEST=backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java"
if not exist "%SRC%" (echo EDU_R3_ERROR=PATCH_FILE_MISSING & exit /b 1)
if exist "%DEST%" (echo EDU_R3_ERROR=TARGET_EXISTS_NO_OVERWRITE & exit /b 2)
if not exist "backend\src\main\java\com\behsazan\corebanking\referencedata\education\web" mkdir "backend\src\main\java\com\behsazan\corebanking\referencedata\education\web"
copy /y "%SRC%" "%DEST%" >nul
if errorlevel 1 (echo EDU_R3_ERROR=COPY_FAILED & exit /b 3)
fc /b "%SRC%" "%DEST%" >nul
if errorlevel 1 (echo EDU_R3_ERROR=VERIFY_FAILED & exit /b 4)
echo EDU_LOOKUP_R3_SOURCE_INSTALL_PASS
echo NEXT=bin\stop.cmd ^& build-production.cmd ^& bin\start.cmd
