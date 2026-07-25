package com.backend.sensor_data.service.processor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.strategy.TrafficThresholdStrategy;

@Component
public class TrafficProcessor extends AbstractSensorProcessor<TrafficDataDto, TrafficData> {

    private final TrafficDataRepository trafficRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public TrafficProcessor(TrafficThresholdStrategy strategy,
            TrafficDataRepository trafficRepo,
            SimpMessagingTemplate messagingTemplate) {
        super(strategy);
        this.trafficRepo = trafficRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void validate(TrafficDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty())
            throw new IllegalArgumentException("Invalid location: cannot be blank");
        if (dto.getCongestionLevel() == null)
            throw new IllegalArgumentException("Invalid congestion level: cannot be null");
        if (dto.getTrafficDensity() == null)
            throw new IllegalArgumentException("trafficDensity is required");
        if (dto.getTrafficDensity() < 0 || dto.getTrafficDensity() > 500)
            throw new IllegalArgumentException("Invalid traffic density: must be between 0 and 500");
        if (dto.getAvgSpeed() == null)
            throw new IllegalArgumentException("avgSpeed is required");
        if (dto.getAvgSpeed() < 0 || dto.getAvgSpeed() > 120)
            throw new IllegalArgumentException("Invalid average speed: must be between 0 and 120");
    }

    @Override
    protected TrafficData mapToEntity(TrafficDataDto dto) {
        TrafficData entity = new TrafficData();
        entity.setLocation(dto.getLocation());
        entity.setTrafficDensity(dto.getTrafficDensity());
        entity.setAvgSpeed(dto.getAvgSpeed());
        entity.setCongestionLevel(dto.getCongestionLevel());
        return entity;
    }

    @Override
    protected void save(TrafficData entity) {
        trafficRepo.save(entity);
    }

    @Override
    protected void checkThreshold(TrafficData entity) {
        strategy.check(entity, entity.getLocation());
    }

    @Override
    protected void broadcast(TrafficData entity) {
        messagingTemplate.convertAndSend("/topic/traffic", entity);
    }
}