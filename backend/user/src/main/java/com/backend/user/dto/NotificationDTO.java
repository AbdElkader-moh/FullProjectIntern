package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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

    /**
     * Builds a NotificationDTO from a Notification entity.
     * Replaces the old 9-parameter constructor (flagged by SonarQube as
     * exceeding the 7-parameter limit) with a single, named construction point.
     */
    public static NotificationDTO fromEntity(com.backend.user.entity.Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setMetric(notification.getMetric());
        dto.setValue(notification.getValue());
        dto.setThresholdValue(notification.getThresholdValue());
        dto.setAlertType(notification.getAlertType());
        dto.setLocation(notification.getLocation());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
