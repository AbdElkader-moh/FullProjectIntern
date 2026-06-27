import {
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';

import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { SensorDashboardService } from '../../services/sensor-dashboard.service';
import {
  DashboardColumn,
  DashboardConfig,
  DashboardFilterParams,
  DashboardPageResponse,
} from '../../models/dashboard.model';

const DEFAULT_PAGE_SIZE = 20;

@Component({
  selector: 'app-shared-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './shared-analytics.html',
  styleUrl: './shared-analytics.css',
})
export class SharedAnalytics implements OnInit, OnDestroy {
  @Input({ required: true }) config!: DashboardConfig;

  user: UserResponse | null = null;
  unreadCount = 0;

  tableData: any[] = [];
  tableLoading = true;
  tableError = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = DEFAULT_PAGE_SIZE;

  filters: Record<string, string> = {};
  sort = 'timestamp,desc';
  filterDateError = '';
  activeFilterCount = 0;

  locationSuggestions: string[] = [];

  readonly pageSizeOptions = [10, 20, 50, 100];

  private dataSub: Subscription | null = null;
  private locationSub: Subscription | null = null;

  constructor(
    private dashboardService: SensorDashboardService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.pageSize = this.config.pageSize || DEFAULT_PAGE_SIZE;
    this.sort = this.config.defaultSort || 'timestamp,desc';

    this.loadUser();
    this.loadUnreadCount();
    this.loadLocationSuggestions();

    this.route.queryParams.pipe(take(1)).subscribe((params) => {
      this.restoreFiltersFromParams(params);
      this.updateActiveFilterCount();
      this.loadTable();
    });
  }

  ngOnDestroy(): void {
    this.dataSub?.unsubscribe();
    this.locationSub?.unsubscribe();
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
        this.cdr.detectChanges();
      },
      error: () => this.router.navigate(['/signin']),
    });
  }

  private loadUnreadCount(): void {
    this.notificationService.getNotifications().subscribe({
      next: (list) => {
        this.unreadCount = list.filter((item) => !item.isRead).length;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  private loadLocationSuggestions(): void {
    const hasLocationFilter = this.config.filters.some(
      (filter) => filter.key === 'location'
    );

    if (!hasLocationFilter) return;

    this.locationSub?.unsubscribe();

    this.locationSub = this.dashboardService
      .getPage<any>(this.config.baseEndpoint, {
        page: 0,
        size: 200,
        sort: this.config.defaultSort,
      })
      .subscribe({
        next: (page: DashboardPageResponse<any>) => {
          const locations = new Set(
            (page.content ?? [])
              .map((row) => row.location)
              .filter((location) => !!location)
          );

          this.locationSuggestions = Array.from(locations).sort();
          this.cdr.detectChanges();
        },
        error: () => {},
      });
  }

  private restoreFiltersFromParams(params: { [key: string]: any }): void {
    this.config.filters.forEach((filter) => {
      if (params[filter.key]) {
        this.filters[filter.key] = params[filter.key];
      }
    });

    if (params['sort']) this.sort = params['sort'];

    if (params['page']) {
      this.currentPage = Math.max(0, +params['page'] || 0);
    }

    if (params['size']) {
      const size = +params['size'];
      if (this.pageSizeOptions.includes(size)) {
        this.pageSize = size;
      }
    }
  }

  private syncQueryParams(): void {
    const queryParams: { [key: string]: any } = {};

    Object.entries(this.filters).forEach(([key, value]) => {
      if (value) {
        queryParams[key] = value;
      }
    });

    if (this.sort && this.sort !== this.config.defaultSort) {
      queryParams['sort'] = this.sort;
    }

    if (this.currentPage > 0) {
      queryParams['page'] = this.currentPage;
    }

    if (this.pageSize !== DEFAULT_PAGE_SIZE) {
      queryParams['size'] = this.pageSize;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true,
    });
  }

  get firstName(): string {
    return this.user?.firstName || 'User';
  }

  get dashboardRoute(): string {
    if (this.config.baseEndpoint.includes('/traffic')) return '/traffic';
    if (this.config.baseEndpoint.includes('/air')) return '/air';
    if (this.config.baseEndpoint.includes('/light')) return '/lights';

    return '/home';
  }

  get alertsRoute(): string {
    if (this.config.baseEndpoint.includes('/traffic')) return '/traffic-alerts';
    if (this.config.baseEndpoint.includes('/air')) return '/air-alerts';
    if (this.config.baseEndpoint.includes('/light')) return '/lights-alerts';

    return '/notifications';
  }

  get pageTitle(): string {
    return `${this.config.title} Analytics & Search`;
  }

  get pageSubtitle(): string {
    return `Advanced filtering, sorting, and search across ${this.config.title.toLowerCase()} records`;
  }

  get sortOptions(): { value: string; label: string }[] {
    const options = [
      { value: this.config.defaultSort, label: 'Most Recent' },
      { value: 'timestamp,asc', label: 'Oldest First' },
    ];

    this.config.columns
      .filter((column) => column.type === 'number')
      .forEach((column) => {
        options.push({
          value: `${column.key},desc`,
          label: `${column.label}: High to Low`,
        });

        options.push({
          value: `${column.key},asc`,
          label: `${column.label}: Low to High`,
        });
      });

    return options;
  }

  loadTable(): void {
    this.tableLoading = true;
    this.tableError = '';

    this.dataSub?.unsubscribe();

    this.dataSub = this.dashboardService
      .getPage<any>(this.config.baseEndpoint, this.buildFilterParams())
      .subscribe({
        next: (page: DashboardPageResponse<any>) => {
          this.tableData = page.content ?? [];
          this.totalElements = page.totalElements ?? 0;
          this.totalPages = page.totalPages ?? 0;
          this.currentPage = page.number ?? this.currentPage;
          this.pageSize = page.size ?? this.pageSize;
          this.tableLoading = false;

          this.cdr.detectChanges();
        },
        error: (err) => {
          this.tableError = err?.message || 'Failed to load data';
          this.tableLoading = false;

          this.cdr.detectChanges();
        },
      });
  }

private buildFilterParams(): DashboardFilterParams {
  const selectedSort = this.sort || this.config.defaultSort || 'timestamp,desc';
  const [sortField, sortDir] = selectedSort.split(',');

  const params: DashboardFilterParams = {
    page: this.currentPage,
    size: this.pageSize,

    // Keep this because DashboardFilterParams requires it
    sort: selectedSort,

    // Add these because the old Traffic backend expects them
    sortField: sortField || 'timestamp',
    sortDir: (sortDir as 'asc' | 'desc') || 'desc',
  };

  Object.entries(this.filters).forEach(([key, value]) => {
    if (!value) return;

    const filterConfig = this.config.filters.find(
      (filter) => filter.key === key
    );

    params[key] = filterConfig?.type === 'date'
      ? this.toIsoString(value)
      : value;
  });

  return params;
}
  private toIsoString(value: string): string {
    return value && value.length === 16 ? `${value}:00` : value;
  }

  applyFilters(): void {
    const from = this.filters['from'];
    const to = this.filters['to'];

    if (from && to && from > to) {
      this.filterDateError = 'Start date must be before end date';
      return;
    }

    this.filterDateError = '';
    this.currentPage = 0;
    this.updateActiveFilterCount();
    this.syncQueryParams();
    this.loadTable();
  }

  resetFilters(): void {
    this.filters = {};
    this.sort = this.config.defaultSort;
    this.filterDateError = '';
    this.currentPage = 0;
    this.activeFilterCount = 0;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true,
    });

    this.loadTable();
  }

  private updateActiveFilterCount(): void {
    let count = 0;

    Object.values(this.filters).forEach((value) => {
      if (value) count++;
    });

    if (this.sort !== this.config.defaultSort) count++;

    this.activeFilterCount = count;
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;

    this.currentPage = page;
    this.syncQueryParams();
    this.loadTable();
  }

  onPageSizeChange(): void {
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0;
    this.syncQueryParams();
    this.loadTable();
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
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get showingTo(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
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

    if (!style) return {};

    return {
      background: style.background,
      color: style.color,
    };
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

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
