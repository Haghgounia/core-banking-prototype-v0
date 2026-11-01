@echo off
cd /d "%~dp0..\.."
findstr /c:"EDU.V_EDUCATION_FIELD_LOOKUP" "backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java" >nul
if errorlevel 1 (echo EDU_R3_SOURCE_FAIL & exit /b 1)
echo EDU_R3_SOURCE_PASS
