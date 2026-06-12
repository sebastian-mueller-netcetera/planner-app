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

  createSprint(isoYear?: number, isoWeek?: number): Observable<SprintSummary> {
    const body: { isoYear?: number; isoWeek?: number } = {};
    if (isoYear !== undefined) body['isoYear'] = isoYear;
    if (isoWeek !== undefined) body['isoWeek'] = isoWeek;
    return this.http.post<SprintSummary>(this.baseUrl, body);
  }

  getSprints(): Observable<SprintSummary[]> {
    return this.http.get<SprintSummary[]>(this.baseUrl);
  }

  getUpcomingSprints(): Observable<SprintSummary[]> {
    return this.http.get<SprintSummary[]>(`${this.baseUrl}/upcoming`);
  }

  activateSprint(id: string): Observable<SprintSummary> {
    return this.http.post<SprintSummary>(`${this.baseUrl}/${id}/activate`, {});
  }
}
