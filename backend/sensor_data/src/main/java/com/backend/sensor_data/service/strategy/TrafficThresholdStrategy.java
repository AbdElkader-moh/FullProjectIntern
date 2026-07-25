package com.backend.sensor_data.service.strategy;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

@Component
public class TrafficThresholdStrategy extends ThresholdStrategy<TrafficData> {

    private static final String TYPE = "Traffic";
    private static final String METRIC_DENSITY = "Traffic Density";
    private static final String METRIC_SPEED = "Average Speed";

    public TrafficThresholdStrategy(SettingsRepository settingsRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        super(settingsRepository, notificationRepository, messagingTemplate);
    }

    @Override
    public void check(TrafficData entity, String location) {
        checkAlerts(TYPE, METRIC_DENSITY, entity.getTrafficDensity(), location);
        checkAlerts(TYPE, METRIC_SPEED, entity.getAvgSpeed(), location);
    }
}