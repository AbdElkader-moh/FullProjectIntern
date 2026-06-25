package com.backend.sensor_data.dto;

import java.time.LocalDateTime;

public class LightTrendDto {

    private final LocalDateTime timestamp;
    private final Integer brightnessLevel;
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