import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationItem } from '../models/notification.model';
import { BehaviorSubject } from 'rxjs';
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly apiUrl = '/api/users/notifications';
  private readonly httpOptions = { withCredentials: true };
  private _unreadCount = new BehaviorSubject<number>(0);
  unreadCount$ = this._unreadCount.asObservable();
  constructor(private http: HttpClient) {}

  getNotifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.apiUrl, this.httpOptions);
  }

  markAsRead(id: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/read`, {}, this.httpOptions);
  }

  markAllAsRead(): Observable<any> {
    return this.http.put(`${this.apiUrl}/read-all`, {}, this.httpOptions);
  }

  deleteNotification(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, this.httpOptions);
  }
 setUnreadCount(count: number): void {
  this._unreadCount.next(count);
}

incrementUnread(): void {
  this._unreadCount.next(this._unreadCount.value + 1);
}
}
