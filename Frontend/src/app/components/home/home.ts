import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { CommonModule } from '@angular/common';

interface DashboardCard {
  id: string;
  title: string;
  description: string;
  route: string;
  status: 'active';
  colorVar: string;
  gradientFrom: string;
  gradientTo: string;
  metrics: string[];
  icon: string;
}

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  user: UserResponse | null = null;
  unreadCount = 0;
  hoveredCard: string | null = null;

  readonly dashboardCards: DashboardCard[] = [
    {
      id: 'traffic',
      title: 'Traffic Monitoring',
      description: 'Real-time traffic sensor data, congestion analytics and density tracking across all city corridors.',
      route: '/traffic',
      status: 'active',
      colorVar: '--traffic',
      gradientFrom: 'rgba(245,158,11,0.18)',
      gradientTo: 'rgba(245,158,11,0)',
      metrics: ['Traffic Density', 'Avg Speed', 'Congestion Level'],
      icon: 'M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0',
    },
    {
      id: 'light',
      title: 'Street Light Management',
      description: 'Monitor and control street light networks, brightness levels and power consumption across the city.',
      route: '/lights',
      status: 'active',
      colorVar: '--light',
      gradientFrom: 'rgba(167,139,250,0.18)',
      gradientTo: 'rgba(167,139,250,0)',
      metrics: ['Brightness Level', 'Power Usage', 'On/Off Status'],
      icon: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z',
    },
    {
      id: 'air',
      title: 'Air Pollution Monitoring',
      description: 'Track CO, Ozone, PM2.5, PM10, NO2 and SO2 levels with real-time pollution alerts and trend analysis.',
      route: '/air',
      status: 'active',
      colorVar: '--air',
      gradientFrom: 'rgba(52,211,153,0.18)',
      gradientTo: 'rgba(52,211,153,0)',
      metrics: ['CO & Ozone', 'PM2.5 / PM10', 'Pollution Level'],
      icon: 'M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z',
    },
  ];

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.authService.currentUser) {
      this.user = this.authService.currentUser;
    } else {
      this.authService.getProfile().subscribe({
        next: (user) => {
          this.user = user;
          this.cdr.detectChanges();
        },
        error: () => this.router.navigate(['/signin']),
      });
    }

    this.notificationService.unreadCount$.subscribe((count: number) => {
      this.unreadCount = count;
      this.cdr.detectChanges();
    });
  }

  get firstName(): string {
    return this.user?.firstName || 'User';
  }

  navigateTo(card: DashboardCard): void {
    this.router.navigate([card.route]);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
