import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Catches unexpected errors in the Angular zone.
 * If the error is related to authentication (e.g., 401), the user is redirected to the sign‑in page.
 * This prevents the app from crashing and ensures a smooth navigation without a full page refresh.
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private injector: Injector) {}

  handleError(error: any): void {
    const router = this.injector.get(Router);
    // Simple heuristic: if the error message contains "401" or "Unauthorized" redirect.
    const message = error?.message ?? '';
    if (message.includes('401') || message.includes('Unauthorized')) {
      router.navigate(['/signin']).catch(() => {});
    }
    // Log the error for debugging (could be replaced with a logging service).
    console.error('GlobalErrorHandler captured an error:', error);
  }
}
