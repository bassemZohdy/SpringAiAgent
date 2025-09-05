@echo off
setlocal enabledelayedexpansion

echo 🚀 Starting Spring AI Agent Project...

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Check if .env file exists
if not exist .env (
    echo ⚠️  .env file not found. Creating from .env.example...
    copy .env.example .env
    echo 📝 Please edit .env file and add your OpenAI API key, then run this script again.
    exit /b 1
)

REM Check if OPENAI_API_KEY is set
findstr /C:"your-openai-api-key-here" .env >nul
if not errorlevel 1 (
    echo ⚠️  Please set your OPENAI_API_KEY in .env file
    exit /b 1
)

echo 📦 Building and starting services...
docker-compose up -d --build

echo ⏳ Waiting for services to start...
timeout /t 30 /nobreak >nul

echo 🔍 Checking service health...

REM Check Spring AI Agent
curl -f http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 (
    echo ✅ Spring AI Agent is healthy
) else (
    echo ❌ Spring AI Agent is not responding
)

REM Check Angular UI
curl -f http://localhost:4200 >nul 2>&1
if not errorlevel 1 (
    echo ✅ Angular UI is healthy
) else (
    echo ❌ Angular UI is not responding
)

echo.
echo 🎉 Services are starting up!
echo.
echo 📱 Chat UI: http://localhost:4200
echo 🔧 API: http://localhost:8080/v1
echo ❤️  Health: http://localhost:8080/actuator/health
echo.
echo 📋 View logs: docker-compose logs -f
echo 🛑 Stop services: docker-compose down

pause