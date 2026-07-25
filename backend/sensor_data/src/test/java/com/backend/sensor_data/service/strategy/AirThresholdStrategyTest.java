package com.backend.sensor_data.service.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

/**
 * This is the strategy driving the Air Pollution live-toast feature -- the
 * one whose broken WebSocket delivery took an entire debugging session to
 * track down to an nginx proxy header. Covering it directly (rather than
 * only its structurally-identical sibling in SensorDataServiceTest) makes
 * sure the alert-triggering logic itself has real, not just inferred,
 * coverage.
 */
@ExtendWith(MockitoExtension.class)
class AirThresholdStrategyTest {

    @Mock private SettingsRepository settingsRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private AirThresholdStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AirThresholdStrategy(settingsRepository, notificationRepository, messagingTemplate);
    }

    private AirPollutionData reading(float co, float ozone) {
        AirPollutionData data = new AirPollutionData();
        data.setLocation("Alexandria");
        data.setCo(co);
        data.setOzone(ozone);
        return data;
    }

    @Test
    void check_coAboveThreshold_broadcastsAlert() {
        Settings coThreshold = new Settings(10L, "Air", "Carbon Monoxide", 40f, "above");
        when(settingsRepository.findByTypeAndMetric("Air", "Carbon Monoxide"))
                .thenReturn(List.of(coThreshold));
        when(settingsRepository.findByTypeAndMetric("Air", "Ozone"))
                .thenReturn(List.of());

        strategy.check(reading(45f, 100f), "Alexandria"); // co=45 > 40

        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/10"), any(Object.class));
    }

    @Test
    void check_ozoneBelowThreshold_broadcastsAlert() {
        Settings ozoneThreshold = new Settings(11L, "Air", "Ozone", 30f, "below");
        when(settingsRepository.findByTypeAndMetric("Air", "Carbon Monoxide"))
                .thenReturn(List.of());
        when(settingsRepository.findByTypeAndMetric("Air", "Ozone"))
                .thenReturn(List.of(ozoneThreshold));

        strategy.check(reading(10f, 5f), "Alexandria"); // ozone=5 < 30

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/11"), any(Object.class));
    }

    @Test
    void check_neitherThresholdCrossed_noAlertRaised() {
        Settings coThreshold = new Settings(10L, "Air", "Carbon Monoxide", 40f, "above");
        Settings ozoneThreshold = new Settings(11L, "Air", "Ozone", 30f, "below");
        when(settingsRepository.findByTypeAndMetric("Air", "Carbon Monoxide"))
                .thenReturn(List.of(coThreshold));
        when(settingsRepository.findByTypeAndMetric("Air", "Ozone"))
                .thenReturn(List.of(ozoneThreshold));

        strategy.check(reading(15f, 50f), "Alexandria"); // co=15 (not >40), ozone=50 (not <30)

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
