import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';

import { TrafficService } from '../../services/traffic.service';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import {
  TrafficRecord, TrafficPage, TrafficFilterParams,
  CONGESTION_CONFIG, SORT_OPTIONS, CONGESTION_LEVELS, CongestionLevel,
} from '../../models/traffic.model';

const DEFAULT_PAGE_SIZE = 20;

@Component({
  selector: 'app-traffic-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './traffic-analytics.html',
  styleUrl: './traffic-analytics.css',
})
export class TrafficAnalytics implements OnInit, OnDestroy {
  user: UserResponse | null = null;
  unreadCount = 0;

  // Table data
  trafficData: TrafficRecord[] = [];
  tableLoading = true;
  tableError = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;

  // Filters
  filterLocation = '';
  filterCongestion = '';
  filterFrom = '';
  filterTo = '';
  filterSort = 'timestamp,desc';
  filterDateError = '';
  activeFilterCount = 0;

  // Unique locations for dropdown (populated from data)
  locationSuggestions: string[] = [];

  readonly congestionConfig = CONGESTION_CONFIG;
  readonly sortOptions = SORT_OPTIONS;
  readonly congestionLevels = CONGESTION_LEVELS;
  readonly pageSizeOptions = [10, 20, 50, 100];

  private dataSub: Subscription | null = null;
  // FIX: track the location suggestions subscription to unsubscribe on destroy
  private locationSub: Subscription | null = null;

  constructor(
    private trafficService: TrafficService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    // FIX: inject ActivatedRoute for URL query param state persistence
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.loadUnreadCount();
    this.loadLocationSuggestions();

    /**
     * FIX — Issue 3: Filter state persistence across route navigation.
     *
     * Root cause: Each time the user navigated away from /traffic-analytics
     * (e.g. to /traffic) and back, the component was destroyed and recreated,
     * resetting all filter state (location, congestion, date range, sort, page).
     * This caused searches to "stop working" — the user's applied filters were
     * silently lost on navigation.
     *
     * Fix: Read filter state from URL query params on component init.
     * After applying filters or navigating pages, update the URL params.
     * This way:
     *   - Navigating back restores the exact filter state from the URL
     *   - Filtered views are bookmarkable and shareable
     *   - Browser back/forward buttons work correctly
     *
     * The `take(1)` operator ensures we only read params once at init —
     * subsequent param changes are driven by this component itself.
     */
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      this.restoreFiltersFromParams(params);
      this.updateActiveFilterCount();
      this.loadTable();
    });
  }

  ngOnDestroy(): void {
    this.dataSub?.unsubscribe();
    // FIX: unsubscribe location suggestions to prevent memory leaks on fast navigation
    this.locationSub?.unsubscribe();
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

  private loadUnreadCount(): void {
    this.notificationService.getNotifications().subscribe({
      next: (list: any[]) => { this.unreadCount = list.filter((n: any) => !n.isRead).length; },
      error: () => {},
    });
  }

  private loadLocationSuggestions(): void {
    // FIX: cancel previous location request before starting a new one
    this.locationSub?.unsubscribe();
    this.locationSub = this.trafficService.getTrafficData({ page: 0, size: 200 }).subscribe({
      next: (page: TrafficPage) => {
        const locations = new Set(page.content.map(r => r.location));
        this.locationSuggestions = Array.from(locations).sort();
      },
      error: () => {},
    });
  }

  /**
   * FIX — Issue 3: Restore filter values from URL query params.
   * Called once on init so navigation back to this route restores the last search.
   */
  private restoreFiltersFromParams(params: { [key: string]: any }): void {
    if (params['location']) this.filterLocation = params['location'];
    if (params['congestion']) this.filterCongestion = params['congestion'];
    if (params['from']) this.filterFrom = params['from'];
    if (params['to']) this.filterTo = params['to'];
    if (params['sort']) this.filterSort = params['sort'];
    if (params['page']) this.currentPage = Math.max(0, +params['page'] || 0);
    if (params['size']) {
      const s = +params['size'];
      if (this.pageSizeOptions.includes(s)) this.pageSize = s;
    }
  }

  /**
   * FIX — Issue 3: Write current filter + pagination state to URL query params.
   * Only non-default values are written to keep URLs clean.
   * Uses replaceUrl: true so filter changes don't create browser history entries.
   */
  private syncQueryParams(): void {
    const queryParams: { [key: string]: any } = {};
    if (this.filterLocation.trim()) queryParams['location'] = this.filterLocation.trim();
    if (this.filterCongestion) queryParams['congestion'] = this.filterCongestion;
    if (this.filterFrom) queryParams['from'] = this.filterFrom;
    if (this.filterTo) queryParams['to'] = this.filterTo;
    if (this.filterSort && this.filterSort !== 'timestamp,desc') queryParams['sort'] = this.filterSort;
    if (this.currentPage > 0) queryParams['page'] = this.currentPage;
    if (this.pageSize !== DEFAULT_PAGE_SIZE) queryParams['size'] = this.pageSize;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true,   // Replace history entry — don't stack params in browser history
    });
  }

  get firstName(): string { return this.user?.firstName || 'User'; }

  // ── Data Loading ──

  loadTable(): void {
    this.tableLoading = true;
    this.tableError = '';
    const params = this.buildFilterParams();
    this.dataSub?.unsubscribe();
    this.dataSub = this.trafficService.getTrafficData(params).subscribe({
      next: (page: TrafficPage) => {
        this.trafficData = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.number;
        this.tableLoading = false;
      },
      error: (err) => {
        this.tableError = err?.message || 'Failed to load traffic data';
        this.tableLoading = false;
      },
    });
  }

  private buildFilterParams(): TrafficFilterParams {
    const [sortField, sortDir] = (this.filterSort || 'timestamp,desc').split(',');
    return {
      location: this.filterLocation.trim() || undefined,
      congestionLevel: this.filterCongestion || undefined,
      from: this.filterFrom ? this.toIsoString(this.filterFrom) : undefined,
      to: this.filterTo ? this.toIsoString(this.filterTo) : undefined,
      sortField: sortField || 'timestamp',
      sortDir: (sortDir as 'asc' | 'desc') || 'desc',
      page: this.currentPage,
      size: this.pageSize,
    };
  }

  private toIsoString(v: string): string {
    // datetime-local input gives 'YYYY-MM-DDTHH:MM' (16 chars); append ':00' for LocalDateTime
    return v && v.length === 16 ? v + ':00' : v;
  }

  // ── Filter Actions ──

  applyFilters(): void {
    if (this.filterFrom && this.filterTo && this.filterFrom > this.filterTo) {
      this.filterDateError = 'Start date must be before end date';
      return;
    }
    this.filterDateError = '';
    this.currentPage = 0;
    this.updateActiveFilterCount();
    // FIX: persist filter state in URL so navigation back restores the search
    this.syncQueryParams();
    this.loadTable();
  }

  resetFilters(): void {
    this.filterLocation = '';
    this.filterCongestion = '';
    this.filterFrom = '';
    this.filterTo = '';
    this.filterSort = 'timestamp,desc';
    this.filterDateError = '';
    this.currentPage = 0;
    this.activeFilterCount = 0;
    // FIX: clear all query params from URL when resetting filters
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true,
    });
    this.loadTable();
  }

  private updateActiveFilterCount(): void {
    let c = 0;
    if (this.filterLocation.trim()) c++;
    if (this.filterCongestion) c++;
    if (this.filterFrom) c++;
    if (this.filterTo) c++;
    if (this.filterSort !== 'timestamp,desc') c++;
    this.activeFilterCount = c;
  }

  // ── Pagination ──

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    // FIX: sync page number to URL so browser back/forward restores the correct page
    this.syncQueryParams();
    this.loadTable();
  }

  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    // FIX: sync page size to URL
    this.syncQueryParams();
    this.loadTable();
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

  get showingFrom(): number { return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1; }
  get showingTo(): number { return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements); }

  // ── Badge ──

  getCongestionStyle(level: CongestionLevel): { [k: string]: string } {
    const c = CONGESTION_CONFIG[level];
    return c ? { color: c.color, backgroundColor: c.bg } : {};
  }

  /**
   * FIX — Issue 4: Correct timestamp display for all timezones.
   * Spring Boot LocalDateTime has no TZ info (e.g. "2026-05-28T17:44:00").
   * Without 'Z', browsers treat it as LOCAL time — in Egypt (UTC+3) this shows
   * timestamps 3 hours behind actual local time. Appending 'Z' tells the browser
   * to interpret the value as UTC and display it converted to the user's local timezone.
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

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
