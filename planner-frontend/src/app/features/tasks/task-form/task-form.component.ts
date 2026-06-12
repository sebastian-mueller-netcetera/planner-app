import { Component, ChangeDetectionStrategy, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TaskService } from '../../../core/tasks/task.service';
import { TaskLabel, SprintSummary, UserSummary, TaskRequest } from '../../../core/tasks/task.models';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatSnackBarModule,
  ],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly location = inject(Location);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly taskService = inject(TaskService);
  private readonly snackBar = inject(MatSnackBar);

  readonly isEditMode = signal(false);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly availableLabels = signal<TaskLabel[]>([]);
  readonly availableSprints = signal<SprintSummary[]>([]);
  readonly availableAssignees = signal<UserSummary[]>([]);
  readonly selectedLabelValue = signal<string>('');
  readonly pageTitle = signal('New Task');

  private taskId: string | null = null;

  readonly taskForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    dueDate: [null as Date | null],
    assigneeId: ['' as string],
    sprintId: ['' as string],
    labelIds: [[] as string[]],
    syncGoogleCalendarA: [false],
    syncGoogleCalendarB: [false],
  });

  ngOnInit(): void {
    this.taskId = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!this.taskId);
    this.pageTitle.set(this.taskId ? 'Edit Task' : 'New Task');

    this.loadFormData();
  }

  private loadFormData(): void {
    this.loading.set(true);

    forkJoin({
      labels: this.taskService.getLabels(),
      sprints: this.taskService.getSprints(),
      users: this.taskService.getUsers(),
    }).subscribe({
      next: ({ labels, sprints, users }) => {
        this.availableLabels.set(labels);
        this.availableSprints.set(sprints);
        this.availableAssignees.set(users);

        if (this.taskId) {
          this.loadExistingTask(this.taskId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load form data', 'Dismiss', { duration: 3000 });
      }
    });
  }

  private loadExistingTask(id: string): void {
    this.taskService.getTask(id).subscribe({
      next: (task) => {
        this.taskForm.patchValue({
          title: task.title,
          description: task.description || '',
          dueDate: task.dueDate ? new Date(task.dueDate) : null,
          assigneeId: task.assignee?.id || '',
          sprintId: task.sprint?.id || '',
          labelIds: task.labels.map(l => l.id),
          syncGoogleCalendarA: task.syncGoogleCalendarA || false,
          syncGoogleCalendarB: task.syncGoogleCalendarB || false,
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load task', 'Dismiss', { duration: 3000 });
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  onSubmit(): void {
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.taskForm.getRawValue();

    const dto: TaskRequest = {
      title: raw.title,
      description: raw.description || undefined,
      dueDate: raw.dueDate ? raw.dueDate.toISOString().split('T')[0] : null,
      assigneeId: raw.assigneeId || null,
      sprintId: raw.sprintId || null,
      labelIds: raw.labelIds,
      syncGoogleCalendarA: raw.syncGoogleCalendarA,
      syncGoogleCalendarB: raw.syncGoogleCalendarB,
    };

    if (this.isEditMode() && this.taskId) {
      this.taskService.updateTask(this.taskId, dto).subscribe({
        next: () => {
          this.submitting.set(false);
          this.snackBar.open('Task updated', '', { duration: 2000 });
          this.location.back();
        },
        error: () => {
          this.submitting.set(false);
          this.snackBar.open('Failed to update task', 'Dismiss', { duration: 3000 });
        }
      });
    } else {
      this.taskService.createTask(dto).subscribe({
        next: () => {
          this.submitting.set(false);
          this.snackBar.open('Task created', '', { duration: 2000 });
          this.router.navigate(['/backlog']);
        },
        error: () => {
          this.submitting.set(false);
          this.snackBar.open('Failed to create task', 'Dismiss', { duration: 3000 });
        }
      });
    }
  }

  addLabel(labelId: string): void {
    if (!labelId) return;
    const current = this.taskForm.controls.labelIds.value;
    if (!current.includes(labelId)) {
      this.taskForm.controls.labelIds.setValue([...current, labelId]);
    }
    this.selectedLabelValue.set('');
  }

  removeLabel(labelId: string): void {
    const current = this.taskForm.controls.labelIds.value;
    this.taskForm.controls.labelIds.setValue(current.filter(id => id !== labelId));
  }

  getLabelName(labelId: string): string {
    return this.availableLabels().find(l => l.id === labelId)?.name || labelId;
  }

  getLabelColor(labelId: string): string {
    return this.availableLabels().find(l => l.id === labelId)?.color || '#666';
  }
}
