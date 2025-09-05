@echo off
echo Starting Spring Boot with LM Studio configuration...

REM Set environment variables from .env.local
set OPENAI_API_KEY=lm-studio
set OPENAI_BASE_URL=http://localhost:1234/v1
set AI_MODEL=openai/gpt-oss-20b
set SPRING_PROFILES_ACTIVE=dev
set SERVER_PORT=8081

echo Environment configured:
echo   OPENAI_API_KEY=%OPENAI_API_KEY%
echo   OPENAI_BASE_URL=%OPENAI_BASE_URL%  
echo   AI_MODEL=%AI_MODEL%
echo   SERVER_PORT=%SERVER_PORT%
echo.

cd ..\spring-ai-agent
mvn spring-boot:run -Dspring-boot.run.profiles=dev