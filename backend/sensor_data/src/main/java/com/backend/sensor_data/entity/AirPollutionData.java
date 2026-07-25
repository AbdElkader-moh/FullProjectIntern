package com.backend.sensor_data.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "air_pollution_sensors_data")
public class AirPollutionData {
    @Id
    private String id;

    @NotBlank
    private String location;

    @Column(updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    private Float pm2_5;
    private Float pm10;

    @Min(0) @Max(50)
    private Float co;

    private Float no2;
    private Float so2;

    @Min(0) @Max(300)
    private Float ozone;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PollutionLevel pollutionLevel;

    public AirPollutionData() { this.id = java.util.UUID.randomUUID().toString(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Float getPm2_5() { return pm2_5; }
    public void setPm2_5(Float pm2_5) { this.pm2_5 = pm2_5; }
    public Float getPm10() { return pm10; }
    public void setPm10(Float pm10) { this.pm10 = pm10; }
    public Float getCo() { return co; }
    public void setCo(Float co) { this.co = co; }
    public Float getNo2() { return no2; }
    public void setNo2(Float no2) { this.no2 = no2; }
    public Float getSo2() { return so2; }
    public void setSo2(Float so2) { this.so2 = so2; }
    public Float getOzone() { return ozone; }
    public void setOzone(Float ozone) { this.ozone = ozone; }
    public PollutionLevel getPollutionLevel() { return pollutionLevel; }
    public void setPollutionLevel(PollutionLevel pollutionLevel) { this.pollutionLevel = pollutionLevel; }
}
