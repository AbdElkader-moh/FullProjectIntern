import { Component } from '@angular/core';

import { SharedAnalytics } from '../shared-analytics/shared-analytics';
import { AIR_POLLUTION_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-air-pollution-analytics',
  standalone: true,
  imports: [SharedAnalytics],
  templateUrl: './air-pollution-analytics.html',
  styleUrl: './air-pollution-analytics.css',
})
export class AirPollutionAnalytics {
  config = AIR_POLLUTION_DASHBOARD_CONFIG;
}
