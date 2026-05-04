import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

/**
 * Global HTTP interceptor that catches errors from any HTTP request.
 * - If a 401 Unauthorized is received, the user is redirected to the sign‑in page.
 * - Other errors are re‑thrown so components/services can handle them as needed.
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Unauthorized – send the user to the sign‑in page without a page refresh.
          this.router.navigate(['/signin']);
        }
        // Propagate the error so specific services can still display messages.
        return throwError(() => error);
      })
    );
  }
}
