package com.backend.sensor_data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "traffic_sensors_data", indexes = {
        @Index(name = "idx_location", columnList = "location"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_congestion", columnList = "congestionLevel")
})
public class TrafficData extends BaseSensorData {

    @Min(0)
    @Max(500)
    private Integer trafficDensity;

    @Min(0)
    @Max(120)
    private Float avgSpeed;

    @Enumerated(EnumType.STRING)
    @NotNull
    private CongestionLevel congestionLevel;
}
