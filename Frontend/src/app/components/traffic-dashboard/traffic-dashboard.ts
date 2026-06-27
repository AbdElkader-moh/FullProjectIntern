import { Component } from '@angular/core';

import { SharedDashboard } from '../shared-dashboard/shared-dashboard';
import { TRAFFIC_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-traffic-dashboard',
  standalone: true,
  imports: [SharedDashboard],
  templateUrl: './traffic-dashboard.html',
  styleUrl: './traffic-dashboard.css',
})
export class TrafficDashboard {
  config = TRAFFIC_DASHBOARD_CONFIG;
}

