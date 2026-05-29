import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Subscription, interval, forkJoin, of } from 'rxjs';
import { startWith, catchError } from 'rxjs/operators';

interface AirRecord {
  id: string;
  location: string;
  timestamp: string;
  co: number;
  ozone: number;
  pm2_5: number;
  pm10: number;
  no2: number;
  so2: number;
  pollutionLevel: 'Good' | 'Moderate' | 'Unhealthy' | 'Very_Unhealthy';
}

interface AirPage {
  content: AirRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface AirStats {
  totalRecords: number;
  averageCo: number;
  averageOzone: number;
  averagePm25: number;
  goodCount: number;
  unhealthyCount: number;
}

const POLLUTION_COLORS: Record<string, { background: string; color: string }> = {
  Good:          { background: 'rgba(52,211,153,0.15)',  color: '#34d399' },
  Moderate:      { background: 'rgba(251,191,36,0.15)',  color: '#fbbf24' },
  Unhealthy:     { background: 'rgba(245,158,11,0.15)',  color: '#f59e0b' },
  Very_Unhealthy:{ background: 'rgba(239,68,68,0.15)',   color: '#ef4444' },
};

@Component({
  selector: 'app-air-pollution-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './air-pollution-dashboard.html',
  styleUrl: './air-pollution-dashboard.css',
})
export class AirPollutionDashboard implements OnInit, OnDestroy {
  stats: AirStats | null = null;
  page: AirPage | null = null;
  isLoading = true;
  hasError = false;
  lastRefreshed: Date | null = null;
  currentPage = 0;
  readonly pageSize = 20;

  filterLocation = '';
  filterPollution = '';
  filterFrom = '';
  filterTo = '';
  sortBy = 'timestamp,desc';

  readonly pollutionLevels = ['Good', 'Moderate', 'Unhealthy', 'Very_Unhealthy'];
  readonly sortOptions = [
    { label: 'Most Recent',  value: 'timestamp,desc' },
    { label: 'CO ↓',         value: 'co,desc' },
    { label: 'CO ↑',         value: 'co,asc' },
    { label: 'Ozone ↓',      value: 'ozone,desc' },
    { label: 'PM2.5 ↓',      value: 'pm2_5,desc' },
  ];

  private refreshSub!: Subscription;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.refreshSub = interval(60000).pipe(startWith(0)).subscribe(() => this.loadAll());
  }

  ngOnDestroy(): void { this.refreshSub?.unsubscribe(); }

  loadAll(): void {
    this.isLoading = true;
    this.hasError = false;
    forkJoin({
      stats: this.http.get<AirStats>('/api/sensors/air/stats').pipe(catchError(() => of(null))),
      page:  this.http.get<AirPage>('/api/sensors/air', { params: this.buildParams() }).pipe(catchError(() => of(null))),
    }).subscribe(({ stats, page }) => {
      if (!stats && !page) this.hasError = true;
      this.stats = stats;
      this.page  = page;
      this.isLoading = false;
      this.lastRefreshed = new Date();
      this.cdr.detectChanges();
    });
  }

  loadPage(): void {
    this.isLoading = true;
    this.http.get<AirPage>('/api/sensors/air', { params: this.buildParams() })
      .pipe(catchError(() => of(null)))
      .subscribe(page => {
        this.page = page;
        this.isLoading = false;
        this.lastRefreshed = new Date();
        this.cdr.detectChanges();
      });
  }

  applyFilters(): void { this.currentPage = 0; this.loadPage(); }
  clearFilters(): void {
    this.filterLocation = ''; this.filterPollution = '';
    this.filterFrom = ''; this.filterTo = ''; this.sortBy = 'timestamp,desc';
    this.currentPage = 0; this.loadPage();
  }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.loadPage(); } }
  nextPage(): void { if (this.currentPage < (this.page?.totalPages ?? 1) - 1) { this.currentPage++; this.loadPage(); } }
  get totalPages(): number { return this.page?.totalPages ?? 0; }

  pollutionStyle(level: string): { background: string; color: string } {
    return POLLUTION_COLORS[level] ?? POLLUTION_COLORS['Good'];
  }

  pollutionLabel(level: string): string {
    return level.replace('_', ' ');
  }

  trackById(_: number, r: AirRecord): string { return r.id; }
  goHome(): void { this.router.navigate(['/home']); }

  private buildParams(): HttpParams {
    let p = new HttpParams().set('page', this.currentPage).set('size', this.pageSize);
    if (this.filterLocation.trim()) p = p.set('location', this.filterLocation.trim());
    if (this.filterPollution)       p = p.set('pollutionLevel', this.filterPollution);
    if (this.filterFrom)            p = p.set('from', this.toIso(this.filterFrom));
    if (this.filterTo)              p = p.set('to', this.toIso(this.filterTo));
    const [field, dir] = this.sortBy.split(',');
    if (field) p = p.set('sort', `${field},${dir}`);
    return p;
  }

  private toIso(v: string): string {
    return v.length === 16 ? `${v}:00` : v.slice(0, 19);
  }
}
