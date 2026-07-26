package com.backend.sensor_data.service.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.backend.sensor_data.service.processor.AirPollutionProcessor;
import com.backend.sensor_data.service.processor.StreetLightProcessor;
import com.backend.sensor_data.service.processor.TrafficProcessor;

class SensorProcessorFactoryTest {

    private final AirPollutionProcessor airProcessor = mock(AirPollutionProcessor.class);
    private final StreetLightProcessor lightProcessor = mock(StreetLightProcessor.class);
    private final TrafficProcessor trafficProcessor = mock(TrafficProcessor.class);

    private final SensorProcessorFactory factory =
            new SensorProcessorFactory(airProcessor, lightProcessor, trafficProcessor);

    @Test
    void getProcessor_air_returnsAirProcessor() {
        assertSame(airProcessor, factory.getProcessor("AIR"));
        assertSame(airProcessor, factory.getProcessor("air"));
    }

    @Test
    void getProcessor_light_returnsLightProcessor() {
        assertSame(lightProcessor, factory.getProcessor("LIGHT"));
    }

    @Test
    void getProcessor_traffic_returnsTrafficProcessor() {
        assertSame(trafficProcessor, factory.getProcessor("TRAFFIC"));
    }

    @Test
    void getProcessor_unknownType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> factory.getProcessor("UNKNOWN"));
    }
}
