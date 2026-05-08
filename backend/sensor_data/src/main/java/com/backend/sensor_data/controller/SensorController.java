package com.backend.sensor_data.controller;

import com.backend.sensor_data.dto.*;
import com.backend.sensor_data.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensor Data Ingestion", description = "Endpoints for receiving IoT sensor telemetry — traffic, air quality, and street lighting")
public class SensorController {
    private static final Logger logger = LoggerFactory.getLogger(SensorController.class);

    private final SensorDataService sensorDataService;

    public SensorController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @PostMapping("/traffic")
    @Operation(summary = "Submit traffic sensor data", description = "Persists a traffic sensor reading and checks user-configured alert thresholds for trafficDensity and avgSpeed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Traffic data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveTrafficData(@RequestBody TrafficDataDto data) {
        sensorDataService.saveTrafficData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Traffic data saved successfully.");
    }

    @PostMapping("/air")
    @Operation(summary = "Submit air pollution sensor data", description = "Persists an air quality sensor reading and checks user-configured alert thresholds for CO and ozone levels.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Air pollution data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveAirData(@RequestBody AirPollutionDataDto data) {
        sensorDataService.saveAirPollutionData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Air pollution data saved successfully.");
    }

    @PostMapping("/light")
    @Operation(summary = "Submit street light sensor data", description = "Persists a street light sensor reading and checks user-configured alert thresholds for brightnessLevel and powerConsumption.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Street light data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveLightData(@RequestBody StreetLightDataDto data) {
        sensorDataService.saveStreetLightData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Street light data saved successfully.");
    }
}
