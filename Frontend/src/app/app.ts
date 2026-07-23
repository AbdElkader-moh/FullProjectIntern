import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';

// Alert auto-dismiss delay in milliseconds (matches F#9 requirement: 5 seconds)
const ALERT_DISMISS_MS = 5_000;

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
  private dismissTimer: ReturnType<typeof setTimeout> | null = null;

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
        // FIX: deactivate STOMP client on logout so it doesn't try to reconnect
        this.stompClient?.deactivate();
        this.unreadCount = 0;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
    if (this.dismissTimer) clearTimeout(this.dismissTimer);
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
      // Relative path so this resolves through nginx's /ws proxy in every
      // environment. A hardcoded http://localhost:8081/ws only ever worked on
      // a local dev machine where frontend and backend share a host.
      webSocketFactory: () => new SockJS('/ws'),

      // FIX: Auto-reconnect after 5 seconds on disconnect (network interruption, container restart)
      reconnectDelay: 5_000,

      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);

          this.alertBanner = `⚠️ ${alert.type} — ${alert.metric} is ${alert.alertType} threshold (${alert.value}) at ${alert.location}`;
          this.notificationService.incrementUnread();
          this.cdr.detectChanges();

          /**
           * FIX: Auto-dismiss the alert banner after 5 seconds (F#9 requirement).
           * Clear any existing dismiss timer first to prevent premature dismissal
           * when multiple alerts arrive in rapid succession — each new alert
           * resets the 5-second countdown.
           */
          if (this.dismissTimer) clearTimeout(this.dismissTimer);
          this.dismissTimer = setTimeout(() => {
            this.alertBanner = '';
            this.dismissTimer = null;
            this.cdr.detectChanges();
          }, ALERT_DISMISS_MS);
        });
      },

      onStompError: (frame) => {
        // Log STOMP protocol errors silently; reconnectDelay handles automatic reconnection
        console.error('[AppComponent WebSocket] STOMP error:', frame);
      },
    });

    this.stompClient.activate();
  }

  dismissAlert(): void {
    this.alertBanner = '';
    if (this.dismissTimer) {
      clearTimeout(this.dismissTimer);
      this.dismissTimer = null;
    }
    this.cdr.detectChanges();
  }
}
