# Spring AI Agent Project

A multi-module project providing unified OpenAI-compatible chat completions API with streaming support, multiple LLM providers, and modern Angular UI.

## 🚀 Features

- **Unified API**: Single `/v1/chat/completions` endpoint for streaming and non-streaming
- **OpenAI Integration**: Full OpenAI API compatibility with gpt-3.5-turbo and gpt-4 models
- **Streaming Support**: Real-time Server-Sent Events (SSE) streaming
- **Thread Management**: OpenAI Assistants API-compatible conversation threads
- **Modern UI**: Angular Material Design interface with streaming toggle
- **OpenAI Compatible**: Drop-in replacement for OpenAI Chat Completions API

## Project Structure

```
spring-ai-agent/
├── agent/                 # Java library for Agent and AIAgent representations
├── spring-ai-agent/      # Spring Boot app providing OpenAI-compatible APIs
├── ui/                   # Angular chat interface
├── docker-compose.yml    # Docker orchestration
└── README.md
```

## Quick Start with Docker

1. **Clone and setup environment**:
   ```bash
   cp .env.example .env
   # Edit .env and add your OpenAI API key
   ```

2. **Start all services**:
   ```bash
   docker-compose up -d
   ```

3. **Access the application**:
   - **Chat UI**: http://localhost:4200 (with streaming toggle and provider selection)
   - **API**: http://localhost:8080/v1 (OpenAI-compatible endpoints)
   - **Health Check**: http://localhost:8080/actuator/health
   - **API Docs**: See [API_USAGE.md](./API_USAGE.md)

## Development Setup

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 18+
- Docker & Docker Compose (for production deployment)

### Quick Development Start
```bash
# Linux/macOS
./run-dev.sh

# Windows
run-dev.bat
```

The development script will:
1. Check prerequisites
2. Create .env from .env.example if needed
3. Build agent library
4. Install Angular dependencies
5. Start Spring Boot API on port 8080
6. Start Angular UI on port 4200 with proxy configuration

### Manual Development Setup

#### Agent Library
```bash
cd agent
mvn clean install
```

#### Spring AI Agent
```bash
cd spring-ai-agent
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Angular UI
```bash
cd ui
npm install
npm start
```

## 🔌 API Endpoints

### Chat Completions
- `POST /v1/chat/completions` - Unified chat completions (streaming/non-streaming)
  - Query params: `stream=true/false`
  - Headers: `X-LLM-Provider: openai`

### Thread Management
- `POST /v1/threads` - Create conversation thread
- `GET /v1/threads/{id}` - Get thread details
- `GET /v1/threads/{id}/messages` - List thread messages  
- `POST /v1/threads/{id}/messages` - Add message to thread

### System
- `GET /v1/models` - Available models
- `GET /actuator/health` - Health check

## Docker Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up --build -d

# Scale services
docker-compose up -d --scale spring-ai-agent=2
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | Required for OpenAI provider |
| `SPRING_PROFILES_ACTIVE` | Spring profiles | `docker` |
| `SERVER_PORT` | Spring Boot port | `8080` |
| `UI_PORT` | Angular UI port | `4200` |

### API Usage Examples

```bash
# Non-streaming chat
curl -s http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "X-LLM-Provider: openai" \
  -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"Hello!"}]}'

# Streaming chat
curl -N http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -H "X-LLM-Provider: openai" \
  -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"Tell me a story"}],"stream":true}'
```

## 🏗️ Architecture

### Backend (Spring Boot)
- **Agent Library**: Core interfaces and implementations for Agent/AIAgent
- **Provider System**: Pluggable LLM provider architecture
  - OpenAI Provider (via Spring AI ChatClient)
- **Thread Storage**: In-memory conversation management
- **Streaming Engine**: SSE-based real-time response streaming

### Frontend (Angular)
- **Chat Service**: Unified service supporting streaming and non-streaming
- **Material UI**: Modern, responsive chat interface
- **Real-time Streaming**: EventSource and fetch-based streaming
- **OpenAI Compatible**: Full compatibility with OpenAI Chat Completions API

### Deployment
- **Docker**: Multi-stage containerized builds
- **Development**: Hot-reload development environment
- **Production**: Optimized production containers

## 📖 Documentation

- [API Usage Guide](./API_USAGE.md) - Comprehensive API documentation with examples
- [TODO List](./TODO.md) - Project roadmap and task tracking
- [CLAUDE.md](./CLAUDE.md) - Development setup and build instructions

## 🤝 Contributing

Refer to [TODO.md](./TODO.md) for current tasks and roadmap. New tasks can be assigned using the standard format provided in the TODO file.