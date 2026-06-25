package com.backend.sensor_data.dto;

import java.time.LocalDateTime;

public class AirTrendDto {

    private final LocalDateTime timestamp;
    private final Float co;
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