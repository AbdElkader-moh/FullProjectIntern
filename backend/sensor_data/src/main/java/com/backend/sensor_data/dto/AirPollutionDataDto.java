package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.PollutionLevel;

public class AirPollutionDataDto {
    private String location;
    
    private Float pm2_5;
    private Float pm10;
    
    private Float co;
    
    private Float no2;
    private Float so2;
    
    private Float ozone;
    
    private PollutionLevel pollutionLevel;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
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
