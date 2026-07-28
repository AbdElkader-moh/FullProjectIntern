package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "StreetLightDataDto", description = "Payload for submitting a street light sensor reading")
public class StreetLightDataDto extends BaseSensorDataDto {

    @Schema(description = "Brightness level of the street light (0–100)", example = "75", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer brightnessLevel;

    @Schema(description = "Power consumption of the street light in watts", example = "45.5", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Float powerConsumption;

    @Schema(description = "Operational status of the street light", example = "ON", allowableValues = {"ON", "OFF"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private Status status;
}
