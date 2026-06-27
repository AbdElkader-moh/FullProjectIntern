import { Component } from '@angular/core';

import { SharedAnalytics } from '../shared-analytics/shared-analytics';
import { STREET_LIGHT_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-street-light-analytics',
  standalone: true,
  imports: [SharedAnalytics],
  templateUrl: './street-light-analytics.html',
  styleUrl: './street-light-analytics.css',
})
export class StreetLightAnalytics {
  config = STREET_LIGHT_DASHBOARD_CONFIG;
}
