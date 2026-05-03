import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../../services/auth.service';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { vi } from 'vitest';

import { Signin } from './signin';

describe('Signin', () => {
  let component: Signin;
  let fixture: ComponentFixture<Signin>;
  let mockAuthService: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      login: () => of({}),
      getProfile: () => of({}),
      loggedIn: new BehaviorSubject<boolean>(false)
    };

    await TestBed.configureTestingModule({
      imports: [Signin],
      providers: [
        provideRouter([{ path: 'home', component: class {} }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Signin);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize the form with empty values', () => {
    expect(component.signinForm.get('email')?.value).toBe('');
    expect(component.signinForm.get('password')?.value).toBe('');
  });

  it('should validate email format', () => {
    const emailControl = component.signinForm.get('email');
    emailControl?.setValue('invalid-email');
    expect(emailControl?.valid).toBe(false);
    expect(emailControl?.hasError('email')).toBe(true);
    
    emailControl?.setValue('test@example.com');
    expect(emailControl?.valid).toBe(true);
  });

  it('should call login and navigate to home on success', () => {
    component.signinForm.setValue({
      email: 'test@example.com',
      password: 'password'
    });
    
    const loginSpy = vi.spyOn(mockAuthService, 'login').mockReturnValue(of({}));
    const profileSpy = vi.spyOn(mockAuthService, 'getProfile').mockReturnValue(of({}));
    const navigateSpy = vi.spyOn(router, 'navigate');
    
    component.onSubmit();
    
    expect(loginSpy).toHaveBeenCalled();
    expect(profileSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/home']);
  });

  it('should show error message on login failure', () => {
    component.signinForm.setValue({
      email: 'test@example.com',
      password: 'wrong'
    });
    
    vi.spyOn(mockAuthService, 'login').mockReturnValue(throwError(() => ({ error: { message: 'Invalid credentials' } })));
    
    component.onSubmit();
    
    expect(component.errorMessage).toBe('Invalid credentials');
  });
});
