package com.backend.sensor_data.service.strategy;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

@Component

public class AirThresholdStrategy extends ThresholdStrategy<AirPollutionData> {

    private static final String TYPE = "Air";
    private static final String METRIC_CO = "Carbon Monoxide";
    private static final String METRIC_OZONE = "Ozone";

    public AirThresholdStrategy(SettingsRepository settingsRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        super(settingsRepository, notificationRepository, messagingTemplate);
    }

    @Override
    public void check(AirPollutionData entity, String location) {
        checkAlerts(TYPE, METRIC_CO, entity.getCo(), location);
        checkAlerts(TYPE, METRIC_OZONE, entity.getOzone(), location);
    }

}
