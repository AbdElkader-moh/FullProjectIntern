package com.backend.sensor_data.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;

/**
 * Covers saveTrafficData's validation and its threshold-crossing/alert path --
 * the exact mechanism behind the live-toast feature (persist a Notification
 * and broadcast to /topic/alerts/{userId} only when a matching Settings row
 * is actually crossed).
 */
@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {

    @Mock private TrafficDataRepository trafficRepo;
    @Mock private AirPollutionDataRepository airRepo;
    @Mock private StreetLightDataRepository lightRepo;
    @Mock private SettingsRepository settingsRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SensorProcessorFactory processorFactory;

    private SensorDataService service;

    @BeforeEach
    void setUp() {
        service = new SensorDataService(trafficRepo, airRepo, lightRepo, settingsRepository,
                messagingTemplate, notificationRepository, processorFactory);
    }

    private TrafficDataDto validDto() {
        TrafficDataDto dto = new TrafficDataDto();
        dto.setLocation("Alexandria");
        dto.setTrafficDensity(450);
        dto.setAvgSpeed(15.0f);
        dto.setCongestionLevel(CongestionLevel.Severe);
        return dto;
    }

    // ---------- Validation ----------

    @Test
    void saveTrafficData_blankLocation_throws() {
        TrafficDataDto dto = validDto();
        dto.setLocation("  ");
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_nullCongestionLevel_throws() {
        TrafficDataDto dto = validDto();
        dto.setCongestionLevel(null);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_nullTrafficDensity_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(null);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_trafficDensityAboveRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(501);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_trafficDensityBelowRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(-1);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_nullAvgSpeed_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(null);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    @Test
    void saveTrafficData_avgSpeedAboveRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(121f);
        assertThrows(IllegalArgumentException.class, () -> service.saveTrafficData(dto));
    }

    // ---------- Persistence ----------

    @Test
    void saveTrafficData_validRequest_savesEntityWithMappedFields() {
        when(settingsRepository.findByTypeAndMetric(anyString(), anyString())).thenReturn(List.of());

        TrafficData saved = service.saveTrafficData(validDto());

        assertEquals("Alexandria", saved.getLocation());
        assertEquals(450, saved.getTrafficDensity());
        assertEquals(15.0f, saved.getAvgSpeed());
        assertEquals(CongestionLevel.Severe, saved.getCongestionLevel());
        verify(trafficRepo).save(saved);
    }

    // ---------- Threshold / alert logic ----------

    @Test
    void saveTrafficData_aboveThresholdCrossed_persistsNotificationAndBroadcasts() {
        Settings threshold = new Settings(42L, "Traffic", "Traffic Density", 100f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(threshold));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        service.saveTrafficData(validDto()); // trafficDensity=450 > 100

        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/42"), any(Object.class));
    }

    @Test
    void saveTrafficData_thresholdNotCrossed_noAlertRaised() {
        // trafficDensity=450 does not satisfy an "above 1000" threshold
        Settings threshold = new Settings(42L, "Traffic", "Traffic Density", 1000f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(threshold));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        service.saveTrafficData(validDto());

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void saveTrafficData_belowDirectionThreshold_crossedWhenValueLower() {
        // avgSpeed=15.0 is below a "below 30" threshold on Average Speed
        Settings threshold = new Settings(7L, "Traffic", "Average Speed", 30f, "below");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of());
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of(threshold));

        service.saveTrafficData(validDto());

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/7"), any(Object.class));
    }

    @Test
    void saveTrafficData_multipleUsersWithMatchingThresholds_broadcastsToEachUserTopic() {
        Settings userA = new Settings(1L, "Traffic", "Traffic Density", 100f, "above");
        Settings userB = new Settings(2L, "Traffic", "Traffic Density", 200f, "above");
        when(settingsRepository.findByTypeAndMetric("Traffic", "Traffic Density"))
                .thenReturn(List.of(userA, userB));
        when(settingsRepository.findByTypeAndMetric("Traffic", "Average Speed"))
                .thenReturn(List.of());

        service.saveTrafficData(validDto()); // trafficDensity=450 crosses both

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/1"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/2"), any(Object.class));
    }
}
