#!/bin/bash

# Spring AI Agent Project Startup Script

set -e

echo "🚀 Starting Spring AI Agent Project..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Creating from .env.example..."
    cp .env.example .env
    echo "📝 Please edit .env file and add your OpenAI API key, then run this script again."
    exit 1
fi

# Check if OPENAI_API_KEY is set
if grep -q "your-openai-api-key-here" .env; then
    echo "⚠️  Please set your OPENAI_API_KEY in .env file"
    exit 1
fi

echo "📦 Building and starting services..."
docker-compose up -d --build

echo "⏳ Waiting for services to start..."
sleep 30

echo "🔍 Checking service health..."

# Check Spring AI Agent
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Spring AI Agent is healthy"
else
    echo "❌ Spring AI Agent is not responding"
fi

# Check Angular UI
if curl -f http://localhost:4200 > /dev/null 2>&1; then
    echo "✅ Angular UI is healthy"
else
    echo "❌ Angular UI is not responding"
fi

echo ""
echo "🎉 Services are starting up!"
echo ""
echo "📱 Chat UI: http://localhost:4200"
echo "🔧 API: http://localhost:8080/v1"
echo "❤️  Health: http://localhost:8080/actuator/health"
echo ""
echo "📋 View logs: docker-compose logs -f"
echo "🛑 Stop services: docker-compose down"