export interface NotificationItem {
  id: string;
  type: string;
  metric: string;
  value: number;
  thresholdValue: number;
  alertType: string;
  location: string;
  isRead: boolean;
  createdAt: string;
}
