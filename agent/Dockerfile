# Agent Library Dockerfile
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the library
RUN mvn clean install -DskipTests

# Create a minimal runtime image (for library, we mainly need the built jar)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar agent-library.jar

# This is primarily for building and installing the library
# The actual usage will be as a dependency in other containers
CMD ["echo", "Agent library built successfully"]