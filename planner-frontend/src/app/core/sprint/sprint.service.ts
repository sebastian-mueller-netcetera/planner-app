import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SprintSummary } from '../tasks/task.models';

@Injectable({ providedIn: 'root' })
export class SprintService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/sprints`;

  getCurrentSprint(): Observable<SprintSummary> {
    return this.http.get<SprintSummary>(`${this.baseUrl}/current`);
  }

  createSprint(): Observable<SprintSummary> {
    return this.http.post<SprintSummary>(this.baseUrl, {});
  }

  getSprints(): Observable<SprintSummary[]> {
    return this.http.get<SprintSummary[]>(this.baseUrl);
  }
}
