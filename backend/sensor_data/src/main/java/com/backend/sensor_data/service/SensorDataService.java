package com.backend.sensor_data.service;

import com.backend.sensor_data.dto.*;
import com.backend.sensor_data.entity.*;
import com.backend.sensor_data.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

@Service
public class SensorDataService {
    private static final Logger logger = LoggerFactory.getLogger(SensorDataService.class);

    private final TrafficDataRepository trafficRepo;
    private final AirPollutionDataRepository airRepo;
    private final StreetLightDataRepository lightRepo;
    private final SettingsRepository settingsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final NotificationRepository notificationRepository;

    public SensorDataService(TrafficDataRepository trafficRepo, AirPollutionDataRepository airRepo, StreetLightDataRepository lightRepo, SettingsRepository settingsRepository, SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository) {
        this.trafficRepo = trafficRepo;
        this.airRepo = airRepo;
        this.lightRepo = lightRepo;
        this.settingsRepository = settingsRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public TrafficData saveTrafficData(TrafficDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid location: cannot be blank");
        }
        if (dto.getCongestionLevel() == null) {
            throw new IllegalArgumentException("Invalid congestion level: cannot be null");
        }
        if (dto.getTrafficDensity() == null) {
            throw new IllegalArgumentException("trafficDensity is required");
        }
        if (dto.getTrafficDensity() < 0 || dto.getTrafficDensity() > 500) {
            throw new IllegalArgumentException("Invalid traffic density: must be between 0 and 500");
        }
        if (dto.getAvgSpeed() == null) {
            throw new IllegalArgumentException("avgSpeed is required");
        }
        if (dto.getAvgSpeed() < 0 || dto.getAvgSpeed() > 120) {
            throw new IllegalArgumentException("Invalid average speed: must be between 0 and 120");
        }

        TrafficData data = new TrafficData();
        data.setLocation(dto.getLocation());
        data.setTrafficDensity(dto.getTrafficDensity());
        data.setAvgSpeed(dto.getAvgSpeed());
        data.setCongestionLevel(dto.getCongestionLevel());

        trafficRepo.save(data);

        checkAlerts("Traffic", "Traffic Density", data.getTrafficDensity(), data.getLocation());
        checkAlerts("Traffic", "avgSpeed", data.getAvgSpeed(), data.getLocation());

        return data;
    }

    @Transactional
    public AirPollutionData saveAirPollutionData(AirPollutionDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid location: cannot be blank");
        }
        if (dto.getPollutionLevel() == null) {
            throw new IllegalArgumentException("Invalid pollution level: cannot be null");
        }
        if (dto.getCo() == null) {
            throw new IllegalArgumentException("co is required");
        }
        if (dto.getCo() < 0 || dto.getCo() > 50) {
            throw new IllegalArgumentException("Invalid CO level: must be between 0 and 50");
        }
        if (dto.getOzone() == null) {
            throw new IllegalArgumentException("ozone is required");
        }
        if (dto.getOzone() < 0 || dto.getOzone() > 300) {
            throw new IllegalArgumentException("Invalid ozone level: must be between 0 and 300");
        }

        AirPollutionData data = new AirPollutionData();
        data.setLocation(dto.getLocation());
        data.setPm2_5(dto.getPm2_5());
        data.setPm10(dto.getPm10());
        data.setCo(dto.getCo());
        data.setNo2(dto.getNo2());
        data.setSo2(dto.getSo2());
        data.setOzone(dto.getOzone());
        data.setPollutionLevel(dto.getPollutionLevel());

        airRepo.save(data);

        checkAlerts("Air", "Carbon Monoxide", data.getCo(), data.getLocation());
        checkAlerts("Air", "ozone", data.getOzone(), data.getLocation());

        return data;
    }

    @Transactional
    public StreetLightData saveStreetLightData(StreetLightDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid location: cannot be blank");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("Invalid status: cannot be null");
        }
        if (dto.getBrightnessLevel() == null) {
            throw new IllegalArgumentException("brightnessLevel is required");
        }
        if (dto.getBrightnessLevel() < 0 || dto.getBrightnessLevel() > 100) {
            throw new IllegalArgumentException("Invalid brightness level: must be between 0 and 100");
        }
        if (dto.getPowerConsumption() == null) {
            throw new IllegalArgumentException("powerConsumption is required");
        }
        if (dto.getPowerConsumption() < 0 || dto.getPowerConsumption() > 5000) {
            throw new IllegalArgumentException("Invalid power consumption: must be between 0 and 5000");
        }

        StreetLightData data = new StreetLightData();
        data.setLocation(dto.getLocation());
        data.setBrightnessLevel(dto.getBrightnessLevel());
        data.setPowerConsumption(dto.getPowerConsumption());
        data.setStatus(dto.getStatus());

        lightRepo.save(data);

        checkAlerts("Light", "Brightness Level", data.getBrightnessLevel(), data.getLocation());
        checkAlerts("Light", "powerConsumption", data.getPowerConsumption(), data.getLocation());

        return data;
    }

    private void checkAlerts(String type, String metric, Number value, String location) {
        List<Settings> matchingSettings = settingsRepository.findByTypeAndMetric(type, metric);

        for (Settings setting : matchingSettings) {
            float threshold = setting.getThresholdValue();
            String alertType = setting.getAlertType();
            Long userId = setting.getUserId();
            float actual = value.floatValue();

            boolean triggered =
                    ("above".equalsIgnoreCase(alertType) && actual > threshold) ||
                            ("below".equalsIgnoreCase(alertType) && actual < threshold);

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
                            "location", location
                    )
            );
        }
    }
}
