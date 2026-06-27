export type DashboardColumnType = 'text' | 'number' | 'date' | 'badge';

export interface DashboardColumn {
  key: string;
  label: string;
  type?: DashboardColumnType;
  suffix?: string;
}

export type DashboardFilterType = 'text' | 'select' | 'date';

export interface DashboardFilter {
  key: string;
  label: string;
  type: DashboardFilterType;
  options?: string[];
}

export interface DashboardStatCard {
  key: string;
  label: string;
  suffix?: string;
}

export interface DashboardChartField {
  key: string;
  label: string;
  suffix?: string;
}

export interface DashboardBadgeStyle {
  background: string;
  color: string;
}

export interface DashboardConfig {
  title: string;
  icon: string;
  description: string;

  baseEndpoint: string;

  columns: DashboardColumn[];
  filters: DashboardFilter[];
  statsCards: DashboardStatCard[];
  chartFields: DashboardChartField[];

  defaultSort: string;
  pageSize: number;

  badgeField?: string;
  badgeStyles?: Record<string, DashboardBadgeStyle>;
}

export interface DashboardPageResponse<T = any> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DashboardFilterParams {
  page: number;
  size: number;
  sort: string;
  [key: string]: string | number | undefined;
}
