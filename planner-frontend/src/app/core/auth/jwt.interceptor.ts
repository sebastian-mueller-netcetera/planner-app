import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { catchError, from, switchMap, throwError } from 'rxjs';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Only attach token to API requests
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  // Skip auth header for auth endpoints (login, refresh)
  const isAuthEndpoint = req.url.includes('/api/v1/auth/login') ||
                         req.url.includes('/api/v1/auth/refresh');

  if (isAuthEndpoint) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const authedReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.headers.has('X-Retry-After-Refresh')) {
        return from(authService.refreshToken()).pipe(
          switchMap((tokens) => {
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${tokens.accessToken}`,
                'X-Retry-After-Refresh': 'true',
              },
            });
            return next(retryReq);
          }),
          catchError((refreshError) => {
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }

      return throwError(() => error);
    })
  );
};
