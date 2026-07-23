import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { NotificationService } from '../../services/notification.service';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationItem } from '../../models/notification.model';
import { CONGESTION_CONFIG, CongestionLevel } from '../../models/traffic.model';

const DEFAULT_PAGE_SIZE = 15;

/** This page is scoped to Traffic alerts only (Requirements #1, #9). */
const ALERT_TYPE_TRAFFIC = 'Traffic';

/** Traffic metric classification (Requirement #8). */
const METRIC_TRAFFIC_DENSITY = 'Traffic Density';
const METRIC_AVERAGE_SPEED = 'Average Speed';

/** Toast lifetime in ms (Requirement #6 — ~5 seconds). */
const TOAST_DURATION_MS = 5000;
/** Exit-animation duration before a toast is removed from the DOM. */
const TOAST_EXIT_MS = 350;

/**
 * A single non-blocking toast. Each toast owns its own dismissal timer so that
 * rapid / simultaneous alerts stack independently instead of overwriting a
 * single shared banner (Requirements #11, #12).
 */
interface AlertToast {
  id: number;
  metric: string;
  value: number;
  thresholdValue: number;
  alertType: string;
  location: string;
  severity: string;
  /** true once the exit animation has started — drives the CSS leave state. */
  leaving: boolean;
}

@Component({
  selector: 'app-traffic-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './traffic-alerts.html',
  styleUrl: './traffic-alerts.css',
})
export class TrafficAlerts implements OnInit, OnDestroy {
  user: UserResponse | null = null;

  /**
   * Traffic-only unread count for THIS page's display.
   *
   * IMPORTANT: this is intentionally NOT pushed to NotificationService
   * .setUnreadCount(), because that drives a GLOBAL unread badge shared with
   * the header and the Notifications page. Pushing a traffic-only subset would
   * undercount the global badge. Per-id mutations (markAsRead / delete) still
   * call the service so the backend stays correct; the global count is owned by
   * components that load the full notification list.
   */
  unreadCount = 0;

  // Traffic-only alerts from backend
  allAlerts: NotificationItem[] = [];
  filteredAlerts: NotificationItem[] = [];
  pagedAlerts: NotificationItem[] = [];

  isLoading = true;
  errorMessage = '';

  /** Active non-blocking toasts (Requirement #11 — multiple handled correctly). */
  toasts: AlertToast[] = [];
  private toastSeq = 0;
  private toastTimers = new Map<number, ReturnType<typeof setTimeout>>();

  // Filters
  filterDateFrom = '';
  filterDateTo = '';
  filterLocation = '';
  /** Metric-based classification filter (Requirement #8): '', 'Traffic Density', 'Average Speed'. */
  filterMetric = '';
  filterSort = 'newest';
  filterDateError = '';
  activeFilterCount = 0;

  // Pagination
  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;
  totalPages = 0;

  readonly pageSizeOptions = [10, 15, 25, 50];
  readonly congestionConfig = CONGESTION_CONFIG;

  /** Traffic metric options for the classification filter (Requirement #8). */
  readonly metricOptions: string[] = [METRIC_TRAFFIC_DENSITY, METRIC_AVERAGE_SPEED];

  // Unique locations derived from alerts
  locationOptions: string[] = [];

  private stompClient!: Client;

  /**
   * Track the isLoggedIn$ subscription for proper teardown.
   * Without storing it, every navigation to this page accumulated a live
   * subscription on the same BehaviorSubject, each able to open its own
   * WebSocket. Stored here and torn down in ngOnDestroy.
   */
  private isLoggedInSub: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.loadAlerts();
    this.isLoggedInSub = this.authService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn && !this.stompClient?.active) {
        this.connectWebSocket();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
    this.isLoggedInSub?.unsubscribe();
    // Clear any pending toast timers to avoid callbacks after teardown.
    this.toastTimers.forEach(t => clearTimeout(t));
    this.toastTimers.clear();
  }

  private loadUser(): void {
    const current = this.authService.currentUser;
    if (current) { this.user = current; }
    else {
      this.authService.getProfile().subscribe({
        next: (u: UserResponse) => (this.user = u),
        error: () => this.router.navigate(['/signin']),
      });
    }
  }

  get firstName(): string { return this.user?.firstName || 'User'; }

  // ── Data Loading ──

  loadAlerts(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        // Requirement #1 / #9: scope strictly to Traffic alerts at the source,
        // so filtering, pagination, location options and the unread count are
        // all traffic-only downstream.
        this.allAlerts = data
          .filter(n => n.type === ALERT_TYPE_TRAFFIC)
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

        const locs = new Set(this.allAlerts.map(a => a.location).filter(Boolean));
        this.locationOptions = Array.from(locs).sort();
        this.unreadCount = this.allAlerts.filter(n => !n.isRead).length;
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

  // ── WebSocket ──

  private connectWebSocket(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) return;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);

          // Requirement #1 / #9: ignore non-traffic alerts on this page —
          // no toast, no list mutation. (The global Notifications page still
          // handles those.)
          if (alert?.type !== ALERT_TYPE_TRAFFIC) return;

          // Build a normalised NotificationItem and prepend it locally instead
          // of refetching the whole list on every event (Requirement #12 —
          // rapid incoming events). Backend has already persisted it.
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

          // Avoid duplicates if a backend id is present and already known.
          const known = alert.id && this.allAlerts.some(a => a.id === alert.id);
          if (!known) {
            this.allAlerts = [item, ...this.allAlerts];
            if (item.location && !this.locationOptions.includes(item.location)) {
              this.locationOptions = [...this.locationOptions, item.location].sort();
            }
            this.unreadCount = this.allAlerts.filter(n => !n.isRead).length;
            // Global badge: increment by one (service-owned primitive) rather
            // than overwriting the global count from this partial list.
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

  // ── Toasts (non-blocking, independent timers) ──

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
    if (existing) { clearTimeout(existing); this.toastTimers.delete(id); }
    // Mark leaving to trigger the exit animation, then remove from the DOM.
    this.toasts = this.toasts.map(t => (t.id === id ? { ...t, leaving: true } : t));
    this.cdr.detectChanges();
    setTimeout(() => {
      this.toasts = this.toasts.filter(t => t.id !== id);
      this.cdr.detectChanges();
    }, TOAST_EXIT_MS);
  }

  toastIcon(): string { return '🚗'; }

  // ── Client-side Filtering ──

  applyFilters(): void {
    if (this.filterDateFrom && this.filterDateTo && this.filterDateFrom > this.filterDateTo) {
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

    // Date range
    if (this.filterDateFrom) {
      const from = new Date(this.filterDateFrom).getTime();
      filtered = filtered.filter(a => new Date(a.createdAt).getTime() >= from);
    }
    if (this.filterDateTo) {
      const to = new Date(this.filterDateTo).getTime();
      filtered = filtered.filter(a => new Date(a.createdAt).getTime() <= to);
    }

    // Location
    if (this.filterLocation.trim()) {
      const loc = this.filterLocation.trim().toLowerCase();
      filtered = filtered.filter(a => a.location?.toLowerCase().includes(loc));
    }

    // Metric classification (Requirement #8)
    if (this.filterMetric) {
      filtered = filtered.filter(a => a.metric === this.filterMetric);
    }

    // Sorting
    if (this.filterSort === 'newest') {
      filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } else if (this.filterSort === 'oldest') {
      filtered.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else if (this.filterSort === 'severity') {
      // Sort by breach magnitude (a numeric score), not by alert direction.
      filtered.sort((a, b) => this.getSeverityScore(b) - this.getSeverityScore(a));
    }

    this.filteredAlerts = filtered;
    this.totalPages = Math.ceil(filtered.length / this.pageSize) || 1;
    if (this.currentPage >= this.totalPages) this.currentPage = Math.max(0, this.totalPages - 1);
    this.updatePagedAlerts();
  }

  private updatePagedAlerts(): void {
    const start = this.currentPage * this.pageSize;
    this.pagedAlerts = this.filteredAlerts.slice(start, start + this.pageSize);
  }

  private updateActiveFilterCount(): void {
    let c = 0;
    if (this.filterDateFrom) c++;
    if (this.filterDateTo) c++;
    if (this.filterLocation.trim()) c++;
    if (this.filterMetric) c++;
    if (this.filterSort !== 'newest') c++;
    this.activeFilterCount = c;
  }

  // ── Pagination ──

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
    let end = Math.min(this.totalPages, start + max);
    if (end - start < max) start = Math.max(0, end - max);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }

  get showingFrom(): number { return this.filteredAlerts.length === 0 ? 0 : this.currentPage * this.pageSize + 1; }
  get showingTo(): number { return Math.min((this.currentPage + 1) * this.pageSize, this.filteredAlerts.length); }

  // ── Alert actions ──

  markRead(n: NotificationItem): void {
    if (n.isRead) return;
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        n.isRead = true;
        this.unreadCount = this.allAlerts.filter(a => !a.isRead).length;
        this.cdr.detectChanges();
      },
    });
  }

  markAllRead(): void {
    // Backend marks ALL of the user's notifications read (not just traffic).
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.allAlerts.forEach(n => (n.isRead = true));
        this.unreadCount = 0;
        // Safe to reset the GLOBAL count: the backend read-all clears everything.
        this.notificationService.setUnreadCount(0);
        this.cdr.detectChanges();
      },
    });
  }

  deleteAlert(n: NotificationItem): void {
    this.notificationService.deleteNotification(n.id).subscribe({
      next: () => {
        this.allAlerts = this.allAlerts.filter(a => a.id !== n.id);
        this.unreadCount = this.allAlerts.filter(a => !a.isRead).length;
        this.applyClientFilters();
        this.cdr.detectChanges();
      },
    });
  }

  // ── Display helpers ──

  /**
   * Continuous breach score, direction-aware and safe for value === 0.
   *   above: how far the value overshoots the threshold (value/threshold - 1)
   *   below: how far the value undershoots the threshold (1 - value/threshold)
   * Higher = more severe. A speed of 0 against any positive threshold yields
   * the maximum below-score of 1.0 — correctly the most severe case.
   */
  private getSeverityScore(alert: NotificationItem): number {
    const t = alert.thresholdValue;
    const v = alert.value;
    if (t == null || v == null || t <= 0) return 0;
    if (alert.alertType === 'below') {
      // Lower value => higher severity; clamp to [0, 1].
      return Math.max(0, Math.min(1, 1 - v / t));
    }
    // 'above' (and any default): higher value => higher severity.
    return Math.max(0, v / t - 1);
  }

  getSeverityLevel(alert: NotificationItem): string {
    const score = this.getSeverityScore(alert);
    // Bands chosen to mirror the previous 1.5x / 1.15x intent:
    //   above: 50% over => High, 15% over => Medium
    //   below: 50% under => High, 15% under => Medium
    if (score >= 0.5) return 'High';
    if (score >= 0.15) return 'Medium';
    return 'Low';
  }

  getSeverityStyle(severity: string): { [k: string]: string } {
    switch (severity) {
      case 'High': return { color: '#ffffff', backgroundColor: 'rgba(220,38,38,0.85)' };
      case 'Medium': return { color: '#fbbf24', backgroundColor: 'rgba(245,158,11,0.15)' };
      default: return { color: '#34d399', backgroundColor: 'rgba(16,185,129,0.15)' };
    }
  }

  /** Metric-based icon for traffic classification (Requirement #8). */
  metricIcon(metric: string): string {
    if (metric === METRIC_TRAFFIC_DENSITY) return '🚦';
    if (metric === METRIC_AVERAGE_SPEED) return '🏎️';
    return '🚗';
  }

  formatTimestamp(iso: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
