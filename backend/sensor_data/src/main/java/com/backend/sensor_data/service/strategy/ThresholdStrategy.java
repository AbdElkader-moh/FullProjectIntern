package com.backend.sensor_data.service.strategy;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.entity.Notification;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;

public abstract class ThresholdStrategy<T> {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdStrategy.class);

    protected final SettingsRepository settingsRepository;
    protected final NotificationRepository notificationRepository;
    protected final SimpMessagingTemplate messagingTemplate;

    protected ThresholdStrategy(SettingsRepository settingsRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.settingsRepository = settingsRepository;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public abstract void check(T entity, String location);

    protected void checkAlerts(String type, String metric, Number value, String location) {
        List<Settings> matchingSettings = settingsRepository.findByTypeAndMetric(type, metric);

        for (Settings setting : matchingSettings) {
            float threshold = setting.getThresholdValue();
            String alertType = setting.getAlertType();
            Long userId = setting.getUserId();
            float actual = value.floatValue();

            boolean triggered = ("above".equalsIgnoreCase(alertType) && actual > threshold)
                    || ("below".equalsIgnoreCase(alertType) && actual < threshold);

            if (!triggered) continue;

            logger.warn("[ALERT] {} {} ({}) exceeded threshold ({}) at {}",
                    type, metric, actual, threshold, location);

            Notification notification = new Notification(
                    userId, type, metric, actual, threshold, alertType, location);
            notificationRepository.save(notification);

            messagingTemplate.convertAndSend(
                    "/topic/alerts/" + userId,
                    Map.of(
                            "type", type,
                            "metric", metric,
                            "value", actual,
                            "thresholdValue", threshold,
                            "alertType", alertType,
                            "location", location));
        }
    }
}