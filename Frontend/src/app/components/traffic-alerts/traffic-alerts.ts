import { Component } from '@angular/core';

import { SharedAlerts } from '../shared-alerts/shared-alerts';
import { TRAFFIC_ALERTS_CONFIG } from '../../models/alerts-config.model';

@Component({
  selector: 'app-traffic-alerts',
  standalone: true,
  imports: [SharedAlerts],
  templateUrl: './traffic-alerts.html',
  styleUrl: './traffic-alerts.css',
})
export class TrafficAlerts {
  config = TRAFFIC_ALERTS_CONFIG;
}
