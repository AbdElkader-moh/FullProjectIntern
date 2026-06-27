export interface AlertsConfig {
  title: string;
  icon: string;

  notificationType: string;

  dashboardRoute: string;
  analyticsRoute: string;

  toastTitle: string;
  toastIcon: string;

  allMetricsLabel: string;
  metricOptions: string[];
}

export const TRAFFIC_ALERTS_CONFIG: AlertsConfig = {
  title: 'Traffic Alerts',
  icon: '🚗',

  notificationType: 'Traffic',

  dashboardRoute: '/traffic',
  analyticsRoute: '/traffic-analytics',

  toastTitle: 'Traffic Alert',
  toastIcon: '🚗',

  allMetricsLabel: 'All Traffic Alerts',
  metricOptions: ['Traffic Density', 'Average Speed'],
};

export const AIR_ALERTS_CONFIG: AlertsConfig = {
  title: 'Air Pollution Alerts',
  icon: '💨',

  notificationType: 'Air',

  dashboardRoute: '/air',
  analyticsRoute: '/air-analytics',

  toastTitle: 'Air Pollution Alert',
  toastIcon: '💨',

  allMetricsLabel: 'All Air Alerts',
  metricOptions: ['Carbon Monoxide', 'Ozone'],
};

export const LIGHT_ALERTS_CONFIG: AlertsConfig = {
  title: 'Street Light Alerts',
  icon: '💡',

  notificationType: 'Light',

  dashboardRoute: '/lights',
  analyticsRoute: '/lights-analytics',

  toastTitle: 'Street Light Alert',
  toastIcon: '💡',

  allMetricsLabel: 'All Light Alerts',
  metricOptions: ['Brightness Level', 'Power Consumption'],
};
