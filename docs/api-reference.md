# Spring AI Agent API Reference

The Spring AI Agent backend exposes an OpenAI-compatible REST API that supports chat completions, thread management, and real-time streaming. All endpoints are prefixed with `/v1` and return JSON unless noted otherwise.

## Authentication & Headers

* `Authorization`: Optional bearer token for upstream providers (the backend reads `OPENAI_API_KEY`).
* `X-LLM-Provider`: Overrides the active provider (defaults to `openai`).
* `X-Use-Memory-Advisor`: `true` enables the in-memory conversation advisor when requesting non-streaming completions.
* `Content-Type`: `application/json` for all write operations.
* `Accept`: Use `text/event-stream` for SSE streaming responses.

## Chat Completions

### `POST /v1/chat/completions`

Creates a chat completion using the configured provider.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `model` | `string` | ✅ | Provider-specific model identifier. |
| `messages` | `Message[]` | ✅ | Ordered conversation history. Roles: `user`, `assistant`, or `system`. |
| `temperature` | `number` | ❌ | Sampling temperature (`0.0`–`2.0`). Defaults to `0.7`. |
| `max_tokens` | `number` | ❌ | Hard limit for generated tokens. |
| `thread_id` | `string` | ❌ | Associates the completion with an existing thread. |
| `stream` | `boolean` | ❌ | When `true`, the response is streamed over SSE. |

`Message` objects contain `role` and `content` fields. Validation enforces non-empty content and supported roles.

#### Non-Streaming Response

Returns an OpenAI-compatible payload:

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1699000000,
  "model": "gpt-3.5-turbo",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello!"
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

#### Streaming Response

When `stream` is `true`, the endpoint emits Server-Sent Events (`text/event-stream`). Each event contains a `chat.completion.chunk` payload with delta tokens, and the stream terminates with `data: [DONE]`.

### Provider Models

* `GET /v1/models` — Lists available models for the active provider.
* `GET /v1/sessions/stats` — Returns simple session metrics used by the UI.

## Thread Management

### `POST /v1/threads`
Creates a new conversation thread. Request body accepts optional `title` and `metadata` fields. Responds with a `thread` object containing identifiers, timestamps, and message counters.

### `GET /v1/threads`
Returns a paginated-style list response with thread metadata. The body mirrors the OpenAI list shape (`object`, `data`, `has_more`, `first_id`, `last_id`).

### `GET /v1/threads/{threadId}`
Retrieves a single thread by ID.

### `POST /v1/threads/{threadId}`
Updates a thread’s `title` or `metadata`.

### `DELETE /v1/threads/{threadId}`
Deletes a thread and responds with `{ id, object: "thread.deleted", deleted: true }`.

### `POST /v1/threads/{threadId}/messages`
Appends a message to an existing thread. Required body fields:

```json
{
  "role": "user",
  "content": "Hello again"
}
```

Returns the persisted message with timestamps.

### `GET /v1/threads/{threadId}/messages`
Returns a list response containing all messages for the thread, ordered by creation time.

## Error Handling

Errors follow OpenAI’s error shape:

```json
{
  "error": {
    "message": "Model not found",
    "type": "model_not_found",
    "code": "404"
  }
}
```

HTTP status codes map to the documented OpenAI semantics (e.g., `401` for invalid API keys, `429` for rate limits, `400` for validation failures, and `500` for server errors).

## Streaming Notes

* SSE responses stay open until `[DONE]` is emitted or the client disconnects.
* Use an `AbortController` or close the HTTP connection to cancel a streaming request.
* The UI consolidates streamed chunks into a final completion for display.
