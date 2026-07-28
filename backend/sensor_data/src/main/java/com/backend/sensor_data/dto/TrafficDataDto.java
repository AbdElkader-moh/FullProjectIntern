package com.backend.sensor_data.dto;

import com.backend.sensor_data.entity.CongestionLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrafficDataDto extends BaseSensorDataDto {
    private Integer trafficDensity;
    private Float avgSpeed;
    private CongestionLevel congestionLevel;
}
