import { DashboardConfig } from './dashboard.model';

export const AIR_POLLUTION_DASHBOARD_CONFIG: DashboardConfig = {
  title: 'Air Pollution Monitoring',
  icon: '💨',
  description: 'Monitor air pollution readings such as CO, Ozone, PM2.5, PM10, NO2 and SO2.',

  baseEndpoint: '/api/sensors/air',

  columns: [
    { key: 'location', label: 'Location', type: 'text' },
    { key: 'co', label: 'CO', type: 'number' },
    { key: 'ozone', label: 'Ozone', type: 'number' },
    { key: 'pm2_5', label: 'PM2.5', type: 'number' },
    { key: 'pm10', label: 'PM10', type: 'number' },
    { key: 'no2', label: 'NO2', type: 'number' },
    { key: 'so2', label: 'SO2', type: 'number' },
    { key: 'pollutionLevel', label: 'Pollution Level', type: 'badge' },
    { key: 'timestamp', label: 'Timestamp', type: 'date' },
  ],

  filters: [
    { key: 'location', label: 'Location', type: 'text' },
    {
      key: 'pollutionLevel',
      label: 'Pollution Level',
      type: 'select',
      options: ['Good', 'Moderate', 'Unhealthy', 'Very_Unhealthy'],
    },
    { key: 'from', label: 'From Date', type: 'date' },
    { key: 'to', label: 'To Date', type: 'date' },
  ],

  statsCards: [
  { key: 'totalRecords', label: 'Total Records' },
  { key: 'averageCo', label: 'Average CO' },
  { key: 'averageOzone', label: 'Average Ozone' },
  { key: 'totalAlerts', label: 'Total Alerts' },
  { key: 'pollutionLevelBreakdown.Good', label: 'Good Readings' },
  { key: 'pollutionLevelBreakdown.Unhealthy', label: 'Unhealthy Readings' },
],

  chartFields: [
    { key: 'co', label: 'CO' },
    { key: 'ozone', label: 'Ozone' },
    { key: 'pm2_5', label: 'PM2.5' },
  ],

  defaultSort: 'timestamp,desc',
  pageSize: 20,

  badgeField: 'pollutionLevel',
  badgeStyles: {
    Good: {
      background: 'rgba(52,211,153,0.15)',
      color: '#34d399',
    },
    Moderate: {
      background: 'rgba(251,191,36,0.15)',
      color: '#fbbf24',
    },
    Unhealthy: {
      background: 'rgba(245,158,11,0.15)',
      color: '#f59e0b',
    },
    Very_Unhealthy: {
      background: 'rgba(239,68,68,0.15)',
      color: '#ef4444',
    },
  },
};

export const STREET_LIGHT_DASHBOARD_CONFIG: DashboardConfig = {
  title: 'Street Light Management',
  icon: '💡',
  description: 'Monitor street light brightness, power consumption, and ON/OFF status.',

  baseEndpoint: '/api/sensors/light',

  columns: [
    { key: 'location', label: 'Location', type: 'text' },
    { key: 'brightnessLevel', label: 'Brightness Level', type: 'number', suffix: '%' },
    { key: 'powerConsumption', label: 'Power Consumption', type: 'number', suffix: ' kWh' },
    { key: 'status', label: 'Status', type: 'badge' },
    { key: 'timestamp', label: 'Timestamp', type: 'date' },
  ],

  filters: [
    { key: 'location', label: 'Location', type: 'text' },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ON', 'OFF'],
    },
    { key: 'from', label: 'From Date', type: 'date' },
    { key: 'to', label: 'To Date', type: 'date' },
  ],

 statsCards: [
  { key: 'totalRecords', label: 'Total Records' },
  { key: 'averageBrightness', label: 'Average Brightness', suffix: '%' },
  { key: 'averagePowerConsumption', label: 'Average Power', suffix: ' kWh' },
  { key: 'statusBreakdown.ON', label: 'Lights ON' },
  { key: 'statusBreakdown.OFF', label: 'Lights OFF' },
],

  chartFields: [
    { key: 'brightnessLevel', label: 'Brightness Level', suffix: '%' },
    { key: 'powerConsumption', label: 'Power Consumption' },
  ],

  defaultSort: 'timestamp,desc',
  pageSize: 20,

  badgeField: 'status',
  badgeStyles: {
    ON: {
      background: 'rgba(167,139,250,0.15)',
      color: '#a78bfa',
    },
    OFF: {
      background: 'rgba(107,114,128,0.15)',
      color: '#9ca3af',
    },
  },
};
export const TRAFFIC_DASHBOARD_CONFIG: DashboardConfig = {
  title: 'Traffic Monitoring Dashboard',
  icon: '🚗',
  description: 'Real-time traffic sensor data and analytics.',

  baseEndpoint: '/api/sensors/traffic',

  columns: [
    { key: 'location', label: 'Location', type: 'text' },
    { key: 'timestamp', label: 'Timestamp', type: 'date' },
    { key: 'trafficDensity', label: 'Traffic Density', type: 'number', suffix: ' veh/hr' },
    { key: 'avgSpeed', label: 'Average Speed', type: 'number', suffix: ' km/h' },
    { key: 'congestionLevel', label: 'Congestion Level', type: 'badge' },
  ],

  filters: [
    { key: 'location', label: 'Location', type: 'text' },
    {
      key: 'congestionLevel',
      label: 'Congestion Level',
      type: 'select',
      options: ['Low', 'Moderate', 'High', 'Severe'],
    },
    { key: 'from', label: 'From Date', type: 'date' },
    { key: 'to', label: 'To Date', type: 'date' },
  ],

  statsCards: [
    { key: 'totalRecords', label: 'Total Records' },
    { key: 'averageTrafficDensity', label: 'Average Density', suffix: 'vehicles/hr' },
    { key: 'averageSpeed', label: 'Average Speed', suffix: 'km/h' },
    { key: 'highCongestionCount', label: 'High Congestion' },
    { key: 'severeCongestionCount', label: 'Severe Congestion' },
  ],

  chartFields: [
    { key: 'trafficDensity', label: 'Traffic Density', suffix: 'vehicles/hr' },
    { key: 'avgSpeed', label: 'Average Speed', suffix: 'km/h' },
  ],

  defaultSort: 'timestamp,desc',
  pageSize: 20,

  badgeField: 'congestionLevel',
  badgeStyles: {
    Low: {
      background: 'rgba(52,211,153,0.15)',
      color: '#34d399',
    },
    Moderate: {
      background: 'rgba(251,191,36,0.15)',
      color: '#fbbf24',
    },
    High: {
      background: 'rgba(245,158,11,0.15)',
      color: '#f59e0b',
    },
    Severe: {
      background: 'rgba(239,68,68,0.15)',
      color: '#ef4444',
    },
  },
};
