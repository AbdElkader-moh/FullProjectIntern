package com.backend.sensor_data.service.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.strategy.TrafficThresholdStrategy;

class TrafficProcessorTest {

    private TrafficThresholdStrategy strategy;
    private TrafficDataRepository trafficRepo;
    private SimpMessagingTemplate messagingTemplate;
    private TrafficProcessor processor;

    @BeforeEach
    void setUp() {
        strategy = mock(TrafficThresholdStrategy.class);
        trafficRepo = mock(TrafficDataRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        processor = new TrafficProcessor(strategy, trafficRepo, messagingTemplate);
    }

    private TrafficDataDto validDto() {
        TrafficDataDto dto = new TrafficDataDto();
        dto.setLocation("Downtown");
        dto.setTrafficDensity(120);
        dto.setAvgSpeed(45.0f);
        dto.setCongestionLevel(CongestionLevel.Moderate);
        return dto;
    }

    @Test
    void process_mapsAllFieldsFromDtoToEntity() {
        processor.process(validDto());

        ArgumentCaptor<TrafficData> captor = ArgumentCaptor.forClass(TrafficData.class);
        verify(trafficRepo).save(captor.capture());

        TrafficData saved = captor.getValue();
        assertEquals("Downtown", saved.getLocation());
        assertEquals(120, saved.getTrafficDensity());
        assertEquals(45.0f, saved.getAvgSpeed());
        assertEquals(CongestionLevel.Moderate, saved.getCongestionLevel());
    }

    @Test
    void process_blankLocation_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setLocation("   ");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_nullCongestionLevel_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setCongestionLevel(null);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_nullTrafficDensity_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(null);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_trafficDensityOutOfRange_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(600);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_nullAvgSpeed_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(null);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_avgSpeedOutOfRange_throwsIllegalArgumentException() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(200.0f);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void process_checksThresholdWithSavedEntity() {
        processor.process(validDto());
        verify(strategy).check(any(TrafficData.class), eq("Downtown"));
    }

    @Test
    void process_broadcastsSavedEntity() {
        processor.process(validDto());
        verify(messagingTemplate).convertAndSend(eq("/topic/traffic"), any(TrafficData.class));
    }
}