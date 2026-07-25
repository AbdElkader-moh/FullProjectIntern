package com.backend.sensor_data.service.strategy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

@ExtendWith(MockitoExtension.class)
class TrafficThresholdStrategyTest {

    @Mock private SettingsRepository settingsRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TrafficThresholdStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TrafficThresholdStrategy(settingsRepository, notificationRepository, messagingTemplate);
    }

    private TrafficData trafficData() {
        TrafficData data = new TrafficData();
        data.setLocation("Alexandria");
        data.setTrafficDensity(450);
        data.setAvgSpeed(15.0f);
        data.setCongestionLevel(CongestionLevel.Severe);
        return data;
    }

    @Test
    void check_aboveThresholdCrossed_persistsNotificationAndBroadcasts() {
        Settings threshold = new Settings(42L, "Traffic", "Traffic Density", 100f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(threshold));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        strategy.check(trafficData(), "Alexandria"); // trafficDensity=450 > 100

        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/42"), any(Object.class));
    }

    @Test
    void check_thresholdNotCrossed_noAlertRaised() {
        Settings threshold = new Settings(42L, "Traffic", "Traffic Density", 1000f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(threshold));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        strategy.check(trafficData(), "Alexandria");

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void check_belowDirectionThreshold_crossedWhenValueLower() {
        Settings threshold = new Settings(7L, "Traffic", "Average Speed", 30f, "below");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of());
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of(threshold));

        strategy.check(trafficData(), "Alexandria"); // avgSpeed=15.0 below 30

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/7"), any(Object.class));
    }

    @Test
    void check_multipleUsersWithMatchingThresholds_broadcastsToEachUserTopic() {
        Settings userA = new Settings(1L, "Traffic", "Traffic Density", 100f, "above");
        Settings userB = new Settings(2L, "Traffic", "Traffic Density", 200f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(userA, userB));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        strategy.check(trafficData(), "Alexandria"); // trafficDensity=450 crosses both

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/1"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/2"), any(Object.class));
    }
}