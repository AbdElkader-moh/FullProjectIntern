package com.backend.sensor_data.service.processor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.service.strategy.AirThresholdStrategy;

@Component
public class AirPollutionProcessor extends AbstractSensorProcessor<AirPollutionDataDto, AirPollutionData> {

    private final AirPollutionDataRepository airRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public AirPollutionProcessor(AirThresholdStrategy strategy,
            AirPollutionDataRepository airRepo,
            SimpMessagingTemplate messagingTemplate) {
        super(strategy);
        this.airRepo = airRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void validate(AirPollutionDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty())
            throw new IllegalArgumentException("Location cannot be blank");
        if (dto.getPollutionLevel() == null)
            throw new IllegalArgumentException("Pollution level cannot be null");
        if (dto.getCo() == null)
            throw new IllegalArgumentException("CO level is required");
        if (dto.getCo() < 0 || dto.getCo() > 50)
            throw new IllegalArgumentException("CO must be between 0 and 50 ppm");
        if (dto.getOzone() == null)
            throw new IllegalArgumentException("Ozone level is required");
        if (dto.getOzone() < 0 || dto.getOzone() > 300)
            throw new IllegalArgumentException("Ozone must be between 0 and 300 ppb");
    }

    @Override
    protected AirPollutionData mapToEntity(AirPollutionDataDto dto) {
        AirPollutionData entity = new AirPollutionData();
        entity.setLocation(dto.getLocation());
        entity.setCo(dto.getCo());
        entity.setOzone(dto.getOzone());
        entity.setPm25(dto.getPm25());
        entity.setPm10(dto.getPm10());
        entity.setNo2(dto.getNo2());
        entity.setSo2(dto.getSo2());
        entity.setPollutionLevel(dto.getPollutionLevel());
        return entity;
    }

    @Override
    protected void save(AirPollutionData entity) {
        airRepo.save(entity);
    }

    @Override
    protected void checkThreshold(AirPollutionData entity) {
        strategy.check(entity, entity.getLocation());
    }

    @Override
    protected void broadcast(AirPollutionData entity) {
        messagingTemplate.convertAndSend("/topic/air", entity);
    }
}