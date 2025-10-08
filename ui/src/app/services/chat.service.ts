import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatRequest {
  model: string;
  messages: ChatMessage[];
  stream?: boolean;
  temperature?: number;
  max_tokens?: number;
  thread_id?: string;
}

export interface ChatResponse {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: Array<{
    index: number;
    message: {
      role: string;
      content: string;
    };
    finish_reason: string;
  }>;
  usage: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  };
}

export interface StreamChunk {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: Array<{
    index: number;
    delta: {
      role?: string;
      content?: string;
    };
    finish_reason?: string;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = '/v1';

  constructor(private http: HttpClient) {}

  createCompletion(request: ChatRequest, options: { stream: boolean, provider?: string } = { stream: false }): Observable<ChatResponse> {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    if (options.provider) {
      headers = headers.set('X-LLM-Provider', options.provider);
    }

    const body = {
      ...request,
      stream: options.stream
    };

    if (options.stream) {
      return this.createStreamingCompletion(body, headers);
    } else {
      return this.http.post<ChatResponse>(`${this.baseUrl}/chat/completions`, body, { headers });
    }
  }

  private createStreamingCompletion(request: ChatRequest, headers: HttpHeaders): Observable<ChatResponse> {
    return new Observable(observer => {
      const controller = new AbortController();
      const signal = controller.signal;

      // Use fetch API for streaming since EventSource doesn't support POST
      fetch(`${this.baseUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          ...(headers.get('X-LLM-Provider') && { 'X-LLM-Provider': headers.get('X-LLM-Provider')! })
        },
        body: JSON.stringify(request),
        signal
      })
      .then(response => {
        if (!response.ok) {
          // Try to parse OpenAI-compatible error
          return response.text().then(text => {
            try {
              const json = JSON.parse(text);
              observer.error(json);
            } catch {
              observer.error({ status: response.status, message: response.statusText, body: text });
            }
            return Promise.resolve();
          });
        }
        if (!response.body) {
          throw new Error('No response body');
        }

        const reader = response.body.getReader();
        let buffer = '';
        let fullContent = '';
        let responseId = '';
        let model = '';

        const readChunk = (): Promise<void> => {
          return reader.read().then(({ done, value }) => {
            if (done) {
              // Create final response
              const finalResponse: ChatResponse = {
                id: responseId,
                object: 'chat.completion',
                created: Math.floor(Date.now() / 1000),
                model: model,
                choices: [{
                  index: 0,
                  message: {
                    role: 'assistant',
                    content: fullContent
                  },
                  finish_reason: 'stop'
                }],
                usage: {
                  prompt_tokens: 0,
                  completion_tokens: 0,
                  total_tokens: 0
                }
              };
              observer.next(finalResponse);
              observer.complete();
              return;
            }

            buffer += new TextDecoder().decode(value);
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6);
                if (data === '[DONE]') {
                  return;
                }
                
                try {
                  const chunk: StreamChunk = JSON.parse(data);
                  responseId = chunk.id;
                  model = chunk.model;
                  
                  if (chunk.choices && chunk.choices[0] && chunk.choices[0].delta && chunk.choices[0].delta.content) {
                    fullContent += chunk.choices[0].delta.content;
                  }
                } catch (e) {
                  console.warn('Failed to parse chunk:', data);
                }
              }
            }

            return readChunk();
          });
        };

        return readChunk();
      })
      .catch(error => {
        // Normalize error shape
        const err = (error && error.message) ? { error: { message: error.message, type: 'network_error', code: 'network_error' } } : error;
        observer.error(err);
      });

      // Cleanup on unsubscribe
      return () => {
        try { controller.abort(); } catch {}
      };
    });
  }
}
