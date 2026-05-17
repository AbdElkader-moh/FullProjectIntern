import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import {
  SensorType,
  AlertType,
  TRAFFIC_METRICS,
  AIR_METRICS,
  LIGHT_METRICS,
  METRIC_CONSTRAINTS,
} from '../../models/threshold.model';

// Matches the backend SettingsDTO exactly
export interface SettingsDTO {
  id?: string;
  type: string;        // "Traffic" | "Air" | "Light"
  metric: string;
  thresholdValue: number;
  alertType: string;   // "above" | "below"
}

// Maps frontend SensorType ('traffic') → backend type string ('Traffic')
const SENSOR_TYPE_MAP: Record<SensorType, string> = {
  traffic: 'Traffic',
  air: 'Air',
  light: 'Light',
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
})
export class Settings implements OnInit {
  // ── Form state ────────────────────────────────────────────────
  sensorType: SensorType = 'traffic';
  metric: string = 'Traffic Density';
  thresholdValue: number = 0;
  alertType: AlertType = 'above';

  availableMetrics: string[] = TRAFFIC_METRICS;
  currentConstraint = METRIC_CONSTRAINTS['Traffic Density'];

  // ── List state ────────────────────────────────────────────────
  savedThresholds: SettingsDTO[] = [];

  // ── Inline edit state ─────────────────────────────────────────
  // When non-null, that row is being edited in place
  editingId: string | null = null;
  editValue: number = 0;
  editAlertType: string = 'above';

  // ── UI feedback ───────────────────────────────────────────────
  successMessage: string = '';
  errorMessage: string = '';
  isSubmitting: boolean = false;
  deletingId: string | null = null;   // tracks which row's delete is in flight

  private readonly apiUrl = '/api/users/settings';
  private readonly httpOptions = { withCredentials: true };

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadThresholds();
  }

  // ── Form helpers ──────────────────────────────────────────────

  onSensorTypeChange(): void {
  if (this.sensorType === 'traffic') this.availableMetrics = TRAFFIC_METRICS;
  else if (this.sensorType === 'air') this.availableMetrics = AIR_METRICS;
  else this.availableMetrics = LIGHT_METRICS;

  this.metric = this.availableMetrics[0];

  setTimeout(() => {        // ← wait for Angular to render new <option>s
    this.onMetricChange();
    this.cdr.detectChanges();
  });
}

  onMetricChange(): void {
  console.log('metric:', this.metric);
  console.log('constraint:', METRIC_CONSTRAINTS[this.metric]);
  this.currentConstraint = METRIC_CONSTRAINTS[this.metric] ?? { min: 0, max: 100 };
  this.thresholdValue = this.currentConstraint.min;
  }

  // ── Load ──────────────────────────────────────────────────────

  loadThresholds(): void {
    this.http.get<SettingsDTO[]>(this.apiUrl, this.httpOptions).subscribe({
      next: (data) => {
        this.savedThresholds = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load thresholds.';
        this.cdr.detectChanges();
      },
    });
  }

  // ── Create / upsert ───────────────────────────────────────────

onSubmit(): void {
  this.successMessage = '';
  this.errorMessage = '';
  

  const { min, max } = this.currentConstraint;
  if (this.thresholdValue < min || this.thresholdValue > max) {
    this.errorMessage = `Value must be between ${min} and ${max}.`;
    this.cdr.detectChanges();
    return;
  }

  this.isSubmitting = true;

  const payload: SettingsDTO = {
    type: SENSOR_TYPE_MAP[this.sensorType],
    metric: this.metric,
    thresholdValue: this.thresholdValue,
    alertType: this.alertType,
  };

  console.log('payload:', JSON.stringify(payload));

  this.http
    .post<SettingsDTO>(this.apiUrl, payload, this.httpOptions)
    .pipe(finalize(() => { this.isSubmitting = false; this.cdr.detectChanges(); }))
    .subscribe({
      next: () => {
        this.successMessage = 'Threshold saved successfully!';
        this.loadThresholds();
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to save threshold. Please try again.';
        this.cdr.detectChanges();
      },
    });
}

  // ── Inline edit ───────────────────────────────────────────────

  startEdit(setting: SettingsDTO): void {
    this.editingId = setting.id ?? null;
    this.editValue = setting.thresholdValue;
    this.editAlertType = setting.alertType;
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  cancelEdit(): void {
    this.editingId = null;
    this.cdr.detectChanges();
  }

  saveEdit(setting: SettingsDTO): void {
    if (!setting.id) return;

    const constraint = METRIC_CONSTRAINTS[setting.metric] ?? { min: 0, max: 100 };
    if (this.editValue < constraint.min || this.editValue > constraint.max) {
      this.errorMessage = `Value must be between ${constraint.min} and ${constraint.max}.`;
      this.cdr.detectChanges();
      return;
    }

    // PUT /settings/{id} — targeted single-record update
    this.http
      .put<SettingsDTO>(
        `${this.apiUrl}/${setting.id}`,
        { thresholdValue: this.editValue, alertType: this.editAlertType },
        this.httpOptions
      )
      .pipe(
        finalize(() => {
          this.editingId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.successMessage = 'Threshold updated successfully!';
          this.loadThresholds();
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to update threshold.';
          this.cdr.detectChanges();
        },
      });
  }

  // ── Delete ────────────────────────────────────────────────────

  deleteThreshold(setting: SettingsDTO): void {
    if (!setting.id) {
      this.errorMessage = 'Cannot delete: setting has no ID.';
      this.cdr.detectChanges();
      return;
    }

    this.deletingId = setting.id;

    this.http
      .delete(`${this.apiUrl}/${setting.id}`, this.httpOptions)
      .pipe(
        finalize(() => {
          this.deletingId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.loadThresholds();
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to delete threshold.';
          this.cdr.detectChanges();
        },
      });
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }
}
