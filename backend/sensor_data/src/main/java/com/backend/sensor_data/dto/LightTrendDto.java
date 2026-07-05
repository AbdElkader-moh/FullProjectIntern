package com.backend.sensor_data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "LightTrendDto", description = "A single street light trend data point for dashboard time-series charts")
public class LightTrendDto {

    @Schema(description = "Timestamp of the sensor reading in ISO 8601 format", example = "2026-06-30T14:35:00")
    private final LocalDateTime timestamp;

    @Schema(description = "Brightness level recorded at this timestamp (0–100)", example = "75", minimum = "0", maximum = "100")
    private final Integer brightnessLevel;

    @Schema(description = "Power consumption recorded at this timestamp in watts", example = "45.5", minimum = "0")
    private final Float powerConsumption;

    public LightTrendDto(LocalDateTime timestamp, Integer brightnessLevel, Float powerConsumption) {
        this.timestamp = timestamp;
        this.brightnessLevel = brightnessLevel;
        this.powerConsumption = powerConsumption;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Integer getBrightnessLevel() { return brightnessLevel; }
    public Float getPowerConsumption() { return powerConsumption; }
}