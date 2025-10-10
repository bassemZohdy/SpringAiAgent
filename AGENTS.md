# Repository Guidelines for Agentic Coding

## Project Structure & Module Organization
- Root Maven multi-module: `pom.xml` with `agent` (library) and `spring-ai-agent` (Spring Boot API). UI in `ui/` (Angular).
- Java sources: `agent/src/main/java`, `spring-ai-agent/src/main/java`.
- Java tests: `agent/src/test/java`, `spring-ai-agent/src/test/java`.
- Angular app: `ui/src/app` with components, services; tests under `ui/src` as `*.spec.ts`.
- Scripts: `scripts/` (Windows/Linux dev helpers), `run-dev.bat` (root), `docker-compose.yml` for containerized run.

## Build, Test, and Development Commands
- Backend build: `mvnw clean install` (Windows: `mvnw.cmd`) at repo root.
- Run API (dev): `mvnw -pl spring-ai-agent spring-boot:run -Dspring-boot.run.profiles=dev`.
- UI dev server: `cd ui && npm install && npm start` (proxy config enabled).
- All tests: `mvnw test` (Java) and `cd ui && npm test` (Angular).
- Single Java test: `mvnw test -Dtest=ClassName` or `mvnw -pl agent test -Dtest=TaskPriorityTest`.
- Single Angular test: `cd ui && npm test -- --test-name-pattern="ComponentName"`.
- One-shot dev (Windows): `scripts\run-dev.bat` or `run-dev.bat`.
- Docker (both services): `docker-compose up -d --build` and `docker-compose down`.

## Coding Style & Naming Conventions
- Java (21): 4-space indent, packages `ai.demo.agent.*`, `ai.demo.springagent.*`. Tests end with `*Test` or `*Tests`.
- Spring Boot: Controllers in `controller`, services in `service`, DTOs in `dto`, configs in `config`.
- TypeScript/Angular: 2-space indent, kebab-case file names (e.g., `enhanced-chat.component.ts`), services `*.service.ts`.
- JSON/YAML: 2 spaces, keep keys ordered logically.
- Imports: Group Java imports (java.*, javax.*, org.*, com.*) then alphabetically. TypeScript imports: third-party then local.
- Error handling: Use Spring's `@RestControllerAdvice` for global exceptions, custom exceptions extend `RuntimeException`.
- Types: Use specific types (e.g., `LocalDateTime` not `String`), prefer `Optional<T>` over null returns.

## Testing Guidelines
- Java: JUnit 5 and Spring Boot Test. Place tests under `src/test/java`. Run with `mvnw test`. Module-specific: `mvnw -pl agent test`.
- Angular: Karma/Jasmine via `npm test`. Coverage: `npm run test -- --code-coverage` (output in `ui/coverage`). Name tests `*.spec.ts` alongside source.
- Test naming: Use descriptive names `shouldDoSomethingWhenCondition()`. Mock dependencies with `@MockBean` in Spring tests.

## Commit & Pull Request Guidelines
- Use clear, imperative commits. Prefer Conventional Commits (e.g., `feat:`, `fix:`, `docs:`) when possible.
- PRs should include: concise description, linked issues (`Closes #123`), steps to reproduce/test, and screenshots for UI changes.
- Ensure `mvnw test` and `npm test` pass before requesting review.
- Always run tests before committing changes.

## Security & Configuration Tips
- Local dev env vars: `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `AI_MODEL`, `SERVER_PORT`. For LM Studio: `OPENAI_API_KEY=lm-studio`, `OPENAI_BASE_URL=http://localhost:1234/v1`.
- Windows dev script validates `.env.local` if present: see `scripts/run-dev.bat`. For LM Studio specifics, see `LM_STUDIO_SETUP.md`.
- Do not commit secrets. Use `.env.local` for local overrides.
- Linting: Java uses Maven compiler plugin, Angular uses built-in TypeScript compiler. No separate lint commands needed.
