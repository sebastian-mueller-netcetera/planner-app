import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/app-shell.component').then(m => m.AppShellComponent),
    children: [
      { path: '', redirectTo: 'backlog', pathMatch: 'full' },
      {
        path: 'backlog',
        loadComponent: () => import('./features/backlog/backlog.component').then(m => m.BacklogComponent)
      },
      {
        path: 'sprint',
        loadComponent: () => import('./features/sprint/sprint-board.component').then(m => m.SprintBoardComponent)
      },
      {
        path: 'sprint/:id',
        loadComponent: () => import('./features/sprint/sprint-board.component').then(m => m.SprintBoardComponent)
      },
      {
        path: 'tasks/new',
        loadComponent: () => import('./features/tasks/task-form/task-form.component').then(m => m.TaskFormComponent)
      },
      {
        path: 'tasks/:id',
        loadComponent: () => import('./features/tasks/task-detail/task-detail.component').then(m => m.TaskDetailComponent)
      },
      {
        path: 'tasks/:id/edit',
        loadComponent: () => import('./features/tasks/task-form/task-form.component').then(m => m.TaskFormComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
