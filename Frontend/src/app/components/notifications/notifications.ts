import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { NotificationService } from '../../services/notification.service';
import { AuthService } from '../../services/auth.service';
import { NotificationItem } from '../../models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notifications.html',
  styleUrl: './notifications.css',
})
export class Notifications implements OnInit, OnDestroy {
  notifications: NotificationItem[] = [];
  isLoading = true;
  errorMessage = '';
  alertBanner = '';

  private stompClient!: Client;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
    this.authService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn && !this.stompClient?.active) {
        this.connectWebSocket();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }

  load(): void {
    this.isLoading = true;
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        this.notifications = data.sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load notifications.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private connectWebSocket(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) return;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);
          this.alertBanner = `⚠️ ${alert.type} — ${alert.metric} is ${alert.alertType} threshold (${alert.value}) at ${alert.location}`;
          this.load();
          this.cdr.detectChanges();
          setTimeout(() => {
            this.alertBanner = '';
            this.cdr.detectChanges();
          }, 5000);
        });
      },
    });

    this.stompClient.activate();
  }

markRead(n: NotificationItem): void {
  if (n.isRead) return;
  this.notificationService.markAsRead(n.id).subscribe({
    next: () => {
      n.isRead = true;
      this.notificationService.setUnreadCount(
        this.notifications.filter(n => !n.isRead).length
      );
      this.cdr.detectChanges();
    },
  });
}

markAllRead(): void {
  this.notificationService.markAllAsRead().subscribe({
    next: () => {
      this.notifications.forEach((n) => (n.isRead = true));
      this.notificationService.setUnreadCount(0);
      this.cdr.detectChanges();
    },
  });
}

  goHome(): void {
    this.router.navigate(['/home']);
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => !n.isRead).length;
  }

  typeIcon(type: string): string {
    if (type === 'Traffic') return '🚗';
    if (type === 'Air') return '💨';
    if (type === 'Light') return '💡';
    return '🔔';
  }
}
