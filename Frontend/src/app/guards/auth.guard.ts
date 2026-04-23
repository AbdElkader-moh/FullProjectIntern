import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Try to fetch the current user profile to validate the session
  return authService.getProfile().pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/signin']);
      return of(false);
    })
  );
};
