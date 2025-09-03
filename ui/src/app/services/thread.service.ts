import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Thread {
  id: string;
  object: string;
  created_at: number;
  title: string;
  metadata?: any;
  message_count: number;
  last_activity: number;
}

export interface ThreadMessage {
  id: string;
  thread_id: string;
  role: 'user' | 'assistant';
  content: string;
  created_at: number;
  metadata?: any;
}

export interface CreateThreadRequest {
  title?: string;
  metadata?: any;
}

@Injectable({
  providedIn: 'root'
})
export class ThreadService {
  private apiUrl = '/v1';
  private currentThreadSubject = new BehaviorSubject<Thread | null>(null);
  public currentThread$ = this.currentThreadSubject.asObservable();

  constructor(private http: HttpClient) {}

  createThread(request: CreateThreadRequest = {}): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/threads`, request);
  }

  getAllThreads(): Observable<Thread[]> {
    return this.http.get<any>(`${this.apiUrl}/threads`).pipe(
      map(response => response.data || [])
    );
  }

  getThread(threadId: string): Observable<Thread> {
    return this.http.get<Thread>(`${this.apiUrl}/threads/${threadId}`);
  }

  updateThread(threadId: string, request: CreateThreadRequest): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/threads/${threadId}`, request);
  }

  deleteThread(threadId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/threads/${threadId}`);
  }

  getThreadMessages(threadId: string): Observable<ThreadMessage[]> {
    return this.http.get<any>(`${this.apiUrl}/threads/${threadId}/messages`).pipe(
      map(response => response.data || [])
    );
  }

  setCurrentThread(thread: Thread | null) {
    this.currentThreadSubject.next(thread);
  }

  getCurrentThread(): Thread | null {
    return this.currentThreadSubject.value;
  }
}