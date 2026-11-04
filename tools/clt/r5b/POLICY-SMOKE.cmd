@echo off
setlocal EnableExtensions
if "%~1"=="" (echo CLT_R5B_ERROR=TARGET_REQUIRED & exit /b 2)
set "TARGET=%~f1"
set "ROOT=%TEMP%\clt-r5b-%RANDOM%-%RANDOM%"
if not exist "%ROOT%" mkdir "%ROOT%"
set "SRC=%TARGET%\backend\src\main\java\com\behsazan\corebanking\collateral"
set "SMOKE=%TARGET%\tools\clt\r5b\UnallocationIntakePolicySmoke.java"
javac -encoding UTF-8 -d "%ROOT%" "%SRC%\capacity\CollateralCapacityModels.java" "%SRC%\capacity\CollateralCapacityPolicy.java" "%SRC%\allocation\CollateralAllocationRequestModels.java" "%SRC%\allocation\CollateralAllocationRequestPolicy.java" "%SRC%\unallocation\CollateralUnallocationModels.java" "%SRC%\unallocation\CollateralUnallocationPolicy.java" "%SRC%\unallocation\CollateralUnallocationIntakePolicy.java" "%SMOKE%"
if errorlevel 1 (rmdir /s /q "%ROOT%" & exit /b 1)
java -cp "%ROOT%" UnallocationIntakePolicySmoke
set "CODE=%ERRORLEVEL%"
rmdir /s /q "%ROOT%"
exit /b %CODE%
