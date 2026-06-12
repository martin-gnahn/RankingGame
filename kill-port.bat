@echo off
setlocal

if "%~1"=="" (
    echo Usage: %~nx0 PORT
    echo Example: %~nx0 8080
    exit /b 1
)

set "PORT=%~1"
set "FOUND="

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    set "FOUND=1"
    echo Killing PID %%P on port %PORT%...
    taskkill /F /PID %%P
)

if not defined FOUND (
    echo No process found listening on port %PORT%.
    exit /b 1
)

exit /b 0
