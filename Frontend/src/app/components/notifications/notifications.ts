import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notifications.html',
  styleUrl: './notifications.css',
})
export class Notifications implements OnInit {
  notifications: Notification[] = [];
  isLoading = true;
  errorMessage = '';

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        this.notifications = data.sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load notifications.';
        this.isLoading = false;
      },
    });
  }

  markRead(n: Notification): void {
    if (n.isRead) return;
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => (n.isRead = true),
    });
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => this.notifications.forEach((n) => (n.isRead = true)),
    });
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => !n.isRead).length;
  }

  typeIcon(type: string): string {
    if (type === 'traffic') return '🚗';
    if (type === 'air') return '💨';
    if (type === 'light') return '💡';
    return '🔔';
  }
}