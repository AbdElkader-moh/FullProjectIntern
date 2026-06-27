import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  DashboardFilterParams,
  DashboardPageResponse,
} from '../models/dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class SensorDashboardService {
  constructor(private http: HttpClient) {}

  getPage<T>(
    baseEndpoint: string,
    filters: DashboardFilterParams
  ): Observable<DashboardPageResponse<T>> {
    let params = new HttpParams()
      .set('page', String(filters.page))
      .set('size', String(filters.size))
      .set('sort', filters.sort);

    Object.entries(filters).forEach(([key, value]) => {
      if (
        key !== 'page' &&
        key !== 'size' &&
        key !== 'sort' &&
        value !== undefined &&
        value !== null &&
        value !== ''
      ) {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<DashboardPageResponse<T>>(baseEndpoint, {
      params,
      withCredentials: true,
    });
  }

  getStats<T>(baseEndpoint: string): Observable<T> {
    return this.http.get<T>(`${baseEndpoint}/stats`, {
      withCredentials: true,
    });
  }

  getTrends<T>(baseEndpoint: string): Observable<T[]> {
    return this.http.get<T[]>(`${baseEndpoint}/trends`, {
      withCredentials: true,
    });
  }
}
