import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';

import { Home } from './home';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let mockAuthService: any;

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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
