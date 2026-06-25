package com.backend.sensor_data.service.processor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.service.strategy.LightThresholdStrategy;

@Component
public class StreetLightProcessor extends AbstractSensorProcessor<StreetLightDataDto, StreetLightData> {

    private final StreetLightDataRepository lightRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public StreetLightProcessor(LightThresholdStrategy strategy,
            StreetLightDataRepository lightRepo,
            SimpMessagingTemplate messagingTemplate) {
        super(strategy);
        this.lightRepo = lightRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void validate(StreetLightDataDto dto) {
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty())
            throw new IllegalArgumentException("Location cannot be blank");
        if (dto.getStatus() == null)
            throw new IllegalArgumentException("Status cannot be null");
        if (dto.getBrightnessLevel() == null)
            throw new IllegalArgumentException("Brightness level is required");
        if (dto.getBrightnessLevel() < 0 || dto.getBrightnessLevel() > 100)
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        if (dto.getPowerConsumption() == null)
            throw new IllegalArgumentException("Power consumption is required");
        if (dto.getPowerConsumption() < 0 || dto.getPowerConsumption() > 5000)
            throw new IllegalArgumentException("Power consumption must be between 0 and 5000");
    }

    @Override
    protected StreetLightData mapToEntity(StreetLightDataDto dto) {
        StreetLightData entity = new StreetLightData();
        entity.setLocation(dto.getLocation());
        entity.setBrightnessLevel(dto.getBrightnessLevel());
        entity.setPowerConsumption(dto.getPowerConsumption());
        entity.setStatus(dto.getStatus());
        return entity;
    }

    @Override
    protected void save(StreetLightData entity) {
        lightRepo.save(entity);
    }

    @Override
    protected void checkThreshold(StreetLightData entity) {
        strategy.check(entity, entity.getLocation());
    }

    @Override
    protected void broadcast(StreetLightData entity) {
        messagingTemplate.convertAndSend("/topic/light", entity);
    }
}