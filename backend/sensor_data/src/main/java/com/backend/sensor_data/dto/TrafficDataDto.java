package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.CongestionLevel;

public class TrafficDataDto {
    private String location;
    
    private Integer trafficDensity;
    
    private Float avgSpeed;
    
    private CongestionLevel congestionLevel;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
