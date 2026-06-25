package com.backend.sensor_data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private Float value;

    @Column(name = "threshold_value", nullable = false)
    private Float thresholdValue;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(nullable = false)
    private String location;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public Notification(Long userId, String type, String metric, Float value, Float thresholdValue, String alertType, String location) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.metric = metric;
        this.value = value;
        this.thresholdValue = thresholdValue;
        this.alertType = alertType;
        this.location = location;
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
