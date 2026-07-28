package com.backend.sensor_data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseSensorDataDto {

    @Schema(description = "Physical location of the sensor", example = "Downtown", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;
}
