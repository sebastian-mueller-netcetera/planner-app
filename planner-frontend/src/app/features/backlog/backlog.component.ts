import {
  Component,
  ChangeDetectionStrategy,
  signal,
  inject,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';

import { TaskService } from '../../core/tasks/task.service';
import { TaskListItem, TaskStatus, SprintSummary } from '../../core/tasks/task.models';

@Component({
  selector: 'app-backlog',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatChipsModule,
    MatMenuModule,
    RouterLink,
    DatePipe,
  ],
  templateUrl: './backlog.component.html',
  styleUrl: './backlog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BacklogComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tasks = signal<TaskListItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<TaskStatus | ''>('');
  readonly sprints = signal<SprintSummary[]>([]);
  readonly movingTaskId = signal<string | null>(null);

  private readonly search$ = new Subject<string>();
  private readonly filter$ = new Subject<TaskStatus | ''>();

  readonly statusOptions: { value: TaskStatus | ''; label: string }[] = [
    { value: '', label: 'All' },
    { value: 'BACKLOG', label: 'Backlog' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'DONE', label: 'Done' },
  ];

  ngOnInit(): void {
    this.taskService.getSprints()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sprints) => this.sprints.set(sprints));

    this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.loading.set(true)),
        switchMap((q) =>
          this.taskService
            .getTasks({ q, status: this.statusFilter() || undefined })
            .pipe(catchError((err) => {
              this.error.set(err?.error?.message || 'Failed to load tasks');
              return of({ content: [], totalElements: 0, totalPages: 0, page: 0 });
            }))
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.tasks.set(page.content);
        this.loading.set(false);
      });

    this.filter$
      .pipe(
        distinctUntilChanged(),
        tap(() => this.loading.set(true)),
        switchMap((status) =>
          this.taskService
            .getTasks({ q: this.searchQuery(), status: status || undefined })
            .pipe(catchError((err) => {
              this.error.set(err?.error?.message || 'Failed to load tasks');
              return of({ content: [], totalElements: 0, totalPages: 0, page: 0 });
            }))
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.tasks.set(page.content);
        this.loading.set(false);
      });

    this.loadTasks();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.error.set(null);
    this.search$.next(value);
  }

  onStatusChange(status: TaskStatus | ''): void {
    this.statusFilter.set(status);
    this.error.set(null);
    this.filter$.next(status);
  }

  moveToSprint(event: Event, taskId: string, sprintId: string | null): void {
    event.preventDefault();
    event.stopPropagation();

    const previousTasks = this.tasks();
    const sprintName = sprintId
      ? this.sprints().find(s => s.id === sprintId)?.name ?? null
      : null;

    // Optimistic update
    this.movingTaskId.set(taskId);
    this.tasks.set(
      previousTasks.map(t =>
        t.id === taskId
          ? { ...t, sprint: sprintId ? { id: sprintId, name: sprintName! } : null }
          : t
      )
    );

    this.taskService.updateTask(taskId, { sprintId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.movingTaskId.set(null),
        error: () => {
          // Revert on failure
          this.tasks.set(previousTasks);
          this.movingTaskId.set(null);
        },
      });
  }

  private loadTasks(): void {
    this.loading.set(true);
    this.error.set(null);
    this.taskService
      .getTasks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.tasks.set(page.content);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.message || 'Failed to load tasks');
          this.loading.set(false);
        },
      });
  }
}
