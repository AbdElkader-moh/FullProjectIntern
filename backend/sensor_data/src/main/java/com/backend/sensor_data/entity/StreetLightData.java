package com.backend.sensor_data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "street_light_sensors_data")
public class StreetLightData {
    @Id
    private String id;

    @NotBlank
    private String location;

    @Column(updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now(ZoneId.of("UTC"));

    @Min(0) @Max(100)
    private Integer brightnessLevel;

    @Min(0) @Max(5000)
    private Float powerConsumption;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    public StreetLightData() { this.id = java.util.UUID.randomUUID().toString(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Integer getBrightnessLevel() { return brightnessLevel; }
    public void setBrightnessLevel(Integer brightnessLevel) { this.brightnessLevel = brightnessLevel; }
    public Float getPowerConsumption() { return powerConsumption; }
    public void setPowerConsumption(Float powerConsumption) { this.powerConsumption = powerConsumption; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
