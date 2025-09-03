import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatToolbarModule, MatIconModule, MatButtonModule],
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
      padding: 24px;
      display: flex;
      flex-direction: column;
    }
  `]
})
export class AppComponent {
  title = 'Spring AI Agent Chat';
}