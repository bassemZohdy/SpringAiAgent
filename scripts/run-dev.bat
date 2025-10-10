@echo off
setlocal enabledelayedexpansion

title Spring AI Agent Development Mode

echo [SETUP] Starting Spring AI Agent in Development Mode...
echo.

REM Check prerequisites
echo [CHECK] Checking prerequisites...

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH. Please install Java 21 or higher.
    pause
    exit /b 1
)

REM Get Java version (simplified check)
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)
echo [OK] Java !JAVA_VERSION! found

REM Check Maven or Maven Daemon
set MAVEN_CMD=
where mvnd >nul 2>&1
if not errorlevel 1 goto :use_mvnd

where mvn >nul 2>&1
if not errorlevel 1 goto :use_mvn

echo [ERROR] Neither Maven (mvn) nor Maven Daemon (mvnd) is installed or in PATH. Please install Maven 3.9 or higher.
pause
exit /b 1

:use_mvnd
set MAVEN_CMD=mvnd
echo [OK] Maven Daemon (mvnd) found
goto :maven_done

:use_mvn
set MAVEN_CMD=mvn
echo [OK] Maven (mvn) found
goto :maven_done

:maven_done

REM Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed or not in PATH. Please install Node.js 18 or higher.
    pause
    exit /b 1
)

for /f %%i in ('node --version') do set NODE_VERSION=%%i
echo [OK] Node.js !NODE_VERSION! found

REM Check environment configuration (prefer .env; fallback to .env.local)
set ENV_FILE=..\.env
if exist ..\.env (
    echo [OK] Using .env for development
) else if exist ..\.env.local (
    set ENV_FILE=..\.env.local
    echo [OK] Using .env.local for development
) else (
    echo [WARN] No .env or .env.local found.
    if exist ..\.env.example (
        echo [INFO] Creating .env from .env.example...
        copy ..\.env.example ..\.env >nul
        echo [WARN] Please edit .env file and set your configuration
        echo For OpenAI API, set: OPENAI_API_KEY=sk-your-actual-key-here
        pause
    ) else (
        echo [ERROR] No environment configuration files found. Cannot create environment configuration.
        pause
        exit /b 1
    )
)

REM Load environment variables from file
echo [INFO] Loading environment variables from %ENV_FILE%
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    set line=%%A
    if not "!line!"=="" if not "!line:~0,1!"=="#" set "%%A=%%B"
)

REM Validate configuration
if not defined OPENAI_API_KEY (
    echo [ERROR] OPENAI_API_KEY is not set. Please set it in %ENV_FILE%.
    pause
    exit /b 1
)

if /I "%OPENAI_API_KEY%"=="your-openai-api-key-here" (
    echo [ERROR] OPENAI_API_KEY is set to placeholder value. Update %ENV_FILE%.
    pause
    exit /b 1
)

if /I "%OPENAI_API_KEY%"=="lm-studio" (
    echo [INFO] Using LM Studio configuration for local development
    if /I not "%OPENAI_BASE_URL%"=="http://localhost:1234/v1" (
        echo [ERROR] LM Studio base URL not configured. Expected: http://localhost:1234/v1
        pause
        exit /b 1
    )
)

echo [OK] Environment loaded and validated from %ENV_FILE%
echo.

REM Ensure low-cost model defaults for OpenAI
if not defined OPENAI_BASE_URL set OPENAI_BASE_URL=https://api.openai.com
if /I "%OPENAI_BASE_URL%"=="https://api.openai.com" (
    if not defined AI_MODEL (
        set AI_MODEL=gpt-4o-mini
        echo [INFO] AI_MODEL not set; defaulting to gpt-4o-mini for low-cost testing
    ) else if /I "%AI_MODEL%"=="gpt-5-nano" (
        set AI_MODEL=gpt-4o-mini
        echo [INFO] Switching AI_MODEL from gpt-5-nano to gpt-4o-mini for compatibility and cost
    )
)

echo [BUILD] Building and starting services...

REM Step 1: Build and install agent library
echo [INFO] Building agent library...
cd ..\agent
call %MAVEN_CMD% clean install -DskipTests
if errorlevel 1 (
    echo [ERROR] Failed to build agent library
    pause
    exit /b 1
)
cd ..\scripts
echo [OK] Agent library built and installed

REM Step 2: Install Angular dependencies
echo [INFO] Installing Angular dependencies...
cd ..\ui
if not exist node_modules (
    call npm install
    if errorlevel 1 (
        echo [ERROR] Failed to install Angular dependencies
        pause
        exit /b 1
    )
    echo [OK] Angular dependencies installed
) else (
    echo [INFO] Angular dependencies already installed
)
cd ..\scripts

REM Kill any process using ports 8080 (API) and 4200 (UI) to ensure clean restart
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr LISTENING') do (
    echo [INFO] Stopping process on port 8080 (PID %%p)
    taskkill /F /PID %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":4200" ^| findstr LISTENING') do (
    echo [INFO] Stopping process on port 4200 (PID %%p)
    taskkill /F /PID %%p >nul 2>&1
)

REM Step 3: Start Spring Boot application in background
echo [INFO] Starting Spring Boot application...
cd ..\spring-ai-agent
start "Spring Boot" /min cmd /c "%MAVEN_CMD% spring-boot:run -Dspring-boot.run.profiles=dev > ../spring-boot.log 2>&1"
cd ..\scripts

REM Wait for Spring Boot port to listen (don’t require health=UP)
echo [INFO] Waiting for Spring Boot port 8080 to listen...
set /a count=0
:wait_spring_boot
timeout /t 3 /nobreak >nul
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr LISTENING') do (
    echo [OK] Spring Boot is listening on 8080 (PID %%p)
    goto spring_boot_ready
)
set /a count+=1
if !count! gtr 40 (
    echo [WARN] Spring Boot did not confirm listening within 2 minutes. Proceeding anyway; check spring-boot.log.
    goto spring_boot_ready
)
goto wait_spring_boot

:spring_boot_ready
echo [OK] Proceeding after Spring Boot startup wait

REM Step 4: Start Angular UI in background
echo [INFO] Starting Angular UI...
cd ..\ui
start "Angular UI" /min cmd /c "npm start > ../angular.log 2>&1"
cd ..\scripts

REM Wait for Angular to start
echo [INFO] Waiting for Angular UI to start...
set /a count=0
:wait_angular
timeout /t 5 /nobreak >nul
curl -f http://localhost:4200 >nul 2>&1
if not errorlevel 1 goto angular_ready
set /a count+=1
if !count! gtr 24 (
    echo [WARN] Angular UI may still be starting. Check http://localhost:4200 in a few moments.
    goto angular_ready
)
goto wait_angular

:angular_ready
echo [OK] Angular UI started

echo.
echo [SUCCESS] Development environment is ready!
echo.
echo [UI] Chat UI:      http://localhost:4200
echo [API] API:          http://localhost:8080/v1
echo [HEALTH] Health:      http://localhost:8080/actuator/health
echo.
echo [LOGS] Logs:
echo    Spring Boot:  type spring-boot.log
echo    Angular UI:   type angular.log
echo.
echo [ACTION] Press any key to open services in browser, or Ctrl+C to exit

pause >nul

REM Open services in default browser
start http://localhost:4200
start http://localhost:8080/actuator/health

echo.
echo [BROWSER] Services opened in browser
echo [INFO] You can monitor logs in the respective windows
echo [INFO] Close the Spring Boot and Angular UI windows to stop services
echo.
pause
