# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multi-module Spring Boot project providing unified OpenAI-compatible chat completions API with streaming support:
- **agent**: Java library providing base Agent and AiAgent interfaces with lifecycle management, memory, and metrics
- **spring-ai-agent**: Spring Boot application offering OpenAI-compatible chat completions API with SSE streaming
- **ui**: Angular 17 Material Design frontend providing chat interface

## Development Commands

### Quick Development Start
```bash
# Linux/macOS - automatic setup, build, and start
./scripts/run-dev.sh

# Windows - automatic setup, build, and start
scripts\run-dev.bat
```

Development scripts handle:
- Prerequisites checking (Java 21+, Maven 3.9+, Node.js 18+)
- Environment configuration (.env/.env.local setup)
- Agent library build and install
- Dependency installation and service startup
- Clean port management (8080/4200)
- Background process management with logs

### Manual Development Setup
```bash
# Step 1: Build agent library (required dependency)
cd agent && mvn clean install

# Step 2: Start Spring Boot with dev profile
cd spring-ai-agent && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Step 3: Start Angular UI (separate terminal)
cd ui && npm install && npm start
```

### Testing Commands
```bash
# Agent library tests
cd agent && mvn test

# Spring Boot application tests
cd spring-ai-agent && mvn test

# Angular unit tests
cd ui && npm test

# Angular E2E tests (Playwright)
cd ui && npm run test:e2e

# OpenAI API validation tests (requires OpenAI API key)
./scripts/test-openai.sh    # Linux/macOS
scripts\test-openai.bat     # Windows
```

### Docker Development
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Rebuild and restart
docker-compose up --build -d

# Stop all services
docker-compose down
```

## Architecture Overview

### Agent Library (`agent/`)
Core abstractions in `ai.demo.agent.base` package with state machine architecture:

**Core Interfaces:**
- **Agent<TASK, RESULT>**: Base interface for task processors with lifecycle management
- **AiAgent<TASK, PROMPT, CHAT_RESPONSE, RESULT>**: Extension for AI/LLM agents with transformation pipeline
- **BaseAgent**: Abstract implementation with single-threaded execution, metrics, memory, and state management

**State Machine Flow:**
```
CREATED → STARTING → STARTED → PAUSING → PAUSED → STOPPING → STOPPED → RESETTING → CREATED
```

**Key Components:**
- Task hierarchy: TaskPriority, TaskStatus, TaskSize, TaskAttempt
- AgentMemory: Execution history and learning recording with compaction
- AgentMetrics: Performance monitoring and statistics
- CompletableFuture-based async processing with single-threaded executor

### Spring AI Agent (`spring-ai-agent/`)
OpenAI-compatible REST API in `ai.demo.springagent` package:

**Core Controllers:**
- **ChatController**: `/v1/chat/completions` (streaming + non-streaming), `/v1/models`, `/v1/sessions/stats`
- **ThreadController**: OpenAI Assistants API-compatible thread management

**Key Services:**
- **ChatService**: Spring AI ChatClient integration with provider abstraction
- **MemoryAdvisor**: Optional memory context injection for conversations

**Provider Architecture:**
- OpenAI provider via Spring AI ChatClient
- Environment-based provider selection via `X-LLM-Provider` header
- Health check endpoints: `/actuator/health/provider`

**Streaming Implementation:**
- Server-Sent Events (SSE) via SseEmitter
- Async processing with `@EnableAsync`
- OpenAI-compatible chunk format

### Angular UI (`ui/`)
Angular 17 Material Design application:

**Key Features:**
- Real-time streaming chat interface with EventSource
- Provider selection and streaming toggle
- Material Design components (MatCard, MatFormField, MatButton)
- Proxy configuration for local development (`proxy.conf.json`)
- OpenAI API compatibility

**Architecture:**
- ChatService unified API client
- Streaming/non-streaming mode switching
- Error handling and reconnection logic
- TypeScript interfaces matching OpenAI API format

## Environment Configuration

### Primary Configuration Files
- `.env` - Production/OpenAI configuration
- `.env.local` - Local development (prioritized for development overrides)
- `.env.example` - Template with all available options

### Key Environment Variables
```bash
# OpenAI Configuration
OPENAI_API_KEY=your-openai-api-key-here    # Required for OpenAI
OPENAI_BASE_URL=https://api.openai.com     # OpenAI API endpoint

# AI Model Configuration
AI_MODEL=gpt-5-nano                        # Default cost-optimized model

# Development vs Production
SPRING_PROFILES_ACTIVE=dev                 # Use 'dev' for local, 'docker' for containers
SERVER_PORT=8080                           # Spring Boot port
UI_PORT=4200                               # Angular UI port
```

### Environment Loading Priority
1. `.env.local` (highest priority, for local development)
2. `.env` (fallback/production configuration)
3. System environment variables
4. Spring Boot application.properties

## Development Workflow

### First-time Setup
1. Copy environment template: `cp .env.example .env.local`
2. Configure OpenAI API key in `.env.local`
3. Run quick start script: `./scripts/run-dev.sh` or `scripts\run-dev.bat`
4. Access services:
   - Chat UI: http://localhost:4200
   - API: http://localhost:8080/v1
   - Health: http://localhost:8080/actuator/health

### Code Architecture Patterns
- **Agent Library**: Template method pattern with abstract `doProcess()` method
- **Spring Boot**: Controller → Service → Provider layered architecture
- **Angular**: Service → Component → Template pattern with Material Design
- **Error Handling**: Global exception mapping to OpenAI-compatible format
- **Streaming**: SSE with async CompletableFuture processing

### Important Development Notes
- **Java 21** required across all modules
- Agent library **must be built first** before spring-ai-agent compilation
- Spring AI **1.0.0-M4** milestone repository configured for dependencies
- **Single-threaded execution** in BaseAgent for thread safety
- **Memory compaction** automatically generates summaries when entry count > 10
- **OpenAI integration** requires valid `OPENAI_API_KEY` configured
- **Port management**: Development scripts ensure clean 8080/4200 startup
- **Environment post-processor** loads `.env.local` when `dev` profile active

## Testing Strategy

### Unit Testing
- Agent library: JUnit 5 tests for BaseAgent lifecycle and state management
- Spring Boot: MockMvc tests for controllers, service layer tests
- Angular: Jasmine/Karma unit tests for services and components

### Integration Testing
- API endpoint compatibility with OpenAI specification
- SSE streaming functionality and error handling
- Environment configuration loading
- Provider health checks

### E2E Testing
- Playwright tests for Angular UI chat functionality
- API validation tests for OpenAI integration
- Docker compose service orchestration

## Common Development Tasks

### Adding New LLM Provider
1. Implement provider interface in `spring-ai-agent/src/main/java/ai/demo/springagent/provider/`
2. Add provider configuration properties
3. Update ChatService provider selection logic
4. Add health check implementation
5. Update controller to accept new provider via header

### Extending Agent Library
1. Extend BaseAgent for domain-specific functionality
2. Implement `doProcess()` method with task-specific logic
3. Override lifecycle hooks (`onTaskStarted`, `onTaskCompleted`, etc.)
4. Add custom memory summarization via `generateMemorySummary()`

### UI Component Development
1. Use Angular Material components for consistency
2. Follow OpenAI API format for request/response interfaces
3. Implement proper error handling for streaming failures
4. Update proxy configuration if adding new API endpoints

## API Compatibility

The implementation maintains **full OpenAI API compatibility**:
- Chat Completions API specification (v1)
- Assistants API format for threads and messages
- Server-Sent Events streaming standard
- OpenAI error response format with proper HTTP status codes
- Drop-in replacement capability for OpenAI clients

## Task Management

Refer to `TODO.md` for current project tasks, roadmap, and issue tracking. This file is actively maintained and should be consulted for:
- Current implementation status and completed features
- Pending tasks with priority levels (High/Medium/Low)
- Known issues and technical debt items
- Future enhancement plans and architectural decisions
- Standard task assignment format for new work

The TODO.md file is the single source of truth for project status and should be updated as features are completed or new requirements are identified.