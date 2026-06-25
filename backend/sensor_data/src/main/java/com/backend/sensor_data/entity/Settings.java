package com.backend.sensor_data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Settings {
    @Id
    private String id;
    private Long userId;
    private String type;
    private String metric;
    private Float thresholdValue;
    private String alertType;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Settings() {}

    public Settings(Long userId, String type, String metric, Float thresholdValue, String alertType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.alertType = alertType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public Float getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Float thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
