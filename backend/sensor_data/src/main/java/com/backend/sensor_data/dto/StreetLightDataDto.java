package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.Status;

public class StreetLightDataDto {
    private String location;
    
    private Integer brightnessLevel;
    
    private Float powerConsumption;
    
    private Status status;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getBrightnessLevel() {
        return brightnessLevel;
    }

    public void setBrightnessLevel(Integer brightnessLevel) {
        this.brightnessLevel = brightnessLevel;
    }

    public Float getPowerConsumption() {
        return powerConsumption;
    }

    public void setPowerConsumption(Float powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
