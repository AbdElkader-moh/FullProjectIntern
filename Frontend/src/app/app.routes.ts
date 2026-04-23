import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'signin', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () =>
      import('./components/home/home').then((m) => m.Home),
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./components/profile/profile').then((m) => m.Profile),
    canActivate: [authGuard],
  },
  {
    path: 'signin',
    loadComponent: () =>
      import('./components/signin/signin').then((m) => m.Signin),
  },
  {
    path: 'signup',
    loadComponent: () =>
      import('./components/signup/signup').then((m) => m.Signup),
  },
  { path: '**', redirectTo: 'signin' },
];
