package com.backend.sensor_data.service.strategy;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

@Component
public class LightThresholdStrategy extends ThresholdStrategy<StreetLightData> {

    private static final String TYPE = "Light";
    private static final String METRIC_POWER = "Power Consumption";
    private static final String METRIC_BRIGHTNESS = "Brightness Level";

    public LightThresholdStrategy(SettingsRepository settingsRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        super(settingsRepository, notificationRepository, messagingTemplate);
    }

    @Override
    public void check(StreetLightData entity, String location) {
        checkAlerts(TYPE, METRIC_POWER, entity.getPowerConsumption(), location);
        checkAlerts(TYPE, METRIC_BRIGHTNESS, entity.getBrightnessLevel(), location);
    }

}