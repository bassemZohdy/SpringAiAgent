#!/usr/bin/env bash
set -euo pipefail

API_URL="http://localhost:8080/v1"
MODEL="${AI_MODEL:-openai/gpt-oss-20b}"

echo "[INFO] LM Studio E2E API smoke tests"

echo "[TEST] Non-streaming completion"
HTTP=$(curl -s -o resp.json -w "HTTP:%{http_code}\n" -H "Content-Type: application/json" "$API_URL/chat/completions" -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"Ping\"}]}" | tail -n1)
cat resp.json; echo
echo "$HTTP"; echo

echo "[TEST] Streaming completion"
HTTP=$(curl -N -s -o stream.txt -w "HTTP:%{http_code}\n" -H "Content-Type: application/json" -H "Accept: text/event-stream" "$API_URL/chat/completions" -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"Stream test\"}],\"stream\":true}" | tail -n1)
cat stream.txt; echo
echo "$HTTP"; echo

echo "[INFO] Done. Health: http://localhost:8080/actuator/health/provider"

