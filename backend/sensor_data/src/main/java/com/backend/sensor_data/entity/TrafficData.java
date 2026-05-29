package com.backend.sensor_data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_sensors_data", indexes = {
        @Index(name = "idx_location", columnList = "location"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_congestion", columnList = "congestionLevel")
})
public class TrafficData {
    @Id
    private String id;

    @NotBlank
    private String location;

    @Column(updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Min(0)
    @Max(500)
    private Integer trafficDensity;

    @Min(0)
    @Max(120)
    private Float avgSpeed;

    @Enumerated(EnumType.STRING)
    @NotNull
    private CongestionLevel congestionLevel;

    public TrafficData() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTrafficDensity() {
        return trafficDensity;
    }

    public void setTrafficDensity(Integer trafficDensity) {
        this.trafficDensity = trafficDensity;
    }

    public Float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(Float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(CongestionLevel congestionLevel) {
        this.congestionLevel = congestionLevel;
    }
}
