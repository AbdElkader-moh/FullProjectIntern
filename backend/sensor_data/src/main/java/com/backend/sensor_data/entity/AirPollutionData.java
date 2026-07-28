package com.backend.sensor_data.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "air_pollution_sensors_data")
public class AirPollutionData extends BaseSensorData {

    @Column(name = "pm2_5")
    private Float pm25;

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

    @JsonProperty("pm2_5")
    public Float getPm25() { return pm25; }

    @JsonProperty("pm2_5")
    public void setPm25(Float pm25) { this.pm25 = pm25; }
}
