# Spring AI Agent Project

A multi-module project with Agent library, Spring AI implementation, and Angular UI for chat functionality.

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
   - **Chat UI**: http://localhost:4200
   - **API**: http://localhost:8080/v1
   - **Health Check**: http://localhost:8080/actuator/health

## Development Setup

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 18+
- Docker & Docker Compose

### Agent Library
```bash
cd agent
mvn clean install
```

### Spring AI Agent
```bash
cd spring-ai-agent
mvn spring-boot:run
```

### Angular UI
```bash
cd ui
npm install
npm start
```

## API Endpoints

- `POST /v1/chat/completions` - Chat completions (OpenAI-compatible)
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

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | Required |
| `SPRING_PROFILES_ACTIVE` | Spring profiles | `docker` |
| `SERVER_PORT` | Spring Boot port | `8080` |
| `UI_PORT` | Angular UI port | `4200` |

## Architecture

- **Agent Library**: Core interfaces and implementations for Agent/AIAgent
- **Spring AI Agent**: REST API server with OpenAI-compatible endpoints
- **Angular UI**: Modern chat interface with real-time messaging
- **Docker**: Containerized deployment with multi-stage builds