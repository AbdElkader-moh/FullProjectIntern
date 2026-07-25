package com.backend.sensor_data.service.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.strategy.TrafficThresholdStrategy;

@ExtendWith(MockitoExtension.class)
class TrafficProcessorTest {

    @Mock private TrafficThresholdStrategy strategy;
    @Mock private TrafficDataRepository trafficRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TrafficProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TrafficProcessor(strategy, trafficRepo, messagingTemplate);
    }

    private TrafficDataDto validDto() {
        TrafficDataDto dto = new TrafficDataDto();
        dto.setLocation("Alexandria");
        dto.setTrafficDensity(450);
        dto.setAvgSpeed(15.0f);
        dto.setCongestionLevel(CongestionLevel.Severe);
        return dto;
    }

    // ---------- Validation ----------

    @Test
    void process_blankLocation_throws() {
        TrafficDataDto dto = validDto();
        dto.setLocation("  ");
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_nullCongestionLevel_throws() {
        TrafficDataDto dto = validDto();
        dto.setCongestionLevel(null);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_nullTrafficDensity_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(null);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_trafficDensityAboveRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(501);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_trafficDensityBelowRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(-1);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_nullAvgSpeed_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(null);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    void process_avgSpeedAboveRange_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(121f);
        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    // ---------- Persistence ----------

    @Test
    void process_validRequest_savesEntityWithMappedFields() {
        processor.process(validDto());

        ArgumentCaptor<TrafficData> captor = ArgumentCaptor.forClass(TrafficData.class);
        verify(trafficRepo).save(captor.capture());

        TrafficData saved = captor.getValue();
        assertEquals("Alexandria", saved.getLocation());
        assertEquals(450, saved.getTrafficDensity());
        assertEquals(15.0f, saved.getAvgSpeed());
        assertEquals(CongestionLevel.Severe, saved.getCongestionLevel());
    }

    @Test
    void process_validRequest_invokesThresholdStrategy() {
        processor.process(validDto());
        verify(strategy).check(any(TrafficData.class), any(String.class));
    }
}