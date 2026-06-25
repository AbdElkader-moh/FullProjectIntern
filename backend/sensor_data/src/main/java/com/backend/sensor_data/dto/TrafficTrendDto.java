package com.backend.sensor_data.dto;

import java.time.LocalDateTime;

public class TrafficTrendDto {

    private LocalDateTime timestamp;
    private Integer trafficDensity;
    private Float avgSpeed;

    public TrafficTrendDto(LocalDateTime timestamp, Integer trafficDensity, Float avgSpeed) {
        this.timestamp = timestamp;
        this.trafficDensity = trafficDensity;
        this.avgSpeed = avgSpeed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getTrafficDensity() {
        return trafficDensity;
    }

    public Float getAvgSpeed() {
        return avgSpeed;
    }
}