package com.backend.sensor_data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

/**
 * Covers the logic Notification actually has beyond plain accessors:
 *   - both constructors generate a non-null, unique UUID id
 *   - the all-args constructor sets every field and forces isRead=false
 *   - createdAt defaults to "now" in UTC at construction time
 *   - getters/setters round-trip correctly, including overriding the defaults
 */
class NotificationTest {

    // ---------------- no-arg constructor ----------------

    @Test
    void noArgConstructor_generatesNonNullId() {
        Notification notification = new Notification();

        assertThat(notification.getId()).isNotNull().isNotBlank();
    }

    @Test
    void noArgConstructor_defaultsIsReadToFalse() {
        Notification notification = new Notification();

        assertThat(notification.getIsRead()).isFalse();
    }

    @Test
    void noArgConstructor_defaultsCreatedAtToApproximatelyNowUtc() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("UTC"));

        Notification notification = new Notification();

        LocalDateTime after = LocalDateTime.now(ZoneId.of("UTC"));
        assertThat(notification.getCreatedAt())
                .isNotNull()
                .isBetween(before.minus(1, ChronoUnit.SECONDS), after.plus(1, ChronoUnit.SECONDS));
    }

    @Test
    void twoInstances_haveDifferentIds() {
        Notification first = new Notification();
        Notification second = new Notification();

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    // ---------------- all-args constructor ----------------

    @Test
    void allArgsConstructor_setsAllFieldsCorrectly() {
        Notification notification = new Notification(
                42L, "Light", "Brightness Level", 15.0f, 20.0f, "BELOW_THRESHOLD", "Main St & 5th Ave");

        assertThat(notification.getUserId()).isEqualTo(42L);
        assertThat(notification.getType()).isEqualTo("Light");
        assertThat(notification.getMetric()).isEqualTo("Brightness Level");
        assertThat(notification.getValue()).isEqualTo(15.0f);
        assertThat(notification.getThresholdValue()).isEqualTo(20.0f);
        assertThat(notification.getAlertType()).isEqualTo("BELOW_THRESHOLD");
        assertThat(notification.getLocation()).isEqualTo("Main St & 5th Ave");
    }

    @Test
    void allArgsConstructor_generatesNonNullId() {
        Notification notification = new Notification(
                1L, "Traffic", "Traffic Density", 400.0f, 350.0f, "ABOVE_THRESHOLD", "Elm St");

        assertThat(notification.getId()).isNotNull().isNotBlank();
    }

    @Test
    void allArgsConstructor_forcesIsReadFalseRegardlessOfPriorState() {
        Notification notification = new Notification(
                1L, "Air", "Carbon Monoxide", 60.0f, 50.0f, "ABOVE_THRESHOLD", "Oak Ave");

        assertThat(notification.getIsRead()).isFalse();
    }

    @Test
    void allArgsConstructor_alsoDefaultsCreatedAtToApproximatelyNowUtc() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("UTC"));

        Notification notification = new Notification(
                1L, "Traffic", "Average Speed", 10.0f, 20.0f, "BELOW_THRESHOLD", "Elm St");

        LocalDateTime after = LocalDateTime.now(ZoneId.of("UTC"));
        assertThat(notification.getCreatedAt())
                .isBetween(before.minus(1, ChronoUnit.SECONDS), after.plus(1, ChronoUnit.SECONDS));
    }

    // ---------------- setters / overrides ----------------

    @Test
    void setId_overridesGeneratedId() {
        Notification notification = new Notification();

        notification.setId("custom-id-123");

        assertThat(notification.getId()).isEqualTo("custom-id-123");
    }

    @Test
    void setIsRead_canMarkAsRead() {
        Notification notification = new Notification();

        notification.setIsRead(true);

        assertThat(notification.getIsRead()).isTrue();
    }

    @Test
    void setCreatedAt_overridesDefaultTimestamp() {
        Notification notification = new Notification();
        LocalDateTime fixedTime = LocalDateTime.of(2026, java.time.Month.JANUARY, 1, 0, 0, 0);

        notification.setCreatedAt(fixedTime);

        assertThat(notification.getCreatedAt()).isEqualTo(fixedTime);
    }

    @Test
    void settersAndGetters_roundTripForAllRemainingFields() {
        Notification notification = new Notification();

        notification.setUserId(99L);
        notification.setType("Air");
        notification.setMetric("Carbon Monoxide");
        notification.setValue(45.5f);
        notification.setThresholdValue(40.0f);
        notification.setAlertType("ABOVE_THRESHOLD");
        notification.setLocation("Downtown");

        assertThat(notification.getUserId()).isEqualTo(99L);
        assertThat(notification.getType()).isEqualTo("Air");
        assertThat(notification.getMetric()).isEqualTo("Carbon Monoxide");
        assertThat(notification.getValue()).isEqualTo(45.5f);
        assertThat(notification.getThresholdValue()).isEqualTo(40.0f);
        assertThat(notification.getAlertType()).isEqualTo("ABOVE_THRESHOLD");
        assertThat(notification.getLocation()).isEqualTo("Downtown");
    }
}
