import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { ThreadService, Thread } from '../services/thread.service';
import { ThreadDialogComponent } from './thread-dialog.component';

@Component({
  selector: 'app-thread-list',
  standalone: true,
  imports: [
    CommonModule,
    MatListModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatMenuModule
  ],
  template: `
    <mat-card class="thread-list-card">
      <mat-card-header>
        <mat-card-title class="thread-list-title">
          <mat-icon>forum</mat-icon>
          Conversations
        </mat-card-title>
        <button mat-icon-button (click)="createNewThread()" class="new-thread-btn">
          <mat-icon>add</mat-icon>
        </button>
      </mat-card-header>
      
      <mat-card-content class="thread-list-content">
        <div *ngIf="threads.length === 0" class="empty-state">
          <mat-icon>chat_bubble_outline</mat-icon>
          <p>No conversations yet</p>
          <button mat-raised-button color="primary" (click)="createNewThread()">
            Start New Chat
          </button>
        </div>
        
        <mat-list *ngIf="threads.length > 0" class="thread-list">
          <mat-list-item 
            *ngFor="let thread of threads; trackBy: trackThread"
            [class.active]="currentThread?.id === thread.id"
            (click)="selectThread(thread)"
            class="thread-item">
            
            <div class="thread-content">
              <div class="thread-header">
                <span class="thread-title">{{ thread.title }}</span>
                <button mat-icon-button [matMenuTriggerFor]="threadMenu" (click)="$event.stopPropagation()">
                  <mat-icon>more_vert</mat-icon>
                </button>
                
                <mat-menu #threadMenu="matMenu">
                  <button mat-menu-item (click)="editThread(thread)">
                    <mat-icon>edit</mat-icon>
                    <span>Rename</span>
                  </button>
                  <button mat-menu-item (click)="deleteThread(thread)" class="delete-option">
                    <mat-icon>delete</mat-icon>
                    <span>Delete</span>
                  </button>
                </mat-menu>
              </div>
              
              <div class="thread-meta">
                <span class="message-count">{{ thread.message_count }} messages</span>
                <span class="last-activity">{{ formatLastActivity(thread.last_activity) }}</span>
              </div>
            </div>
          </mat-list-item>
        </mat-list>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .thread-list-card {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    
    .thread-list-title {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 500 !important;
      color: #1976d2 !important;
    }
    
    .new-thread-btn {
      margin-left: auto;
      background-color: #1976d2;
      color: white;
    }
    
    .thread-list-content {
      flex: 1;
      overflow: hidden;
      padding: 0 !important;
    }
    
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 16px;
      color: #666;
      padding: 32px;
      text-align: center;
    }
    
    .empty-state mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      opacity: 0.5;
    }
    
    .thread-list {
      max-height: 100%;
      overflow-y: auto;
    }
    
    .thread-item {
      border-bottom: 1px solid #e0e0e0;
      cursor: pointer;
      transition: background-color 0.2s;
      height: auto !important;
      padding: 16px !important;
    }
    
    .thread-item:hover {
      background-color: #f5f5f5;
    }
    
    .thread-item.active {
      background-color: #e3f2fd;
      border-left: 4px solid #1976d2;
    }
    
    .thread-content {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .thread-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    
    .thread-title {
      font-weight: 500;
      color: #333;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .thread-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #666;
    }
    
    .delete-option {
      color: #f44336;
    }
    
    /* Custom scrollbar */
    .thread-list::-webkit-scrollbar {
      width: 6px;
    }
    
    .thread-list::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }
    
    .thread-list::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 3px;
    }
    
    .thread-list::-webkit-scrollbar-thumb:hover {
      background: #a8a8a8;
    }
  `]
})
export class ThreadListComponent implements OnInit {
  @Output() threadSelected = new EventEmitter<Thread>();
  
  threads: Thread[] = [];
  currentThread: Thread | null = null;

  constructor(
    private threadService: ThreadService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadThreads();
    this.threadService.currentThread$.subscribe(thread => {
      this.currentThread = thread;
    });
  }

  loadThreads() {
    this.threadService.getAllThreads().subscribe({
      next: (threads) => {
        this.threads = threads;
      },
      error: (error) => {
        console.error('Error loading threads:', error);
      }
    });
  }

  trackThread(index: number, thread: Thread): string {
    return thread.id;
  }

  selectThread(thread: Thread) {
    this.threadService.setCurrentThread(thread);
    this.threadSelected.emit(thread);
  }

  createNewThread() {
    const dialogRef = this.dialog.open(ThreadDialogComponent, {
      width: '400px',
      data: { title: 'New Chat', isEdit: false }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.threadService.createThread({ title: result.title }).subscribe({
          next: (newThread) => {
            this.loadThreads();
            this.selectThread(newThread);
          },
          error: (error) => {
            console.error('Error creating thread:', error);
          }
        });
      }
    });
  }

  editThread(thread: Thread) {
    const dialogRef = this.dialog.open(ThreadDialogComponent, {
      width: '400px',
      data: { title: thread.title, isEdit: true }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.threadService.updateThread(thread.id, { title: result.title }).subscribe({
          next: () => {
            this.loadThreads();
          },
          error: (error) => {
            console.error('Error updating thread:', error);
          }
        });
      }
    });
  }

  deleteThread(thread: Thread) {
    if (confirm(`Are you sure you want to delete "${thread.title}"?`)) {
      this.threadService.deleteThread(thread.id).subscribe({
        next: () => {
          this.loadThreads();
          if (this.currentThread?.id === thread.id) {
            this.threadService.setCurrentThread(null);
          }
        },
        error: (error) => {
          console.error('Error deleting thread:', error);
        }
      });
    }
  }

  formatLastActivity(timestamp: number): string {
    const date = new Date(timestamp * 1000);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);
    const diffDays = diffHours / 24;

    if (diffHours < 1) {
      return 'Just now';
    } else if (diffHours < 24) {
      return `${Math.floor(diffHours)}h ago`;
    } else if (diffDays < 7) {
      return `${Math.floor(diffDays)}d ago`;
    } else {
      return date.toLocaleDateString();
    }
  }
}