package com.backend.sensor_data.service.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.Notification;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

class TrafficThresholdStrategyTest {

    private SettingsRepository settingsRepository;
    private NotificationRepository notificationRepository;
    private SimpMessagingTemplate messagingTemplate;
    private TrafficThresholdStrategy strategy;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(SettingsRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        strategy = new TrafficThresholdStrategy(settingsRepository, notificationRepository, messagingTemplate);
    }

    private TrafficData trafficData(int density, float avgSpeed) {
        TrafficData data = new TrafficData();
        data.setLocation("Downtown");
        data.setTrafficDensity(density);
        data.setAvgSpeed(avgSpeed);
        data.setCongestionLevel(CongestionLevel.High);
        return data;
    }

    private Settings settingsFor(float threshold, String alertType, Long userId) {
        Settings settings = new Settings();
        settings.setThresholdValue(threshold);
        settings.setAlertType(alertType);
        settings.setUserId(userId);
        return settings;
    }

    @Test
    void check_densityAboveThreshold_savesNotificationAndBroadcasts() {
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(settingsFor(100f, "above", 1L)));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        strategy.check(trafficData(150, 45.0f), "Downtown");

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/1"), any(Object.class));
    }

    @Test
    void check_speedBelowThreshold_savesNotificationAndBroadcasts() {
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of());
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of(settingsFor(20f, "below", 2L)));

        strategy.check(trafficData(50, 10.0f), "Downtown");

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/2"), any(Object.class));
    }

    @Test
    void check_valueWithinThreshold_noAlertTriggered() {
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(settingsFor(200f, "above", 1L)));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of(settingsFor(20f, "below", 1L)));

        strategy.check(trafficData(50, 60.0f), "Downtown");

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void check_noMatchingSettings_noInteractionsWithNotificationOrMessaging() {
        when(settingsRepository.findByTypeAndMetric(anyString(), anyString()))
                .thenReturn(List.of());

        strategy.check(trafficData(300, 20.0f), "Downtown");

        verifyNoInteractions(notificationRepository, messagingTemplate);
    }
}