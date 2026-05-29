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

@Component({
  selector: 'app-traffic-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './traffic-alerts.html',
  styleUrl: './traffic-alerts.css',
})
export class TrafficAlerts implements OnInit, OnDestroy {
  user: UserResponse | null = null;
  unreadCount = 0;

  // All alerts from backend
  allAlerts: NotificationItem[] = [];
  filteredAlerts: NotificationItem[] = [];
  pagedAlerts: NotificationItem[] = [];

  isLoading = true;
  errorMessage = '';
  alertBanner = '';

  // Filters
  filterDateFrom = '';
  filterDateTo = '';
  filterLocation = '';
  filterType = '';
  filterSort = 'newest';
  filterDateError = '';
  activeFilterCount = 0;

  // Pagination
  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;
  totalPages = 0;

  readonly pageSizeOptions = [10, 15, 25, 50];
  readonly congestionConfig = CONGESTION_CONFIG;
  readonly alertTypes: string[] = ['Traffic', 'Air', 'Light'];

  // Unique locations derived from alerts
  locationOptions: string[] = [];

  private stompClient!: Client;

  /**
   * BUG #2 FIX — Track the isLoggedIn$ subscription for proper teardown.
   *
   * Root cause: ngOnInit called this.authService.isLoggedIn$.subscribe(...)
   * but the returned Subscription was never stored or unsubscribed. Every
   * navigation to the alerts page accumulated a new live subscription on the
   * same BehaviorSubject. After 5 navigations, 5 simultaneous handlers were
   * active — each could call connectWebSocket() independently, creating
   * multiple stompClient instances and WebSocket connections.
   *
   * Fix: store the subscription and unsubscribe it in ngOnDestroy.
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
    // BUG #2 FIX: assign to isLoggedInSub so ngOnDestroy can unsubscribe it.
    // Previously this was a fire-and-forget subscribe() — no reference kept,
    // no way to cancel, subscription accumulated on every component visit.
    this.isLoggedInSub = this.authService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn && !this.stompClient?.active) {
        this.connectWebSocket();
      }
    });
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
    // BUG #2 FIX: was missing — caused a new leak on every component visit
    this.isLoggedInSub?.unsubscribe();
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
        this.allAlerts = data.sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        // Extract unique locations
        const locs = new Set(this.allAlerts.map(a => a.location).filter(Boolean));
        this.locationOptions = Array.from(locs).sort();
        this.unreadCount = this.allAlerts.filter(n => !n.isRead).length;
        this.notificationService.setUnreadCount(this.unreadCount);
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
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      onConnect: () => {
        this.stompClient.subscribe(`/topic/alerts/${userId}`, (msg: IMessage) => {
          const alert = JSON.parse(msg.body);
          this.alertBanner = `⚠️ ${alert.type} — ${alert.metric} is ${alert.alertType} threshold (${alert.value}) at ${alert.location}`;
          this.loadAlerts();
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
    this.filterType = '';
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

    // Type
    if (this.filterType) {
      filtered = filtered.filter(a => a.type === this.filterType);
    }

    // Sorting
    if (this.filterSort === 'newest') {
      filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } else if (this.filterSort === 'oldest') {
      filtered.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else if (this.filterSort === 'severity') {
      const severityOrder: Record<string, number> = { above: 2, below: 1 };
      filtered.sort((a, b) => (severityOrder[b.alertType] || 0) - (severityOrder[a.alertType] || 0));
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
    if (this.filterType) c++;
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
        this.notificationService.setUnreadCount(this.unreadCount);
        this.cdr.detectChanges();
      },
    });
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.allAlerts.forEach(n => (n.isRead = true));
        this.unreadCount = 0;
        this.notificationService.setUnreadCount(0);
        this.cdr.detectChanges();
      },
    });
  }

  deleteAlert(n: NotificationItem): void {
    this.notificationService.deleteNotification(n.id).subscribe({
      next: () => {
        if (!n.isRead) {
          this.unreadCount = Math.max(0, this.unreadCount - 1);
          this.notificationService.setUnreadCount(this.unreadCount);
        }
        this.allAlerts = this.allAlerts.filter(a => a.id !== n.id);
        this.applyClientFilters();
        this.cdr.detectChanges();
      },
    });
  }

  // ── Display helpers ──

  getSeverityLevel(alert: NotificationItem): string {
    // Determine severity based on value vs threshold
    if (!alert.value || !alert.thresholdValue) return 'Low';
    const ratio = alert.alertType === 'above'
      ? alert.value / alert.thresholdValue
      : alert.thresholdValue / alert.value;
    if (ratio >= 1.5) return 'High';
    if (ratio >= 1.15) return 'Medium';
    return 'Low';
  }

  getSeverityStyle(severity: string): { [k: string]: string } {
    switch (severity) {
      case 'High': return { color: '#ffffff', backgroundColor: 'rgba(220,38,38,0.85)' };
      case 'Medium': return { color: '#fbbf24', backgroundColor: 'rgba(245,158,11,0.15)' };
      default: return { color: '#34d399', backgroundColor: 'rgba(16,185,129,0.15)' };
    }
  }

  typeIcon(type: string): string {
    if (type === 'Traffic') return '🚗';
    if (type === 'Air') return '💨';
    if (type === 'Light') return '💡';
    return '🔔';
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
