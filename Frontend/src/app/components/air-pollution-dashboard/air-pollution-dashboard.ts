import { Component } from '@angular/core';

import { SharedDashboard } from '../shared-dashboard/shared-dashboard';
import { AIR_POLLUTION_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-air-pollution-dashboard',
  standalone: true,
  imports: [SharedDashboard],
  templateUrl: './air-pollution-dashboard.html',
  styleUrl: './air-pollution-dashboard.css',
})
export class AirPollutionDashboard {
  config = AIR_POLLUTION_DASHBOARD_CONFIG;
}
