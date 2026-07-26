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

import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.strategy.TrafficThresholdStrategy;

/**
 * Covers TrafficProcessor's 4 validation checks (each with a boundary case
 * for trafficDensity/avgSpeed), plus mapToEntity, save, checkThreshold, and
 * broadcast -- the latter closing the gap H4 flagged (Traffic previously had
 * no WebSocket broadcast at all).
 *
 * getCongestionLevel()/setCongestionLevel() take the CongestionLevel enum
 * (Low, Moderate, High, Severe), confirmed against
 * com.backend.sensor_data.entity.CongestionLevel.
 * trafficDensity is Integer; avgSpeed is Float (confirmed via compiler
 * feedback -- the two fields use different numeric types).
 */
@ExtendWith(MockitoExtension.class)
class TrafficProcessorTest {

    @Mock
    private TrafficThresholdStrategy strategy;

    @Mock
    private TrafficDataRepository trafficRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TrafficProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TrafficProcessor(strategy, trafficRepo, messagingTemplate);
    }

    private TrafficDataDto validDto() {
        TrafficDataDto dto = new TrafficDataDto();
        dto.setLocation("Main St & 5th Ave");
        dto.setCongestionLevel(CongestionLevel.Moderate);
        dto.setTrafficDensity(250);
        dto.setAvgSpeed(60.0f);
        return dto;
    }

    // ---------------- validate() ----------------

    @Test
    void validate_validDto_doesNotThrow() {
        assertThatCode(() -> processor.validate(validDto())).doesNotThrowAnyException();
    }

    @Test
    void validate_nullLocation_throws() {
        TrafficDataDto dto = validDto();
        dto.setLocation(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid location: cannot be blank");
    }

    @Test
    void validate_blankLocation_throws() {
        TrafficDataDto dto = validDto();
        dto.setLocation("   ");

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid location: cannot be blank");
    }

    @Test
    void validate_nullCongestionLevel_throws() {
        TrafficDataDto dto = validDto();
        dto.setCongestionLevel(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid congestion level: cannot be null");
    }

    @Test
    void validate_nullTrafficDensity_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trafficDensity is required");
    }

    @Test
    void validate_trafficDensityBelowZero_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(-1);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid traffic density: must be between 0 and 500");
    }

    @Test
    void validate_trafficDensityAboveFiveHundred_throws() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(501);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid traffic density: must be between 0 and 500");
    }

    @Test
    void validate_trafficDensityAtLowerBoundZero_isValid() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(0);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_trafficDensityAtUpperBoundFiveHundred_isValid() {
        TrafficDataDto dto = validDto();
        dto.setTrafficDensity(500);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_nullAvgSpeed_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("avgSpeed is required");
    }

    @Test
    void validate_avgSpeedBelowZero_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(-1.0f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid average speed: must be between 0 and 120");
    }

    @Test
    void validate_avgSpeedAboveOneTwenty_throws() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(121.0f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid average speed: must be between 0 and 120");
    }

    @Test
    void validate_avgSpeedAtLowerBoundZero_isValid() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(0.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_avgSpeedAtUpperBoundOneTwenty_isValid() {
        TrafficDataDto dto = validDto();
        dto.setAvgSpeed(120.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    // ---------------- mapToEntity() ----------------

    @Test
    void mapToEntity_copiesAllFieldsCorrectly() {
        TrafficDataDto dto = validDto();

        TrafficData entity = processor.mapToEntity(dto);

        assertThat(entity.getLocation()).isEqualTo(dto.getLocation());
        assertThat(entity.getTrafficDensity()).isEqualTo(dto.getTrafficDensity());
        assertThat(entity.getAvgSpeed()).isEqualTo(dto.getAvgSpeed());
        assertThat(entity.getCongestionLevel()).isEqualTo(dto.getCongestionLevel());
    }

    // ---------------- save() ----------------

    @Test
    void save_delegatesToRepository() {
        TrafficData entity = new TrafficData();

        processor.save(entity);

        verify(trafficRepo).save(entity);
    }

    // ---------------- checkThreshold() ----------------

    @Test
    void checkThreshold_delegatesToStrategyWithEntityLocation() {
        TrafficData entity = new TrafficData();
        entity.setLocation("Main St & 5th Ave");

        processor.checkThreshold(entity);

        verify(strategy).check(entity, "Main St & 5th Ave");
    }

    // ---------------- broadcast() ----------------

    @Test
    void broadcast_sendsToTrafficTopic() {
        // Closes the H4 gap: Traffic previously had no broadcast() step at all.
        TrafficData entity = new TrafficData();

        processor.broadcast(entity);

        verify(messagingTemplate).convertAndSend("/topic/traffic", entity);
    }
}
