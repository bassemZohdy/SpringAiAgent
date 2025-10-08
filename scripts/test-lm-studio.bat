@echo off
setlocal enabledelayedexpansion

echo [INFO] LM Studio E2E API smoke tests

set API_URL=http://localhost:8080/v1

echo [TEST] Non-streaming completion
curl -s -o resp.json -w "HTTP:%{http_code}\n" -H "Content-Type: application/json" %API_URL%/chat/completions -d "{\"model\":\"%AI_MODEL%\",\"messages\":[{\"role\":\"user\",\"content\":\"Ping\"}]}"
type resp.json
echo.

echo [TEST] Streaming completion
curl -N -s -o stream.txt -w "HTTP:%{http_code}\n" -H "Content-Type: application/json" -H "Accept: text/event-stream" %API_URL%/chat/completions -d "{\"model\":\"%AI_MODEL%\",\"messages\":[{\"role\":\"user\",\"content\":\"Stream test\"}],\"stream\":true}"
type stream.txt
echo.

echo [INFO] Done. Verify outputs above. For health: http://localhost:8080/actuator/health/provider

