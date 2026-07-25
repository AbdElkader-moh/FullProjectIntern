package com.backend.user.dto;

import com.backend.user.entity.Notification;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationDTOTest {

    @Test
    void fromEntity_mapsAllFieldsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2026, Month.JULY, 24, 10, 30);

        Notification notification = new Notification();
        notification.setId("notif-123");
        notification.setUserId(42L);
        notification.setType("Traffic");
        notification.setMetric("trafficDensity");
        notification.setValue(95.5f);
        notification.setThresholdValue(80.0f);
        notification.setAlertType("above");
        notification.setLocation("Zone A - Main St");
        notification.setIsRead(false);
        notification.setCreatedAt(createdAt);

        NotificationDTO dto = NotificationDTO.fromEntity(notification);

        assertEquals("notif-123", dto.getId());
        assertEquals("Traffic", dto.getType());
        assertEquals("trafficDensity", dto.getMetric());
        assertEquals(95.5f, dto.getValue());
        assertEquals(80.0f, dto.getThresholdValue());
        assertEquals("above", dto.getAlertType());
        assertEquals("Zone A - Main St", dto.getLocation());
        assertEquals(false, dto.getIsRead());
        assertEquals(createdAt, dto.getCreatedAt());
    }

    @Test
    void fromEntity_handlesNullOptionalFields() {
        Notification notification = new Notification();
        notification.setId("notif-456");
        notification.setType("Air");
        notification.setMetric("co");
        notification.setValue(10.0f);
        notification.setThresholdValue(50.0f);
        notification.setAlertType("below");
        notification.setLocation("Zone B");
        // isRead defaults to false on the entity; createdAt defaults to now() -
        // both are still non-null, so this test focuses on confirming no
        // exception is thrown when mapping a minimally-populated entity.

        NotificationDTO dto = NotificationDTO.fromEntity(notification);

        assertEquals("notif-456", dto.getId());
        assertEquals("Air", dto.getType());
    }
}
