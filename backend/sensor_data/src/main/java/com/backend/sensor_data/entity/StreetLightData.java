package com.backend.sensor_data.entity;

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
@Table(name = "street_light_sensors_data")
public class StreetLightData extends BaseSensorData {

    @Min(0) @Max(100)
    private Integer brightnessLevel;

    @Min(0) @Max(5000)
    private Float powerConsumption;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;
}
