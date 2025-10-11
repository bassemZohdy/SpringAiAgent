# Agent Framework Development Guidelines

## 📁 Project Structure & Module Organization

### Multi-Module Architecture
- **Root Maven multi-module**: `pom.xml` with `agent` (library) and `spring-ai-agent` (Spring Boot API). UI in `ui/` (Angular).
- **Agent Library**: `agent/src/main/java` with organized package structure
- **Spring Boot API**: `spring-ai-agent/src/main/java`
- **Test Structure**: Mirrors main package structure in both modules
- **Angular UI**: `ui/src/app` with components, services; tests under `ui/src` as `*.spec.ts`.
- **Scripts**: `scripts/` (Windows/Linux dev helpers), `run-dev.bat` (root), `docker-compose.yml` for containerized run.

### Agent Library Package Organization
```
agent/src/main/java/ai/demo/agent/
├── base/                   # Core abstractions and utilities
│   ├── Agent.java          # Base agent interface
│   ├── AiAgent.java        # AI/LLM specialization
│   ├── BaseAgent.java      # Abstract implementation with state machine
│   ├── AgentConfiguration.java
│   ├── AgentException.java
│   ├── AgentMemory.java
│   ├── AgentState.java
│   └── task/               # Task abstractions
│       ├── Task.java
│       ├── TaskAttempt.java
│       ├── TaskPriority.java
│       ├── TaskSize.java
│       └── TaskStatus.java
├── chat/                   # Chat agent specialization
│   └── ChatAgent.java      # Conversational agent interface
├── task/                   # Task agent specialization
│   └── TaskAgent.java      # Discrete task processing interface
└── metrics/                # Performance metrics
    ├── AgentMetrics.java   # Abstract base metrics
    ├── TaskAgentMetrics.java  # Task-specific metrics
    └── ChatAgentMetrics.java  # Chat-specific metrics
```

### Test Package Structure
Test structure mirrors main code organization:
```
agent/src/test/java/ai/demo/agent/
├── base/                   # Tests for base components
├── chat/                   # Chat agent tests
├── task/                   # Task agent tests
└── metrics/                # Metrics tests
```

## Build, Test, and Development Commands
- Backend build: `mvnw clean install` (Windows: `mvnw.cmd`) at repo root.
- Run API (dev): `mvnw -pl spring-ai-agent spring-boot:run -Dspring-boot.run.profiles=dev`.
- UI dev server: `cd ui && npm install && npm start` (proxy config enabled).
- All tests: `mvnw test` (Java) and `cd ui && npm test` (Angular).
- Single Java test: `mvnw test -Dtest=ClassName` or `mvnw -pl agent test -Dtest=TaskPriorityTest`.
- Single Angular test: `cd ui && npm test -- --test-name-pattern="ComponentName"`.
- One-shot dev (Windows): `scripts\run-dev.bat` or `run-dev.bat`.
- Docker (both services): `docker-compose up -d --build` and `docker-compose down`.

## 🎨 Coding Style & Naming Conventions

### Java (21)
- **Indentation**: 4-space indent
- **Packages**:
  - Agent library: `ai.demo.agent.base.*`, `ai.demo.agent.chat.*`, `ai.demo.agent.task.*`, `ai.demo.agent.metrics.*`
  - Spring Boot: `ai.demo.springagent.*`
- **Test naming**: Tests end with `*Test` or `*Tests`
- **Class naming**:
  - Agent interfaces: `Agent`, `ChatAgent`, `TaskAgent`, `AiAgent`
  - Implementations: `BaseAgent`, `YourAgentName`
  - Metrics: `AgentMetrics`, `TaskAgentMetrics`, `ChatAgentMetrics`
- **Method naming**: Use clear, descriptive names following Java conventions

### Spring Boot
- **Controllers**: in `controller` package
- **Services**: in `service` package
- **DTOs**: in `dto` package
- **Configs**: in `config` package
- **Error handling**: Use Spring's `@RestControllerAdvice` for global exceptions

### TypeScript/Angular
- **Indentation**: 2-space indent
- **File naming**: kebab-case file names (e.g., `enhanced-chat.component.ts`)
- **Services**: `*.service.ts`
- **Components**: `*.component.ts`
- **Models**: `*.model.ts` or `*.interface.ts`

### General Guidelines
- **JSON/YAML**: 2 spaces, keep keys ordered logically
- **Imports**: Group Java imports (java.*, javax.*, org.*, com.*) then alphabetically
- **TypeScript imports**: third-party then local
- **Error handling**: Custom exceptions extend `RuntimeException`
- **Types**: Use specific types, prefer `Optional<T>` over null returns

## 🧪 Testing Guidelines

### Java Testing
- **Framework**: JUnit 5 and Spring Boot Test
- **Test location**: Place tests under `src/test/java` mirroring main package structure
- **Running tests**:
  - All tests: `mvnw test`
  - Agent library: `mvnw -pl agent test`
  - Specific test: `mvnw test -Dtest=ClassName`
- **Test naming**: Use descriptive names `shouldDoSomethingWhenCondition()`
- **Mocking**: Mock dependencies with `@MockBean` in Spring tests
- **Coverage**: Comprehensive test coverage for all agent components

### Angular Testing
- **Framework**: Karma/Jasmine via `npm test`
- **Coverage**: `npm run test -- --code-coverage` (output in `ui/coverage`)
- **Test files**: Name tests `*.spec.ts` alongside source
- **Specific test**: `cd ui && npm test -- --test-name-pattern="ComponentName"`

### Agent-Specific Testing
- **Agent lifecycle**: Test all state transitions (CREATED → STARTING → STARTED → etc.)
- **Metrics**: Verify metrics collection for both success and failure scenarios
- **Memory**: Test memory compaction and summary generation
- **Specialized agents**: Test both ChatAgent and TaskAgent implementations

## 📝 Commit & Pull Request Guidelines
- Use clear, imperative commits. Prefer Conventional Commits (e.g., `feat:`, `fix:`, `docs:`)
- PRs should include: concise description, linked issues (`Closes #123`), steps to reproduce/test
- Include screenshots for UI changes
- Ensure `mvnw test` and `npm test` pass before requesting review
- Always run tests before committing changes

## 🔒 Security & Configuration Tips
- **Environment variables**: `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `AI_MODEL`, `SERVER_PORT`
- **Local overrides**: Use `.env.local` for local development settings
- **Security**: Never commit secrets or API keys
- **Windows development**: Use `scripts/run-dev.bat` for automated setup
- **Linting**: Java uses Maven compiler plugin, Angular uses built-in TypeScript compiler

## 🚀 Agent Development Best Practices

### Creating New Agents
1. **Choose the right interface**:
   - Use `TaskAgent<TASK, RESULT>` for discrete task processing
   - Use `ChatAgent<REQUEST, RESPONSE>` for conversational interactions
   - Use `AiAgent` for LLM-based transformation pipelines

2. **Extend BaseAgent**:
   ```java
   public class YourAgent extends BaseAgent<YourTask, YourResult> {
       public YourAgent() {
           super("YourAgent", "1.0.0", configuration, capabilities);
       }

       @Override
       protected YourResult doProcess(YourTask task) {
           // Your implementation here
       }
   }
   ```

3. **Implement lifecycle hooks**:
   - `onTaskStarted()` - Called when task processing begins
   - `onTaskCompleted()` - Called on successful completion
   - `onTaskFailed()` - Called when processing fails

4. **Use appropriate metrics**:
   - Task agents: `TaskAgentMetrics`
   - Chat agents: `ChatAgentMetrics`

### Package Organization Rules
- **base/**: Only core abstractions and utilities
- **chat/**: Chat-specific interfaces and implementations
- **task/**: Task-specific interfaces and implementations
- **metrics/**: Metrics classes and related utilities
- **Test structure**: Must mirror main package structure
