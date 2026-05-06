package com.backend.user.dto;

public class SettingsDTO {
    private String id;
    private String type;
    private String metric;
    private Float thresholdValue;
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
