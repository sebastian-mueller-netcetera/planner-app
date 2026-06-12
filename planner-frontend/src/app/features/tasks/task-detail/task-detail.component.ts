import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../../core/tasks/task.service';
import { Task, TaskStatus, Comment } from '../../../core/tasks/task.models';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatInputModule,
    MatFormFieldModule,
    RouterLink,
    DatePipe,
    LowerCasePipe,
    FormsModule,
  ],
  templateUrl: './task-detail.component.html',
  styleUrl: './task-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly taskService = inject(TaskService);
  private readonly snackBar = inject(MatSnackBar);

  readonly task = signal<Task | null>(null);
  readonly loading = signal(true);
  readonly commentBody = signal('');
  readonly submittingComment = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTask(id);
    }
  }

  private loadTask(id: string): void {
    this.loading.set(true);
    this.taskService.getTask(id).subscribe({
      next: (task) => {
        this.task.set(task);
        this.loading.set(false);
      },
      error: () => {
        this.task.set(null);
        this.loading.set(false);
        this.snackBar.open('Failed to load task', 'Dismiss', { duration: 3000 });
      }
    });
  }

  get statusLabel(): string {
    const status = this.task()?.status;
    switch (status) {
      case 'BACKLOG': return 'Start';
      case 'IN_PROGRESS': return 'Complete';
      case 'DONE': return 'Reopen';
      default: return '';
    }
  }

  get statusIcon(): string {
    const status = this.task()?.status;
    switch (status) {
      case 'BACKLOG': return 'play_arrow';
      case 'IN_PROGRESS': return 'check_circle';
      case 'DONE': return 'replay';
      default: return '';
    }
  }

  cycleStatus(): void {
    const current = this.task();
    if (!current) return;

    const nextStatus: Record<TaskStatus, TaskStatus> = {
      'BACKLOG': 'IN_PROGRESS',
      'IN_PROGRESS': 'DONE',
      'DONE': 'BACKLOG',
    };

    const newStatus = nextStatus[current.status];
    this.taskService.updateTask(current.id, { status: newStatus }).subscribe({
      next: (updated) => {
        this.task.set(updated);
        this.snackBar.open(`Status changed to ${newStatus.replace('_', ' ')}`, '', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to update status', 'Dismiss', { duration: 3000 })
    });
  }

  deleteTask(): void {
    const current = this.task();
    if (!current) return;

    if (!confirm(`Delete "${current.title}"? This cannot be undone.`)) return;

    this.taskService.deleteTask(current.id).subscribe({
      next: () => {
        this.snackBar.open('Task deleted', '', { duration: 2000 });
        this.router.navigate(['/backlog']);
      },
      error: () => this.snackBar.open('Failed to delete task', 'Dismiss', { duration: 3000 })
    });
  }

  addComment(): void {
    const current = this.task();
    const body = this.commentBody().trim();
    if (!current || !body) return;

    this.submittingComment.set(true);
    this.taskService.addComment(current.id, body).subscribe({
      next: (comment) => {
        this.task.set({
          ...current,
          comments: [...current.comments, comment],
        });
        this.commentBody.set('');
        this.submittingComment.set(false);
      },
      error: () => {
        this.submittingComment.set(false);
        this.snackBar.open('Failed to add comment', 'Dismiss', { duration: 3000 });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/backlog']);
  }
}
