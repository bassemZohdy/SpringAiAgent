import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface ThreadDialogData {
  title: string;
  isEdit: boolean;
}

@Component({
  selector: 'app-thread-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.isEdit ? 'Rename Conversation' : 'New Conversation' }}</h2>
    
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Conversation Title</mat-label>
        <input matInput [(ngModel)]="title" placeholder="Enter a title..." maxlength="100">
      </mat-form-field>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!title.trim()">
        {{ data.isEdit ? 'Save' : 'Create' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
    }
    
    mat-dialog-content {
      min-width: 300px;
    }
  `]
})
export class ThreadDialogComponent {
  title: string;

  constructor(
    public dialogRef: MatDialogRef<ThreadDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ThreadDialogData
  ) {
    this.title = data.title;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.title.trim()) {
      this.dialogRef.close({ title: this.title.trim() });
    }
  }
}