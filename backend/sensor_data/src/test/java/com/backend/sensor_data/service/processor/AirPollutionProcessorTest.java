package com.backend.sensor_data.service.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.service.strategy.AirThresholdStrategy;

class AirPollutionProcessorTest {

    private AirThresholdStrategy strategy;
    private AirPollutionDataRepository airRepo;
    private SimpMessagingTemplate messagingTemplate;
    private AirPollutionProcessor processor;

    @BeforeEach
    void setUp() {
        strategy = mock(AirThresholdStrategy.class);
        airRepo = mock(AirPollutionDataRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        processor = new AirPollutionProcessor(strategy, airRepo, messagingTemplate);
    }

    private AirPollutionDataDto validDto() {
        AirPollutionDataDto dto = new AirPollutionDataDto();
        dto.setLocation("Downtown");
        dto.setCo(1.2f);
        dto.setOzone(0.6f);
        dto.setPm25(22.5f);
        dto.setPm10(40.0f);
        dto.setNo2(0.8f);
        dto.setSo2(0.4f);
        dto.setPollutionLevel(PollutionLevel.Good);
        return dto;
    }

    @Test
    void process_mapsPm25FromDtoToEntity() {
        AirPollutionDataDto dto = validDto();

        processor.process(dto);

        ArgumentCaptor<AirPollutionData> captor = ArgumentCaptor.forClass(AirPollutionData.class);
        verify(airRepo).save(captor.capture());

        AirPollutionData saved = captor.getValue();
        assertEquals(22.5f, saved.getPm25());
        assertEquals("Downtown", saved.getLocation());
        assertEquals(1.2f, saved.getCo());
        assertEquals(0.6f, saved.getOzone());
        assertEquals(40.0f, saved.getPm10());
        assertEquals(0.8f, saved.getNo2());
        assertEquals(0.4f, saved.getSo2());
        assertEquals(PollutionLevel.Good, saved.getPollutionLevel());
    }

    @Test
    void process_blankLocation_throwsIllegalArgumentException() {
        AirPollutionDataDto dto = validDto();
        dto.setLocation("  ");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(airRepo);
    }

    @Test
    void process_coOutOfRange_throwsIllegalArgumentException() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(999f);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
        verifyNoInteractions(airRepo);
    }

    @Test
    void process_broadcastsSavedEntity() {
        processor.process(validDto());
        verify(messagingTemplate).convertAndSend(eq("/topic/air"), any(AirPollutionData.class));
    }
}