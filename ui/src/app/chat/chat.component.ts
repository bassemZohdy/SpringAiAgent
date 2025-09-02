import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="chat-container">
      <div class="chat-header">
        <h2>Chat with AI Agent</h2>
        <div class="connection-status" [class.connected]="isConnected">
          {{ isConnected ? 'Connected' : 'Disconnected' }}
        </div>
      </div>
      
      <div class="chat-messages" #messagesContainer>
        <div *ngFor="let message of messages" 
             class="message" 
             [class.user-message]="message.role === 'user'"
             [class.assistant-message]="message.role === 'assistant'">
          <div class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ message.timestamp | date:'short' }}</div>
          </div>
        </div>
        
        <div *ngIf="isLoading" class="message assistant-message">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <div class="input-group">
          <textarea 
            [(ngModel)]="currentMessage"
            (keydown)="onKeyDown($event)"
            placeholder="Type your message here..."
            [disabled]="isLoading"
            rows="3"></textarea>
          <button 
            (click)="sendMessage()"
            [disabled]="!currentMessage.trim() || isLoading"
            class="btn btn-primary">
            {{ isLoading ? 'Sending...' : 'Send' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      max-width: 800px;
      margin: 0 auto;
      height: 600px;
      display: flex;
      flex-direction: column;
      border: 1px solid #ddd;
      border-radius: 8px;
      background: white;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }
    
    .chat-header {
      padding: 1rem;
      border-bottom: 1px solid #eee;
      background-color: #f8f9fa;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .chat-header h2 {
      margin: 0;
      color: #2c3e50;
    }
    
    .connection-status {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 0.8rem;
      background-color: #dc3545;
      color: white;
    }
    
    .connection-status.connected {
      background-color: #28a745;
    }
    
    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    
    .message {
      max-width: 70%;
    }
    
    .user-message {
      align-self: flex-end;
    }
    
    .assistant-message {
      align-self: flex-start;
    }
    
    .message-content {
      padding: 0.75rem 1rem;
      border-radius: 18px;
      position: relative;
    }
    
    .user-message .message-content {
      background-color: #007bff;
      color: white;
    }
    
    .assistant-message .message-content {
      background-color: #f1f1f1;
      color: #333;
    }
    
    .message-text {
      word-wrap: break-word;
      white-space: pre-wrap;
    }
    
    .message-time {
      font-size: 0.7rem;
      opacity: 0.7;
      margin-top: 4px;
    }
    
    .typing-indicator {
      display: flex;
      gap: 4px;
      align-items: center;
    }
    
    .typing-indicator span {
      width: 8px;
      height: 8px;
      background-color: #999;
      border-radius: 50%;
      animation: typing 1.4s infinite ease-in-out;
    }
    
    .typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
    .typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
    
    @keyframes typing {
      0%, 80%, 100% { opacity: 0.3; }
      40% { opacity: 1; }
    }
    
    .chat-input {
      padding: 1rem;
      border-top: 1px solid #eee;
      background-color: #f8f9fa;
    }
    
    .input-group {
      display: flex;
      gap: 0.5rem;
      align-items: flex-end;
    }
    
    .input-group textarea {
      flex: 1;
      border: 1px solid #ddd;
      border-radius: 6px;
      padding: 0.5rem;
      font-family: inherit;
      font-size: 14px;
      resize: none;
    }
    
    .input-group textarea:focus {
      outline: none;
      border-color: #007bff;
    }
  `]
})
export class ChatComponent {
  messages: Message[] = [];
  currentMessage = '';
  isLoading = false;
  isConnected = true;
  
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  sendMessage() {
    if (!this.currentMessage.trim()) return;
    
    const userMessage: Message = {
      role: 'user',
      content: this.currentMessage,
      timestamp: new Date()
    };
    
    this.messages.push(userMessage);
    const messageToSend = this.currentMessage;
    this.currentMessage = '';
    this.isLoading = true;
    
    const chatRequest = {
      model: 'gpt-3.5-turbo',
      messages: this.messages.map(m => ({ role: m.role, content: m.content }))
    };
    
    this.http.post<any>(`${this.apiUrl}/chat/completions`, chatRequest).subscribe({
      next: (response) => {
        const assistantMessage: Message = {
          role: 'assistant',
          content: response.choices[0].message.content,
          timestamp: new Date()
        };
        this.messages.push(assistantMessage);
        this.isLoading = false;
        this.scrollToBottom();
      },
      error: (error) => {
        console.error('Error:', error);
        this.isLoading = false;
        this.isConnected = false;
        
        const errorMessage: Message = {
          role: 'assistant',
          content: 'Sorry, I encountered an error while processing your message. Please check the connection to the Spring AI Agent service.',
          timestamp: new Date()
        };
        this.messages.push(errorMessage);
        this.scrollToBottom();
      }
    });
  }
  
  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
  
  private scrollToBottom() {
    setTimeout(() => {
      const messagesContainer = document.querySelector('.chat-messages');
      if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
      }
    }, 100);
  }
}