package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.PollutionLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AirPollutionDataDto", description = "Payload for submitting an air pollution sensor reading")
public class AirPollutionDataDto {

    @Schema(description = "Physical location of the sensor", example = "Downtown", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Schema(description = "PM2.5 particulate matter concentration in µg/m³", example = "22.5", minimum = "0")
    private Float pm2_5;

    @Schema(description = "PM10 particulate matter concentration in µg/m³", example = "40.0", minimum = "0")
    private Float pm10;

    @Schema(description = "Carbon monoxide level in ppm", example = "1.2", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Float co;

    @Schema(description = "Nitrogen dioxide level in ppm", example = "0.8", minimum = "0")
    private Float no2;

    @Schema(description = "Sulfur dioxide level in ppm", example = "0.4", minimum = "0")
    private Float so2;

    @Schema(description = "Ozone level in ppm", example = "0.6", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Float ozone;

    @Schema(description = "Overall air pollution classification", example = "Good", requiredMode = Schema.RequiredMode.REQUIRED)
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