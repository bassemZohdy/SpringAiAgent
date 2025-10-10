# Troubleshooting Guide

Use this guide to diagnose common runtime, build, and integration issues for Spring AI Agent.

## Backend Issues

### 1. `401 Unauthorized` / `invalid_api_key`
* Verify `OPENAI_API_KEY` is set for the running profile.
* When using LM Studio, set a placeholder key (e.g., `lm-studio`).
* Check `/actuator/health/provider` for detailed provider status.

### 2. `404 model_not_found`
* Ensure `AI_MODEL` matches the provider’s model identifier (`gpt-3.5-turbo`, `openai/gpt-oss-20b`, etc.).
* For LM Studio, confirm the model is loaded in the local server UI.

### 3. Slow or Stalled Streaming
* Confirm the client requests `Accept: text/event-stream` and keeps the HTTP connection open.
* Proxy/load balancers must support streaming; disable buffering for SSE routes.
* Use `curl -N` or the provided smoke-test scripts to reproduce outside the UI.

### 4. Validation Errors (`400 invalid_request`)
* Requests must include at least one `messages` entry with `role` (`user|assistant|system`) and non-empty `content`.
* `temperature` must be within `0.0`–`2.0`, and `max_tokens` must be positive.

## Front-end Issues

### 1. UI Cannot Reach API
* Check the proxy configuration (`ui/proxy.conf.json`) when running `npm start`.
* In production, set `API_BASE_URL` (for container deployments) or ensure the UI is hosted behind the same origin as the API.

### 2. Streaming Hangs in Browser
* The UI falls back to `fetch` + SSE parsing; inspect browser dev tools for network disconnects.
* Network errors trigger OpenAI-style error payloads—verify the backend returned JSON rather than HTML.

### 3. Build Size Warnings
* Production builds now split large components into lazy chunks and dynamically import the Markdown renderer.
* If custom changes re-introduce warnings, audit Angular Material imports and keep heavy dependencies inside `@defer` blocks.

## Deployment & Ops

### 1. Docker Container Fails Health Check
* Inspect logs with `docker-compose logs spring-ai-agent`.
* Confirm the container has connectivity to the provider endpoint.

### 2. Kubernetes Pods CrashLoopBackOff
* Describe the pod (`kubectl describe pod <name>`) to inspect events.
* Ensure secrets/config maps provide required environment variables.
* Verify liveness/readiness probes match exposed ports (`/actuator/health`).

### 3. Thread Data Lost After Restart
* Thread storage is in-memory by default. Introduce persistent storage (Redis, database) or export threads before shutdown if persistence is required.

## Diagnostic Commands

```bash
# Verify provider health
curl http://localhost:8080/actuator/health/provider

# Test streaming locally
curl -N http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"ping"}],"stream":true}'

# Angular production build check
cd ui && npm install && npm run build -- --configuration production
```

Keep this guide updated as new providers, storage options, or deployment topologies are added.
