import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import {
  TrafficPage,
  TrafficStats,
  TrafficTrendPoint,
  CongestionSummary,
  TrafficFilterParams,
} from '../models/traffic.model';

const BASE_URL = '/api/sensors/traffic';
const REQUEST_TIMEOUT = 15_000;

@Injectable({ providedIn: 'root' })
export class TrafficService {
  constructor(private http: HttpClient) {}

  getTrafficData(params: TrafficFilterParams): Observable<TrafficPage> {
    let httpParams = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size));

    if (params.location?.trim()) {
      httpParams = httpParams.set('location', params.location.trim());
    }
    if (params.congestionLevel) {
      httpParams = httpParams.set('congestionLevel', params.congestionLevel);
    }
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    if (params.sortField && params.sortDir) {
      httpParams = httpParams.set('sort', `${params.sortField},${params.sortDir}`);
    }

    return this.http.get<TrafficPage>(BASE_URL, { params: httpParams }).pipe(
      timeout(REQUEST_TIMEOUT),
      catchError(this.handleError('traffic data'))
    );
  }

  getStats(): Observable<TrafficStats> {
    return this.http.get<TrafficStats>(`${BASE_URL}/stats`).pipe(
      timeout(REQUEST_TIMEOUT),
      catchError(this.handleError('traffic stats'))
    );
  }

  getTrends(): Observable<TrafficTrendPoint[]> {
    return this.http.get<TrafficTrendPoint[]>(`${BASE_URL}/trends`).pipe(
      timeout(REQUEST_TIMEOUT),
      catchError(this.handleError('trend data'))
    );
  }

  getCongestionSummary(): Observable<CongestionSummary> {
    return this.http.get<CongestionSummary>(`${BASE_URL}/congestion-summary`).pipe(
      timeout(REQUEST_TIMEOUT),
      catchError(this.handleError('congestion summary'))
    );
  }

  private handleError(context: string) {
    return (error: any): Observable<never> => {
      let message = `Failed to load ${context}`;
      if (error.name === 'TimeoutError') {
        message = `Request timed out loading ${context}. Is the sensor service running?`;
      } else if (error.status === 0) {
        message = `Cannot reach the sensor service. Please verify it is running on port 8081.`;
      } else if (error.status) {
        message = `Error ${error.status} loading ${context}`;
      }
      console.error(`[TrafficService] ${message}`, error);
      return throwError(() => ({ message, status: error.status || 0, original: error }));
    };
  }
}
