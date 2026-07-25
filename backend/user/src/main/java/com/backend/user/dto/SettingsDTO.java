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
    public void validateThreshold() {
        validateThreshold(this.type, this.metric);
    }

    public void validateThreshold(String type, String metric) {
        float min;
        float max;

        switch (type) {
            case "Traffic" -> {
                switch (metric) {
                    case "trafficDensity", "Traffic Density" -> { min = 0; max = 500; }
                    case "avgSpeed", "Avg Speed", "Average Speed"             -> { min = 0; max = 120; }
                    default -> throw new IllegalArgumentException("Unknown Traffic metric: " + metric);
                }
            }
            case "Air" -> {
                switch (metric) {
                    case "co", "Carbon Monoxide" -> { min = 0; max = 50;  }
                    case "ozone", "Ozone"        -> { min = 0; max = 300; }
                    default -> throw new IllegalArgumentException("Unknown Air metric: " + metric);
                }
            }
            case "Light" -> {
                switch (metric) {
                    case "brightnessLevel", "Brightness Level"     -> { min = 0; max = 100;  }
                    case "powerConsumption", "Power Consumption"   -> { min = 0; max = 5000; }
                    default -> throw new IllegalArgumentException("Unknown Light metric: " + metric);
                }
            }
            default -> throw new IllegalArgumentException("Unknown sensor type: " + type);
        }

        if (thresholdValue == null || thresholdValue < min || thresholdValue > max) {
            throw new IllegalArgumentException(
                    "Threshold for " + metric + " must be between " + (int) min + " and " + (int) max + "."
            );
        }

        if (!"above".equalsIgnoreCase(alertType) && !"below".equalsIgnoreCase(alertType)) {
            throw new IllegalArgumentException("alertType must be 'above' or 'below'.");
        }
    }
}
