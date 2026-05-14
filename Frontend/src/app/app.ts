import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class AppComponent implements OnInit, OnDestroy {
  alertBanner = '';
  unreadCount = 0;
  private stompClient!: Client;

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.authService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.loadUnreadCount();
        if (!this.stompClient?.active) {
          this.connectWebSocket();
        }
      } else {
        this.stompClient?.deactivate();
        this.unreadCount = 0;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }

  loadUnreadCount(): void {
    this.notificationService.getNotifications().subscribe({
    next: (data) => {
    this.notificationService.setUnreadCount(data.filter(n => !n.isRead).length);
      }
    });
  }

  private connectWebSocket(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) return;

    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8081/ws/websocket',
      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);
          this.alertBanner = `⚠️ ${alert.type} — ${alert.metric} is ${alert.alertType} threshold (${alert.value}) at ${alert.location}`;
          this.unreadCount++;
          this.notificationService.incrementUnread();
          this.cdr.detectChanges();
        });
      },
    });

    this.stompClient.activate();
  }

  dismissAlert(): void {
    this.alertBanner = '';
    this.cdr.detectChanges();
  }
}