import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TextFieldModule } from '@angular/cdk/text-field';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    TextFieldModule
  ],
  template: `
    <mat-card class="chat-card">
      <mat-card-header class="chat-header">
        <mat-card-title class="chat-title">
          <mat-icon>chat</mat-icon>
          Chat with AI Agent
        </mat-card-title>
        <div class="header-actions">
          <mat-chip [class.connected]="isConnected" [class.disconnected]="!isConnected">
            <mat-icon>{{ isConnected ? 'wifi' : 'wifi_off' }}</mat-icon>
            {{ isConnected ? 'Connected' : 'Disconnected' }}
          </mat-chip>
        </div>
      </mat-card-header>
      
      <mat-card-content class="chat-content">
        <div class="chat-messages" #messagesContainer>
          <div *ngFor="let message of messages; trackBy: trackMessage" 
               class="message-wrapper"
               [class.user-message]="message.role === 'user'"
               [class.assistant-message]="message.role === 'assistant'">
            <div class="message-bubble">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ message.timestamp | date:'short' }}</div>
            </div>
          </div>
          
          <div *ngIf="isLoading" class="message-wrapper assistant-message loading-message">
            <div class="message-bubble">
              <mat-spinner [diameter]="24"></mat-spinner>
              <span class="loading-text">AI is thinking...</span>
            </div>
          </div>
        </div>
      </mat-card-content>
      
      <mat-card-actions class="chat-input">
        <mat-form-field appearance="outline" class="message-input">
          <mat-label>Type your message...</mat-label>
          <textarea 
            matInput
            [(ngModel)]="currentMessage"
            (keydown)="onKeyDown($event)"
            [disabled]="isLoading"
            rows="3"
            cdkTextareaAutosize
            #autosize="cdkTextareaAutosize"
            cdkAutosizeMinRows="1"
            cdkAutosizeMaxRows="4"></textarea>
          <mat-icon matSuffix *ngIf="currentMessage.trim()">edit</mat-icon>
        </mat-form-field>
        <button 
          mat-fab
          color="primary"
          (click)="sendMessage()"
          [disabled]="!currentMessage.trim() || isLoading"
          class="send-button">
          <mat-icon>{{ isLoading ? 'hourglass_empty' : 'send' }}</mat-icon>
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .chat-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      max-width: 900px;
      margin: 0 auto;
    }
    
    .chat-header {
      padding: 16px 24px !important;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;
    }
    
    .chat-title {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 500 !important;
      color: #1976d2 !important;
    }
    
    .header-actions {
      display: flex;
      align-items: center;
    }
    
    .header-actions mat-chip {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .header-actions mat-chip.connected {
      background-color: #4caf50;
      color: white;
    }
    
    .header-actions mat-chip.disconnected {
      background-color: #f44336;
      color: white;
    }
    
    .chat-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      padding: 0 !important;
    }
    
    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      background-color: #fafafa;
    }
    
    .message-wrapper {
      max-width: 75%;
      display: flex;
    }
    
    .message-wrapper.user-message {
      align-self: flex-end;
    }
    
    .message-wrapper.assistant-message {
      align-self: flex-start;
    }
    
    .message-bubble {
      padding: 12px 16px;
      border-radius: 18px;
      position: relative;
      box-shadow: 0 1px 3px rgba(0,0,0,0.12);
      animation: messageSlide 0.3s ease-out;
    }
    
    .user-message .message-bubble {
      background: linear-gradient(135deg, #1976d2, #1565c0);
      color: white;
      margin-left: auto;
    }
    
    .assistant-message .message-bubble {
      background-color: white;
      color: #333;
      border: 1px solid #e0e0e0;
    }
    
    .message-text {
      word-wrap: break-word;
      white-space: pre-wrap;
      line-height: 1.5;
      font-size: 14px;
    }
    
    .message-time {
      font-size: 11px;
      opacity: 0.6;
      margin-top: 6px;
      text-align: right;
    }
    
    .assistant-message .message-time {
      text-align: left;
    }
    
    .loading-message .message-bubble {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
    }
    
    .loading-text {
      font-size: 14px;
      color: #666;
      font-style: italic;
    }
    
    .chat-input {
      padding: 16px 24px !important;
      display: flex;
      gap: 12px;
      align-items: flex-end;
      background-color: white;
      border-top: 1px solid #e0e0e0;
    }
    
    .message-input {
      flex: 1;
    }
    
    .send-button {
      margin-bottom: 8px;
    }
    
    @keyframes messageSlide {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    /* Custom scrollbar */
    .chat-messages::-webkit-scrollbar {
      width: 6px;
    }
    
    .chat-messages::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }
    
    .chat-messages::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 3px;
    }
    
    .chat-messages::-webkit-scrollbar-thumb:hover {
      background: #a8a8a8;
    }
  `]
})
export class ChatComponent {
  messages: Message[] = [];
  currentMessage = '';
  isLoading = false;
  isConnected = true;
  currentModel = 'gpt-5-nano'; // Default model
  
  private apiUrl = '/v1';

  constructor(private http: HttpClient) {
    this.loadAvailableModels();
  }

  trackMessage(index: number, message: Message): string {
    return message.timestamp.getTime().toString();
  }

  private loadAvailableModels() {
    this.http.get<any>(`${this.apiUrl}/models`).subscribe({
      next: (response) => {
        if (response.data && response.data.length > 0) {
          this.currentModel = response.data[0].id;
        }
        this.isConnected = true;
      },
      error: (error) => {
        console.warn('Could not load models, using default:', error);
        this.isConnected = false;
      }
    });
  }

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
      model: this.currentModel,
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