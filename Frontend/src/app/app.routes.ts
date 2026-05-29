import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'signin', pathMatch: 'full' },
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
    path: 'settings',
    loadComponent: () =>
      import('./components/settings/settings').then((m) => m.Settings),
    canActivate: [authGuard],
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./components/notifications/notifications').then(
        (m) => m.Notifications,
      ),
    canActivate: [authGuard],
  },
  // Sprint 3 — F#7: Traffic Monitoring Dashboard
  {
    path: 'traffic',
    loadComponent: () =>
      import('./components/traffic-dashboard/traffic-dashboard').then(
        (m) => m.TrafficDashboard,
      ),
    canActivate: [authGuard],
  },
  // Sprint 3 — F#8: Traffic Analytics & Search
  {
    path: 'traffic-analytics',
    loadComponent: () =>
      import('./components/traffic-analytics/traffic-analytics').then(
        (m) => m.TrafficAnalytics,
      ),
    canActivate: [authGuard],
  },
  // Sprint 3 — F#9: Traffic Alerts
  {
    path: 'traffic-alerts',
    loadComponent: () =>
      import('./components/traffic-alerts/traffic-alerts').then(
        (m) => m.TrafficAlerts,
      ),
    canActivate: [authGuard],
  },
  {
    path: 'air',
    loadComponent: () =>
      import('./components/air-pollution-dashboard/air-pollution-dashboard').then(
        (m) => m.AirPollutionDashboard,
      ),
    canActivate: [authGuard],
  },
  {
    path: 'lights',
    loadComponent: () =>
      import('./components/street-light-dashboard/street-light-dashboard').then(
        (m) => m.StreetLightDashboard,
      ),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'signin' },
];
