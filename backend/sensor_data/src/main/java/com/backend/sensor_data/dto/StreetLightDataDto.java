package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StreetLightDataDto", description = "Payload for submitting a street light sensor reading")
public class StreetLightDataDto {

    @Schema(description = "Physical location of the street light sensor", example = "Corniche Road", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Schema(description = "Brightness level of the street light (0–100)", example = "75", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer brightnessLevel;

    @Schema(description = "Power consumption of the street light in watts", example = "45.5", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Float powerConsumption;

    @Schema(description = "Operational status of the street light", example = "ON", allowableValues = {"ON", "OFF"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private Status status;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getBrightnessLevel() { return brightnessLevel; }
    public void setBrightnessLevel(Integer brightnessLevel) { this.brightnessLevel = brightnessLevel; }

    public Float getPowerConsumption() { return powerConsumption; }
    public void setPowerConsumption(Float powerConsumption) { this.powerConsumption = powerConsumption; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}