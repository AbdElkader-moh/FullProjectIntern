import { Component } from '@angular/core';

import { SharedDashboard } from '../shared-dashboard/shared-dashboard';
import { STREET_LIGHT_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-street-light-dashboard',
  standalone: true,
  imports: [SharedDashboard],
  templateUrl: './street-light-dashboard.html',
  styleUrl: './street-light-dashboard.css',
})
export class StreetLightDashboard {
  config = STREET_LIGHT_DASHBOARD_CONFIG;
}
