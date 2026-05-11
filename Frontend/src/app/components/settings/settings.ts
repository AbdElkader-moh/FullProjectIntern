import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import {
  ThresholdSetting,
  SensorType,
  AlertType,
  TRAFFIC_METRICS,
  AIR_METRICS,
  LIGHT_METRICS,
  METRIC_CONSTRAINTS,
} from '../../models/threshold.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
})
export class Settings implements OnInit {
  sensorType: SensorType = 'traffic';
  metric: string = 'trafficDensity';
  thresholdValue: number = 0;
  alertType: AlertType = 'above';

  availableMetrics: string[] = TRAFFIC_METRICS;
  currentConstraint = METRIC_CONSTRAINTS['trafficDensity'];

  savedThresholds: ThresholdSetting[] = [];
  successMessage: string = '';
  errorMessage: string = '';
  isSubmitting: boolean = false;

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadThresholds();
  }

  onSensorTypeChange(): void {
    if (this.sensorType === 'traffic') this.availableMetrics = TRAFFIC_METRICS;
    else if (this.sensorType === 'air') this.availableMetrics = AIR_METRICS;
    else this.availableMetrics = LIGHT_METRICS;

    this.metric = this.availableMetrics[0];
    this.onMetricChange();
  }

  onMetricChange(): void {
    this.currentConstraint = METRIC_CONSTRAINTS[this.metric] ?? {min: 0, max: 100};
    this.thresholdValue = this.currentConstraint.min;
  }

  loadThresholds(): void {
    this.notificationService.getThresholds().subscribe({
      next: (data) => (this.savedThresholds = data),
      error: () => (this.errorMessage = 'Failed to load thresholds.'),
    });
  }

  onSubmit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    const { min, max } = this.currentConstraint;
    if (this.thresholdValue < min || this.thresholdValue > max) {
      this.errorMessage = `Value must be between ${min} and ${max}.`;
      return;
    }

    this.isSubmitting = true;
    const payload: ThresholdSetting = {
      sensorType: this.sensorType,
      metric: this.metric,
      thresholdValue: this.thresholdValue,
      alertType: this.alertType,
    };

    this.notificationService.saveThreshold(payload).subscribe({
      next: () => {
        this.successMessage = 'Threshold saved successfully!';
        this.isSubmitting = false;
        this.loadThresholds();
      },
      error: () => {
        this.errorMessage = 'Failed to save threshold. Please try again.';
        this.isSubmitting = false;
      },
    });
  }

  deleteThreshold(metric: string): void {
    this.notificationService.deleteThreshold(metric).subscribe({
      next: () => this.loadThresholds(),
      error: () => (this.errorMessage = 'Failed to delete threshold.'),
    });
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }
}