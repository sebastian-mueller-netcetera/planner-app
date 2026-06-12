import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatSlideToggleModule,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent {
  private readonly authService = inject(AuthService);

  readonly calendarConnected = signal(false);

  connectCalendar(): void {
    // OAuth not yet configured — button is disabled in template
  }

  disconnectCalendar(): void {
    // OAuth not yet configured — button is disabled in template
  }

  logout(): void {
    this.authService.logout();
  }
}
