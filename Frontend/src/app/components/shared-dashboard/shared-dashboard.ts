import {
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, forkJoin, interval, of } from 'rxjs';
import { catchError, startWith } from 'rxjs/operators';

import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { SensorDashboardService } from '../../services/sensor-dashboard.service';
import { NotificationItem } from '../../models/notification.model';
import {
  DashboardColumn,
  DashboardConfig,
  DashboardFilterParams,
  DashboardPageResponse,
} from '../../models/dashboard.model';

const AUTO_REFRESH_MS = 60_000;
const CW = 700;
const CH = 280;
const PAD = { top: 20, right: 20, bottom: 45, left: 55 };
const AREA_X = PAD.left;
const AREA_Y = PAD.top;
const AREA_W = CW - PAD.left - PAD.right;
const AREA_H = CH - PAD.top - PAD.bottom;

@Component({
  selector: 'app-shared-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './shared-dashboard.html',
  styleUrl: './shared-dashboard.css',
})
export class SharedDashboard implements OnInit, OnDestroy {
  @Input({ required: true }) config!: DashboardConfig;

  user: UserResponse | null = null;
  unreadCount = 0;

  stats: Record<string, any> | null = null;
  statsLoading = true;
  statsError = '';

  tableData: any[] = [];
  tableLoading = true;
  tableError = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  readonly pageSizeOptions = [5, 10, 20, 50];

  filters: Record<string, string> = {};
  sort = 'timestamp,desc';

  trendData: any[] = [];
  chartsLoading = true;
  chartsError = '';
  trendsError = false;

  recentAlerts: NotificationItem[] = [];
  alertsLoading = true;
  alertsError = '';

  lastRefreshed: Date | null = null;
  autoRefreshEnabled = true;
  isRefreshing = false;

  readonly chartViewBox = `0 0 ${CW} ${CH}`;
  readonly gridX = AREA_X;
  readonly gridY = AREA_Y;
  readonly gridW = AREA_W;
  readonly gridH = AREA_H;
  readonly gridBottom = AREA_Y + AREA_H;
  readonly gridRight = AREA_X + AREA_W;

  private firstLoad = true;

  private autoRefreshSub: Subscription | null = null;
  private statsSub: Subscription | null = null;
  private tableSub: Subscription | null = null;
  private chartsSub: Subscription | null = null;
  private alertsSub: Subscription | null = null;

  constructor(
    private dashboardService: SensorDashboardService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.pageSize = this.config.pageSize;
    this.sort = this.config.defaultSort;
    this.loadUser();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
    this.statsSub?.unsubscribe();
    this.tableSub?.unsubscribe();
    this.chartsSub?.unsubscribe();
    this.alertsSub?.unsubscribe();
  }

  private loadUser(): void {
    const current = this.authService.currentUser;

    if (current) {
      this.user = current;
      return;
    }

    this.authService.getProfile().subscribe({
      next: (user: UserResponse) => {
        this.user = user;
        this.cdr.markForCheck();
      },
      error: () => this.router.navigate(['/signin']),
    });
  }

  get firstName(): string {
    return this.user?.firstName || 'User';
  }

  loadAllData(): void {
    const showSpinners = this.firstLoad;
    this.firstLoad = false;
    this.isRefreshing = !showSpinners;

    this.loadStats(showSpinners);
    this.loadTable(showSpinners);
    this.loadCharts(showSpinners);
    this.loadRecentAlerts(showSpinners);
  }

  loadStats(showLoading = true): void {
    if (showLoading) {
      this.statsLoading = true;
      this.statsError = '';
    }

    this.statsSub?.unsubscribe();

    this.statsSub = this.dashboardService
      .getStats<Record<string, any>>(this.config.baseEndpoint)
      .subscribe({
        next: (stats) => {
          this.stats = stats;
          this.statsLoading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          if (showLoading || !this.stats) {
            this.statsError = err?.message || 'Failed to load statistics';
          }

          this.statsLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  loadTable(showLoading = true): void {
    if (showLoading) {
      this.tableLoading = true;
    }

    this.tableError = '';
    this.tableSub?.unsubscribe();

    this.tableSub = this.dashboardService
      .getPage<any>(this.config.baseEndpoint, this.buildFilterParams())
      .subscribe({
        next: (page: DashboardPageResponse<any>) => {
          this.tableData = page.content ?? [];
          this.totalElements = page.totalElements ?? 0;
          this.totalPages = page.totalPages ?? 0;
          this.currentPage = page.number ?? this.currentPage;
          this.pageSize = page.size ?? this.pageSize;

          this.tableLoading = false;
          this.isRefreshing = false;
          this.lastRefreshed = new Date();

          this.cdr.markForCheck();
        },
        error: (err) => {
          if (showLoading || !this.tableData.length) {
            this.tableError = err?.message || 'Failed to load sensor data';
          }

          this.tableLoading = false;
          this.isRefreshing = false;

          this.cdr.markForCheck();
        },
      });
  }

  loadCharts(showLoading = true): void {
    if (showLoading) {
      this.chartsLoading = true;
      this.chartsError = '';
      this.trendsError = false;
    }

    this.chartsSub?.unsubscribe();

    this.chartsSub = this.dashboardService
      .getTrends<any>(this.config.baseEndpoint)
      .pipe(
        catchError(() => {
          this.trendsError = true;
          return of([] as any[]);
        })
      )
      .subscribe({
        next: (trends) => {
          this.trendData = (trends ?? []).slice().reverse();
          this.chartsLoading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          if (showLoading || !this.trendData.length) {
            this.chartsError = err?.message || 'Failed to load chart data';
          }

          this.chartsLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  loadRecentAlerts(showLoading = true): void {
    if (showLoading) {
      this.alertsLoading = true;
      this.alertsError = '';
    }

    this.alertsSub?.unsubscribe();

    this.alertsSub = this.notificationService.getNotifications().subscribe({
      next: (alerts: NotificationItem[]) => {
        this.recentAlerts = alerts
          .sort(
            (a, b) =>
              new Date(b.createdAt).getTime() -
              new Date(a.createdAt).getTime()
          )
          .slice(0, 5);

        this.unreadCount = alerts.filter((alert) => !alert.isRead).length;
        this.alertsLoading = false;
        this.lastRefreshed = new Date();
        this.isRefreshing = false;

        this.cdr.markForCheck();
      },
      error: () => {
        if (showLoading || !this.recentAlerts.length) {
          this.alertsError = 'Failed to load recent alerts';
        }

        this.alertsLoading = false;
        this.isRefreshing = false;

        this.cdr.markForCheck();
      },
    });
  }

  private buildFilterParams(): DashboardFilterParams {
    const params: DashboardFilterParams = {
      page: this.currentPage,
      size: this.pageSize,
      sort: this.sort,
    };

    Object.entries(this.filters).forEach(([key, value]) => {
      if (!value) return;

      const filterConfig = this.config.filters.find((filter) => filter.key === key);
      params[key] = filterConfig?.type === 'date' ? this.toIso(value) : value;
    });

    return params;
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.tableData = [];
    this.loadTable(true);
  }

  clearFilters(): void {
    this.filters = {};
    this.sort = this.config.defaultSort;
    this.currentPage = 0;
    this.tableData = [];
    this.loadTable(true);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;

    this.currentPage = page;
    this.tableData = [];
    this.loadTable(true);
  }

  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    this.tableData = [];
    this.loadTable(true);
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const max = 5;
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
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get showingTo(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  startAutoRefresh(): void {
    this.stopAutoRefresh();

    if (this.autoRefreshEnabled) {
      this.autoRefreshSub = interval(AUTO_REFRESH_MS)
        .pipe(startWith(0))
        .subscribe(() => this.loadAllData());
    } else {
      this.loadAllData();
    }
  }

  stopAutoRefresh(): void {
    this.autoRefreshSub?.unsubscribe();
    this.autoRefreshSub = null;
  }

  toggleAutoRefresh(): void {
    this.autoRefreshEnabled = !this.autoRefreshEnabled;

    if (this.autoRefreshEnabled) {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  manualRefresh(): void {
    this.loadAllData();
  }

  getCellValue(row: any, key: string): any {
    return row?.[key];
  }

  formatCell(row: any, column: DashboardColumn): string {
    const value = this.getCellValue(row, column.key);

    if (value === null || value === undefined || value === '') {
      return '—';
    }

    if (column.type === 'date') {
      return this.formatTimestamp(String(value));
    }

    if (column.type === 'number') {
      const numericValue = Number(value);
      const formatted = Number.isFinite(numericValue)
        ? numericValue.toLocaleString('en-GB', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 1,
          })
        : String(value);

      return `${formatted}${column.suffix ?? ''}`;
    }

    return String(value).replace('_', ' ');
  }

  getBadgeStyle(value: string): Record<string, string> {
    const style = this.config.badgeStyles?.[value];

    if (!style) {
      return {};
    }

    return {
      background: style.background,
      color: style.color,
    };
  }

  getStatValue(key: string): any {
    return this.stats?.[key];
  }

  getStatAccent(index: number): string {
    const colors = ['#6366f1', '#8b5cf6', '#06b6d4', '#f97316', '#ef4444'];
    return colors[index % colors.length];
  }

  formatStatValue(value: any): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    if (typeof value === 'number') {
      return value.toLocaleString('en-GB', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 1,
      });
    }

    return String(value);
  }

  get firstChartField() {
    return this.config.chartFields[0];
  }

  get secondChartField() {
    return this.config.chartFields[1];
  }

  get hasTrendData(): boolean {
    return this.trendData.length > 0;
  }

  get xLabels(): { x: number; label: string }[] {
    if (!this.trendData.length) return [];

    const n = this.trendData.length;
    const step = Math.max(1, Math.ceil(n / 10));
    const labels: { x: number; label: string }[] = [];

    for (let i = 0; i < n; i += step) {
      const x = n === 1 ? AREA_X + AREA_W / 2 : AREA_X + (i / (n - 1)) * AREA_W;
      labels.push({
        x,
        label: this.formatChartTime(this.trendData[i].timestamp),
      });
    }

    return labels;
  }

  getYTicks(fieldKey: string): { y: number; label: string }[] {
    const maxVal = this.getChartMax(fieldKey);
    const ticks: { y: number; label: string }[] = [];

    for (let i = 0; i <= 4; i++) {
      const val = (maxVal / 4) * i;
      const y = AREA_Y + AREA_H - (val / maxVal) * AREA_H;
      ticks.push({
        y,
        label: Math.round(val).toString(),
      });
    }

    return ticks;
  }

  getChartMax(fieldKey: string): number {
    if (!this.trendData.length) return 100;

    const max = Math.max(
      ...this.trendData.map((point) => Number(point[fieldKey]) || 0)
    );

    return this.niceRound(max || 100);
  }

  getLinePath(fieldKey: string): string {
    const n = this.trendData.length;
    if (n < 2) return '';

    const max = this.getChartMax(fieldKey);

    return this.trendData
      .map((point, index) => {
        const value = Number(point[fieldKey]) || 0;
        const x = AREA_X + (index / (n - 1)) * AREA_W;
        const y = AREA_Y + AREA_H - (value / max) * AREA_H;

        return `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  getAreaPath(fieldKey: string): string {
    if (this.trendData.length < 2) return '';

    const bottom = AREA_Y + AREA_H;
    const lastX = AREA_X + AREA_W;

    return `${this.getLinePath(fieldKey)} L${lastX},${bottom} L${AREA_X},${bottom} Z`;
  }

  getDots(fieldKey: string): { cx: number; cy: number; val: number; tip: string }[] {
    const n = this.trendData.length;
    if (!n) return [];

    const max = this.getChartMax(fieldKey);

    return this.trendData.map((point, index) => {
      const value = Number(point[fieldKey]) || 0;

      return {
        cx: n === 1 ? AREA_X + AREA_W / 2 : AREA_X + (index / (n - 1)) * AREA_W,
        cy: AREA_Y + AREA_H - (value / max) * AREA_H,
        val: value,
        tip: `${this.formatChartTime(point.timestamp)}: ${value}`,
      };
    });
  }

  getBars(fieldKey: string): {
    x: number;
    y: number;
    w: number;
    h: number;
    val: number;
    tip: string;
  }[] {
    const n = this.trendData.length;
    if (!n) return [];

    const max = this.getChartMax(fieldKey);
    const barW = Math.min(40, (AREA_W / n) * 0.7);
    const gap = AREA_W / n;

    return this.trendData.map((point, index) => {
      const value = Number(point[fieldKey]) || 0;
      const h = Math.max(1, (value / max) * AREA_H);

      return {
        x: AREA_X + index * gap + (gap - barW) / 2,
        y: AREA_Y + AREA_H - h,
        w: barW,
        h,
        val: value,
        tip: `${this.formatChartTime(point.timestamp)}: ${value}`,
      };
    });
  }

  getAlertSeverity(alert: NotificationItem): string {
    if (!alert.value || !alert.thresholdValue) return 'Low';

    const ratio =
      alert.alertType === 'above'
        ? alert.value / alert.thresholdValue
        : alert.thresholdValue / alert.value;

    if (ratio >= 1.5) return 'High';
    if (ratio >= 1.15) return 'Medium';

    return 'Low';
  }

  getAlertSeverityStyle(severity: string): { [key: string]: string } {
    switch (severity) {
      case 'High':
        return { color: '#ffffff', backgroundColor: 'rgba(220,38,38,0.85)' };
      case 'Medium':
        return { color: '#fbbf24', backgroundColor: 'rgba(245,158,11,0.15)' };
      default:
        return { color: '#34d399', backgroundColor: 'rgba(16,185,129,0.15)' };
    }
  }

  typeIcon(type: string): string {
    if (type === 'Traffic') return '🚗';
    if (type === 'Air') return '💨';
    if (type === 'Light') return '💡';

    return '🔔';
  }

  private niceRound(value: number): number {
    if (value <= 0) return 100;

    const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
    const normalized = value / magnitude;

    let nice: number;

    if (normalized <= 1.2) nice = 1.5;
    else if (normalized <= 2) nice = 2;
    else if (normalized <= 3.5) nice = 4;
    else if (normalized <= 5) nice = 5;
    else if (normalized <= 7.5) nice = 8;
    else nice = 10;

    return nice * magnitude;
  }

  private toUtcDate(iso: string): Date {
    if (!iso) return new Date(NaN);

    const hasTimezone = iso.endsWith('Z') || /[+-]\d{2}:?\d{2}$/.test(iso);

    return new Date(hasTimezone ? iso : `${iso}Z`);
  }

  formatTimestamp(iso: string): string {
    if (!iso) return '—';

    try {
      const date = this.toUtcDate(iso);

      if (isNaN(date.getTime())) return '—';

      return date.toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });
    } catch {
      return '—';
    }
  }

  formatChartTime(iso: string): string {
    if (!iso) return '';

    try {
      const date = this.toUtcDate(iso);

      if (isNaN(date.getTime())) return '';

      return date.toLocaleTimeString('en-GB', {
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  }

  private toIso(value: string): string {
    return value.length === 16 ? `${value}:00` : value.slice(0, 19);
  }

  get analyticsRoute(): string {
    if (this.config.baseEndpoint.includes('/traffic')) return '/traffic-analytics';
    if (this.config.baseEndpoint.includes('/air')) return '/air-analytics';
    if (this.config.baseEndpoint.includes('/light')) return '/lights-analytics';

    return '/home';
}

get alertsRoute(): string {
  if (this.config.baseEndpoint.includes('/traffic')) return '/traffic-alerts';
  if (this.config.baseEndpoint.includes('/air')) return '/air-alerts';
  if (this.config.baseEndpoint.includes('/light')) return '/lights-alerts';

  return '/notifications';
}

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
