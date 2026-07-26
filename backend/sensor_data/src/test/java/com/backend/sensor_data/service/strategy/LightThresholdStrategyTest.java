package com.backend.sensor_data.service.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

/**
 * LightThresholdStrategy.check() is a thin delegation to the inherited
 * checkAlerts() (defined on ThresholdStrategy<T>, not shown to me). Since I
 * don't have that superclass's source, this test spies on the real strategy
 * instance and stubs out checkAlerts() itself, so we verify ONLY the piece
 * LightThresholdStrategy actually owns: that check() calls checkAlerts()
 * exactly twice, with the right (type, metric, value, location) for both
 * power consumption and brightness level.
 *
 * ASSUMPTION: checkAlerts(String type, String metric, Number value, String location)
 * is a protected, non-final, non-static instance method on ThresholdStrategy.
 * If the real signature differs (different param types/order, or it's
 * private/final/static and therefore un-spyable), this won't compile as-is —
 * please share ThresholdStrategy.java and I'll correct it.
 *
 * If ThresholdStrategy.checkAlerts() actually contains the real repository/
 * notification/websocket logic, consider ALSO adding an integration-style
 * test class that mocks SettingsRepository/NotificationRepository/
 * SimpMessagingTemplate directly and exercises checkAlerts() for real (not
 * spied away) to get coverage on that shared logic too — this test alone
 * only proves LightThresholdStrategy wires it correctly, not that checkAlerts
 * itself behaves correctly.
 */
@ExtendWith(MockitoExtension.class)
class LightThresholdStrategyTest {

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private LightThresholdStrategy strategy;

    @BeforeEach
    void setUp() {
        LightThresholdStrategy real =
                new LightThresholdStrategy(settingsRepository, notificationRepository, messagingTemplate);
        strategy = spy(real);
        doNothing().when(strategy).checkAlerts(any(), any(), any(), any());
    }

    @Test
    void check_callsCheckAlertsForPowerConsumption() {
        StreetLightData entity = new StreetLightData();
        entity.setPowerConsumption(1200.0f);
        entity.setBrightnessLevel(75);
        String location = "Main St & 5th Ave";

        strategy.check(entity, location);

        verify(strategy).checkAlerts(
                "Light",
                "Power Consumption",
                entity.getPowerConsumption(),
                location);
    }

    @Test
    void check_callsCheckAlertsForBrightnessLevel() {
        StreetLightData entity = new StreetLightData();
        entity.setPowerConsumption(1200.0f);
        entity.setBrightnessLevel(75);
        String location = "Main St & 5th Ave";

        strategy.check(entity, location);

        verify(strategy).checkAlerts(
                "Light",
                "Brightness Level",
                entity.getBrightnessLevel(),
                location);
    }

    @Test
    void check_callsCheckAlertsExactlyTwice_noMoreNoLess() {
        StreetLightData entity = new StreetLightData();
        entity.setPowerConsumption(500.0f);
        entity.setBrightnessLevel(50);

        strategy.check(entity, "Elm St");

        verify(strategy, times(2)).checkAlerts(any(), any(), any(), any());
    }

    @Test
    void check_passesThroughDifferentLocationsCorrectly() {
        StreetLightData entity = new StreetLightData();
        entity.setPowerConsumption(300.0f);
        entity.setBrightnessLevel(20);

        strategy.check(entity, "Oak Ave & 2nd St");

        verify(strategy).checkAlerts(eq("Light"), eq("Power Consumption"), any(), eq("Oak Ave & 2nd St"));
        verify(strategy).checkAlerts(eq("Light"), eq("Brightness Level"), any(), eq("Oak Ave & 2nd St"));
    }
}
