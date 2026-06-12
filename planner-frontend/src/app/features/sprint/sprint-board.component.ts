import { Component, ChangeDetectionStrategy, signal, inject, OnInit, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { SprintService } from '../../core/sprint/sprint.service';
import { TaskService } from '../../core/tasks/task.service';
import { SprintSummary, TaskListItem, TaskStatus } from '../../core/tasks/task.models';

interface BoardColumn {
  id: TaskStatus;
  label: string;
  tasks: TaskListItem[];
}

@Component({
  selector: 'app-sprint-board',
  standalone: true,
  imports: [
    DragDropModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatCardModule,
  ],
  templateUrl: './sprint-board.component.html',
  styleUrl: './sprint-board.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SprintBoardComponent implements OnInit {
  private readonly sprintService = inject(SprintService);
  private readonly taskService = inject(TaskService);

  readonly sprint = signal<SprintSummary | null>(null);
  readonly tasks = signal<TaskListItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly noActiveSprint = signal(false);
  readonly creatingSpint = signal(false);
  readonly upcomingSprints = signal<SprintSummary[]>([]);
  readonly showNewSprintForm = signal(false);
  readonly newSprintYear = signal<number>(new Date().getFullYear());
  readonly newSprintWeek = signal<number>(0);

  readonly sprintName = computed(() => {
    const s = this.sprint();
    return s ? s.name : 'Sprint Board';
  });

  readonly sprintWeekLabel = computed(() => {
    const s = this.sprint();
    if (!s) return '';
    return `W${s.isoWeek} · ${s.startDate} → ${s.endDate}`;
  });

  readonly columns = computed<BoardColumn[]>(() => {
    const allTasks = this.tasks();
    return [
      { id: 'BACKLOG' as TaskStatus, label: 'Todo', tasks: allTasks.filter(t => t.status === 'BACKLOG') },
      { id: 'IN_PROGRESS' as TaskStatus, label: 'In Progress', tasks: allTasks.filter(t => t.status === 'IN_PROGRESS') },
      { id: 'DONE' as TaskStatus, label: 'Done', tasks: allTasks.filter(t => t.status === 'DONE') },
    ];
  });

  ngOnInit(): void {
    this.loadSprint();
    this.loadUpcomingSprints();
  }

  loadSprint(): void {
    this.loading.set(true);
    this.error.set(null);
    this.noActiveSprint.set(false);

    this.sprintService.getCurrentSprint().subscribe({
      next: (sprint) => {
        this.sprint.set(sprint);
        this.loadTasks(sprint.id);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.noActiveSprint.set(true);
          this.loading.set(false);
        } else {
          this.error.set('Failed to load sprint. Please try again.');
          this.loading.set(false);
        }
      }
    });
  }

  createSprint(): void {
    this.creatingSpint.set(true);
    this.sprintService.createSprint().subscribe({
      next: (sprint) => {
        this.creatingSpint.set(false);
        this.noActiveSprint.set(false);
        this.sprint.set(sprint);
        this.loadTasks(sprint.id);
      },
      error: () => {
        this.creatingSpint.set(false);
        this.error.set('Failed to create sprint. Please try again.');
      }
    });
  }

  activateSprint(upcoming: SprintSummary): void {
    this.sprintService.activateSprint(upcoming.id).subscribe({
      next: () => {
        this.loadSprint();
        this.loadUpcomingSprints();
      },
      error: () => {
        this.error.set('Failed to activate sprint. Please try again.');
      }
    });
  }

  createNewSprint(): void {
    const year = this.newSprintYear();
    const week = this.newSprintWeek() || undefined;
    this.sprintService.createSprint(year, week).subscribe({
      next: () => {
        this.loadUpcomingSprints();
        this.showNewSprintForm.set(false);
      },
      error: () => {
        this.error.set('Failed to create sprint. Please try again.');
      }
    });
  }

  onDrop(event: CdkDragDrop<TaskListItem[]>, targetStatus: TaskStatus): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    const task = event.previousContainer.data[event.previousIndex];
    const previousStatus = task.status;

    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex,
    );
    this.tasks.update(ts =>
      ts.map(t => t.id === task.id ? { ...t, status: targetStatus } : t)
    );

    this.taskService.updateTask(task.id, { status: targetStatus }).subscribe({
      error: () => {
        this.tasks.update(ts =>
          ts.map(t => t.id === task.id ? { ...t, status: previousStatus } : t)
        );
      }
    });
  }

  moveTask(task: TaskListItem, direction: 'left' | 'right'): void {
    const statusOrder: TaskStatus[] = ['BACKLOG', 'IN_PROGRESS', 'DONE'];
    const currentIndex = statusOrder.indexOf(task.status);
    const newIndex = direction === 'left' ? currentIndex - 1 : currentIndex + 1;

    if (newIndex < 0 || newIndex >= statusOrder.length) return;

    const newStatus = statusOrder[newIndex];

    this.tasks.update(tasks =>
      tasks.map(t => t.id === task.id ? { ...t, status: newStatus } : t)
    );

    this.taskService.updateTask(task.id, { status: newStatus }).subscribe({
      error: () => {
        this.tasks.update(tasks =>
          tasks.map(t => t.id === task.id ? { ...t, status: task.status } : t)
        );
      }
    });
  }

  getRelativeDate(dueDate: string | null): string {
    if (!dueDate) return '';
    const due = new Date(dueDate);
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    due.setHours(0, 0, 0, 0);

    const diffMs = due.getTime() - now.getTime();
    const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays < 0) return 'Overdue';
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Tomorrow';
    return `${diffDays} days`;
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  getInitial(name: string): string {
    return name.charAt(0).toUpperCase();
  }

  private loadTasks(sprintId: string): void {
    this.taskService.getTasks({ sprintId, size: 100 }).subscribe({
      next: (page) => {
        this.tasks.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load tasks. Please try again.');
        this.loading.set(false);
      }
    });
  }

  private loadUpcomingSprints(): void {
    this.sprintService.getUpcomingSprints().subscribe({
      next: (sprints) => this.upcomingSprints.set(sprints),
      error: () => this.upcomingSprints.set([])
    });
  }
}
