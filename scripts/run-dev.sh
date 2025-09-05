#!/bin/bash

# Spring AI Agent Development Mode Script
# This script starts all services locally for development

set -e

echo "🔧 Starting Spring AI Agent in Development Mode..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check prerequisites
echo "🔍 Checking prerequisites..."

# Check Java
if ! java -version >/dev/null 2>&1; then
    print_error "Java is not installed or not in PATH. Please install Java 21 or higher."
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
fi
print_status "Java $JAVA_VERSION found"

# Check Maven or Maven Daemon
MAVEN_CMD=""
if command -v mvnd >/dev/null 2>&1; then
    MAVEN_CMD="mvnd"
    print_status "Maven Daemon (mvnd) found"
elif command -v mvn >/dev/null 2>&1; then
    MAVEN_CMD="mvn"
    print_status "Maven (mvn) found"
else
    print_error "Neither Maven (mvn) nor Maven Daemon (mvnd) is installed or in PATH. Please install Maven 3.9 or higher."
fi

# Check Node.js
if ! node --version >/dev/null 2>&1; then
    print_error "Node.js is not installed or not in PATH. Please install Node.js 18 or higher."
fi

NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    print_error "Node.js 18 or higher is required. Current version: $NODE_VERSION"
fi
print_status "Node.js v$(node --version | cut -d'v' -f2) found"

# Check environment configuration (prioritize .env.local for development)
ENV_FILE=".env.local"
if [ ! -f .env.local ]; then
    print_warning ".env.local file not found."
    if [ -f .env.local.example ]; then
        print_info "Creating .env.local from .env.local.example..."
        cp .env.local.example .env.local
        print_status "Using .env.local for local development with LM Studio support"
    elif [ -f .env ]; then
        print_info "Using existing .env file"
        ENV_FILE=".env"
    elif [ -f .env.example ]; then
        print_info "Creating .env.local from .env.example..."
        cp .env.example .env.local
        print_warning "Please edit .env.local file and set your configuration"
        echo ""
        echo "For local LM Studio testing, .env.local is already configured."
        echo "For OpenAI API, set: OPENAI_API_KEY=sk-your-actual-key-here"
        echo ""
        read -p "Press Enter after reviewing .env.local configuration..."
    else
        print_error "No environment configuration files found. Cannot create environment configuration."
    fi
else
    print_status "Using .env.local for development"
fi

# Source environment variables from the selected file
if [ -f "$ENV_FILE" ]; then
    export $(cat "$ENV_FILE" | grep -v '^#' | xargs)
    print_info "Environment loaded from $ENV_FILE"
fi

# Check configuration based on environment
if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "your-openai-api-key-here" ]; then
    if [ "$OPENAI_API_KEY" = "lm-studio" ]; then
        print_info "Using LM Studio configuration for local development"
        if [ -z "$OPENAI_BASE_URL" ] || [ "$OPENAI_BASE_URL" = "https://api.openai.com" ]; then
            print_error "LM Studio base URL not configured. Expected: http://localhost:1234/v1"
        fi
    else
        print_error "OPENAI_API_KEY is not set in $ENV_FILE file. Please set your OpenAI API key or use LM Studio configuration."
    fi
fi
print_status "Environment configuration validated"

# Function to cleanup background processes
cleanup() {
    echo ""
    print_info "Shutting down services..."
    
    # Kill background processes if they exist
    if [ ! -z "$AGENT_PID" ] && kill -0 $AGENT_PID 2>/dev/null; then
        print_info "Stopping Spring Boot application (PID: $AGENT_PID)"
        kill $AGENT_PID
    fi
    
    if [ ! -z "$UI_PID" ] && kill -0 $UI_PID 2>/dev/null; then
        print_info "Stopping Angular UI (PID: $UI_PID)"
        kill $UI_PID
    fi
    
    print_status "Cleanup completed"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

echo ""
echo "🏗️  Building and starting services..."

# Step 1: Build and install agent library
print_info "Building agent library..."
cd ../scripts/agent
if ! $MAVEN_CMD clean install -DskipTests; then
    print_error "Failed to build agent library"
fi
cd ../scripts
print_status "Agent library built and installed"

# Step 2: Install Angular dependencies
print_info "Installing Angular dependencies..."
cd ../scripts/ui
if [ ! -d node_modules ]; then
    if ! npm install; then
        print_error "Failed to install Angular dependencies"
    fi
    print_status "Angular dependencies installed"
else
    print_info "Angular dependencies already installed"
fi
cd ../scripts

# Step 3: Start Spring Boot application in background
print_info "Starting Spring Boot application..."
cd ../scripts/spring-ai-agent
$MAVEN_CMD spring-boot:run -Dspring-boot.run.profiles=dev > ../spring-boot.log 2>&1 &
AGENT_PID=$!
cd ../scripts

# Wait for Spring Boot to start
print_info "Waiting for Spring Boot to start..."
for i in {1..60}; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        break
    fi
    if ! kill -0 $AGENT_PID 2>/dev/null; then
        print_error "Spring Boot application failed to start. Check spring-boot.log for details."
    fi
    sleep 2
done

if ! curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
    print_error "Spring Boot application did not start within 2 minutes. Check spring-boot.log for details."
fi
print_status "Spring Boot application started (PID: $AGENT_PID)"

# Step 4: Start Angular UI in background
print_info "Starting Angular UI..."
cd ../scripts/ui
npm start > ../angular.log 2>&1 &
UI_PID=$!
cd ../scripts

# Wait for Angular to start
print_info "Waiting for Angular UI to start..."
for i in {1..60}; do
    if curl -f http://localhost:4200 >/dev/null 2>&1; then
        break
    fi
    if ! kill -0 $UI_PID 2>/dev/null; then
        print_error "Angular UI failed to start. Check angular.log for details."
    fi
    sleep 3
done

if ! curl -f http://localhost:4200 >/dev/null 2>&1; then
    print_warning "Angular UI may still be starting. Check http://localhost:4200 in a few moments."
fi
print_status "Angular UI started (PID: $UI_PID)"

echo ""
echo "🎉 Development environment is ready!"
echo ""
echo "📱 Chat UI:      http://localhost:4200"
echo "🔧 API:          http://localhost:8080/v1"
echo "❤️  Health:      http://localhost:8080/actuator/health"
echo ""
echo "📋 Logs:"
echo "   Spring Boot:  tail -f spring-boot.log"
echo "   Angular UI:   tail -f angular.log"
echo ""
echo "🛑 Press Ctrl+C to stop all services"

# Keep script running and show logs
while true; do
    sleep 1
    
    # Check if processes are still running
    if ! kill -0 $AGENT_PID 2>/dev/null; then
        print_error "Spring Boot application stopped unexpectedly"
    fi
    
    if ! kill -0 $UI_PID 2>/dev/null; then
        print_error "Angular UI stopped unexpectedly"
    fi
done