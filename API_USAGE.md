# Spring AI Agent - Unified OpenAI-Compatible API

This document describes the unified API endpoints that provide OpenAI-compatible chat completions with streaming support and multiple LLM provider selection.

## Features

- **Unified Endpoint**: Single `/v1/chat/completions` endpoint for both streaming and non-streaming requests
- **Provider Selection**: Support for multiple LLM providers via `X-LLM-Provider` header
- **Thread Support**: In-memory conversation threads following OpenAI Assistants API format
- **Streaming**: Server-Sent Events (SSE) streaming support
- **OpenAI Compatibility**: Request/response format matches OpenAI Chat Completions API

## API Endpoints

### Chat Completions

**Endpoint**: `POST /v1/chat/completions`

**Headers**:
- `Content-Type: application/json`
- `X-LLM-Provider: openai|anthropic` (optional, defaults to `openai`)

**Request Body**:
```json
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {"role": "user", "content": "Hello, how are you?"}
  ],
  "stream": false,
  "temperature": 0.7,
  "max_tokens": 1000,
  "thread_id": "thread_abc123"
}
```

**Non-Streaming Response**:
```json
{
  "id": "chatcmpl-abc123",
  "object": "chat.completion",
  "created": 1699000000,
  "model": "gpt-3.5-turbo",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! I'm doing well, thank you for asking. How can I help you today?"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

### Thread Management

#### Create Thread
```bash
curl -X POST http://localhost:8080/v1/threads \\
  -H "Content-Type: application/json" \\
  -d '{"title": "My Conversation"}'
```

#### Add Message to Thread
```bash
curl -X POST http://localhost:8080/v1/threads/thread_abc123/messages \\
  -H "Content-Type: application/json" \\
  -d '{"role": "user", "content": "Hello there!"}'
```

#### List Thread Messages
```bash
curl http://localhost:8080/v1/threads/thread_abc123/messages
```

## Usage Examples

### 1. Non-Streaming Chat Completion (OpenAI Provider)

```bash
curl -s http://localhost:8080/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "X-LLM-Provider: openai" \\
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {"role": "user", "content": "Explain quantum computing in simple terms"}
    ],
    "temperature": 0.7,
    "max_tokens": 500
  }'
```

### 2. Streaming Chat Completion (OpenAI Provider)

```bash
curl -N http://localhost:8080/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Accept: text/event-stream" \\
  -H "X-LLM-Provider: openai" \\
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {"role": "user", "content": "Write a short story about a robot"}
    ],
    "stream": true,
    "temperature": 0.8
  }'
```

**Streaming Response Format**:
```
data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1699000000,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"role":"assistant","content":"Once"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1699000000,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":" upon"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1699000000,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":" a"},"finish_reason":null}]}

data: [DONE]
```

### 3. Anthropic Provider

```bash
curl -s http://localhost:8080/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "X-LLM-Provider: anthropic" \\
  -d '{
    "model": "claude-3-haiku-20240307",
    "messages": [
      {"role": "user", "content": "What are the benefits of renewable energy?"}
    ],
    "temperature": 0.6,
    "max_tokens": 400
  }'
```

### 4. Chat with Thread Context

```bash
# First, create a thread
THREAD_ID=$(curl -s -X POST http://localhost:8080/v1/threads \\
  -H "Content-Type: application/json" \\
  -d '{"title": "Energy Discussion"}' | jq -r '.id')

# Add a message to the thread
curl -X POST http://localhost:8080/v1/threads/$THREAD_ID/messages \\
  -H "Content-Type: application/json" \\
  -d '{"role": "user", "content": "Tell me about solar energy"}'

# Use the thread in chat completion
curl -s http://localhost:8080/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -d "{
    \"model\": \"gpt-3.5-turbo\",
    \"messages\": [
      {\"role\": \"user\", \"content\": \"What about wind energy?\"}
    ],
    \"thread_id\": \"$THREAD_ID\"
  }"
```

### 5. Streaming with Thread Context

```bash
curl -N http://localhost:8080/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Accept: text/event-stream" \\
  -d "{
    \"model\": \"gpt-3.5-turbo\",
    \"messages\": [
      {\"role\": \"user\", \"content\": \"Compare both energy sources\"}
    ],
    \"stream\": true,
    \"thread_id\": \"$THREAD_ID\"
  }"
```

## Web UI Features

The Angular frontend provides:

- **Provider Selection**: Dropdown to choose between OpenAI and Anthropic
- **Streaming Toggle**: Enable/disable real-time response streaming
- **Thread Management**: Automatic thread creation and message history
- **Real-time Responses**: Live streaming of AI responses with proper formatting

## Configuration

### Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key (required for OpenAI provider)
- `ANTHROPIC_API_KEY`: Your Anthropic API key (required for Anthropic provider)
- `SERVER_PORT`: Spring Boot server port (default: 8080)

### Provider Configuration

The system automatically selects the appropriate provider based on the `X-LLM-Provider` header:
- `openai` (default): Uses Spring AI with OpenAI ChatClient
- `anthropic`: Uses WebClient to call Anthropic Claude Messages API

## Error Handling

All errors follow OpenAI error format:

```json
{
  "error": {
    "message": "Provider authentication failed",
    "type": "provider_error", 
    "code": "authentication_failed"
  }
}
```

## Thread Storage

- Threads are stored in-memory using ConcurrentHashMap
- Messages are automatically associated with threads
- Thread context is included in chat completions when `thread_id` is provided
- History is tail-truncated if it exceeds token limits

## Compatibility

This implementation maintains compatibility with:
- OpenAI Chat Completions API
- OpenAI Assistants API (threads and messages)
- Server-Sent Events (SSE) streaming standard
- Angular HTTP client and EventSource/fetch APIs