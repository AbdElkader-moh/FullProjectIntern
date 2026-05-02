import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService, LoginRequest, ApiResponse, UserResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that there are no outstanding requests
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send a login request and return an ApiResponse', () => {
    const mockLoginRequest: LoginRequest = { email: 'test@test.com', password: 'password123' };
    const mockApiResponse: ApiResponse = { message: 'Login successful' };

    service.login(mockLoginRequest).subscribe(response => {
      expect(response.message).toEqual('Login successful');
    });

    // Expect a single POST request to the login endpoint
    const req = httpTestingController.expectOne('/api/users/login');
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(mockLoginRequest);

    // Flush the mock response
    req.flush(mockApiResponse);
  });

  it('should fetch the user profile and update the logged-in state', () => {
    const mockUserResponse: UserResponse = {
      id: 1,
      email: 'test@test.com',
      firstName: 'John',
      lastName: 'Doe',
      profilePicture: '',
      password: ''
    };

    expect(service.isLoggedIn).toBe(false);

    service.getProfile().subscribe(user => {
      expect(user).toEqual(mockUserResponse);
      expect(service.currentUser).toEqual(mockUserResponse);
      expect(service.isLoggedIn).toBe(true);
    });

    const req = httpTestingController.expectOne('/api/users/me');
    expect(req.request.method).toEqual('GET');
    req.flush(mockUserResponse);
  });
});
