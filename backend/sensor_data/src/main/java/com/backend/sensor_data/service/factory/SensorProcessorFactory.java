package com.backend.sensor_data.service.factory;

import org.springframework.stereotype.Component;

import com.backend.sensor_data.service.processor.AbstractSensorProcessor;
import com.backend.sensor_data.service.processor.AirPollutionProcessor;
import com.backend.sensor_data.service.processor.StreetLightProcessor;

@Component
public class SensorProcessorFactory {

    private final AirPollutionProcessor airProcessor;
    private final StreetLightProcessor lightProcessor;

    public SensorProcessorFactory(AirPollutionProcessor airProcessor,
            StreetLightProcessor lightProcessor) {
        this.airProcessor = airProcessor;
        this.lightProcessor = lightProcessor;
    }

    @SuppressWarnings("unchecked")
    public <D> AbstractSensorProcessor<D, ?> getProcessor(String type) {
        return switch (type.toUpperCase()) {
            case "AIR"   -> (AbstractSensorProcessor<D, ?>) airProcessor;
            case "LIGHT" -> (AbstractSensorProcessor<D, ?>) lightProcessor;
            default -> throw new IllegalArgumentException("Unknown sensor type: " + type);
        };
    }
}