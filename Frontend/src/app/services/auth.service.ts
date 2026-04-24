import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  profilePicture: string;
  password: string; // bcrypt hash returned from server
}

export interface ApiResponse {
  message: string;
}

export interface SignupRequest {
  email: string;
  firstName: string;
  lastName: string;
  profilePicture?: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateProfilePictureRequest {
  profilePicture: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = '/api/users';
  private readonly httpOptions = { withCredentials: true };

  private loggedIn = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.loggedIn.asObservable();

  currentUser: UserResponse | null = null;

  constructor(private http: HttpClient) {}

  get isLoggedIn(): boolean {
    return this.loggedIn.value;
  }

  signup(request: SignupRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(
      `${this.apiUrl}/signup`,
      request,
      this.httpOptions
    );
  }

  login(request: LoginRequest): Observable<ApiResponse> {
    return this.http
      .post<ApiResponse>(`${this.apiUrl}/login`, request, this.httpOptions)
      .pipe(tap(() => this.loggedIn.next(true)));
  }

  getProfile(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.apiUrl}/me`, this.httpOptions)
      .pipe(
        tap((user) => {
          this.currentUser = user;
          this.loggedIn.next(true);
        })
      );
  }

  updateProfilePicture(profilePicture: string): Observable<UserResponse> {
    return this.http
      .put<UserResponse>(
        `${this.apiUrl}/me`,
        { profilePicture } as UpdateProfilePictureRequest,
        this.httpOptions
      )
      .pipe(
        tap((user) => {
          this.currentUser = user;
        })
      );
  }

  logout(): Observable<ApiResponse> {
    return this.http
      .post<ApiResponse>(`${this.apiUrl}/logout`, {}, this.httpOptions)
      .pipe(
        tap(() => {
          this.currentUser = null;
          this.loggedIn.next(false);
        })
      );
  }
}