@echo off
setlocal enabledelayedexpansion

title Spring AI Agent Development Mode

echo 🔧 Starting Spring AI Agent in Development Mode...
echo.

REM Check prerequisites
echo 🔍 Checking prerequisites...

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java is not installed or not in PATH. Please install Java 21 or higher.
    pause
    exit /b 1
)

REM Get Java version (simplified check)
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)
echo ✅ Java !JAVA_VERSION! found

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven is not installed or not in PATH. Please install Maven 3.9 or higher.
    pause
    exit /b 1
)
echo ✅ Maven found

REM Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Node.js is not installed or not in PATH. Please install Node.js 18 or higher.
    pause
    exit /b 1
)

for /f %%i in ('node --version') do set NODE_VERSION=%%i
echo ✅ Node.js !NODE_VERSION! found

REM Check environment configuration
if not exist .env (
    echo ⚠️  .env file not found.
    if exist .env.example (
        echo ℹ️  Creating .env from .env.example...
        copy .env.example .env >nul
        echo ⚠️  Please edit .env file and set your OPENAI_API_KEY
        echo.
        echo Example:
        echo   OPENAI_API_KEY=sk-your-actual-key-here
        echo.
        pause
    ) else (
        echo ❌ .env.example file not found. Cannot create environment configuration.
        pause
        exit /b 1
    )
)

REM Check if OPENAI_API_KEY is set
findstr /C:"OPENAI_API_KEY=your-openai-api-key-here" .env >nul
if not errorlevel 1 (
    echo ❌ OPENAI_API_KEY is not set in .env file. Please set your OpenAI API key.
    pause
    exit /b 1
)

findstr /C:"OPENAI_API_KEY=" .env >nul
if errorlevel 1 (
    echo ❌ OPENAI_API_KEY is not found in .env file. Please set your OpenAI API key.
    pause
    exit /b 1
)

echo ✅ Environment configuration loaded
echo.

echo 🏗️  Building and starting services...

REM Step 1: Build and install agent library
echo ℹ️  Building agent library...
cd agent
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ❌ Failed to build agent library
    pause
    exit /b 1
)
cd ..
echo ✅ Agent library built and installed

REM Step 2: Install Angular dependencies
echo ℹ️  Installing Angular dependencies...
cd ui
if not exist node_modules (
    call npm install
    if errorlevel 1 (
        echo ❌ Failed to install Angular dependencies
        pause
        exit /b 1
    )
    echo ✅ Angular dependencies installed
) else (
    echo ℹ️  Angular dependencies already installed
)
cd ..

REM Step 3: Start Spring Boot application in background
echo ℹ️  Starting Spring Boot application...
cd spring-ai-agent
start "Spring Boot" /min cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../spring-boot.log 2>&1"
cd ..

REM Wait for Spring Boot to start
echo ℹ️  Waiting for Spring Boot to start...
set /a count=0
:wait_spring_boot
timeout /t 3 /nobreak >nul
curl -f http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 goto spring_boot_ready
set /a count+=1
if !count! gtr 40 (
    echo ❌ Spring Boot application did not start within 2 minutes. Check spring-boot.log for details.
    pause
    exit /b 1
)
goto wait_spring_boot

:spring_boot_ready
echo ✅ Spring Boot application started

REM Step 4: Start Angular UI in background
echo ℹ️  Starting Angular UI...
cd ui
start "Angular UI" /min cmd /c "npm start > ../angular.log 2>&1"
cd ..

REM Wait for Angular to start
echo ℹ️  Waiting for Angular UI to start...
set /a count=0
:wait_angular
timeout /t 5 /nobreak >nul
curl -f http://localhost:4200 >nul 2>&1
if not errorlevel 1 goto angular_ready
set /a count+=1
if !count! gtr 24 (
    echo ⚠️  Angular UI may still be starting. Check http://localhost:4200 in a few moments.
    goto angular_ready
)
goto wait_angular

:angular_ready
echo ✅ Angular UI started

echo.
echo 🎉 Development environment is ready!
echo.
echo 📱 Chat UI:      http://localhost:4200
echo 🔧 API:          http://localhost:8080/v1
echo ❤️  Health:      http://localhost:8080/actuator/health
echo.
echo 📋 Logs:
echo    Spring Boot:  type spring-boot.log
echo    Angular UI:   type angular.log
echo.
echo 🛑 Press any key to open services in browser, or Ctrl+C to exit

pause >nul

REM Open services in default browser
start http://localhost:4200
start http://localhost:8080/actuator/health

echo.
echo 🌐 Services opened in browser
echo 📋 You can monitor logs in the respective windows
echo 🛑 Close the Spring Boot and Angular UI windows to stop services
echo.
pause