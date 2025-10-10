import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThreadListComponent } from './threads/thread-list.component';
import { EnhancedChatComponent } from './chat/enhanced-chat.component';
import { Thread } from './services/thread.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ThreadListComponent, EnhancedChatComponent],
  template: `
    <div class="app-container">
      <header class="app-toolbar">
        <div class="toolbar-content">
          <span class="material-icons app-icon">smart_toy</span>
          <span class="app-title">Spring AI Agent Chat</span>
          <span class="spacer"></span>
          <button class="icon-button" type="button" aria-label="Settings">
            <span class="material-icons">settings</span>
          </button>
        </div>
      </header>
      <main class="app-content">
        <div class="chat-layout">
          <div class="thread-sidebar">
            @defer (on viewport) {
              <app-thread-list (threadSelected)="onThreadSelected($event)"></app-thread-list>
            } @placeholder {
              <div class="sidebar-placeholder">
                <span class="loading-indicator"></span>
                <span>Loading conversations…</span>
              </div>
            }
          </div>
          <div class="chat-main">
            @defer (on viewport) {
              <app-enhanced-chat [selectedThread]="selectedThread"></app-enhanced-chat>
            } @placeholder {
              <div class="chat-placeholder">
                <span class="loading-indicator"></span>
                <p>Preparing chat experience…</p>
              </div>
            }
          </div>
        </div>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-container {
      height: 100vh;
      display: flex;
      flex-direction: column;
      background-color: #fafafa;
    }
    
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      background: linear-gradient(135deg, #1976d2 0%, #764ba2 100%);
      color: white;
    }

    .toolbar-content {
      display: flex;
      align-items: center;
      width: 100%;
      padding: 0 24px;
      min-height: 64px;
    }

    .app-icon {
      margin-right: 16px;
      font-size: 28px;
    }

    .icon-button {
      border: none;
      background: transparent;
      color: inherit;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      cursor: pointer;
      transition: background-color 0.2s ease;
    }

    .icon-button:hover {
      background-color: rgba(255, 255, 255, 0.15);
    }

    .icon-button .material-icons {
      font-size: 24px;
    }

    .app-title {
      font-size: 1.25rem;
      font-weight: 500;
    }
    
    .spacer {
      flex: 1 1 auto;
    }
    
    .app-content {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    
    .chat-layout {
      flex: 1;
      display: flex;
      gap: 0;
      overflow: hidden;
    }
    
    .thread-sidebar {
      width: 320px;
      min-width: 320px;
      border-right: 1px solid #e0e0e0;
      background-color: white;
    }

    .chat-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 24px;
      overflow: hidden;
    }

    .sidebar-placeholder,
    .chat-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      height: 100%;
      color: #666;
      text-align: center;
      padding: 24px;
    }

    .loading-indicator {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      border: 3px solid rgba(25, 118, 210, 0.2);
      border-top-color: #1976d2;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      0% {
        transform: rotate(0deg);
      }
      100% {
        transform: rotate(360deg);
      }
    }
  `]
})
export class AppComponent {
  title = 'Spring AI Agent Chat';
  selectedThread: Thread | null = null;

  onThreadSelected(thread: Thread) {
    this.selectedThread = thread;
  }
}