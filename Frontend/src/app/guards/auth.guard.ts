import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Already verified in-memory — skip the network call.
  // This prevents a getProfile() HTTP request on every navigation
  // between protected routes, and avoids the catchError below
  // firing and redirecting to /signin while an error message is displayed.
  if (authService.isLoggedIn) {
    return true;
  }

  // Cold start only: check if a server session exists (e.g. page refresh).
  return authService.getProfile().pipe(
    map(() => true),
    catchError(() => {
      return of(false);
    })
  );
};