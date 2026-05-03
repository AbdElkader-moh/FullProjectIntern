import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Profile } from './profile';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { vi } from 'vitest';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  let mockAuthService: any;
  let router: Router;

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
        provideRouter([{ path: 'signin', component: class {} }]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
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

  it('should call changePassword and show success message', () => {
    component.oldPassword = 'old';
    component.newPassword = 'new';
    component.confirmPassword = 'new';
    
    const changeSpy = vi.spyOn(mockAuthService, 'changePassword').mockReturnValue(of({ message: 'Password updated!' }));
    
    component.changePassword();
    
    expect(changeSpy).toHaveBeenCalledWith({ oldPassword: 'old', newPassword: 'new' });
    expect(component.passwordMessage).toBe('Password updated!');
    expect(component.oldPassword).toBe('');
  });

  it('should show error if passwords do not match', () => {
    component.oldPassword = 'old';
    component.newPassword = 'new';
    component.confirmPassword = 'different';
    
    component.changePassword();
    
    expect(component.passwordError).toBe('New passwords do not match.');
  });

  it('should show error if changePassword fails', () => {
    component.oldPassword = 'old';
    component.newPassword = 'new';
    component.confirmPassword = 'new';
    
    vi.spyOn(mockAuthService, 'changePassword').mockReturnValue(throwError(() => ({ error: { message: 'Failed' } })));
    
    component.changePassword();
    
    expect(component.passwordError).toBe('Failed');
  });

  it('should call logout and navigate to signin', () => {
    const logoutSpy = vi.spyOn(mockAuthService, 'logout').mockReturnValue(of({}));
    const navigateSpy = vi.spyOn(router, 'navigate');
    
    component.logout();
    
    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/signin']);
  });
});
