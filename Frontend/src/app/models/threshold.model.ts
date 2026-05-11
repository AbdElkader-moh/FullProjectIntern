export type SensorType = 'traffic' | 'air' | 'light';
export type AlertType = 'above' | 'below';

export interface ThresholdSetting {
    sensorType: SensorType;
    metric: string;
    thresholdValue: number;
    alertType: AlertType;
}

export const TRAFFIC_METRICS = ['Traffic Density', 'Average Speed'];
export const AIR_METRICS = ['Carbon Monoxide', 'Ozone'];
export const LIGHT_METRICS = ['Brightness Level', 'Power Consumption'];

export const METRIC_CONSTRAINTS: Record<string, { min: number; max: number }> = {
  trafficDensity: { min: 0, max: 500 },
  avgSpeed: { min: 0, max: 120 },
  co: { min: 0, max: 50 },
  ozone: { min: 0, max: 300 },
  brightnessLevel: { min: 0, max: 100 },
  powerConsumption: { min: 0, max: 5000 },
};