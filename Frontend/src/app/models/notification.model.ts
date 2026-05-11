export interface Notification {
  id: string;
  userId: number;
  type: string;
  metric: string;
  value: number;
  thresholdValue: number;
  alertType: 'above' | 'below';
  location: string;
  isRead: boolean;
  createdAt: string;
}