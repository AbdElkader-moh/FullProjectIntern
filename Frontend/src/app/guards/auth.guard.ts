import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Already verified in-memory — allow navigation.
  if (authService.isLoggedIn) {
    return true;
  }

  // Cold start: check server session. If fails, redirect to /signin.
  return authService.getProfile().pipe(
    map(() => true),
    catchError(() => {
      // Navigate to sign‑in page on auth failure without a full page refresh.
      router.navigate(['/signin']);
      return of(false);
    })
  );
};