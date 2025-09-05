@echo off
echo Starting SpringAI Agent Development Environment...

REM Set environment variables
set OPENAI_API_KEY=lm-studio
set OPENAI_BASE_URL=http://localhost:1234/v1
set AI_MODEL=openai/gpt-oss-20b
set SERVER_PORT=8080

echo Environment variables set
echo OPENAI_API_KEY=%OPENAI_API_KEY%
echo OPENAI_BASE_URL=%OPENAI_BASE_URL%
echo AI_MODEL=%AI_MODEL%
echo SERVER_PORT=%SERVER_PORT%

REM Build agent library first
echo.
echo Building agent library...
cd agent
call mvn clean install
if errorlevel 1 (
    echo Failed to build agent library
    exit /b 1
)

REM Start Spring Boot application in background
echo.
echo Starting Spring Boot application...
cd ..\spring-ai-agent
start "SpringAI-Agent" /min cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=dev"

REM Give Spring Boot time to start
timeout /t 10

REM Start Angular UI
echo.
echo Starting Angular UI...
cd ..\ui
call npm install
start "Angular-UI" /min cmd /c "npm start"

echo.
echo Development environment started!
echo - Spring Boot: http://localhost:8080
echo - Angular UI: http://localhost:4200
echo - API Documentation: http://localhost:8080/swagger-ui.html (if available)
echo.
echo Press any key to stop all services...
pause

REM Cleanup - kill all processes
echo Stopping services...
taskkill /f /im java.exe 2>nul
taskkill /f /im node.exe 2>nul
echo Done.