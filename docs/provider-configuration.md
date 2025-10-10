# Provider Configuration

Spring AI Agent ships with an OpenAI-compatible provider and supports local development targets such as LM Studio. Configuration is driven by environment variables so deployments can switch providers without code changes.

## Core Environment Variables

| Variable | Description | Default |
| --- | --- | --- |
| `OPENAI_API_KEY` | API key or token for the upstream provider. Required for OpenAI. | `your-openai-api-key-here` |
| `OPENAI_BASE_URL` | Base URL for the provider’s OpenAI-compatible API. | `https://api.openai.com` |
| `AI_MODEL` | Default model identifier used by the backend. | `gpt-5-nano` |
| `AI_MAX_HISTORY_TOKENS` | Token budget for thread recall. | `4096` |
| `AI_CHARS_PER_TOKEN` | Character-to-token heuristic. | `4` |

These values populate Spring AI’s OpenAI client (`spring.ai.openai.*`) and the custom agent settings defined in `application.yml`. Override them per environment by exporting variables or using `.env` files.

## Profiles & Local Overrides

* **`dev` profile** – Enables verbose logging, exposes additional Actuator endpoints, and reads `.env.local` automatically via the dev scripts.
* **`docker` profile** – Activated by Docker Compose and tuned for containerized deployments.

To run locally against OpenAI:

```bash
export OPENAI_API_KEY=sk-your-key
export OPENAI_BASE_URL=https://api.openai.com/v1
export AI_MODEL=gpt-3.5-turbo
./mvnw -pl spring-ai-agent spring-boot:run
```

## LM Studio / Local LLMs

1. Install LM Studio and start the OpenAI-compatible server (default `http://localhost:1234/v1`).
2. Copy `.env.local.example` to `.env.local` and set:
   ```
   OPENAI_API_KEY=lm-studio
   OPENAI_BASE_URL=http://localhost:1234/v1
   AI_MODEL=<model-name-from-lm-studio>
   SPRING_PROFILES_ACTIVE=dev
   ```
3. Launch the dev script (`./scripts/run-dev.sh` or `scripts\run-dev.bat`). The backend picks up `.env.local`, and the UI proxies requests to the Spring Boot API.

## Multiple Providers

At runtime, the `X-LLM-Provider` header selects the provider. The default implementation wires the OpenAI provider, but you can extend the provider registry to add additional backends:

1. Implement a new `ChatProvider` in `spring-ai-agent/provider` that wraps the target API.
2. Register it in the provider configuration and update the factory to map `X-LLM-Provider` values to concrete beans.
3. Document required environment variables for the new provider (API key, base URL, custom parameters).

## Memory Advisor Toggle

The optional memory advisor uses extra thread context to improve responses. Enable it per-request with `X-Use-Memory-Advisor: true`. No additional configuration is required, but keep the token budget (`AI_MAX_HISTORY_TOKENS`) in mind.

## Health & Diagnostics

* `GET /actuator/health/provider` verifies provider connectivity and credentials.
* Increase logging for `ai.demo.springagent` to inspect outbound calls: `LOGGING_LEVEL_AI_DEMO_SPRINGAGENT=DEBUG`.
* Use the provided scripts (`scripts/test-openai.sh`, `scripts/test-lm-studio.sh`) to smoke-test provider integrations.

## Secrets Management Tips

* Never commit real API keys. Use environment variables, Kubernetes secrets, or secret managers (AWS Secrets Manager, HashiCorp Vault).
* For CI/CD, inject provider settings through pipeline variables.
* When rotating keys, restart the API pods or containers so Spring AI reloads credentials.
