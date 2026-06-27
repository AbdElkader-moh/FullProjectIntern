import { Component } from '@angular/core';

import { SharedAlerts } from '../shared-alerts/shared-alerts';
import { LIGHT_ALERTS_CONFIG } from '../../models/alerts-config.model';

@Component({
  selector: 'app-street-light-alerts',
  standalone: true,
  imports: [SharedAlerts],
  templateUrl: './street-light-alerts.html',
  styleUrl: './street-light-alerts.css',
})
export class StreetLightAlerts {
  config = LIGHT_ALERTS_CONFIG;
}
