import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly http        = inject(HttpClient);
  private readonly route       = inject(ActivatedRoute);

  readonly googleCalendarConnected = signal(false);
  readonly googleCalendarError     = signal<string | null>(null);
  readonly connectingCalendar      = signal(false);

  ngOnInit(): void {
    const result = this.route.snapshot.queryParamMap.get('googleCalendar');
    if (result === 'connected') {
      this.googleCalendarConnected.set(true);
    } else if (result === 'error') {
      this.googleCalendarError.set('Could not connect Google Calendar. Please try again.');
    }

    this.loadCalendarStatus();
  }

  async connectCalendar(): Promise<void> {
    this.connectingCalendar.set(true);
    this.googleCalendarError.set(null);
    try {
      const response = await firstValueFrom(
        this.http.get<{ authorizationUrl: string }>(
          `${environment.apiBaseUrl}/api/v1/integrations/google-calendar/connect`
        )
      );
      window.location.href = response.authorizationUrl;
    } catch {
      this.googleCalendarError.set('Could not initiate Google Calendar connection. Please try again.');
      this.connectingCalendar.set(false);
    }
  }

  logout(): void {
    this.authService.logout();
  }

  private async loadCalendarStatus(): Promise<void> {
    try {
      const status = await firstValueFrom(
        this.http.get<{ connected: boolean }>(
          `${environment.apiBaseUrl}/api/v1/integrations/google-calendar/status`
        )
      );
      if (status.connected) {
        this.googleCalendarConnected.set(true);
      }
    } catch {
      // Status check failure is non-fatal — connected remains false
    }
  }
}
