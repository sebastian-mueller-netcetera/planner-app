import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Task, TaskPage, TaskRequest, TaskLabel, SprintSummary, UserSummary, PagedResponse, Comment, TaskQueryParams } from './task.models';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/tasks`;

  getTasks(params: TaskQueryParams = {}): Observable<TaskPage> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.assigneeId) httpParams = httpParams.set('assigneeId', params.assigneeId);
    if (params.labelId) httpParams = httpParams.set('labelId', params.labelId);
    if (params.sprintId) httpParams = httpParams.set('sprintId', params.sprintId);
    if (params.page != null) httpParams = httpParams.set('page', params.page.toString());
    if (params.size != null) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<TaskPage>(this.baseUrl, { params: httpParams });
  }

  getTask(id: string): Observable<Task> {
    return this.http.get<Task>(`${this.baseUrl}/${id}`);
  }

  createTask(dto: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.baseUrl, dto);
  }

  updateTask(id: string, dto: Partial<TaskRequest>): Observable<Task> {
    return this.http.patch<Task>(`${this.baseUrl}/${id}`, dto);
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addComment(taskId: string, body: string): Observable<Comment> {
    return this.http.post<Comment>(`${this.baseUrl}/${taskId}/comments`, { body });
  }

  getLabels(): Observable<TaskLabel[]> {
    return this.http.get<TaskLabel[]>(`${environment.apiBaseUrl}/api/v1/labels`);
  }

  getSprints(): Observable<SprintSummary[]> {
    return this.http.get<PagedResponse<SprintSummary>>(`${environment.apiBaseUrl}/api/v1/sprints`)
      .pipe(map(r => r.content));
  }

  getUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${environment.apiBaseUrl}/api/v1/users`);
  }
}
