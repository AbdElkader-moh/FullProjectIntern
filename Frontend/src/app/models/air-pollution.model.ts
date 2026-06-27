export type PollutionLevel = 'Good' | 'Moderate' | 'Unhealthy' | 'Very_Unhealthy';

export const POLLUTION_LEVELS: PollutionLevel[] = [
  'Good',
  'Moderate',
  'Unhealthy',
  'Very_Unhealthy'
];

export interface PollutionLevelStyle {
  background: string;
  color: string;
}

export const POLLUTION_LEVEL_STYLES: Record<PollutionLevel, PollutionLevelStyle> = {
  Good: {
    background: 'rgba(52,211,153,0.15)',
    color: '#34d399'
  },
  Moderate: {
    background: 'rgba(251,191,36,0.15)',
    color: '#fbbf24'
  },
  Unhealthy: {
    background: 'rgba(245,158,11,0.15)',
    color: '#f59e0b'
  },
  Very_Unhealthy: {
    background: 'rgba(239,68,68,0.15)',
    color: '#ef4444'
  }
};

export interface AirPollutionRecord {
  id: string;
  location: string;
  timestamp: string;
  co: number;
  ozone: number;
  pm2_5: number;
  pm10: number;
  no2: number;
  so2: number;
  pollutionLevel: PollutionLevel;
}

export interface AirPollutionPage {
  content: AirPollutionRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AirPollutionStats {
  totalRecords: number;
  averageCo: number;
  averageOzone: number;
  averagePm25: number;
  goodCount: number;
  unhealthyCount: number;
}

export interface AirPollutionTrendPoint {
  timestamp: string;
  co: number;
  ozone: number;
  pm2_5: number;
}

export interface AirPollutionFilterParams {
  page: number;
  size: number;
  sort?: string;
  location?: string;
  pollutionLevel?: string;
  from?: string;
  to?: string;
}
