package com.backend.sensor_data.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.service.strategy.AirThresholdStrategy;

/**
 * Covers AirPollutionProcessor's 4 validation checks (each with a boundary
 * case for co/ozone), plus mapToEntity, save, checkThreshold, and broadcast.
 *
 * getPollutionLevel()/setPollutionLevel() take the PollutionLevel enum
 * (Good, Moderate, Unhealthy, Very_Unhealthy, Hazardous), confirmed against
 * com.backend.sensor_data.entity.PollutionLevel.
 */
@ExtendWith(MockitoExtension.class)
class AirPollutionProcessorTest {

    @Mock
    private AirThresholdStrategy strategy;

    @Mock
    private AirPollutionDataRepository airRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private AirPollutionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AirPollutionProcessor(strategy, airRepo, messagingTemplate);
    }

    private AirPollutionDataDto validDto() {
        AirPollutionDataDto dto = new AirPollutionDataDto();
        dto.setLocation("Main St & 5th Ave");
        dto.setPollutionLevel(PollutionLevel.Moderate);
        dto.setCo(25.0f);
        dto.setOzone(150.0f);
        dto.setPm25(35.0f);
        dto.setPm10(50.0f);
        dto.setNo2(40.0f);
        dto.setSo2(10.0f);
        return dto;
    }

    // ---------------- validate() ----------------

    @Test
    void validate_validDto_doesNotThrow() {
        assertThatCode(() -> processor.validate(validDto())).doesNotThrowAnyException();
    }

    @Test
    void validate_nullLocation_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setLocation(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location cannot be blank");
    }

    @Test
    void validate_blankLocation_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setLocation("   ");

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location cannot be blank");
    }

    @Test
    void validate_nullPollutionLevel_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setPollutionLevel(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pollution level cannot be null");
    }

    @Test
    void validate_nullCo_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CO level is required");
    }

    @Test
    void validate_coBelowZero_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(-0.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CO must be between 0 and 50 ppm");
    }

    @Test
    void validate_coAboveFifty_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(50.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CO must be between 0 and 50 ppm");
    }

    @Test
    void validate_coAtLowerBoundZero_isValid() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(0.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_coAtUpperBoundFifty_isValid() {
        AirPollutionDataDto dto = validDto();
        dto.setCo(50.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_nullOzone_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setOzone(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ozone level is required");
    }

    @Test
    void validate_ozoneBelowZero_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setOzone(-0.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ozone must be between 0 and 300 ppb");
    }

    @Test
    void validate_ozoneAboveThreeHundred_throws() {
        AirPollutionDataDto dto = validDto();
        dto.setOzone(300.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ozone must be between 0 and 300 ppb");
    }

    @Test
    void validate_ozoneAtLowerBoundZero_isValid() {
        AirPollutionDataDto dto = validDto();
        dto.setOzone(0.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_ozoneAtUpperBoundThreeHundred_isValid() {
        AirPollutionDataDto dto = validDto();
        dto.setOzone(300.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    // ---------------- mapToEntity() ----------------

    @Test
    void mapToEntity_copiesAllFieldsCorrectly() {
        AirPollutionDataDto dto = validDto();

        AirPollutionData entity = processor.mapToEntity(dto);

        assertThat(entity.getLocation()).isEqualTo(dto.getLocation());
        assertThat(entity.getCo()).isEqualTo(dto.getCo());
        assertThat(entity.getOzone()).isEqualTo(dto.getOzone());
        assertThat(entity.getPm25()).isEqualTo(dto.getPm25());
        assertThat(entity.getPm10()).isEqualTo(dto.getPm10());
        assertThat(entity.getNo2()).isEqualTo(dto.getNo2());
        assertThat(entity.getSo2()).isEqualTo(dto.getSo2());
        assertThat(entity.getPollutionLevel()).isEqualTo(dto.getPollutionLevel());
    }

    // ---------------- save() ----------------

    @Test
    void save_delegatesToRepository() {
        AirPollutionData entity = new AirPollutionData();

        processor.save(entity);

        verify(airRepo).save(entity);
    }

    // ---------------- checkThreshold() ----------------

    @Test
    void checkThreshold_delegatesToStrategyWithEntityLocation() {
        AirPollutionData entity = new AirPollutionData();
        entity.setLocation("Main St & 5th Ave");

        processor.checkThreshold(entity);

        verify(strategy).check(entity, "Main St & 5th Ave");
    }

    // ---------------- broadcast() ----------------

    @Test
    void broadcast_sendsToAirTopic() {
        AirPollutionData entity = new AirPollutionData();

        processor.broadcast(entity);

        verify(messagingTemplate).convertAndSend("/topic/air", entity);
    }
}
