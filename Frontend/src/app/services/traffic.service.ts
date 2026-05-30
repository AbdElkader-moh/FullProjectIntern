import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  TrafficFilterParams,
  TrafficPage,
  TrafficStats,
  TrafficTrendPoint,
  CongestionSummary,
} from '../models/traffic.model';

/**
 * Base path for all sensor-service traffic endpoints.
 *
 * The Angular dev proxy must forward /api/sensors → http://localhost:8081.
 * Add the following entry to proxy.conf.json:
 *
 *   "/api/sensors": {
 *     "target": "http://localhost:8081",
 *     "secure": false,
 *     "changeOrigin": true
 *   }
 *
 * Without this entry every request hits port 8080 (user-service) and returns 404,
 * which is caught by loadTable()'s error handler — so tableLoading IS reset to
 * false — but if the model mapping bug (TrafficPage field names) is also present,
 * both issues compound and the table stays stuck in loading.
 */
const TRAFFIC_BASE = '/api/sensors/traffic';

@Injectable({ providedIn: 'root' })
export class TrafficService {

  constructor(private http: HttpClient) {}

  /**
   * GET /api/sensors/traffic
   *
   * Returns a Spring Page<TrafficData> response.
   *
   * BUG FIX — param building:
   *   - "page"  must be 0-based integer string (Spring Pageable name)
   *   - "size"  must be an integer string
   *   - "sort"  must be a single "field,direction" string, e.g. "timestamp,desc"
   *             Spring silently ignores unknown param names like "sortField" or
   *             "sortDir". Sending them as separate params caused the backend to
   *             always use default page (0) and ordering — making all pagination
   *             clicks return the same first-page data.
   *
   * Optional filter params (all skipped when undefined/null/empty):
   *   - location       : partial string match (backend: LIKE %value%)
   *   - congestionLevel: exact enum value (Low | Moderate | High | Severe)
   *   - from / to      : ISO-8601 LocalDateTime e.g. "2026-05-01T00:00:00"
   */
  getTrafficData(filters: TrafficFilterParams): Observable<TrafficPage> {
    let params = new HttpParams()
      .set('page', String(filters.page))
      .set('size', String(filters.size));

    /**
     * Resolve sort parameter — handles two caller styles:
     *
     *   Style A (traffic-dashboard):  filters.sort = 'timestamp,desc'
     *   Style B (traffic-analytics):  filters.sortField = 'timestamp', filters.sortDir = 'desc'
     *
     * Both are normalised into a single "field,direction" string before being
     * sent to the backend. Spring Pageable silently ignores unknown param names
     * (e.g. "sortField" or "sortDir" sent as raw keys), so this normalisation
     * step is mandatory — without it the backend always returns page 0 with
     * default ordering, making every pagination and sort click a no-op.
     */
    const sortParam: string | undefined =
      filters.sort                              // Style A — already combined
      ?? (filters.sortField
          ? `${filters.sortField},${filters.sortDir ?? 'asc'}`  // Style B — combine
          : undefined);

    if (sortParam)               { params = params.set('sort', sortParam); }
    if (filters.location)        { params = params.set('location', filters.location); }
    if (filters.congestionLevel) { params = params.set('congestionLevel', filters.congestionLevel); }
    if (filters.from)            { params = params.set('from', filters.from); }
    if (filters.to)              { params = params.set('to', filters.to); }

    return this.http.get<TrafficPage>(TRAFFIC_BASE, {
      params,
      withCredentials: true,
    });
  }

  /**
   * GET /api/sensors/traffic/stats
   * Returns: { totalRecords, averageTrafficDensity, averageSpeed,
   *             highCongestionCount, severeCongestionCount }
   */
  getStats(): Observable<TrafficStats> {
    return this.http.get<TrafficStats>(`${TRAFFIC_BASE}/stats`, {
      withCredentials: true,
    });
  }

  /**
   * GET /api/sensors/traffic/trends
   * Returns: array of last 50 readings, newest-first.
   * Component reverses the array for chronological left→right chart display.
   */
  getTrends(): Observable<TrafficTrendPoint[]> {
    return this.http.get<TrafficTrendPoint[]>(`${TRAFFIC_BASE}/trends`, {
      withCredentials: true,
    });
  }

  /**
   * GET /api/sensors/traffic/congestion-summary
   * Returns: { "Low": 120, "Moderate": 85, "High": 45, "Severe": 12 }
   */
  getCongestionSummary(): Observable<CongestionSummary> {
    return this.http.get<CongestionSummary>(`${TRAFFIC_BASE}/congestion-summary`, {
      withCredentials: true,
    });
  }
}
