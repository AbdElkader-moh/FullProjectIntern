import { Component } from '@angular/core';

import { SharedAlerts } from '../shared-alerts/shared-alerts';
import { AIR_ALERTS_CONFIG } from '../../models/alerts-config.model';

@Component({
  selector: 'app-air-pollution-alerts',
  standalone: true,
  imports: [SharedAlerts],
  templateUrl: './air-pollution-alerts.html',
  styleUrl: './air-pollution-alerts.css',
})
export class AirPollutionAlerts {
  config = AIR_ALERTS_CONFIG;
}
