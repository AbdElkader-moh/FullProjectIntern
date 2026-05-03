import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Signup } from './signup';

describe('Signup', () => {
  let component: Signup;
  let fixture: ComponentFixture<Signup>;
  let mockAuthService: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      signup: () => of({})
    };

    await TestBed.configureTestingModule({
      imports: [Signup],
      providers: [
        provideRouter([{ path: 'signin', component: class {} }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Signup);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize the form with empty values', () => {
    expect(component.signupForm.get('firstName')?.value).toBe('');
    expect(component.signupForm.get('lastName')?.value).toBe('');
    expect(component.signupForm.get('email')?.value).toBe('');
    expect(component.signupForm.get('password')?.value).toBe('');
  });

  it('should validate required fields', () => {
    const form = component.signupForm;
    expect(form.valid).toBe(false);
    
    form.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123'
    });
    
    expect(form.valid).toBe(true);
  });

  it('should call signup and navigate to signin on success', () => {
    component.signupForm.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123'
    });
    
    const signupSpy = vi.spyOn(mockAuthService, 'signup').mockReturnValue(of({}));
    const navigateSpy = vi.spyOn(router, 'navigate');
    
    component.onSubmit();
    
    expect(signupSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/signin']);
  });

  it('should show error message on signup failure', () => {
    component.signupForm.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123'
    });
    
    vi.spyOn(mockAuthService, 'signup').mockReturnValue(throwError(() => ({ error: { message: 'Email already exists' } })));
    
    component.onSubmit();
    
    expect(component.errorMessage).toBe('Email already exists');
  });

  it('should detect invalid fields correctly', () => {
    const firstName = component.signupForm.get('firstName');
    firstName?.setValue('');
    firstName?.markAsTouched();
    
    expect(component.isFieldInvalid('firstName')).toBe(true);
    
    firstName?.setValue('John');
    expect(component.isFieldInvalid('firstName')).toBe(false);
  });
});
