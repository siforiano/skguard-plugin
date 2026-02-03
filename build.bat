@echo off
REM Build script for SKGuard - Windows
REM Generates both Lite and Premium versions

echo ========================================
echo   SKGuard Build System
echo ========================================
echo.

:menu
echo Select build option:
echo 1. Build Lite (FREE version)
echo 2. Build Premium (PAID version)
echo 3. Build BOTH versions
echo 4. Clean build directory
echo 5. Exit
echo.
set /p choice="Enter choice (1-5): "

if "%choice%"=="1" goto lite
if "%choice%"=="2" goto premium
if "%choice%"=="3" goto both
if "%choice%"=="4" goto clean
if "%choice%"=="5" goto end

echo Invalid choice!
goto menu

:lite
echo.
echo Building SKGuard Lite...
call mvn clean package -P lite
echo.
echo Lite build complete! Check target/SKGuard-Lite-1.0-SNAPSHOT.jar
pause
goto menu

:premium
echo.
echo Building SKGuard Premium...
call mvn clean package -P premium
echo.
echo Premium build complete! Check target/SKGuard-Premium-1.0-SNAPSHOT.jar
pause
goto menu

:both
echo.
echo Building both versions...
echo.
echo [1/2] Building Lite...
call mvn clean package -P lite
echo.
echo [2/2] Building Premium...
call mvn clean package -P premium
echo.
echo Both builds complete!
echo - Lite: target/SKGuard-Lite-1.0-SNAPSHOT.jar
echo - Premium: target/SKGuard-Premium-1.0-SNAPSHOT.jar
pause
goto menu

:clean
echo.
echo Cleaning build directory...
call mvn clean
echo Clean complete!
pause
goto menu

:end
echo Goodbye!
