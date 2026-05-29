// Sprint 3 — Feature #7 Traffic Models
// Maps to sensor-service :8081 DTOs

export type CongestionLevel = 'Low' | 'Moderate' | 'High' | 'Severe';

/** Maps to TrafficData entity returned by GET /api/sensors/traffic */
export interface TrafficRecord {
  id: string;
  location: string;
  timestamp: string;          // ISO-8601 from backend (LocalDateTime serialized)
  trafficDensity: number;     // 0–500 integer
  avgSpeed: number;           // 0.0–120.0 float
  congestionLevel: CongestionLevel;
}

/** Spring Page<TrafficData> wrapper */
export interface TrafficPage {
  content: TrafficRecord[];
  totalElements: number;
  totalPages: number;
  number: number;             // 0-based current page index
  size: number;
}

/** Maps to TrafficStatsDto from GET /api/sensors/traffic/stats */
export interface TrafficStats {
  totalRecords: number;
  averageTrafficDensity: number;
  averageSpeed: number;
  highCongestionCount: number;
  severeCongestionCount: number;
}

/** Maps to TrafficTrendDto from GET /api/sensors/traffic/trends */
export interface TrafficTrendPoint {
  timestamp: string;
  trafficDensity: number;
  avgSpeed: number;
}

/** Maps to Map<String, Long> from GET /api/sensors/traffic/congestion-summary */
export interface CongestionSummary {
  Low: number;
  Moderate: number;
  High: number;
  Severe: number;
}

/** Query parameters for GET /api/sensors/traffic */
export interface TrafficFilterParams {
  location?: string;
  congestionLevel?: string;
  from?: string;              // ISO-8601: 2026-05-01T00:00:00
  to?: string;                // ISO-8601: 2026-05-31T23:59:59
  sortField?: string;
  sortDir?: 'asc' | 'desc';
  page: number;               // 0-based
  size: number;
}

/** WebSocket alert payload from /topic/alerts/{userId} */
export interface TrafficAlert {
  type: string;               // e.g. "Traffic"
  metric: string;             // e.g. "Traffic Density"
  value: number;
  thresholdValue: number;
  alertType: 'above' | 'below';
  location: string;
}

/** Single source of truth for congestion level display config */
export const CONGESTION_CONFIG: Record<CongestionLevel, { color: string; bg: string; barColor: string; label: string }> = {
  Low:      { color: '#34d399', bg: 'rgba(16, 185, 129, 0.15)', barColor: '#10b981', label: 'Low' },
  Moderate: { color: '#fbbf24', bg: 'rgba(245, 158, 11, 0.15)', barColor: '#f59e0b', label: 'Moderate' },
  High:     { color: '#fb923c', bg: 'rgba(249, 115, 22, 0.15)', barColor: '#f97316', label: 'High' },
  Severe:   { color: '#ffffff', bg: 'rgba(220, 38, 38, 0.85)',  barColor: '#ef4444', label: 'Severe' },
};

/** Sort options for filter dropdown */
export const SORT_OPTIONS: { label: string; value: string }[] = [
  { label: 'Most Recent',                 value: 'timestamp,desc' },
  { label: 'Oldest First',                value: 'timestamp,asc' },
  { label: 'Traffic Density (High→Low)',   value: 'trafficDensity,desc' },
  { label: 'Traffic Density (Low→High)',   value: 'trafficDensity,asc' },
  { label: 'Avg Speed (High→Low)',         value: 'avgSpeed,desc' },
  { label: 'Avg Speed (Low→High)',         value: 'avgSpeed,asc' },
];

/** Available congestion levels for filter dropdown */
export const CONGESTION_LEVELS: CongestionLevel[] = ['Low', 'Moderate', 'High', 'Severe'];
