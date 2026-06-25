package com.backend.sensor_data.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.dto.AirStatsDto;
import com.backend.sensor_data.dto.AirTrendDto;
import com.backend.sensor_data.dto.LightStatsDto;
import com.backend.sensor_data.dto.LightTrendDto;
import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.dto.TrafficStatsDto;
import com.backend.sensor_data.dto.TrafficTrendDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.Notification;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.entity.Settings;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;

import jakarta.transaction.Transactional;

@Service
public class SensorDataService {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataService.class);
    private final SensorProcessorFactory processorFactory;

    private final TrafficDataRepository trafficRepo;
    private final AirPollutionDataRepository airRepo;
private final StreetLightDataRepository lightRepo;

    private final SettingsRepository settingsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final NotificationRepository notificationRepository;

    public SensorDataService(TrafficDataRepository trafficRepo, AirPollutionDataRepository airRepo,
        StreetLightDataRepository lightRepo ,SettingsRepository settingsRepository,
        SimpMessagingTemplate messagingTemplate,
        NotificationRepository notificationRepository,
        SensorProcessorFactory processorFactory) {
    this.trafficRepo = trafficRepo;
    this.airRepo = airRepo;
    this.lightRepo = lightRepo;
    this.settingsRepository = settingsRepository;
    this.messagingTemplate = messagingTemplate;
    this.notificationRepository = notificationRepository;
    this.processorFactory = processorFactory;
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
        checkAlerts("Traffic", "Average Speed", data.getAvgSpeed(), data.getLocation());

        return data;
    }

    @Transactional
    public void saveAirPollutionData(AirPollutionDataDto dto) {
        processorFactory.<AirPollutionDataDto>getProcessor("AIR").process(dto);
    }

    @Transactional
    public void saveStreetLightData(StreetLightDataDto dto) {
        processorFactory.<StreetLightDataDto>getProcessor("LIGHT").process(dto);
    }

    public Page<TrafficData> getTrafficData(
            String location,
            CongestionLevel congestionLevel,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        Specification<TrafficData> spec = Specification.where(null);

        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
        }

        if (congestionLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("congestionLevel"), congestionLevel));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        return trafficRepo.findAll(spec, pageable);
    }

    private void checkAlerts(String type, String metric, Number value, String location) {
        List<Settings> matchingSettings = settingsRepository.findByTypeAndMetric(type, metric);

        for (Settings setting : matchingSettings) {
            float threshold = setting.getThresholdValue();
            String alertType = setting.getAlertType();
            Long userId = setting.getUserId();
            float actual = value.floatValue();

            boolean triggered = ("above".equalsIgnoreCase(alertType) && actual > threshold)
                    || ("below".equalsIgnoreCase(alertType) && actual < threshold);

            if (!triggered) {
                continue;
            }

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

    public TrafficStatsDto getTrafficStats() {

        long totalRecords = trafficRepo.count();

        double averageTrafficDensity = trafficRepo.findAll()
                .stream()
                .mapToInt(TrafficData::getTrafficDensity)
                .average()
                .orElse(0);

        double averageSpeed = trafficRepo.findAll()
                .stream()
                .mapToDouble(TrafficData::getAvgSpeed)
                .average()
                .orElse(0);

        long highCongestionCount = trafficRepo.countByCongestionLevel(CongestionLevel.High);

        long severeCongestionCount = trafficRepo.countByCongestionLevel(CongestionLevel.Severe);

        return new TrafficStatsDto(
                totalRecords,
                averageTrafficDensity,
                averageSpeed,
                highCongestionCount,
                severeCongestionCount);
    }

    public List<TrafficTrendDto> getTrafficTrends() {
        return trafficRepo.findTop50ByOrderByTimestampDesc()
                .stream()
                .map(data -> new TrafficTrendDto(
                data.getTimestamp(),
                data.getTrafficDensity(),
                data.getAvgSpeed()))
                .toList();
    }

    public Map<String, Long> getTrafficCongestionSummary() {
        return Map.of(
                "Low", trafficRepo.countByCongestionLevel(CongestionLevel.Low),
                "Moderate", trafficRepo.countByCongestionLevel(CongestionLevel.Moderate),
                "High", trafficRepo.countByCongestionLevel(CongestionLevel.High),
                "Severe", trafficRepo.countByCongestionLevel(CongestionLevel.Severe));
    }

    public Page<AirPollutionData> getAirData(
        String location,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable) {
    Specification<AirPollutionData> spec = Specification.where(null);

    if (location != null && !location.trim().isEmpty()) {
        spec = spec.and(
                (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
    }
    if (from != null) {
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
    }
    if (to != null) {
        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
    }

    return airRepo.findAll(spec, pageable);
}

public AirStatsDto getAirStats() {
    long totalRecords = airRepo.count();
    long totalAlerts = notificationRepository.countByType("Air");

    Double avgCoRaw = airRepo.findAvgCo();
    Double avgOzoneRaw = airRepo.findAvgOzone();
    Double highestCoRaw = airRepo.findMaxCo();
    Double lowestCoRaw = airRepo.findMinCo();
    Double highestOzoneRaw = airRepo.findMaxOzone();
    Double lowestOzoneRaw = airRepo.findMinOzone();

    double avgCo = avgCoRaw != null ? avgCoRaw : 0;
    double avgOzone = avgOzoneRaw != null ? avgOzoneRaw : 0;
    double highestCo = highestCoRaw != null ? highestCoRaw : 0;
    double lowestCo = lowestCoRaw != null ? lowestCoRaw : 0;
    double highestOzone = highestOzoneRaw != null ? highestOzoneRaw : 0;
    double lowestOzone = lowestOzoneRaw != null ? lowestOzoneRaw : 0;

    Map<String, Long> breakdown = Map.of(
        "Good", airRepo.countByPollutionLevel(PollutionLevel.Good),
        "Moderate", airRepo.countByPollutionLevel(PollutionLevel.Moderate),
        "Unhealthy", airRepo.countByPollutionLevel(PollutionLevel.Unhealthy),
        "Very_Unhealthy", airRepo.countByPollutionLevel(PollutionLevel.Very_Unhealthy),
        "Hazardous", airRepo.countByPollutionLevel(PollutionLevel.Hazardous));

    return new AirStatsDto(totalRecords, avgCo, avgOzone,
            highestCo, lowestCo, highestOzone, lowestOzone, totalAlerts, breakdown);
}

public List<AirTrendDto> getAirTrends() {
    return airRepo.findTop50ByOrderByTimestampDesc()
            .stream()
            .map(data -> new AirTrendDto(
                    data.getTimestamp(),
                    data.getCo(),
                    data.getOzone()))
            .toList();
}

public Page<StreetLightData> getLightData(
        String location,
        Status status,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable) {
    Specification<StreetLightData> spec = Specification.where(null);

    if (location != null && !location.trim().isEmpty()) {
        spec = spec.and(
                (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
    }
    if (status != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (from != null) {
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
    }
    if (to != null) {
        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
    }

    return lightRepo.findAll(spec, pageable);
}

public LightStatsDto getLightStats() {
    long totalRecords = lightRepo.count();
    long totalAlerts = notificationRepository.countByType("Light");

    Double avgBrightnessRaw = lightRepo.findAvgBrightnessLevel();
    Double avgPowerRaw = lightRepo.findAvgPowerConsumption();
    Double highestPowerRaw = lightRepo.findMaxPowerConsumption();
    Double lowestBrightnessRaw = lightRepo.findMinBrightnessLevel();

    double avgBrightness = avgBrightnessRaw != null ? avgBrightnessRaw : 0;
    double avgPower = avgPowerRaw != null ? avgPowerRaw : 0;
    double highestPower = highestPowerRaw != null ? highestPowerRaw : 0;
    double lowestBrightness = lowestBrightnessRaw != null ? lowestBrightnessRaw : 0;

    Map<String, Long> statusBreakdown = Map.of(
            "ON", lightRepo.countByStatus(Status.ON),
            "OFF", lightRepo.countByStatus(Status.OFF));

    return new LightStatsDto(totalRecords, avgBrightness, avgPower,
            highestPower, lowestBrightness, totalAlerts, statusBreakdown);
}

public List<LightTrendDto> getLightTrends() {
    return lightRepo.findTop50ByOrderByTimestampDesc()
            .stream()
            .map(data -> new LightTrendDto(
                    data.getTimestamp(),
                    data.getBrightnessLevel(),
                    data.getPowerConsumption()))
            .toList();
}

}
