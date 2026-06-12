import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

const ACCESS_TOKEN_KEY = 'planner_access_token';
const REFRESH_TOKEN_KEY = 'planner_refresh_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/v1/auth`;

  private readonly _isAuthenticated = signal<boolean>(this.hasValidToken());

  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly isLoggedIn = computed(() => this._isAuthenticated());

  async login(credentials: LoginCredentials): Promise<boolean> {
    const response = await firstValueFrom(
      this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
    );

    this.storeTokens({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    });
    this._isAuthenticated.set(true);
    return true;
  }

  async refreshToken(): Promise<AuthTokens> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await firstValueFrom(
      this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
    );

    const tokens: AuthTokens = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    };
    this.storeTokens(tokens);
    this._isAuthenticated.set(true);
    return tokens;
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();

    // Fire-and-forget: notify backend to invalidate refresh token
    if (refreshToken) {
      this.http
        .post(`${this.apiUrl}/logout`, { refreshToken })
        .subscribe({ error: () => {} });
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    this._isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  isTokenExpired(): boolean {
    const token = this.getAccessToken();
    if (!token) return true;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiryMs = payload.exp * 1000;
      return Date.now() >= expiryMs;
    } catch {
      return true;
    }
  }

  private hasValidToken(): boolean {
    return !!this.getAccessToken() && !this.isTokenExpired();
  }

  private storeTokens(tokens: AuthTokens): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }
}
