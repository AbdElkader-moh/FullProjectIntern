import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Profile } from './profile';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { vi } from 'vitest';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  let mockAuthService: any;

  beforeEach(async () => {
    mockAuthService = {
      currentUser: { 
        id: 1, 
        email: 'test@example.com', 
        firstName: 'Test', 
        lastName: 'User',
        password: '$2a$10$hashedpassword'
      },
      getProfile: () => of({ 
        id: 1, 
        email: 'test@example.com', 
        firstName: 'Test', 
        lastName: 'User',
        password: '$2a$10$hashedpassword'
      }),
      changePassword: () => of({ message: 'Success' }),
      logout: () => of({ message: 'Logged out' })
    };

    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display user information', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.user-name')?.textContent).toContain('Test User');
    expect(compiled.querySelector('.user-email')?.textContent).toContain('test@example.com');
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword).toBe(false);
    component.togglePassword();
    expect(component.showPassword).toBe(true);
    component.togglePassword();
    expect(component.showPassword).toBe(false);
  });

  it('should show change password form when button clicked', () => {
    expect(component.isChangePasswordVisible).toBe(false);
    component.toggleChangePassword();
    expect(component.isChangePasswordVisible).toBe(true);
  });
});
