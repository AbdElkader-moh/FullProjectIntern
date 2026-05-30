// ─── Congestion level ─────────────────────────────────────────────────────────
// Values must match the backend enum exactly (case-sensitive).
export type CongestionLevel = 'Low' | 'Moderate' | 'High' | 'Severe';

export const CONGESTION_LEVELS: CongestionLevel[] = ['Low', 'Moderate', 'High', 'Severe'];

export interface CongestionConfig {
  /** Text / dot color */
  color: string;
  /** Badge background */
  bg: string;
  /** Progress bar fill */
  barColor: string;
}

export const CONGESTION_CONFIG: Record<CongestionLevel, CongestionConfig> = {
  Low:      { color: '#34d399', bg: 'rgba(16,185,129,0.15)',  barColor: '#34d399' },
  Moderate: { color: '#fbbf24', bg: 'rgba(245,158,11,0.15)',  barColor: '#fbbf24' },
  High:     { color: '#fb923c', bg: 'rgba(249,115,22,0.15)',  barColor: '#fb923c' },
  Severe:   { color: '#f87171', bg: 'rgba(239,68,68,0.15)',   barColor: '#f87171' },
};

// ─── Table row ────────────────────────────────────────────────────────────────
// Field names match traffic_sensors_data columns (camelCase as serialized by Spring).
export interface TrafficRecord {
  id: string;
  location: string;
  /** ISO-8601 LocalDateTime string, e.g. "2026-05-28T17:44:00" — no timezone suffix. */
  timestamp: string;
  trafficDensity: number;
  avgSpeed: number;
  congestionLevel: CongestionLevel;
}

// ─── Paginated response ───────────────────────────────────────────────────────
// BUG FIX: Spring Boot Page<T> serializes with these EXACT field names.
// Any deviation (e.g. 'data' instead of 'content', 'page' instead of 'number')
// causes page.content === undefined, which makes trafficData = undefined and
// crashes Angular change detection with "Cannot read properties of undefined
// (reading 'length')" — leaving the spinner permanently visible.
export interface TrafficPage {
  /** The records for the current page. */
  content: TrafficRecord[];
  /** Total records across all pages. */
  totalElements: number;
  /** Total number of pages. */
  totalPages: number;
  /**
   * Current page index (0-based).
   * Spring field name is "number" — NOT "page" or "currentPage".
   */
  number: number;
  /** Page size that was requested. */
  size: number;
}

// ─── Stats card response ──────────────────────────────────────────────────────
// GET /api/sensors/traffic/stats
export interface TrafficStats {
  totalRecords: number;
  averageTrafficDensity: number;
  averageSpeed: number;
  highCongestionCount: number;
  severeCongestionCount: number;
}

// ─── Trend data point ─────────────────────────────────────────────────────────
// GET /api/sensors/traffic/trends — array of last 50 readings, newest-first.
// Component reverses the array for chronological chart display.
export interface TrafficTrendPoint {
  timestamp: string;
  trafficDensity: number;
  avgSpeed: number;
}

// ─── Congestion distribution ──────────────────────────────────────────────────
// GET /api/sensors/traffic/congestion-summary — e.g. { "Low": 120, "High": 45, … }
export type CongestionSummary = Record<CongestionLevel, number>;

// ─── Sort options (F#8 — used by traffic-analytics dropdown) ─────────────────
// Each option's `value` is the combined "field,direction" string that the
// analytics component stores in filterSort and binds to the <select> element.
// The service splits or passes this string through to Spring Pageable.
export interface SortOption {
  /** Display label shown in the dropdown */
  label: string;
  /** Combined Spring Pageable sort string, e.g. "trafficDensity,desc" */
  value: string;
}

export const SORT_OPTIONS: SortOption[] = [
  { value: 'timestamp,desc',      label: 'Most Recent'     },
  { value: 'trafficDensity,desc', label: 'Highest Density' },
  { value: 'trafficDensity,asc',  label: 'Lowest Density'  },
  { value: 'avgSpeed,desc',       label: 'Highest Speed'   },
  { value: 'avgSpeed,asc',        label: 'Lowest Speed'    },
];

// ─── Filter / pagination params ───────────────────────────────────────────────
// Spring Pageable requires "sort" as a single "field,direction" string
// (e.g. "timestamp,desc"). The service accepts TWO equivalent input styles
// and always emits the correct combined format to the backend:
//
//   Style A — traffic-dashboard:   { sort: 'timestamp,desc', page, size }
//   Style B — traffic-analytics:   { sortField: 'timestamp', sortDir: 'desc', page, size, ... }
//
// Both styles produce ?sort=timestamp,desc on the wire. Unknown param names such
// as "sortField" and "sortDir" would be silently ignored by Spring if sent raw,
// causing the backend to return page 0 on every call — that is why they must be
// resolved inside the service rather than forwarded directly.
export interface TrafficFilterParams {
  page: number;
  size: number;

  /**
   * Combined Spring Pageable sort string: "field,direction".
   * Used by traffic-dashboard. Examples: "timestamp,desc" | "avgSpeed,asc"
   * If provided, sortField and sortDir are ignored.
   */
  sort?: string;

  /**
   * Separate sort field — used by traffic-analytics (Style B).
   * The service combines this with sortDir before sending to Spring.
   * Ignored when `sort` is already provided.
   */
  sortField?: string;

  /**
   * Sort direction paired with sortField — used by traffic-analytics (Style B).
   * Defaults to 'asc' when sortField is present but sortDir is omitted.
   */
  sortDir?: 'asc' | 'desc';

  /** Partial match — backend performs LIKE %value% */
  location?: string;
  /** Exact match against CongestionLevel enum */
  congestionLevel?: string;
  /** ISO-8601 LocalDateTime, e.g. "2026-05-01T00:00:00" */
  from?: string;
  /** ISO-8601 LocalDateTime, e.g. "2026-05-31T23:59:59" */
  to?: string;
}
