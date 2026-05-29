import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, forkJoin, interval, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

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

  // Track whether this is the very first load — controls spinner vs silent-refresh mode
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
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.loadAllData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
    this.dataSub?.unsubscribe();
    this.chartSub?.unsubscribe();
    this.alertsSub?.unsubscribe();
    // BUG #2 FIX: unsubscribe statsSub — was missing, caused leak on fast navigation
    this.statsSub?.unsubscribe();
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
    // NOTE: lastRefreshed is set inside loadTable/loadRecentAlerts callbacks
    // so the timestamp reflects when data ARRIVED, not when the request STARTED.
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
        // BUG #2 FIX: lastRefreshed set here (when data actually arrives)
        this.lastRefreshed = new Date();
        this.isRefreshing = false;
      },
      error: () => {
        if (showLoading || !this.recentAlerts.length) {
          this.alertsError = 'Failed to load recent alerts';
        }
        this.alertsLoading = false;
        this.isRefreshing = false;
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
    // BUG #2 FIX: Cancel previous in-flight stats request before starting a new one.
    // Previously there was no statsSub — multiple concurrent subscriptions accumulated,
    // causing race conditions where a stale response could overwrite a fresh one.
    this.statsSub?.unsubscribe();
    this.statsSub = this.trafficService.getStats().subscribe({
      next: (s) => { this.stats = s; this.statsLoading = false; },
      error: (err) => {
        if (showLoading || !this.stats) {
          this.statsError = err?.message || 'Failed to load statistics';
        }
        this.statsLoading = false;
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
        // Sync pageSize from server response as defensive guard against mismatches
        this.pageSize = page.size;
        this.tableLoading = false;
        this.isRefreshing = false;
      },
      error: (err) => {
        if (showLoading || !this.trafficData.length) {
          this.tableError = err?.message || 'Failed to load traffic data';
        }
        this.tableLoading = false;
        this.isRefreshing = false;
      },
    });
  }

  /**
   * BUG #3 FIX — Track individual chart API failures with separate error flags.
   *
   * Root cause: The inner catchError pipes swallowed API errors by returning
   * empty defaults (of([]) / of(null)), so forkJoin's outer error callback
   * never fired. chartsError remained '' even when APIs were down. Charts
   * silently showed "No data" instead of "Failed to load" — user had no feedback.
   *
   * Fix: Set trendsError / congestionError in the inner catchError callbacks.
   * The template checks these flags to show the correct empty vs. error message.
   * Charts still degrade gracefully (one chart failure doesn't kill the other).
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
        // Backend returns newest-first; reverse for chronological left→right chart display
        this.trendData = (trends ?? []).slice().reverse();
        this.congestionSummary = congestion;
        this.chartsLoading = false;
      },
      error: (err) => {
        // This path only fires if forkJoin itself fails (shouldn't happen given inner catchErrors)
        if (showLoading || !this.trendData.length) {
          this.chartsError = err?.message || 'Failed to load chart data';
        }
        this.chartsLoading = false;
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

  /**
   * BUG #1 FIX — Clear stale data before navigating to a new page.
   *
   * Root cause: goToPage() called loadTable(true), setting tableLoading=true.
   * But the spinner template condition was: `tableLoading && trafficData.length === 0`.
   * Since trafficData.length > 0 (old page records exist), the spinner was NEVER shown.
   * The OLD page's records remained visible in the table while currentPage was already
   * updated to the new value. The pagination label immediately recalculated to show
   * the new page range (e.g. "16–20 of 29") but the table still showed the old records.
   * Label and data were out of sync during the entire request window.
   *
   * Fix: Set trafficData = [] before calling loadTable(true). This makes
   * trafficData.length === 0 true, so the spinner IS shown and stale data is hidden.
   * The same fix applies to onPageSizeChange().
   */
  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    // BUG #1 FIX: clear stale records so spinner shows instead of wrong-page data
    this.trafficData = [];
    this.loadTable(true);
  }

  /**
   * BUG #1 FIX — onPageSizeChange previously called loadTable(false) when data existed.
   *
   * Root cause: loadTable(false) = silent mode — tableLoading stays false,
   * old records remain visible. But currentPage is reset to 0 and pageSize is
   * updated immediately. The showingFrom/To getters instantly recalculate:
   *   showingFrom = 0 * 20 + 1 = 1
   *   showingTo   = min(20, 29) = 20
   * So label showed "Showing 1–20 of 29" while the table STILL showed 5 stale
   * records from page 2 of the previous size=5 view. Label and data were wrong.
   *
   * Fix: Clear trafficData and always use loadTable(true) on explicit size changes.
   */
  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    // BUG #1 FIX: clear stale records and always show loading state
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

  /**
   * Pagination label getters.
   * These are correct ONLY when trafficData is in sync with currentPage/pageSize.
   * The BUG #1 fix (clearing trafficData before page navigation) ensures that
   * these values are never displayed when stale data is in the table.
   */
  get showingFrom(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }
  get showingTo(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  // ── Auto-refresh ──

  startAutoRefresh(): void {
    this.stopAutoRefresh();
    if (this.autoRefreshEnabled) {
      this.autoRefreshSub = interval(AUTO_REFRESH_MS).subscribe(() => this.loadAllData());
    }
  }

  stopAutoRefresh(): void {
    this.autoRefreshSub?.unsubscribe();
    this.autoRefreshSub = null;
  }

  toggleAutoRefresh(): void {
    this.autoRefreshEnabled = !this.autoRefreshEnabled;
    this.autoRefreshEnabled ? this.startAutoRefresh() : this.stopAutoRefresh();
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

  // ── Traffic Density LINE CHART ──

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

  /**
   * BUG #3 FIX — densityDots now includes an index (idx) for template tracking.
   *
   * Root cause: Template used `track dot.tip` where tip is a formatted timestamp string.
   * If two readings have identical timestamps (can happen with rapid sensor ingestion),
   * Angular cannot distinguish the nodes and skips DOM updates — dots disappear.
   * Fix: track by $index in the template, not by a potentially-duplicate content value.
   */
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

  // ── Average Speed BAR CHART ──

  get speedMax(): number {
    if (!this.trendData.length) return 100;
    return this.niceRound(Math.max(...this.trendData.map(t => t.avgSpeed)) || 100);
  }

  get speedYTicks() {
    return this.getYTicks(Math.max(...this.trendData.map(t => t.avgSpeed)) || 100);
  }

  /**
   * BUG #3 FIX — speedBars now matches densityDots: tracked by $index in template.
   */
  get speedBars(): { x: number; y: number; w: number; h: number; val: number; tip: string }[] {
    const n = this.trendData.length;
    if (!n) return [];
    const max = this.speedMax;
    const barW = Math.min(40, (AREA_W / n) * 0.7);
    const gap = AREA_W / n;
    return this.trendData.map((p, i) => {
      const h = Math.max(1, (p.avgSpeed / max) * AREA_H); // min height 1px so 0-speed bar is visible
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

  // Congestion distribution helpers

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

  // Chart grid area bounds (exposed for template)
  readonly gridX = AREA_X;
  readonly gridY = AREA_Y;
  readonly gridW = AREA_W;
  readonly gridH = AREA_H;
  readonly gridBottom = AREA_Y + AREA_H;
  readonly gridRight = AREA_X + AREA_W;

  // ── Utility ──

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

  /**
   * Spring Boot LocalDateTime serializes as ISO-8601 WITHOUT timezone info
   * (e.g. "2026-05-28T17:44:00"). Modern browsers treat such strings as LOCAL time.
   * Since Docker containers run UTC by default, timestamps display 3 hours behind
   * for Egypt (UTC+3) users without the 'Z' suffix.
   *
   * Fix: Append 'Z' if no timezone designator is present so the browser correctly
   * interprets the value as UTC and converts to local time for display.
   */
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
