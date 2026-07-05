import { ApplicationConfig, ErrorHandler } from '@angular/core';
import { provideRouter } from '@angular/router';
import { 
  provideHttpClient, 
  withInterceptors, 
  withInterceptorsFromDi, 
  HTTP_INTERCEPTORS 
} from '@angular/common/http';
import { HttpErrorInterceptor } from './services/http-error.interceptor';
import { GlobalErrorHandler } from './services/global-error.handler';
import { routes } from './app.routes';
import { jwtInterceptor } from './interceptors/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([jwtInterceptor]),
      withInterceptorsFromDi()
    ),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
  ],
};
