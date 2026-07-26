package com.backend.sensor_data.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;

import jakarta.transaction.Transactional;

@Service
public class SensorDataService {

    private final SensorProcessorFactory processorFactory;

    private final TrafficDataRepository trafficRepo;
    private final AirPollutionDataRepository airRepo;
    private final StreetLightDataRepository lightRepo;

    private final NotificationRepository notificationRepository;

    private static final String LOCATION_KEY = "location";
    private static final String TIMEST_KEY = "timestamp";

    public SensorDataService(TrafficDataRepository trafficRepo, AirPollutionDataRepository airRepo,
            StreetLightDataRepository lightRepo,
            NotificationRepository notificationRepository,
            SensorProcessorFactory processorFactory) {
        this.trafficRepo = trafficRepo;
        this.airRepo = airRepo;
        this.lightRepo = lightRepo;
        this.notificationRepository = notificationRepository;
        this.processorFactory = processorFactory;
    }

    @Transactional
    public void saveTrafficData(TrafficDataDto dto) {
        processorFactory.<TrafficDataDto>getProcessor("TRAFFIC").process(dto);
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

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }

        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get(LOCATION_KEY)), "%" + location.toLowerCase() + "%"));
        }

        if (congestionLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("congestionLevel"), congestionLevel));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(TIMEST_KEY), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(TIMEST_KEY), to));
        }

        return trafficRepo.findAll(spec, pageable);
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
            PollutionLevel pollutionLevel,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<AirPollutionData> spec = Specification.where(null);

        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }

        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and((root, query, cb)
                    -> cb.like(cb.lower(root.get(LOCATION_KEY)), "%" + location.toLowerCase() + "%"));
        }
        if (pollutionLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pollutionLevel"), pollutionLevel));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(TIMEST_KEY), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(TIMEST_KEY), to));
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            spec = spec.and((root, query, cb) -> {
                List<jakarta.persistence.criteria.Order> orders = sort.stream()
                        .map(order -> order.isAscending()
                        ? cb.asc(root.get(order.getProperty()))
                        : cb.desc(root.get(order.getProperty())))
                        .toList();
                query.orderBy(orders);
                return null; // contributes no predicate, only ordering
            });
            // strip the sort from the Pageable so findAll's own QueryUtils
            // sort-translation doesn't run (and re-trigger the bug) or conflict
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
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

        return AirStatsDto.builder()
                .totalRecords(totalRecords)
                .averageCo(avgCo)
                .averageOzone(avgOzone)
                .highestCo(highestCo)
                .lowestCo(lowestCo)
                .highestOzone(highestOzone)
                .lowestOzone(lowestOzone)
                .totalAlerts(totalAlerts)
                .pollutionLevelBreakdown(breakdown)
                .build();
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

        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }

        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get(LOCATION_KEY)), "%" + location.toLowerCase() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(TIMEST_KEY), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(TIMEST_KEY), to));
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
