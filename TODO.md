# TODO - Spring AI Agent Project

This file tracks ongoing tasks, improvements, and future enhancements for the Spring AI Agent project.

## 2025-10-10 Updates
- Completed: Unified dev script under `scripts/run-dev.bat` (root `run-dev.bat` delegates).
- Completed: `.env` prioritized over `.env.local`; OpenAI low-cost default model `gpt-4o-mini` enabled.
- Completed: UI proxy updated to hit Spring API at `http://localhost:8080`.
- Completed: Playwright configuration and smoke e2e added under `ui/` and npm scripts wired.
- Completed: Agent module build unblocked on JDK 24 by removing Lombok from core classes.
- Completed: Dev startup robustness and logging improvements; port-listen readiness check.
- Verified: Servers start and e2e smoke test passes locally.

Follow-ups:
- Return non-null assistant content from provider and extend e2e to cover message send.
- Re-enable LM Studio route by switching `ui/proxy.conf.json` back to `http://localhost:1234` when LM Studio chat is enabled.
- Add integration tests for streaming SSE.

## 🚀 Current Implementation Status

### ✅ Completed Features
- [x] Unified `/v1/chat/completions` endpoint (streaming + non-streaming)
- [x] OpenAI-compatible request/response format
- [x] OpenAI provider integration (Anthropic support removed for simplicity)
- [x] In-memory thread storage with OpenAI Assistants API format
- [x] Server-Sent Events (SSE) streaming support
- [x] Angular UI with streaming toggle and OpenAI provider
- [x] Thread management endpoints (`POST /v1/threads`, `GET/POST /v1/threads/{id}/messages`)
- [x] Material Design UI components integration
- [x] Comprehensive API documentation
- [x] **OpenAI-Compatible Error Handling**: Proper HTTP status codes and error format
- [x] **Structured Logging**: Request/response timing and context logging
- [x] **Global Exception Handler**: Centralized error management with proper mapping

### 🔄 In Progress
- [ ] Unit test coverage expansion
- [x] **LM Studio Integration Fix**: Spring Boot environment variable loading for local development

### 📋 Pending Tasks

#### High Priority
- [x] **Local Development Environment**: Fix Spring Boot configuration to properly load .env.local for LM Studio integration
- [x] **Development Scripts**: Ensure run-dev.sh/run-dev.bat correctly restart services with updated environment
- [ ] **LM Studio Testing**: Validate complete UI/API integration with local LM Studio server (smoke scripts added: scripts/test-lm-studio.*)
- [ ] **Testing**: Create integration tests for streaming endpoints
- [x] **Testing**: Add unit tests for error handling and exception mapping
- [x] **Documentation**: Update main README.md with error handling features and LM Studio setup
- [x] **Input Validation**: Add comprehensive request parameter validation

#### Medium Priority  
- [x] **Token Estimation**: Implement approximate token counting + truncation
- [ ] **Rate Limiting**: Add rate limiting for API endpoints
- [ ] **Metrics**: Add performance metrics collection
- [ ] **Security**: Implement API key rotation and secure storage
- [ ] **Caching**: Add response caching for repeated requests

#### Low Priority
- [ ] **Provider Extensions**: Add support for additional LLM providers (Gemini, Llama, etc.)
- [ ] **Thread Persistence**: Optional database persistence for threads
- [ ] **Websocket Support**: Real-time bidirectional communication
- [ ] **Thread Sharing**: Multi-user thread collaboration features
- [ ] **Advanced Streaming**: Support for function calls in streaming mode

## 🐛 Known Issues
- [x] ~~OpenAI API authentication errors cause 500 responses instead of proper error format~~ ✅ Fixed
- [x] **Environment Loading**: Spring Boot not loading .env.local configuration for LM Studio integration
- [x] **Development Scripts**: run-dev.bat not properly restarting services with updated environment variables  
- [x] Angular streaming service needs better error handling for network failures
- [x] Thread context truncation implemented with approximate token budget
- [x] Provider health checks added via Actuator health indicator (provider)

## 🔧 Technical Debt
- [ ] Refactor streaming logic to use proper reactive streams
- [x] Improve TypeScript type safety in Angular services
- [ ] Add proper input validation for all API endpoints
- [x] ~~Implement proper async exception handling~~ ✅ Fixed with GlobalExceptionHandler

## 📚 Documentation Updates Needed
- [x] Update main README.md with unified endpoint information
- [x] Create API reference documentation
- [x] Add deployment guide for Docker and Kubernetes
- [x] Document provider configuration and setup
- [x] Create troubleshooting guide

## 🧪 Testing Requirements
- [ ] Unit tests for all provider implementations
- [ ] Integration tests for streaming endpoints
- [ ] End-to-end tests for Angular UI
- [ ] Load testing for concurrent streaming requests
- [ ] API compatibility tests against OpenAI specification

## 📊 Monitoring & Observability
- [x] Add health check endpoints for each provider (Actuator /health/provider)
- [x] ~~Implement request/response logging~~ ✅ Added structured logging with timing metrics
- [ ] Add metrics for response times and error rates
- [ ] Create dashboard for system monitoring

## 🚀 Future Enhancements
- [ ] **Multi-modal Support**: Image and file upload capabilities
- [ ] **Plugin System**: Extensible architecture for custom providers
- [ ] **Advanced Threading**: Branching conversations and merge capabilities
- [ ] **AI Agent Orchestration**: Multi-agent conversations
- [ ] **Custom Model Fine-tuning**: Support for custom model deployment

---

## 📝 Task Assignment Format

When assigning new tasks, please use this format:

```markdown
### Task: [Brief Description]
**Priority**: High/Medium/Low
**Category**: Feature/Bug/Documentation/Testing
**Assigned**: [Date]
**Description**: Detailed description of the task
**Acceptance Criteria**:
- [ ] Criterion 1
- [ ] Criterion 2
**Estimated Effort**: [Time estimate]
**Dependencies**: [Any blocking tasks]
```

---

## 📅 Version History
- **v1.0.0** - Initial unified API implementation with streaming support
- **v1.1.0** - Provider selection and Angular UI enhancements
- **v1.2.0** - OpenAI-compatible error handling and structured logging (current)

---

## 🎯 Recent Completions (2025-09-04)

### ✅ OpenAI-Compatible Error Handling Implementation
- **ErrorResponse.java**: OpenAI-compatible error format with factory methods
- **GlobalExceptionHandler.java**: Comprehensive exception mapping with proper HTTP status codes
  - 401 Unauthorized → `invalid_api_key` error
  - 429 Too Many Requests → `rate_limit_exceeded` error  
  - 404 Not Found → `model_not_found` error
  - 400 Bad Request → `invalid_request` error
  - 500 Internal Server Error → `internal_error` error
- **ChatService.java**: Removed generic RuntimeExceptions, added structured logging
- **Anthropic Provider Removal**: Simplified codebase to focus on OpenAI only

### ✅ LM Studio Integration Setup (2025-09-04)
- **LM_STUDIO_SETUP.md**: Comprehensive guide for local development with LM Studio
- **.env.local Configuration**: Ready-to-use configuration file for local LM Studio testing
- **.env.local.example**: Template for local development setup
- **Development Scripts Enhancement**: Updated run-dev.sh and run-dev.bat to prioritize .env.local
- **API Validation**: Confirmed LM Studio OpenAI API compatibility (streaming + non-streaming)
- **Model Discovery**: Validated actual model name (`openai/gpt-oss-20b`) from LM Studio

### 🎯 Impact
- Better user experience with clear error messages
- Production-ready error handling for real-world API failures
- Improved debugging with structured logging and timing metrics
- Full OpenAI API compatibility for drop-in replacement
- **Local Development Ready**: Zero-cost testing with LM Studio integration
- **Privacy-First Development**: Local AI testing without external API calls

---

*Last Updated: 2025-10-08*

## 2025-10-08 Updates
- Added High Priority: Backend build failure fix for ChatService param.
- Added Known Issue: Angular CSS budget exceed in enhanced-chat component.
- Added In Progress: Build Unblocker to restore compilation.
- Completed: Backend build compiles after ChatService advisor param fix.
 - Completed: UI builds after raising `anyComponentStyle` budget to 6/8kb (initial bundle still triggers warning threshold).

### 2025-10-08 Updates (cont.)
- Completed: Spring Boot dev profile loads `.env.local` via EnvironmentPostProcessor.
- Completed: `scripts/run-dev.bat` and `scripts/run-dev.sh` load env and perform clean restarts (free 8080/4200).
- Completed: `ChatRequest` input validation tightened (model/messages/roles/temperature/maxTokens).
- Completed: Unit tests for validation and error mapping.
- Completed: README updated with LM Studio setup and error handling docs.
- Pending: Add robust SSE streaming integration test harness (MockMvc + SseEmitter async).

### Known Issues Addendum (2025-10-08)
- [x] ~~Compile Error: `ChatMemory.CONVERSATION_ID` not found (Spring AI 1.0.0-M4)~~ Fixed
- [x] Angular initial bundle exceeds 500kb production warning; component style budget resolved.

### Docs Addendum (2025-10-08)
- [x] Contributor guide added: `AGENTS.md` at repo root.
