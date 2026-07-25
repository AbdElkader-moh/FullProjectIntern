package com.backend.sensor_data.service.factory;

import org.springframework.stereotype.Component;

import com.backend.sensor_data.service.processor.AbstractSensorProcessor;
import com.backend.sensor_data.service.processor.AirPollutionProcessor;
import com.backend.sensor_data.service.processor.StreetLightProcessor;
import com.backend.sensor_data.service.processor.TrafficProcessor;

@Component
public class SensorProcessorFactory {

    private final AirPollutionProcessor airProcessor;
    private final StreetLightProcessor lightProcessor;
    private final TrafficProcessor trafficProcessor;

    public SensorProcessorFactory(AirPollutionProcessor airProcessor,
            StreetLightProcessor lightProcessor, TrafficProcessor trafficProcessor ) {
        this.airProcessor = airProcessor;
        this.lightProcessor = lightProcessor;
        this.trafficProcessor = trafficProcessor;
    }

    @SuppressWarnings("unchecked")
    public <D> AbstractSensorProcessor<D, ?> getProcessor(String type) {
        return switch (type.toUpperCase()) {
            case "AIR"   -> (AbstractSensorProcessor<D, ?>) airProcessor;
            case "LIGHT" -> (AbstractSensorProcessor<D, ?>) lightProcessor;
            case "TRAFFIC" -> (AbstractSensorProcessor<D, ?>) trafficProcessor;
            default -> throw new IllegalArgumentException("Unknown sensor type: " + type);
        };
    }
}