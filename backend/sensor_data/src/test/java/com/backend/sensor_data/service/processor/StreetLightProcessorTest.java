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

import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.service.strategy.LightThresholdStrategy;

/**
 * Covers StreetLightProcessor's 6 validation checks (each with a boundary case),
 * plus mapToEntity, save, checkThreshold, and broadcast.
 *
 * NOTE: validate/mapToEntity/save/checkThreshold/broadcast are `protected` and this
 * test class lives in the same package, so they're exercised directly rather than
 * only indirectly through the (not-shown) AbstractSensorProcessor.process() template method.
 * If a ProcessorTest base/integration test already covers the full process() flow,
 * treat this as the unit-level complement to it.
 */
@ExtendWith(MockitoExtension.class)
class StreetLightProcessorTest {

    @Mock
    private LightThresholdStrategy strategy;

    @Mock
    private StreetLightDataRepository lightRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private StreetLightProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new StreetLightProcessor(strategy, lightRepo, messagingTemplate);
    }

    private StreetLightDataDto validDto() {
        StreetLightDataDto dto = new StreetLightDataDto();
        dto.setLocation("Main St & 5th Ave");
        dto.setStatus(com.backend.sensor_data.entity.Status.ON);
        dto.setBrightnessLevel(75);
        dto.setPowerConsumption(1200.0f);
        return dto;
    }

    // ---------------- validate() ----------------

    @Test
    void validate_validDto_doesNotThrow() {
        assertThatCode(() -> processor.validate(validDto())).doesNotThrowAnyException();
    }

    @Test
    void validate_nullLocation_throws() {
        StreetLightDataDto dto = validDto();
        dto.setLocation(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location cannot be blank");
    }

    @Test
    void validate_blankLocation_throws() {
        StreetLightDataDto dto = validDto();
        dto.setLocation("   ");

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location cannot be blank");
    }

    @Test
    void validate_nullStatus_throws() {
        StreetLightDataDto dto = validDto();
        dto.setStatus(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status cannot be null");
    }

    @Test
    void validate_nullBrightness_throws() {
        StreetLightDataDto dto = validDto();
        dto.setBrightnessLevel(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Brightness level is required");
    }

    @Test
    void validate_brightnessBelowZero_throws() {
        StreetLightDataDto dto = validDto();
        dto.setBrightnessLevel(-1);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Brightness must be between 0 and 100");
    }

    @Test
    void validate_brightnessAboveHundred_throws() {
        StreetLightDataDto dto = validDto();
        dto.setBrightnessLevel(101);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Brightness must be between 0 and 100");
    }

    @Test
    void validate_brightnessAtLowerBoundZero_isValid() {
        StreetLightDataDto dto = validDto();
        dto.setBrightnessLevel(0);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_brightnessAtUpperBoundHundred_isValid() {
        StreetLightDataDto dto = validDto();
        dto.setBrightnessLevel(100);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_nullPowerConsumption_throws() {
        StreetLightDataDto dto = validDto();
        dto.setPowerConsumption(null);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Power consumption is required");
    }

    @Test
    void validate_powerConsumptionBelowZero_throws() {
        StreetLightDataDto dto = validDto();
        dto.setPowerConsumption(-0.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Power consumption must be between 0 and 5000");
    }

    @Test
    void validate_powerConsumptionAboveFiveThousand_throws() {
        StreetLightDataDto dto = validDto();
        dto.setPowerConsumption(5000.1f);

        assertThatThrownBy(() -> processor.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Power consumption must be between 0 and 5000");
    }

    @Test
    void validate_powerConsumptionAtLowerBoundZero_isValid() {
        StreetLightDataDto dto = validDto();
        dto.setPowerConsumption(0.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_powerConsumptionAtUpperBoundFiveThousand_isValid() {
        StreetLightDataDto dto = validDto();
        dto.setPowerConsumption(5000.0f);

        assertThatCode(() -> processor.validate(dto)).doesNotThrowAnyException();
    }

    // ---------------- mapToEntity() ----------------

    @Test
    void mapToEntity_copiesAllFieldsCorrectly() {
        StreetLightDataDto dto = validDto();

        StreetLightData entity = processor.mapToEntity(dto);

        assertThat(entity.getLocation()).isEqualTo(dto.getLocation());
        assertThat(entity.getBrightnessLevel()).isEqualTo(dto.getBrightnessLevel());
        assertThat(entity.getPowerConsumption()).isEqualTo(dto.getPowerConsumption());
        assertThat(entity.getStatus()).isEqualTo(dto.getStatus());
    }

    // ---------------- save() ----------------

    @Test
    void save_delegatesToRepository() {
        StreetLightData entity = new StreetLightData();

        processor.save(entity);

        verify(lightRepo).save(entity);
    }

    // ---------------- checkThreshold() ----------------

    @Test
    void checkThreshold_delegatesToStrategyWithEntityLocation() {
        StreetLightData entity = new StreetLightData();
        entity.setLocation("Main St & 5th Ave");

        processor.checkThreshold(entity);

        verify(strategy).check(entity, "Main St & 5th Ave");
    }

    // ---------------- broadcast() ----------------

    @Test
    void broadcast_sendsToLightTopic() {
        StreetLightData entity = new StreetLightData();

        processor.broadcast(entity);

        verify(messagingTemplate).convertAndSend("/topic/light", entity);
    }
}
