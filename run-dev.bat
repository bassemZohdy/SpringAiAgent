@echo off
echo 🚀 Spring AI Agent Development Launcher
echo.
echo This script will start the development environment using the scripts folder.
echo.
if exist "scripts\run-dev.bat" (
    echo Starting development environment...
    call scripts\run-dev.bat
) else (
    echo ❌ Error: scripts\run-dev.bat not found!
    echo Please ensure you're running this from the project root directory.
    pause
)