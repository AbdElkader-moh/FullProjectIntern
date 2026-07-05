package com.backend.sensor_data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "AirTrendDto", description = "A single air pollution trend data point for dashboard time-series charts")
public class AirTrendDto {

    @Schema(description = "Timestamp of the sensor reading in ISO 8601 format", example = "2026-06-30T14:35:00")
    private final LocalDateTime timestamp;

    @Schema(description = "Carbon monoxide level recorded at this timestamp in ppm", example = "1.2", minimum = "0")
    private final Float co;

    @Schema(description = "Ozone level recorded at this timestamp in ppm", example = "0.6", minimum = "0")
    private final Float ozone;

    public AirTrendDto(LocalDateTime timestamp, Float co, Float ozone) {
        this.timestamp = timestamp;
        this.co = co;
        this.ozone = ozone;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Float getCo() { return co; }
    public Float getOzone() { return ozone; }
}