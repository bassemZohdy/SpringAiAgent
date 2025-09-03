# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multi-module Spring Boot project for AI agents with three main components:
- **agent**: Java library providing base Agent and AiAgent interfaces with lifecycle management, memory, and metrics
- **spring-ai-agent**: Spring Boot application offering OpenAI-compatible chat completions API
- **ui**: Angular 17 frontend providing chat interface

## Build and Development Commands

### Quick Development Start
```bash
# Linux/macOS
./run-dev.sh

# Windows  
run-dev.bat
```

The development script automatically handles all setup steps and starts both backend and frontend.

### Manual Development Setup
```bash
# Build agent library first (required dependency)
cd agent && mvn clean install

# Run Spring Boot application with dev profile
cd spring-ai-agent && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run Angular UI with proxy (separate terminal)
cd ui && npm install && npm start
```

### Docker Development
```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Rebuild and restart
docker-compose up --build -d

# Stop all services
docker-compose down
```

### Testing
```bash
# Test agent library
cd agent && mvn test

# Test Spring Boot app
cd spring-ai-agent && mvn test

# Test Angular UI
cd ui && npm test
```

## Architecture Overview

### Agent Library (`agent/`)
Core abstractions in `ai.demo.agent.base` package:
- **Agent<TASK, RESULT>**: Base interface for task processors with lifecycle management
- **AiAgent<TASK, PROMPT, CHAT_RESPONSE, RESULT>**: Extension for AI/LLM agents with transformation pipeline
- **BaseAgent**: Abstract implementation providing state management, metrics, memory, and single-threaded execution
- **Task hierarchy**: TaskPriority, TaskStatus, TaskSize, TaskAttempt for structured task management

Key features:
- State machine: CREATED → STARTING → STARTED → PAUSING → PAUSED → STOPPING → STOPPED → RESETTING
- Memory system for recording execution history and learnings
- Metrics collection for performance monitoring
- Single-threaded task processing with CompletableFuture

### Spring AI Agent (`spring-ai-agent/`)
REST API service in `ai.demo.springagent` package:
- **ChatController**: OpenAI-compatible `/v1/chat/completions` and `/v1/models` endpoints
- **ChatService**: Integrates Spring AI's ChatClient for LLM interactions
- **DTOs**: ChatRequest/ChatResponse matching OpenAI API format

Uses Spring AI version 1.0.0-M4 with OpenAI integration.

### Angular UI (`ui/`)
Angular 17 application providing chat interface that communicates with the Spring Boot API.
- Includes proxy configuration for local development (`proxy.conf.json`)
- Calls `/v1` API endpoints matching Spring Boot controller

## Environment Configuration

Environment variables are configured in `.env.example` (copy to `.env` for local development):
- `OPENAI_API_KEY`: Your OpenAI API key (required)
- `SPRING_PROFILES_ACTIVE`: Set to `docker` for containerized deployment
- `SERVER_PORT`: Spring Boot port (default: 8080)
- `UI_PORT`: Angular UI port (default: 4200)

## Development Notes

- Java 21 is required across all modules
- Agent library must be built and installed before spring-ai-agent
- Spring AI milestone repository is configured for accessing Spring AI dependencies
- Multi-stage Docker builds optimize container sizes
- Health checks are configured for both backend and frontend services
- Async processing enabled with `@EnableAsync` for streaming support
- Provider implementations use Reactive patterns (Mono/Flux) with blocking adapters for Spring MVC
- Angular Material components integrated for modern UI experience

## API Compatibility

The implementation maintains full compatibility with:
- OpenAI Chat Completions API specification
- OpenAI Assistants API (threads and messages)
- Server-Sent Events (SSE) streaming standard
- Angular HTTP client and streaming APIs

## Task Management

Refer to `TODO.md` for current project tasks, roadmap, and issue tracking. This file is actively maintained and should be consulted for:
- Current implementation status
- Pending tasks and priorities
- Known issues and technical debt
- Future enhancement plans
- Task assignment format for new work