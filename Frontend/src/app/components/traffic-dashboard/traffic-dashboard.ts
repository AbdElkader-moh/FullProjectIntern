import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, forkJoin, interval, of } from 'rxjs';
import { catchError, startWith } from 'rxjs/operators';

import { TrafficService } from '../../services/traffic.service';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { NotificationItem } from '../../models/notification.model';
import {
  TrafficRecord, TrafficPage, TrafficStats,
  TrafficTrendPoint, CongestionSummary, TrafficFilterParams,
  CONGESTION_CONFIG, CONGESTION_LEVELS, CongestionLevel,
} from '../../models/traffic.model';

const AUTO_REFRESH_MS = 60_000;
const DEFAULT_PAGE_SIZE = 10;

// SVG chart dimensions (viewBox-based, so responsive)
const CW = 700, CH = 280;
const PAD = { top: 20, right: 20, bottom: 45, left: 55 };
const AREA_X = PAD.left, AREA_Y = PAD.top;
const AREA_W = CW - PAD.left - PAD.right;
const AREA_H = CH - PAD.top - PAD.bottom;

@Component({
  selector: 'app-traffic-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './traffic-dashboard.html',
  styleUrl: './traffic-dashboard.css',
})
export class TrafficDashboard implements OnInit, OnDestroy {
  user: UserResponse | null = null;
  unreadCount = 0;

  stats: TrafficStats | null = null;
  statsLoading = true;
  statsError = '';

  trafficData: TrafficRecord[] = [];
  tableLoading = true;
  tableError = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;

  trendData: TrafficTrendPoint[] = [];
  congestionSummary: CongestionSummary | null = null;
  chartsLoading = true;
  chartsError = '';

  /**
   * BUG #3 FIX — Track individual chart API errors separately.
   *
   * Root cause: loadCharts() uses per-observable catchError(() => of([])) so
   * forkJoin ALWAYS resolves its `next` callback — the outer `error` callback
   * never fires. chartsError was never set when APIs failed. Charts showed
   * "No trend data available" with no indication that it was actually an API error.
   *
   * Fix: Set these flags inside the individual catchError callbacks.
   * Template uses them to show "Failed to load" vs "No data available" messages.
   */
  trendsError = false;
  congestionError = false;

  lastRefreshed: Date | null = null;
  autoRefreshEnabled = true;
  isRefreshing = false;

  recentAlerts: NotificationItem[] = [];
  alertsLoading = true;
  alertsError = '';

  readonly congestionConfig = CONGESTION_CONFIG;
  readonly congestionLevels = CONGESTION_LEVELS;
  readonly pageSizeOptions = [5, 10, 20, 50];

  readonly chartViewBox = `0 0 ${CW} ${CH}`;

  /**
   * firstLoad tracks whether we are on the very first data fetch after
   * component creation. On first load, full spinners are shown.
   * On subsequent auto-refresh calls, data updates silently in-place.
   *
   * IMPORTANT: Because startAutoRefresh() now uses startWith(0), the
   * interval emits immediately at t=0 and calls loadAllData(). That first
   * emission must consume the firstLoad=true state so it shows spinners.
   * ngOnInit no longer calls loadAllData() directly — the interval handles it.
   * This eliminates the race where ngOnInit and the t=0 interval emission
   * both called loadAllData() simultaneously (double request on init).
   */
  private firstLoad = true;

  // ── Subscriptions — all tracked for clean teardown ──
  private autoRefreshSub: Subscription | null = null;
  private dataSub: Subscription | null = null;
  private chartSub: Subscription | null = null;
  private alertsSub: Subscription | null = null;

  /**
   * BUG #2 FIX — Add statsSub tracking.
   *
   * Root cause: loadStats() had no subscription tracking. Multiple concurrent
   * calls (e.g., initial load + manual refresh before first completes) would
   * create multiple active subscriptions. A stale response could overwrite a
   * fresh one (race condition). No cleanup happened on component destroy.
   *
   * Fix: Track the subscription. Cancel the previous request before starting
   * a new one. Unsubscribe in ngOnDestroy.
   */
  private statsSub: Subscription | null = null;

  constructor(
    private trafficService: TrafficService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    /**
     * RENDER BUG FIX — Inject ChangeDetectorRef for zoneless change detection.
     *
     * Root cause: This application runs without zone.js (zoneless mode).
     * In a zoneless build, Angular's change detection is NOT automatically
     * triggered when RxJS Observable callbacks (next/error) update component
     * state. The HTTP responses were arriving and mutating fields such as
     * `stats`, `trafficData`, `trendData`, and `*Loading` correctly in memory,
     * but Angular never re-evaluated the template bindings — so the view stayed
     * frozen on the initial spinner/empty state indefinitely.
     *
     * Why user gestures "fixed" it: DOM event handlers (button clicks, select
     * change) are one of the few remaining triggers that cause Angular to run
     * a change-detection pass even in zoneless mode. When the user clicked
     * "Auto Refresh" or changed the page-size select, Angular re-checked the
     * template, found the already-populated fields, and rendered the data that
     * had been silently sitting in the component for potentially minutes.
     *
     * Fix: Call this.cdr.markForCheck() at the end of every async callback
     * that mutates component state, exactly as AppComponent already does for
     * its STOMP WebSocket callbacks. markForCheck() is preferred over
     * detectChanges() because it is re-entrancy-safe and schedules the check
     * in the next CD pass rather than executing synchronously mid-callback.
     *
     * This pattern is safe under both zoneless and zone-based configurations:
     * if zone.js is present, markForCheck() is a cheap, harmless no-op.
     */
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadUser();
    /**
     * DATA SYNC FIX — Do NOT call loadAllData() here directly.
     *
     * Root cause of the original bug: with the proxy misconfigured, every
     * API call on init silently failed (proxied to wrong port). The spinners
     * showed briefly, errors were set, but because all four sections failed
     * simultaneously the UX looked like "nothing loaded."
     *
     * With the proxy now correctly routing /api/sensors → :8081, the primary
     * fix is the proxy.conf.json change. However, we also restructure init
     * here to use startAutoRefresh() exclusively as the data trigger:
     *
     *   startAutoRefresh() → interval(60s).pipe(startWith(0))
     *                       → emits immediately at t=0
     *                       → calls loadAllData() with firstLoad=true (spinners)
     *                       → then every 60s calls loadAllData() silently
     *
     * Benefits:
     * 1. Single code path for both initial load and periodic refresh.
     * 2. No double-request race (previously ngOnInit AND t=0 both fired loadAllData).
     * 3. Re-navigation recreates the component → ngOnInit → startAutoRefresh →
     *    immediate t=0 emission → fresh data load. Works correctly every time.
     * 4. toggleAutoRefresh() re-calls startAutoRefresh() → new t=0 emission →
     *    data loads immediately when user re-enables auto-refresh.
     */
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
    this.dataSub?.unsubscribe();
    this.chartSub?.unsubscribe();
    this.alertsSub?.unsubscribe();
    this.statsSub?.unsubscribe();
  }

  private loadUser(): void {
    const current = this.authService.currentUser;
    if (current) {
      this.user = current;
      // currentUser is synchronous — no markForCheck() needed here.
    } else {
      this.authService.getProfile().subscribe({
        next: (u: UserResponse) => {
          this.user = u;
          this.cdr.markForCheck(); // async: notify Angular to re-render the username
        },
        error: () => this.router.navigate(['/signin']),
      });
    }
  }

  get firstName(): string { return this.user?.firstName || 'User'; }

  // ── Data Loading ──

  /**
   * Orchestrates all data loading. On first load, full spinners appear.
   * On subsequent calls (auto-refresh), data updates silently in-place
   * without replacing content with spinners, preventing disruptive UI flashes.
   */
  loadAllData(): void {
    const showSpinners = this.firstLoad;
    this.firstLoad = false;
    this.isRefreshing = !showSpinners;

    this.loadStats(showSpinners);
    this.loadTable(showSpinners);
    this.loadCharts(showSpinners);
    this.loadRecentAlerts(showSpinners);
  }

  loadRecentAlerts(showLoading = true): void {
    if (showLoading) {
      this.alertsLoading = true;
      this.alertsError = '';
    }
    this.alertsSub?.unsubscribe();
    this.alertsSub = this.notificationService.getNotifications().subscribe({
      next: (data: NotificationItem[]) => {
        this.recentAlerts = data
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5);
        this.unreadCount = data.filter(n => !n.isRead).length;
        this.alertsLoading = false;
        this.lastRefreshed = new Date();
        this.isRefreshing = false;
        this.cdr.markForCheck(); // zoneless: render alerts list, unread count, last-refreshed timestamp
      },
      error: () => {
        if (showLoading || !this.recentAlerts.length) {
          this.alertsError = 'Failed to load recent alerts';
        }
        this.alertsLoading = false;
        this.isRefreshing = false;
        this.cdr.markForCheck(); // zoneless: render error state
      },
    });
  }

  getAlertSeverity(alert: NotificationItem): string {
    if (!alert.value || !alert.thresholdValue) return 'Low';
    const ratio = alert.alertType === 'above'
      ? alert.value / alert.thresholdValue
      : alert.thresholdValue / alert.value;
    if (ratio >= 1.5) return 'High';
    if (ratio >= 1.15) return 'Medium';
    return 'Low';
  }

  getAlertSeverityStyle(severity: string): { [k: string]: string } {
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

  loadStats(showLoading = true): void {
    if (showLoading) {
      this.statsLoading = true;
      this.statsError = '';
    }
    this.statsSub?.unsubscribe();
    this.statsSub = this.trafficService.getStats().subscribe({
      next: (s) => {
        this.stats = s;
        this.statsLoading = false;
        this.cdr.markForCheck(); // zoneless: render stat cards
      },
      error: (err) => {
        if (showLoading || !this.stats) {
          this.statsError = err?.message || 'Failed to load statistics';
        }
        this.statsLoading = false;
        this.cdr.markForCheck(); // zoneless: render error state in stats section
      },
    });
  }

  loadTable(showLoading = true): void {
    if (showLoading) {
      this.tableLoading = true;
    }
    this.tableError = '';
    const params = this.buildFilterParams();
    this.dataSub?.unsubscribe();
    this.dataSub = this.trafficService.getTrafficData(params).subscribe({
      next: (page: TrafficPage) => {
        this.trafficData = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.number;
        this.pageSize = page.size;
        this.tableLoading = false;
        this.isRefreshing = false;
        this.cdr.markForCheck(); // zoneless: render table rows and pagination controls
      },
      error: (err) => {
        if (showLoading || !this.trafficData.length) {
          this.tableError = err?.message || 'Failed to load traffic data';
        }
        this.tableLoading = false;
        this.isRefreshing = false;
        this.cdr.markForCheck(); // zoneless: render table error state
      },
    });
  }

  /**
   * BUG #3 FIX — Track individual chart API failures with separate error flags.
   */
  loadCharts(showLoading = true): void {
    if (showLoading) {
      this.chartsLoading = true;
      this.chartsError = '';
      this.trendsError = false;
      this.congestionError = false;
    }

    this.chartSub?.unsubscribe();
    this.chartSub = forkJoin({
      trends: this.trafficService.getTrends().pipe(
        catchError(() => {
          this.trendsError = true;
          return of([] as TrafficTrendPoint[]);
        })
      ),
      congestion: this.trafficService.getCongestionSummary().pipe(
        catchError(() => {
          this.congestionError = true;
          return of(null as CongestionSummary | null);
        })
      ),
    }).subscribe({
      next: ({ trends, congestion }) => {
        this.trendData = (trends ?? []).slice().reverse();
        this.congestionSummary = congestion;
        this.chartsLoading = false;
        this.cdr.markForCheck(); // zoneless: render SVG charts and congestion bars
      },
      error: (err) => {
        if (showLoading || !this.trendData.length) {
          this.chartsError = err?.message || 'Failed to load chart data';
        }
        this.chartsLoading = false;
        this.cdr.markForCheck(); // zoneless: render chart error state
      },
    });
  }

  private buildFilterParams(): TrafficFilterParams {
    return {
      sortField: 'timestamp',
      sortDir: 'desc',
      page: this.currentPage,
      size: this.pageSize,
    };
  }

  // ── Pagination ──

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.trafficData = [];
    this.loadTable(true);
  }

  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    this.trafficData = [];
    this.loadTable(true);
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const max = 5;
    let start = Math.max(0, this.currentPage - Math.floor(max / 2));
    let end = Math.min(this.totalPages, start + max);
    if (end - start < max) start = Math.max(0, end - max);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }

  get showingFrom(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get showingTo(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  // ── Auto-refresh ──

  startAutoRefresh(): void {
    this.stopAutoRefresh();
    /**
     * DATA SYNC FIX — Use startWith(0) so the interval emits immediately at t=0.
     *
     * Before: interval(60_000) → first emission after 60 seconds. Initial data
     * load relied entirely on the separate loadAllData() call in ngOnInit.
     * If that call failed (e.g. proxy misconfigured, route resolver delay),
     * the dashboard showed empty spinners for 60 seconds until the first
     * auto-refresh tick finally fired.
     *
     * After: interval(60_000).pipe(startWith(0)) → emits 0 immediately on
     * subscribe, then 1, 2, 3... every 60 seconds. The t=0 emission calls
     * loadAllData() with firstLoad=true, showing full spinners. Subsequent
     * emissions call it with firstLoad=false, updating silently.
     *
     * When autoRefreshEnabled is false (user paused it), we still fire one
     * immediate load so the user always sees current data, then stop the interval.
     */
    if (this.autoRefreshEnabled) {
      this.autoRefreshSub = interval(AUTO_REFRESH_MS)
        .pipe(startWith(0))
        .subscribe(() => this.loadAllData());
    } else {
      // Even with auto-refresh disabled, load data once on init/re-enable
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
      /**
       * Re-enabling auto-refresh: call startAutoRefresh() which uses startWith(0)
       * → triggers an immediate data load, then resumes 60s interval.
       * User gets fresh data the moment they re-enable, not 60s later.
       */
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  manualRefresh(): void { this.loadAllData(); }

  // ── Congestion badge ──

  getCongestionStyle(level: CongestionLevel): { [k: string]: string } {
    const c = CONGESTION_CONFIG[level];
    return c ? { color: c.color, backgroundColor: c.bg } : {};
  }

  // ═══════ SVG CHART HELPERS ═══════

  getYTicks(maxVal: number): { y: number; label: string }[] {
    const ticks: { y: number; label: string }[] = [];
    const niceMax = this.niceRound(maxVal);
    for (let i = 0; i <= 4; i++) {
      const val = (niceMax / 4) * i;
      const y = AREA_Y + AREA_H - (val / niceMax) * AREA_H;
      ticks.push({ y, label: Math.round(val).toString() });
    }
    return ticks;
  }

  get xLabels(): { x: number; label: string }[] {
    if (!this.trendData.length) return [];
    const n = this.trendData.length;
    const step = Math.max(1, Math.ceil(n / 10));
    const labels: { x: number; label: string }[] = [];
    for (let i = 0; i < n; i += step) {
      const x = n === 1 ? AREA_X + AREA_W / 2 : AREA_X + (i / (n - 1)) * AREA_W;
      labels.push({ x, label: this.formatChartTime(this.trendData[i].timestamp) });
    }
    return labels;
  }

  get densityMax(): number {
    if (!this.trendData.length) return 100;
    return this.niceRound(Math.max(...this.trendData.map(t => t.trafficDensity)) || 100);
  }

  get densityYTicks() {
    return this.getYTicks(Math.max(...this.trendData.map(t => t.trafficDensity)) || 100);
  }

  get densityLinePath(): string {
    const n = this.trendData.length;
    if (n < 2) return '';
    const max = this.densityMax;
    return this.trendData.map((p, i) => {
      const x = AREA_X + (i / (n - 1)) * AREA_W;
      const y = AREA_Y + AREA_H - (p.trafficDensity / max) * AREA_H;
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  get densityAreaPath(): string {
    if (this.trendData.length < 2) return '';
    const bottom = AREA_Y + AREA_H;
    const lastX = AREA_X + AREA_W;
    return `${this.densityLinePath} L${lastX},${bottom} L${AREA_X},${bottom} Z`;
  }

  get densityDots(): { cx: number; cy: number; val: number; tip: string }[] {
    const n = this.trendData.length;
    if (!n) return [];
    const max = this.densityMax;
    return this.trendData.map((p, i) => ({
      cx: n === 1 ? AREA_X + AREA_W / 2 : AREA_X + (i / (n - 1)) * AREA_W,
      cy: AREA_Y + AREA_H - (p.trafficDensity / max) * AREA_H,
      val: p.trafficDensity,
      tip: `${this.formatChartTime(p.timestamp)}: ${p.trafficDensity} veh/hr`,
    }));
  }

  get speedMax(): number {
    if (!this.trendData.length) return 100;
    return this.niceRound(Math.max(...this.trendData.map(t => t.avgSpeed)) || 100);
  }

  get speedYTicks() {
    return this.getYTicks(Math.max(...this.trendData.map(t => t.avgSpeed)) || 100);
  }

  get speedBars(): { x: number; y: number; w: number; h: number; val: number; tip: string }[] {
    const n = this.trendData.length;
    if (!n) return [];
    const max = this.speedMax;
    const barW = Math.min(40, (AREA_W / n) * 0.7);
    const gap = AREA_W / n;
    return this.trendData.map((p, i) => {
      const h = Math.max(1, (p.avgSpeed / max) * AREA_H);
      return {
        x: AREA_X + i * gap + (gap - barW) / 2,
        y: AREA_Y + AREA_H - h,
        w: barW,
        h,
        val: p.avgSpeed,
        tip: `${this.formatChartTime(p.timestamp)}: ${p.avgSpeed.toFixed(1)} km/h`,
      };
    });
  }

  get congestionTotal(): number {
    if (!this.congestionSummary) return 1;
    return ((this.congestionSummary.Low || 0) + (this.congestionSummary.Moderate || 0) +
      (this.congestionSummary.High || 0) + (this.congestionSummary.Severe || 0)) || 1;
  }

  getCongestionPercent(level: CongestionLevel): number {
    if (!this.congestionSummary) return 0;
    return Math.round(((this.congestionSummary[level] || 0) / this.congestionTotal) * 100);
  }

  getCongestionCount(level: CongestionLevel): number {
    return this.congestionSummary?.[level] || 0;
  }

  readonly gridX = AREA_X;
  readonly gridY = AREA_Y;
  readonly gridW = AREA_W;
  readonly gridH = AREA_H;
  readonly gridBottom = AREA_Y + AREA_H;
  readonly gridRight = AREA_X + AREA_W;

  private niceRound(val: number): number {
    if (val <= 0) return 100;
    const mag = Math.pow(10, Math.floor(Math.log10(val)));
    const norm = val / mag;
    let nice: number;
    if (norm <= 1.2) nice = 1.5;
    else if (norm <= 2) nice = 2;
    else if (norm <= 3.5) nice = 4;
    else if (norm <= 5) nice = 5;
    else if (norm <= 7.5) nice = 8;
    else nice = 10;
    return nice * mag;
  }

  private toUtcDate(iso: string): Date {
    if (!iso) return new Date(NaN);
    const hasTimezone = iso.endsWith('Z') || /[+-]\d{2}:?\d{2}$/.test(iso);
    return new Date(hasTimezone ? iso : iso + 'Z');
  }

  formatTimestamp(iso: string): string {
    if (!iso) return '—';
    try {
      const d = this.toUtcDate(iso);
      if (isNaN(d.getTime())) return '—';
      return d.toLocaleString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: true,
      });
    } catch {
      return '—';
    }
  }

  formatChartTime(iso: string): string {
    if (!iso) return '';
    try {
      const d = this.toUtcDate(iso);
      if (isNaN(d.getTime())) return '';
      return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
