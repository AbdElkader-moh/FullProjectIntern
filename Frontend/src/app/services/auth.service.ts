import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  profilePicture: string;
  password: string;
}

export interface ApiResponse {
  message: string;
}

export interface SignupRequest {
  email: string;
  firstName: string;
  lastName: string;
  profilePicture?: File | null;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface UpdateProfilePictureRequest {
  profilePicture: File;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = '/api/users';
  private readonly httpOptions = { withCredentials: true };

  private loggedIn = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.loggedIn.asObservable();

  private _currentUser: UserResponse | null = null;
  get currentUser(): UserResponse | null {
    return this._currentUser;
  }

  constructor(private http: HttpClient) {}

  get isLoggedIn(): boolean {
    return this.loggedIn.value;
  }

  private getBackendMessage(err: HttpErrorResponse): string {
    if (err.error?.message) return err.error.message;

    // Sometimes the backend/proxy may return JSON as text instead of parsed JSON.
    if (typeof err.error === 'string') {
      try {
        const parsed = JSON.parse(err.error);
        if (parsed?.message) return parsed.message;
      } catch {
        if (err.error.trim()) return err.error;
      }
    }

    return err.message || 'An unexpected error occurred. Please try again.';
  }

private handleError(err: HttpErrorResponse): Observable<never> {
  const message =
    err.error?.message ||
    (typeof err.error === 'string' ? err.error : null) ||
    err.message ||
    'An unexpected error occurred. Please try again.';

  return throwError(() => ({
    error: { message },
    status: err.status,
  }));
}

  signup(request: SignupRequest): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('email', request.email);
    formData.append('firstName', request.firstName);
    formData.append('lastName', request.lastName);
    formData.append('password', request.password);
    if (request.profilePicture) {
      formData.append('profilePicture', request.profilePicture);
    }

    return this.http
      .post<UserResponse>(`${this.apiUrl}/signup`, formData, this.httpOptions)
      .pipe(catchError((err) => this.handleError(err)));
  }

login(request: LoginRequest): Observable<ApiResponse> {
  return this.http
    .post<ApiResponse>(`${this.apiUrl}/login`, request, this.httpOptions)
    .pipe(
      catchError((err) => this.handleError(err))
    );
}

  getProfile(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.apiUrl}/me`, this.httpOptions)
      .pipe(
        tap((user) => {
          this._currentUser = user;
          this.loggedIn.next(true);
        }),
        catchError((err) => {
          this._currentUser = null;
          this.loggedIn.next(false);
          return this.handleError(err);
        })
      );
  }

  updateProfilePicture(file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('profilePicture', file);

    return this.http
      .put<UserResponse>(
        `${this.apiUrl}/me`,
        formData,
        this.httpOptions
      )
      .pipe(
        tap((user) => {
          this._currentUser = user;
        }),
        catchError((err) => this.handleError(err))
      );
  }

  logout(): Observable<ApiResponse> {
    return this.http
      .post<ApiResponse>(`${this.apiUrl}/logout`, {}, this.httpOptions)
      .pipe(
        tap(() => {
          this._currentUser = null;
          this.loggedIn.next(false);
        }),
        catchError((err) => this.handleError(err))
      );
  }
changePassword(request: ChangePasswordRequest): Observable<ApiResponse> {
  return this.http
    .put<ApiResponse>(
      `${this.apiUrl}/me/password`,
      request,
      this.httpOptions
    )
    .pipe(catchError((err) => this.handleError(err)));
}
}
