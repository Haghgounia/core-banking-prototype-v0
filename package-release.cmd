@echo off
setlocal

for /f "usebackq delims=" %%V in ("VERSION") do set VERSION=%%V
if "%VERSION%"=="" set VERSION=source
set OUT=..\core-banking-prototype-v0-%VERSION%.zip

if exist "%OUT%" del /q "%OUT%"

echo Creating clean source package: %OUT%
tar.exe -a -c -f "%OUT%" ^
  --exclude=frontend/node_modules ^
  --exclude=frontend/dist ^
  --exclude=frontend/.angular ^
  --exclude=backend/target ^
  --exclude=backend/src/main/resources/static ^
  --exclude=app/*.jar ^
  --exclude=app/*.jar.original ^
  --exclude=logs ^
  --exclude=data/document-storage ^
  --exclude=.git ^
  --exclude=.idea ^
  --exclude=.vscode ^
  --exclude=*.log ^
  --exclude=*.tmp ^
  --exclude=*.class ^
  .

if errorlevel 1 (
  echo Packaging failed.
  exit /b 1
)

echo Package created: %OUT%
endlocal
