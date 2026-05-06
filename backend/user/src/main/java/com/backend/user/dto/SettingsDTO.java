package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alert threshold setting")
public class SettingsDTO {

    @Schema(description = "Setting ID (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Sensor type", example = "Traffic", allowableValues = {"Traffic", "Air", "Light"})
    private String type;

    @Schema(description = "Metric name", example = "trafficDensity")
    private String metric;

    @Schema(description = "Threshold value to trigger alert", example = "80.0")
    private Float thresholdValue;

    @Schema(description = "Alert trigger direction", example = "above", allowableValues = {"above", "below"})
    private String alertType;

    public SettingsDTO() {
    }

    public SettingsDTO(String id, String type, String metric, Float thresholdValue, String alertType) {
        this.id = id;
        this.type = type;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.alertType = alertType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Float getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Float thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
}
