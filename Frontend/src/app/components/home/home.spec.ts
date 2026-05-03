import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Home } from './home';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let mockAuthService: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      currentUser: { firstName: 'Test', lastName: 'User' },
      getProfile: () => of({ firstName: 'Test', lastName: 'User' }),
      logout: () => of({ message: 'Logged out' })
    };

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([{ path: 'signin', component: class {} }]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the brand name', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand-name')?.textContent).toContain('Home');
  });

  it('should call logout and navigate to signin on success', () => {
    const logoutSpy = vi.spyOn(mockAuthService, 'logout').mockReturnValue(of({ message: 'Success' }));
    const navigateSpy = vi.spyOn(router, 'navigate');
    
    component.logout();
    
    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/signin']);
  });

  it('should navigate to signin on init if getProfile fails', () => {
    mockAuthService.currentUser = null;
    vi.spyOn(mockAuthService, 'getProfile').mockReturnValue(throwError(() => new Error('Error')));
    const navigateSpy = vi.spyOn(router, 'navigate');
    
    component.ngOnInit();
    
    expect(navigateSpy).toHaveBeenCalledWith(['/signin']);
  });
});
