import {
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { NotificationService } from '../../services/notification.service';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationItem } from '../../models/notification.model';
import { AlertsConfig } from '../../models/alerts-config.model';

const DEFAULT_PAGE_SIZE = 15;
const TOAST_DURATION_MS = 5000;
const TOAST_EXIT_MS = 350;

interface AlertToast {
  id: number;
  metric: string;
  value: number;
  thresholdValue: number;
  alertType: string;
  location: string;
  severity: string;
  leaving: boolean;
}

@Component({
  selector: 'app-shared-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './shared-alerts.html',
  styleUrl: './shared-alerts.css',
})
export class SharedAlerts implements OnInit, OnDestroy {
  @Input({ required: true }) config!: AlertsConfig;

  user: UserResponse | null = null;
  unreadCount = 0;

  allAlerts: NotificationItem[] = [];
  filteredAlerts: NotificationItem[] = [];
  pagedAlerts: NotificationItem[] = [];

  isLoading = true;
  errorMessage = '';

  toasts: AlertToast[] = [];
  private toastSeq = 0;
  private toastTimers = new Map<number, ReturnType<typeof setTimeout>>();

  filterDateFrom = '';
  filterDateTo = '';
  filterLocation = '';
  filterMetric = '';
  filterSort = 'newest';
  filterDateError = '';
  activeFilterCount = 0;

  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;
  totalPages = 0;

  readonly pageSizeOptions = [10, 15, 25, 50];

  locationOptions: string[] = [];

  private stompClient!: Client;
  private isLoggedInSub: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.loadAlerts();

    this.isLoggedInSub = this.authService.isLoggedIn$.subscribe((isLoggedIn) => {
      if (isLoggedIn && !this.stompClient?.active) {
        this.connectWebSocket();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
    this.isLoggedInSub?.unsubscribe();

    this.toastTimers.forEach((timer) => clearTimeout(timer));
    this.toastTimers.clear();
  }

 private loadUser(): void {
  const current = this.authService.currentUser;

  if (current) {
    this.user = current;

    if (!this.stompClient?.active) {
      this.connectWebSocket();
    }

    return;
  }

  this.authService.getProfile().subscribe({
    next: (user: UserResponse) => {
      this.user = user;

      if (!this.stompClient?.active) {
        this.connectWebSocket();
      }

      this.cdr.detectChanges();
    },
    error: () => this.router.navigate(['/signin']),
  });
}

  get firstName(): string {
    return this.user?.firstName || 'User';
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        this.allAlerts = data
          .filter((notification) => notification.type === this.config.notificationType)
          .sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );

        const locations = new Set(
          this.allAlerts.map((alert) => alert.location).filter(Boolean)
        );

        this.locationOptions = Array.from(locations).sort();
        this.unreadCount = this.allAlerts.filter((alert) => !alert.isRead).length;

        this.applyClientFilters();

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load alerts. Please try again.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private connectWebSocket(): void {
    const userId = this.user?.id ?? this.authService.currentUser?.id;
    if (!userId) return;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);

          if (alert?.type !== this.config.notificationType) return;

          const item: NotificationItem = {
            id: alert.id ?? `live-${Date.now()}-${this.toastSeq}`,
            type: alert.type,
            metric: alert.metric,
            value: Number(alert.value),
            thresholdValue: Number(alert.thresholdValue),
            alertType: alert.alertType,
            location: alert.location,
            isRead: false,
            createdAt: alert.createdAt ?? new Date().toISOString(),
          };

          const known =
            alert.id && this.allAlerts.some((existing) => existing.id === alert.id);

          if (!known) {
            this.allAlerts = [item, ...this.allAlerts];

            if (item.location && !this.locationOptions.includes(item.location)) {
              this.locationOptions = [...this.locationOptions, item.location].sort();
            }

            this.unreadCount = this.allAlerts.filter((n) => !n.isRead).length;
            this.notificationService.incrementUnread();
            this.applyClientFilters();
          }

          this.pushToast(item);
          this.cdr.detectChanges();
        });
      },
    });

    this.stompClient.activate();
  }

  private pushToast(item: NotificationItem): void {
    const id = ++this.toastSeq;

    this.toasts = [
      {
        id,
        metric: item.metric,
        value: item.value,
        thresholdValue: item.thresholdValue,
        alertType: item.alertType,
        location: item.location,
        severity: this.getSeverityLevel(item),
        leaving: false,
      },
      ...this.toasts,
    ];

    const timer = setTimeout(() => this.dismissToast(id), TOAST_DURATION_MS);
    this.toastTimers.set(id, timer);
  }

  dismissToast(id: number): void {
    const existing = this.toastTimers.get(id);

    if (existing) {
      clearTimeout(existing);
      this.toastTimers.delete(id);
    }

    this.toasts = this.toasts.map((toast) =>
      toast.id === id ? { ...toast, leaving: true } : toast
    );

    this.cdr.detectChanges();

    setTimeout(() => {
      this.toasts = this.toasts.filter((toast) => toast.id !== id);
      this.cdr.detectChanges();
    }, TOAST_EXIT_MS);
  }

  toastIcon(): string {
    return this.config.toastIcon;
  }

  applyFilters(): void {
    if (
      this.filterDateFrom &&
      this.filterDateTo &&
      this.filterDateFrom > this.filterDateTo
    ) {
      this.filterDateError = 'Start date must be before end date';
      return;
    }

    this.filterDateError = '';
    this.currentPage = 0;
    this.updateActiveFilterCount();
    this.applyClientFilters();
  }

  resetFilters(): void {
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.filterLocation = '';
    this.filterMetric = '';
    this.filterSort = 'newest';
    this.filterDateError = '';
    this.currentPage = 0;
    this.activeFilterCount = 0;

    this.applyClientFilters();
  }

  private applyClientFilters(): void {
    let filtered = [...this.allAlerts];

    if (this.filterDateFrom) {
      const from = new Date(this.filterDateFrom).getTime();
      filtered = filtered.filter(
        (alert) => new Date(alert.createdAt).getTime() >= from
      );
    }

    if (this.filterDateTo) {
      const to = new Date(this.filterDateTo).getTime();
      filtered = filtered.filter(
        (alert) => new Date(alert.createdAt).getTime() <= to
      );
    }

    if (this.filterLocation.trim()) {
      const location = this.filterLocation.trim().toLowerCase();
      filtered = filtered.filter((alert) =>
        alert.location?.toLowerCase().includes(location)
      );
    }

    if (this.filterMetric) {
      filtered = filtered.filter((alert) => alert.metric === this.filterMetric);
    }

    if (this.filterSort === 'newest') {
      filtered.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    } else if (this.filterSort === 'oldest') {
      filtered.sort(
        (a, b) =>
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );
    } else if (this.filterSort === 'severity') {
      filtered.sort((a, b) => this.getSeverityScore(b) - this.getSeverityScore(a));
    }

    this.filteredAlerts = filtered;
    this.totalPages = Math.ceil(filtered.length / this.pageSize) || 1;

    if (this.currentPage >= this.totalPages) {
      this.currentPage = Math.max(0, this.totalPages - 1);
    }

    this.updatePagedAlerts();
  }

  private updatePagedAlerts(): void {
    const start = this.currentPage * this.pageSize;
    this.pagedAlerts = this.filteredAlerts.slice(start, start + this.pageSize);
  }

  private updateActiveFilterCount(): void {
    let count = 0;

    if (this.filterDateFrom) count++;
    if (this.filterDateTo) count++;
    if (this.filterLocation.trim()) count++;
    if (this.filterMetric) count++;
    if (this.filterSort !== 'newest') count++;

    this.activeFilterCount = count;
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;

    this.currentPage = page;
    this.updatePagedAlerts();
  }

  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    this.applyClientFilters();
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const max = 7;

    let start = Math.max(0, this.currentPage - Math.floor(max / 2));
    const end = Math.min(this.totalPages, start + max);

    if (end - start < max) {
      start = Math.max(0, end - max);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    return pages;
  }

  get showingFrom(): number {
    return this.filteredAlerts.length === 0
      ? 0
      : this.currentPage * this.pageSize + 1;
  }

  get showingTo(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.filteredAlerts.length);
  }

  markRead(alert: NotificationItem): void {
    if (alert.isRead) return;

    this.notificationService.markAsRead(alert.id).subscribe({
      next: () => {
        alert.isRead = true;
        this.unreadCount = this.allAlerts.filter((item) => !item.isRead).length;
        this.cdr.detectChanges();
      },
    });
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.allAlerts.forEach((alert) => (alert.isRead = true));
        this.unreadCount = 0;
        this.notificationService.setUnreadCount(0);
        this.cdr.detectChanges();
      },
    });
  }

  deleteAlert(alert: NotificationItem): void {
    this.notificationService.deleteNotification(alert.id).subscribe({
      next: () => {
        this.allAlerts = this.allAlerts.filter((item) => item.id !== alert.id);
        this.unreadCount = this.allAlerts.filter((item) => !item.isRead).length;
        this.applyClientFilters();
        this.cdr.detectChanges();
      },
    });
  }

  private getSeverityScore(alert: NotificationItem): number {
    const threshold = alert.thresholdValue;
    const value = alert.value;

    if (threshold == null || value == null || threshold <= 0) return 0;

    if (alert.alertType === 'below') {
      return Math.max(0, Math.min(1, 1 - value / threshold));
    }

    return Math.max(0, value / threshold - 1);
  }

  getSeverityLevel(alert: NotificationItem): string {
    const score = this.getSeverityScore(alert);

    if (score >= 0.5) return 'High';
    if (score >= 0.15) return 'Medium';
    return 'Low';
  }

  getSeverityStyle(severity: string): { [key: string]: string } {
    switch (severity) {
      case 'High':
        return {
          color: '#ffffff',
          backgroundColor: 'rgba(220,38,38,0.85)',
        };
      case 'Medium':
        return {
          color: '#fbbf24',
          backgroundColor: 'rgba(245,158,11,0.15)',
        };
      default:
        return {
          color: '#34d399',
          backgroundColor: 'rgba(16,185,129,0.15)',
        };
    }
  }

  metricIcon(metric: string): string {
    if (metric === 'Traffic Density') return '🚦';
    if (metric === 'Average Speed') return '🏎️';
    if (metric === 'Carbon Monoxide') return '🧪';
    if (metric === 'Ozone') return '🌫️';
    if (metric === 'Brightness Level') return '💡';
    if (metric === 'Power Consumption') return '⚡';

    return this.config.icon;
  }

  metricShortLabel(metric: string): string {
    if (metric === 'Traffic Density') return 'Density';
    if (metric === 'Average Speed') return 'Speed';
    if (metric === 'Carbon Monoxide') return 'CO';
    if (metric === 'Ozone') return 'Ozone';
    if (metric === 'Brightness Level') return 'Brightness';
    if (metric === 'Power Consumption') return 'Power';

    return metric;
  }

  formatTimestamp(iso: string): string {
    if (!iso) return '—';

    const date = new Date(iso);

    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
