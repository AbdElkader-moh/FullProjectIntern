package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.PollutionLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "AirPollutionDataDto", description = "Payload for submitting an air pollution sensor reading")
public class AirPollutionDataDto extends BaseSensorDataDto {

    @Schema(description = "PM2.5 particulate matter concentration in µg/m³", example = "22.5", minimum = "0")
    @JsonProperty("pm2_5")
    private Float pm25;

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

    @JsonProperty("pm2_5")
    public Float getPm25() { return pm25; }

    @JsonProperty("pm2_5")
    public void setPm25(Float pm25) { this.pm25 = pm25; }
}
