import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { ThreadListComponent } from './threads/thread-list.component';
import { EnhancedChatComponent } from './chat/enhanced-chat.component';
import { Thread } from './services/thread.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatToolbarModule, MatIconModule, MatButtonModule, MatDialogModule, ThreadListComponent, EnhancedChatComponent],
  template: `
    <div class="app-container">
      <mat-toolbar color="primary" class="app-toolbar">
        <mat-icon class="app-icon">smart_toy</mat-icon>
        <span class="app-title">Spring AI Agent Chat</span>
        <span class="spacer"></span>
        <button mat-icon-button>
          <mat-icon>settings</mat-icon>
        </button>
      </mat-toolbar>
      <main class="app-content">
        <div class="chat-layout">
          <div class="thread-sidebar">
            <app-thread-list (threadSelected)="onThreadSelected($event)"></app-thread-list>
          </div>
          <div class="chat-main">
            <app-enhanced-chat [selectedThread]="selectedThread"></app-enhanced-chat>
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
    }
    
    .app-icon {
      margin-right: 16px;
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
  `]
})
export class AppComponent {
  title = 'Spring AI Agent Chat';
  selectedThread: Thread | null = null;

  onThreadSelected(thread: Thread) {
    this.selectedThread = thread;
  }
}