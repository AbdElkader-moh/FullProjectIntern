package com.backend.user.dto;

import java.time.LocalDateTime;

public class NotificationDTO {
    private String id;
    private String type;
    private String metric;
    private Float value;
    private Float thresholdValue;
    private String alertType;
    private String location;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(String id, String type, String metric, Float value, Float thresholdValue,
                           String alertType, String location, Boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.metric = metric;
        this.value = value;
        this.thresholdValue = thresholdValue;
        this.alertType = alertType;
        this.location = location;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public Float getValue() { return value; }
    public void setValue(Float value) { this.value = value; }
    public Float getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Float thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
