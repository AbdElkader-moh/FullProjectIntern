package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Threshold breach notification")
public class NotificationDTO {

    @Schema(description = "Notification ID (UUID)")
    private String id;

    @Schema(description = "Sensor type", example = "Traffic")
    private String type;

    @Schema(description = "Metric that triggered the alert", example = "trafficDensity")
    private String metric;

    @Schema(description = "Actual measured value", example = "95.5")
    private Float value;

    @Schema(description = "Configured threshold value", example = "80.0")
    private Float thresholdValue;

    @Schema(description = "Alert direction (above/below)", example = "above")
    private String alertType;

    @Schema(description = "Sensor location", example = "Zone A - Main St")
    private String location;

    @Schema(description = "Whether the notification has been read", example = "false")
    private Boolean isRead;

    @Schema(description = "Timestamp when the notification was created")
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
