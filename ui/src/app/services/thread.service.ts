import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { OpenAIListResponse } from './api.types';

export interface Thread {
  id: string;
  object: string;
  created_at: number;
  title: string;
  metadata?: Record<string, unknown>;
  message_count: number;
  last_activity: number;
}

export interface ThreadMessage {
  id: string;
  thread_id: string;
  role: 'user' | 'assistant';
  content: string;
  created_at: number;
  metadata?: Record<string, unknown>;
}

export interface CreateThreadRequest {
  title?: string;
  metadata?: Record<string, unknown>;
}

@Injectable({
  providedIn: 'root'
})
export class ThreadService {
  private readonly apiUrl = '/v1';
  private readonly currentThreadSubject = new BehaviorSubject<Thread | null>(null);
  public readonly currentThread$ = this.currentThreadSubject.asObservable();

  constructor(private http: HttpClient) {}

  createThread(request: CreateThreadRequest = {}): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/threads`, request);
  }

  getAllThreads(): Observable<Thread[]> {
    return this.http.get<OpenAIListResponse<Thread>>(`${this.apiUrl}/threads`).pipe(
      map(response => response?.data ?? [])
    );
  }

  getThread(threadId: string): Observable<Thread> {
    return this.http.get<Thread>(`${this.apiUrl}/threads/${threadId}`);
  }

  updateThread(threadId: string, request: CreateThreadRequest): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/threads/${threadId}`, request);
  }

  deleteThread(threadId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/threads/${threadId}`);
  }

  getThreadMessages(threadId: string): Observable<ThreadMessage[]> {
    return this.http.get<OpenAIListResponse<ThreadMessage>>(`${this.apiUrl}/threads/${threadId}/messages`).pipe(
      map(response => response?.data ?? [])
    );
  }

  setCurrentThread(thread: Thread | null) {
    this.currentThreadSubject.next(thread);
  }

  getCurrentThread(): Thread | null {
    return this.currentThreadSubject.value;
  }
}