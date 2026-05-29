import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Subscription, interval, forkJoin, of } from 'rxjs';
import { startWith, catchError } from 'rxjs/operators';

interface LightRecord {
  id: string;
  location: string;
  timestamp: string;
  brightnessLevel: number;
  powerConsumption: number;
  status: 'ON' | 'OFF';
}

interface LightPage {
  content: LightRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface LightStats {
  totalRecords: number;
  averageBrightness: number;
  averagePower: number;
  onCount: number;
  offCount: number;
}

@Component({
  selector: 'app-street-light-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './street-light-dashboard.html',
  styleUrl: './street-light-dashboard.css',
})
export class StreetLightDashboard implements OnInit, OnDestroy {
  stats: LightStats | null = null;
  page: LightPage | null = null;
  isLoading = true;
  hasError = false;
  lastRefreshed: Date | null = null;
  currentPage = 0;
  readonly pageSize = 20;

  filterLocation = '';
  filterStatus = '';
  filterFrom = '';
  filterTo = '';
  sortBy = 'timestamp,desc';

  readonly statusOptions = ['ON', 'OFF'];
  readonly sortOptions = [
    { label: 'Most Recent',       value: 'timestamp,desc' },
    { label: 'Brightness ↓',      value: 'brightnessLevel,desc' },
    { label: 'Brightness ↑',      value: 'brightnessLevel,asc' },
    { label: 'Power Usage ↓',     value: 'powerConsumption,desc' },
    { label: 'Power Usage ↑',     value: 'powerConsumption,asc' },
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
      stats: this.http.get<LightStats>('/api/sensors/light/stats').pipe(catchError(() => of(null))),
      page:  this.http.get<LightPage>('/api/sensors/light', { params: this.buildParams() }).pipe(catchError(() => of(null))),
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
    this.http.get<LightPage>('/api/sensors/light', { params: this.buildParams() })
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
    this.filterLocation = ''; this.filterStatus = '';
    this.filterFrom = ''; this.filterTo = ''; this.sortBy = 'timestamp,desc';
    this.currentPage = 0; this.loadPage();
  }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.loadPage(); } }
  nextPage(): void { if (this.currentPage < (this.page?.totalPages ?? 1) - 1) { this.currentPage++; this.loadPage(); } }
  get totalPages(): number { return this.page?.totalPages ?? 0; }

  statusStyle(status: string): { background: string; color: string } {
    return status === 'ON'
      ? { background: 'rgba(167,139,250,0.15)', color: '#a78bfa' }
      : { background: 'rgba(107,114,128,0.15)', color: '#9ca3af' };
  }

  trackById(_: number, r: LightRecord): string { return r.id; }
  goHome(): void { this.router.navigate(['/home']); }

  private buildParams(): HttpParams {
    let p = new HttpParams().set('page', this.currentPage).set('size', this.pageSize);
    if (this.filterLocation.trim()) p = p.set('location', this.filterLocation.trim());
    if (this.filterStatus)          p = p.set('status', this.filterStatus);
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
