import { Component } from '@angular/core';

import { SharedAnalytics } from '../shared-analytics/shared-analytics';
import { TRAFFIC_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-traffic-analytics',
  standalone: true,
  imports: [SharedAnalytics],
  templateUrl: './traffic-analytics.html',
  styleUrl: './traffic-analytics.css',
})
export class TrafficAnalytics {
  config = TRAFFIC_DASHBOARD_CONFIG;
}
