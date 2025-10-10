import { Component, Input, OnInit, OnChanges, SimpleChanges, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TextFieldModule } from '@angular/cdk/text-field';
import { ThreadService, Thread, ThreadMessage } from '../services/thread.service';
import { ChatService, ChatMessage, ChatRequest } from '../services/chat.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SafeHtml } from '@angular/platform-browser';
import { MarkdownService } from '../services/markdown.service';

interface EnhancedMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  htmlContent?: SafeHtml;
  timestamp: Date;
  isStreaming?: boolean;
  reactions?: string[];
}

interface QuickSuggestion {
  text: string;
  icon: string;
  action: () => void;
}

@Component({
  selector: 'app-enhanced-chat',
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
    MatMenuModule,
    MatTooltipModule,
    TextFieldModule
  ],
  template: `
    <mat-card class="smart-chat-card" [class.dark-theme]="isDarkTheme">
      <!-- Simplified Header -->
      <mat-card-header class="smart-chat-header">
        <mat-card-title class="chat-title">
          <mat-icon>psychology</mat-icon>
          <span>Smart AI Chat</span>
        </mat-card-title>
        
        <!-- Minimal Header Actions -->
        <div class="header-actions">
          <button mat-icon-button (click)="toggleSearch()" matTooltip="Search">
            <mat-icon [class.active]="showSearch">search</mat-icon>
          </button>
          
          <button mat-icon-button [matMenuTriggerFor]="settingsMenu" matTooltip="Settings">
            <mat-icon>settings</mat-icon>
          </button>
        </div>
      </mat-card-header>
      
      <!-- Settings Menu -->
      <mat-menu #settingsMenu="matMenu">
        <button mat-menu-item (click)="toggleTheme()">
          <mat-icon>{{ isDarkTheme ? 'light_mode' : 'dark_mode' }}</mat-icon>
          <span>{{ isDarkTheme ? 'Light' : 'Dark' }} Mode</span>
        </button>
        <button mat-menu-item (click)="toggleMemoryAdvisor()">
          <mat-icon>{{ useMemoryAdvisor ? 'memory' : 'psychology' }}</mat-icon>
          <span>{{ useMemoryAdvisor ? 'Disable' : 'Enable' }} Memory</span>
        </button>
        <button mat-menu-item (click)="toggleStream()">
          <mat-icon>{{ streamEnabled ? 'stream' : 'notes' }}</mat-icon>
          <span>{{ streamEnabled ? 'Disable' : 'Enable' }} Streaming</span>
        </button>
        <button mat-menu-item (click)="toggleSuggestions()">
          <mat-icon>{{ smartSuggestions ? 'lightbulb' : 'lightbulb_outline' }}</mat-icon>
          <span>{{ smartSuggestions ? 'Disable' : 'Enable' }} Suggestions</span>
        </button>
        <button mat-menu-item disabled>
          <mat-icon [class.connected]="isConnected" [class.disconnected]="!isConnected">
            {{ isConnected ? 'wifi' : 'wifi_off' }}
          </mat-icon>
          <span>{{ isConnected ? 'Connected' : 'Disconnected' }}</span>
        </button>
      </mat-menu>
      
      <!-- Search Bar -->
      <div class="search-container" [class.show]="showSearch">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search conversations...</mat-label>
          <input matInput [(ngModel)]="searchQuery" (input)="onSearch()" 
                 placeholder="Search messages...">
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
      </div>
      
      <mat-card-content class="smart-chat-content">
        <!-- Messages Area -->
        <div class="chat-messages" #messagesContainer>
          <!-- Welcome Message -->
          <div *ngIf="messages.length === 0" class="welcome-message">
            <mat-icon class="welcome-icon">psychology</mat-icon>
            <h3>Welcome to Smart AI Chat</h3>
            <p>Ask me anything! I can help with code, explanations, creative writing, and more.</p>
            
            <!-- Quick Start Suggestions -->
            <div class="quick-suggestions">
              <button *ngFor="let suggestion of quickSuggestions" 
                      mat-stroked-button 
                      (click)="selectSuggestion(suggestion)"
                      class="suggestion-chip">
                <mat-icon>{{ suggestion.icon }}</mat-icon>
                {{ suggestion.text }}
              </button>
            </div>
          </div>
          
          <!-- Messages -->
          <div *ngFor="let message of filteredMessages; trackBy: trackMessage" 
               class="message-wrapper"
               [class.user-message]="message.role === 'user'"
               [class.assistant-message]="message.role === 'assistant'"
               [class.highlighted]="isMessageHighlighted(message)">
            
            <div class="message-bubble">
              <!-- User Messages -->
              <div *ngIf="message.role === 'user'" class="message-content">
                <div class="message-text">{{ message.content }}</div>
                <div class="message-meta">
                  <span class="message-time">{{ message.timestamp | date:'short' }}</span>
                </div>
              </div>
              
              <!-- Assistant Messages with Markdown Support -->
              <div *ngIf="message.role === 'assistant'" class="message-content">
                <div class="message-text markdown-content" 
                     [innerHTML]="message.htmlContent || message.content"></div>
                <div class="message-meta">
                  <span class="message-time">{{ message.timestamp | date:'short' }}</span>
                  
                  <!-- Message Actions -->
                  <div class="message-actions">
                    <button mat-icon-button 
                            (click)="copyMessage(message)" 
                            matTooltip="Copy"
                            class="action-btn">
                      <mat-icon>content_copy</mat-icon>
                    </button>
                    <button mat-icon-button 
                            (click)="regenerateResponse(message)" 
                            matTooltip="Regenerate"
                            class="action-btn">
                      <mat-icon>refresh</mat-icon>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Smart Typing Indicator -->
          <div *ngIf="isTyping" class="message-wrapper assistant-message typing-indicator">
            <div class="message-bubble typing-bubble">
              <div class="typing-animation">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
              <span class="typing-text">{{ typingText }}</span>
            </div>
          </div>
        </div>
      </mat-card-content>
      
      <!-- Enhanced Input Area -->
      <mat-card-actions class="smart-chat-input">        
        <!-- Simplified Message Input Row -->
        <div class="input-row">
          <mat-form-field appearance="outline" class="message-input">
            <mat-label>Type your message...</mat-label>
            <textarea 
              matInput
              [(ngModel)]="currentMessage"
              (keydown)="onKeyDown($event)"
              (input)="onMessageInput()"
              [disabled]="isLoading"
              rows="1"
              cdkTextareaAutosize
              #autosize="cdkTextareaAutosize"
              cdkAutosizeMinRows="1"
              cdkAutosizeMaxRows="6"
              maxlength="2000"
              placeholder="Ask me anything..."></textarea>
            
            <!-- Input Indicators -->
            <div matSuffix class="input-indicators">
              <mat-icon *ngIf="isTypingInInput" class="typing-indicator-icon">edit</mat-icon>
              <button mat-icon-button 
                      *ngIf="currentMessage.trim()" 
                      (click)="clearInput()"
                      matTooltip="Clear">
                <mat-icon>clear</mat-icon>
              </button>
            </div>
          </mat-form-field>
          
          <!-- Quick Actions -->
          <div class="quick-actions">
            <button mat-icon-button 
                    [matMenuTriggerFor]="quickActionsMenu"
                    matTooltip="Quick actions"
                    class="quick-action-btn">
              <mat-icon>more_vert</mat-icon>
            </button>
          </div>
          
          <!-- Send Button -->
          <button 
            mat-fab
            color="primary"
            (click)="sendMessage()"
            [disabled]="!canSendMessage()"
            class="send-button"
            [class.loading]="isLoading">
            <mat-icon>{{ getSendIcon() }}</mat-icon>
          </button>
        </div>
        
        <!-- Smart Suggestions -->
        <div *ngIf="smartSuggestions && currentSuggestions.length > 0" class="smart-suggestions">
          <button *ngFor="let suggestion of currentSuggestions" 
                  mat-chip-option 
                  (click)="applySuggestion(suggestion)"
                  class="suggestion-chip">
            <mat-icon>{{ suggestion.icon }}</mat-icon>
            {{ suggestion.text }}
          </button>
        </div>
      </mat-card-actions>
    </mat-card>
    
    <!-- Quick Actions Menu -->
    <mat-menu #quickActionsMenu="matMenu">
      <button mat-menu-item (click)="exportChat()">
        <mat-icon>download</mat-icon>
        Export Chat
      </button>
      <button mat-menu-item (click)="clearChat()">
        <mat-icon>clear_all</mat-icon>
        Clear Chat
      </button>
    </mat-menu>
  `,
  styles: [`
    .smart-chat-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      width: 100%;
      max-width: 100%;
      margin: 0;
      transition: all 0.3s ease;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    }
    
    .dark-theme {
      background-color: #1e1e1e;
      color: #fff;
    }
    
    .dark-theme .smart-chat-header {
      background-color: #2d2d2d;
      border-bottom: 1px solid #404040;
    }
    
    .dark-theme .chat-messages {
      background-color: #1a1a1a;
    }
    
    .smart-chat-header {
      padding: 16px 24px !important;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-bottom: none;
    }
    
    .chat-title {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 600 !important;
      color: white !important;
    }
    
    .ai-chip {
      background: rgba(255, 255, 255, 0.2);
      color: white;
      margin-left: auto;
    }
    
    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    
    .header-actions button {
      color: white;
    }
    
    .header-actions button mat-icon.active {
      color: #ffd700;
    }
    
    .memory-toggle {
      color: white;
    }
    
    .search-container {
      padding: 0 24px;
      overflow: hidden;
      max-height: 0;
      transition: max-height 0.3s ease, padding 0.3s ease;
    }
    
    .search-container.show {
      max-height: 100px;
      padding: 16px 24px;
    }
    
    .search-field {
      width: 100%;
    }
    
    .smart-chat-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      padding: 0 !important;
    }
    
    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 20px 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      background: linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%);
    }
    
    .welcome-message {
      text-align: center;
      padding: 40px 20px;
      color: #666;
    }
    
    .welcome-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      margin-bottom: 16px;
      color: #667eea;
    }
    
    .quick-suggestions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
      margin-top: 24px;
    }
    
    .suggestion-chip {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .message-wrapper {
      max-width: 80%;
      display: flex;
      animation: messageSlideIn 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
    }
    
    .message-wrapper.highlighted {
      animation: highlightPulse 1s ease;
    }
    
    .message-wrapper.user-message {
      align-self: flex-end;
    }
    
    .message-wrapper.assistant-message {
      align-self: flex-start;
    }
    
    .message-bubble {
      padding: 16px 20px;
      border-radius: 20px;
      position: relative;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      backdrop-filter: blur(10px);
      transition: transform 0.2s ease;
    }
    
    .message-bubble:hover {
      transform: translateY(-1px);
    }
    
    .user-message .message-bubble {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white;
      border-bottom-right-radius: 4px;
    }
    
    .assistant-message .message-bubble {
      background: rgba(255, 255, 255, 0.9);
      color: #333;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-bottom-left-radius: 4px;
    }
    
    .message-content {
      width: 100%;
    }
    
    .message-text {
      word-wrap: break-word;
      white-space: pre-wrap;
      line-height: 1.6;
      font-size: 14px;
      margin-bottom: 8px;
    }
    
    .markdown-content {
      line-height: 1.6;
    }
    
    .markdown-content h1, .markdown-content h2, .markdown-content h3 {
      margin: 12px 0 8px 0;
      color: #333;
    }
    
    .markdown-content code {
      background: #f1f3f4;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Consolas', monospace;
    }
    
    .markdown-content pre {
      background: #f8f9fa;
      padding: 12px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 8px 0;
    }
    
    .message-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;
    }
    
    .message-time {
      font-size: 11px;
      opacity: 0.7;
    }
    
    .message-actions {
      display: flex;
      gap: 4px;
      opacity: 0;
      transition: opacity 0.2s ease;
    }
    
    .message-bubble:hover .message-actions {
      opacity: 1;
    }
    
    .action-btn {
      width: 28px;
      height: 28px;
      line-height: 28px;
    }
    
    .action-btn mat-icon {
      font-size: 16px;
    }
    
    /* Smart Typing Indicator */
    .typing-indicator {
      animation: fadeIn 0.3s ease;
    }
    
    .typing-bubble {
      display: flex;
      align-items: center;
      gap: 12px;
      background: rgba(255, 255, 255, 0.9) !important;
      min-height: 50px;
    }
    
    .typing-animation {
      display: flex;
      gap: 4px;
    }
    
    .typing-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #667eea;
      animation: typingBounce 1.4s infinite ease-in-out both;
    }
    
    .typing-dot:nth-child(1) { animation-delay: -0.32s; }
    .typing-dot:nth-child(2) { animation-delay: -0.16s; }
    
    .typing-text {
      font-size: 13px;
      color: #666;
      font-style: italic;
    }
    
    .smart-chat-input {
      padding: 20px 24px !important;
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-top: 1px solid rgba(0, 0, 0, 0.1);
    }
    
    .input-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    
    .control-group {
      display: flex;
      gap: 24px;
    }
    
    .control-group mat-slide-toggle {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .character-counter {
      font-size: 12px;
      color: #666;
      transition: color 0.2s ease;
    }
    
    .character-counter.warning {
      color: #f44336;
      font-weight: 500;
    }
    
    .input-row {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: nowrap;
      width: 100%;
    }
    
    .message-input {
      flex: 1 1 auto;
      min-width: 0; /* allow flex child to shrink properly */
      width: 100%;
    }

    /* Ensure Material form field and textarea stretch fully */
    .message-input .mat-mdc-form-field,
    .message-input .mdc-text-field {
      width: 100%;
    }
    
    .input-indicators {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .typing-indicator-icon {
      color: #4caf50;
      animation: pulse 1.5s infinite;
    }
    
    .quick-actions {
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 8px;
    }
    
    .send-button {
      position: relative;
      align-self: center;
      transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
    }
    
    .send-button:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
    }
    
    .send-button.loading {
      animation: loadingPulse 1.5s infinite;
    }
    
    .smart-suggestions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid rgba(0, 0, 0, 0.1);
    }
    
    /* Animations */
    @keyframes messageSlideIn {
      from {
        opacity: 0;
        transform: translateY(20px) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }
    
    @keyframes highlightPulse {
      0%, 100% { background-color: transparent; }
      50% { background-color: rgba(255, 235, 59, 0.3); }
    }
    
    @keyframes typingBounce {
      0%, 80%, 100% {
        transform: scale(0);
      }
      40% {
        transform: scale(1);
      }
    }
    
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
    
    @keyframes loadingPulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.05); }
    }
    
    /* Connected/Disconnected Status */
    .connected {
      background-color: #4caf50;
      color: white;
    }
    
    .disconnected {
      background-color: #f44336;
      color: white;
    }
    
    /* Custom Scrollbar */
    .chat-messages::-webkit-scrollbar {
      width: 8px;
    }
    
    .chat-messages::-webkit-scrollbar-track {
      background: rgba(0, 0, 0, 0.05);
      border-radius: 4px;
    }
    
    .chat-messages::-webkit-scrollbar-thumb {
      background: rgba(102, 126, 234, 0.3);
      border-radius: 4px;
    }
    
    .chat-messages::-webkit-scrollbar-thumb:hover {
      background: rgba(102, 126, 234, 0.5);
    }
    
    /* Responsive Design */
    @media (max-width: 768px) {
      .smart-chat-card {
        border-radius: 0;
        height: 100vh;
      }
      
      .message-wrapper {
        max-width: 90%;
      }
      
      .header-actions {
        gap: 8px;
      }
      
      .control-group {
        gap: 16px;
      }
      
      .quick-suggestions, .smart-suggestions {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `]
})
export class EnhancedChatComponent implements OnInit, OnChanges, OnDestroy {
  @Input() selectedThread: Thread | null = null;
  
  private destroy$ = new Subject<void>();
  
  // Core properties
  messages: EnhancedMessage[] = [];
  filteredMessages: EnhancedMessage[] = [];
  currentMessage = '';
  isLoading = false;
  isConnected = true;
  currentModel = 'gpt-3.5-turbo';
  
  // Smart features
  isDarkTheme = false;
  useMemoryAdvisor = true;
  streamEnabled = true;
  smartSuggestions = true;
  showSearch = false;
  searchQuery = '';
  
  // Typing indicators
  isTyping = false;
  isTypingInInput = false;
  typingText = 'AI is thinking...';
  
  // Suggestions and quick actions
  quickSuggestions: QuickSuggestion[] = [
    { text: 'Explain a concept', icon: 'lightbulb', action: () => this.applySuggestionText('Can you explain ') },
    { text: 'Write code', icon: 'code', action: () => this.applySuggestionText('Write a function that ') }
  ];
  
  currentSuggestions: QuickSuggestion[] = [];
  
  private apiUrl = '/v1';
  private typingTimeout: any;

  constructor(
    private http: HttpClient,
    private threadService: ThreadService,
    private chatService: ChatService,
    private markdownService: MarkdownService
  ) {
    // Preload markdown renderer in the background so the first render is immediate
    void this.markdownService.preload();
    
    // Load theme from localStorage
    this.isDarkTheme = localStorage.getItem('chatTheme') === 'dark';
  }

  ngOnInit() {
    this.loadAvailableModels();
    this.threadService.currentThread$
      .pipe(takeUntil(this.destroy$))
      .subscribe(thread => {
        this.selectedThread = thread;
        this.loadThreadMessages();
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['selectedThread']) {
      this.loadThreadMessages();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }
  }

  // Core functionality
  trackMessage(index: number, message: EnhancedMessage): string {
    return message.id;
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

  private loadThreadMessages() {
    if (this.selectedThread) {
      this.threadService.getThreadMessages(this.selectedThread.id).subscribe({
        next: (threadMessages) => {
          this.messages = threadMessages.map(tm => {
            const message: EnhancedMessage = {
              id: `${tm.id || Math.random()}`,
              role: tm.role,
              content: tm.content,
              timestamp: new Date(tm.created_at * 1000)
            };

            if (message.role === 'assistant') {
              this.applyMarkdown(message);
            }

            return message;
          });
          this.refreshFilteredMessages();
          this.scrollToBottom();
        },
        error: (error) => {
          console.error('Error loading thread messages:', error);
          this.messages = [];
          this.refreshFilteredMessages();
        }
      });
    } else {
      this.messages = [];
      this.refreshFilteredMessages();
    }
  }

  // Smart Features
  toggleTheme() {
    this.isDarkTheme = !this.isDarkTheme;
    localStorage.setItem('chatTheme', this.isDarkTheme ? 'dark' : 'light');
  }

  toggleMemoryAdvisor() {
    this.useMemoryAdvisor = !this.useMemoryAdvisor;
  }

  toggleStream() {
    this.streamEnabled = !this.streamEnabled;
  }

  toggleSuggestions() {
    this.smartSuggestions = !this.smartSuggestions;
  }

  toggleSearch() {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.searchQuery = '';
      this.refreshFilteredMessages();
    }
  }

  onSearch() {
    this.refreshFilteredMessages();
  }

  isMessageHighlighted(message: EnhancedMessage): boolean {
    return this.searchQuery.trim().length > 0 && 
           message.content.toLowerCase().includes(this.searchQuery.toLowerCase());
  }

  onMessageInput() {
    this.isTypingInInput = true;
    
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }
    
    this.typingTimeout = setTimeout(() => {
      this.isTypingInInput = false;
    }, 1000);
    
    // Generate smart suggestions based on input
    this.generateSmartSuggestions();
  }

  private generateSmartSuggestions() {
    const input = this.currentMessage.toLowerCase();
    this.currentSuggestions = [];
    
    if (input.includes('code') || input.includes('function')) {
      this.currentSuggestions.push({
        text: 'in Python',
        icon: 'code',
        action: () => this.appendToMessage(' in Python')
      });
    }
    
    if (input.includes('explain') || input.includes('what is')) {
      this.currentSuggestions.push({
        text: 'with examples',
        icon: 'lightbulb',
        action: () => this.appendToMessage(' with examples')
      });
    }
    
    if (input.includes('help') || input.includes('how to')) {
      this.currentSuggestions.push({
        text: 'step by step',
        icon: 'format_list_numbered',
        action: () => this.appendToMessage(' step by step')
      });
    }
  }

  selectSuggestion(suggestion: QuickSuggestion) {
    suggestion.action();
  }

  applySuggestion(suggestion: QuickSuggestion) {
    suggestion.action();
  }

  private applySuggestionText(text: string) {
    this.currentMessage = text;
    this.focusInput();
  }

  private appendToMessage(text: string) {
    this.currentMessage += text;
    this.focusInput();
  }

  private focusInput() {
    setTimeout(() => {
      const textarea = document.querySelector('textarea[matInput]') as HTMLTextAreaElement;
      if (textarea) {
        textarea.focus();
      }
    }, 100);
  }

  // Message Actions
  copyMessage(message: EnhancedMessage) {
    navigator.clipboard.writeText(message.content).then(() => {
      // Could add a toast notification here
      console.log('Message copied to clipboard');
    });
  }

  regenerateResponse(message: EnhancedMessage) {
    // Find the user message that preceded this assistant message
    const messageIndex = this.messages.findIndex(m => m.id === message.id);
    if (messageIndex > 0) {
      const userMessage = this.messages[messageIndex - 1];
      if (userMessage.role === 'user') {
        this.currentMessage = userMessage.content;
        // Remove the old assistant response
        this.messages.splice(messageIndex, 1);
        this.refreshFilteredMessages();
        this.sendMessage();
      }
    }
  }

  // Message sending
  canSendMessage(): boolean {
    return this.currentMessage.trim().length > 0 && !this.isLoading;
  }

  getSendIcon(): string {
    if (this.isLoading) return 'hourglass_empty';
    if (this.currentMessage.trim()) return 'send';
    return 'mic';
  }

  sendMessage() {
    if (!this.canSendMessage()) return;
    
    // Create or select thread
    if (!this.selectedThread) {
      this.threadService.createThread({ title: 'New Smart Chat' }).subscribe({
        next: (newThread) => {
          this.threadService.setCurrentThread(newThread);
          this.selectedThread = newThread;
          this.doSendMessage();
        },
        error: (error) => {
          console.error('Error creating thread:', error);
          this.isConnected = false;
        }
      });
    } else {
      this.doSendMessage();
    }
  }

  private doSendMessage() {
    if (!this.canSendMessage()) return;
    
    // Add user message
    const userMessage: EnhancedMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: this.currentMessage,
      timestamp: new Date()
    };
    
    this.messages.push(userMessage);
    this.refreshFilteredMessages();
    
    const messageToSend = this.currentMessage;
    this.currentMessage = '';
    this.currentSuggestions = [];
    
    // Start typing indicator
    this.startTypingIndicator();
    
    // Prepare request
    const chatMessages: ChatMessage[] = [{ role: 'user', content: messageToSend }];
    const chatRequest: ChatRequest = {
      model: this.currentModel,
      messages: chatMessages,
      thread_id: this.selectedThread?.id
    };
    
    // Send with memory advisor if enabled
    const headers = this.useMemoryAdvisor ? { 'X-Use-Memory-Advisor': 'true' } : {};
    
    this.chatService.createCompletion(chatRequest, { 
      stream: this.streamEnabled,
      provider: 'openai'
    }).subscribe({
      next: (response) => {
        this.stopTypingIndicator();
        
        const assistantMessage: EnhancedMessage = {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: response.choices[0].message.content,
          timestamp: new Date()
        };

        this.applyMarkdown(assistantMessage);
        this.messages.push(assistantMessage);
        this.refreshFilteredMessages();
        this.scrollToBottom();
      },
      error: (error) => {
        this.stopTypingIndicator();
        console.error('Error:', error);
        this.isConnected = false;
        
        const errorMessage: EnhancedMessage = {
          id: `error-${Date.now()}`,
          role: 'assistant',
          content: 'I apologize, but I encountered an error. Please check the connection and try again.',
          timestamp: new Date()
        };
        this.applyMarkdown(errorMessage);
        this.messages.push(errorMessage);
        this.refreshFilteredMessages();
        this.scrollToBottom();
      }
    });
  }

  private startTypingIndicator() {
    this.isLoading = true;
    this.isTyping = true;
    
    // Vary typing messages for more personality
    const typingMessages = [
      'AI is thinking...',
      'Processing your request...',
      'Analyzing...',
      'Generating response...',
      'Almost there...'
    ];
    
    let messageIndex = 0;
    const typingInterval = setInterval(() => {
      this.typingText = typingMessages[messageIndex % typingMessages.length];
      messageIndex++;
    }, 1000);
    
    // Store interval for cleanup
    (this as any).typingInterval = typingInterval;
  }

  private stopTypingIndicator() {
    this.isLoading = false;
    this.isTyping = false;
    
    if ((this as any).typingInterval) {
      clearInterval((this as any).typingInterval);
      delete (this as any).typingInterval;
    }
  }

  private applyMarkdown(message: EnhancedMessage) {
    message.htmlContent = this.markdownService.renderSync(message.content);

    this.markdownService.render(message.content)
      .then(html => {
        message.htmlContent = html;
        this.refreshFilteredMessages();
      })
      .catch(error => {
        console.error('Failed to render markdown content', error);
      });
  }

  private refreshFilteredMessages() {
    if (!this.searchQuery.trim()) {
      this.filteredMessages = [...this.messages];
      return;
    }

    const query = this.searchQuery.toLowerCase();
    this.filteredMessages = this.messages.filter(message =>
      message.content.toLowerCase().includes(query)
    );
  }

  // Utility functions
  clearInput() {
    this.currentMessage = '';
    this.currentSuggestions = [];
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

  // Menu actions
  exportChat() {
    const chatData = {
      threadId: this.selectedThread?.id,
      messages: this.messages.map(m => ({
        role: m.role,
        content: m.content,
        timestamp: m.timestamp
      })),
      exportedAt: new Date()
    };
    
    const blob = new Blob([JSON.stringify(chatData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `chat-export-${Date.now()}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  clearChat() {
    if (confirm('Are you sure you want to clear this chat?')) {
      this.messages = [];
      this.filteredMessages = [];
    }
  }

}
