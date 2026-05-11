import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from '../models/notification.model';
import { ThresholdSetting } from '../models/threshold.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.baseUrl}/notifications`);
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/notifications/read-all`, {});
  }

  saveThreshold(setting: ThresholdSetting): Observable<any> {
    return this.http.post(`${this.baseUrl}/settings/threshold`, setting);
  }

  getThresholds(): Observable<ThresholdSetting[]> {
    return this.http.get<ThresholdSetting[]>(`${this.baseUrl}/settings/threshold`);
  }

  deleteThreshold(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/settings/threshold/${id}`);
  }
}