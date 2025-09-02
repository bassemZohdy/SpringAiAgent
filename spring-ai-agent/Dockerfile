# Spring AI Agent Dockerfile
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the entire project structure
COPY pom.xml ./
COPY agent ./agent/
COPY spring-ai-agent ./spring-ai-agent/

# Build from the root to handle multi-module dependencies
RUN mvn clean install -DskipTests -pl agent,spring-ai-agent

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /build/spring-ai-agent/target/*.jar spring-ai-agent.jar

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -D -u 1000 -G spring spring

USER spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "spring-ai-agent.jar"]