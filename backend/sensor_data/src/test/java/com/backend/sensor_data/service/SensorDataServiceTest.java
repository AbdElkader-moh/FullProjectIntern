package com.backend.sensor_data.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.SettingsRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;
import com.backend.sensor_data.service.processor.AbstractSensorProcessor;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {

    @Mock private TrafficDataRepository trafficRepo;
    @Mock private AirPollutionDataRepository airRepo;
    @Mock private StreetLightDataRepository lightRepo;
    @Mock private SettingsRepository settingsRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SensorProcessorFactory processorFactory;
    @Mock private AbstractSensorProcessor<TrafficDataDto, TrafficData> trafficProcessor;

    private SensorDataService service;

    @BeforeEach
    void setUp() {
        service = new SensorDataService(trafficRepo, airRepo, lightRepo, settingsRepository,
                messagingTemplate, notificationRepository, processorFactory);
    }

    private <D> void stubProcessor(String type, AbstractSensorProcessor<D, ?> processor) {
        doReturn(processor).when(processorFactory).<D>getProcessor(type);
    }

    private TrafficDataDto validDto() {
        TrafficDataDto dto = new TrafficDataDto();
        dto.setLocation("Alexandria");
        dto.setTrafficDensity(450);
        dto.setAvgSpeed(15.0f);
        dto.setCongestionLevel(CongestionLevel.Severe);
        return dto;
    }

    @Test
    void saveTrafficData_delegatesToTrafficProcessor() {
        TrafficDataDto dto = validDto();
        stubProcessor("TRAFFIC", trafficProcessor);

        service.saveTrafficData(dto);

        verify(processorFactory).getProcessor("TRAFFIC");
        verify(trafficProcessor).process(dto);
    }
}