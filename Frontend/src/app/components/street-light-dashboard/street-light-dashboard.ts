import { Component } from '@angular/core';

import { SharedDashboard } from '../shared-dashboard/shared-dashboard';
import { STREET_LIGHT_DASHBOARD_CONFIG } from '../../models/dashboard-configs';

@Component({
  selector: 'app-street-light-dashboard', //da html tag name for this component
  standalone: true,  // instead of putting imports in app.module.ts, we can put them here w te2sar t import ay haga (it can manage its own imports)
  imports: [SharedDashboard], // this allows html to use <app-shared-dashboard></app-shared-dashboard>
  templateUrl: './street-light-dashboard.html', //it tells where html file is located
  styleUrl: './street-light-dashboard.css', //it tells where css file is located
})
export class StreetLightDashboard {  //defines the class for this component
  config = STREET_LIGHT_DASHBOARD_CONFIG; //h store streetlight dashboard config in a variable called config so that it can be used in the html file
}
