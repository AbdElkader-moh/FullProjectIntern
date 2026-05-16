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
  'Traffic Density': { min: 0, max: 500 },
  'Average Speed': { min: 0, max: 120 },
  'Carbon Monoxide': { min: 0, max: 50 },
  'Ozone': { min: 0, max: 300 },
  'Brightness Level': { min: 0, max: 100 },
  'Power Consumption': { min: 0, max: 5000 },
};