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
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

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

    // @GetMapping("/traffic")
    // @Operation(summary = "Get traffic sensor data", description = "Retrieves
    // traffic sensor readings with optional filtering by location, congestion
    // level, date range, sorting, and pagination.")
    // public ResponseEntity<Page<TrafficData>> getTrafficData(
    // @RequestParam(required = false) String location,

    // @RequestParam(required = false) CongestionLevel congestionLevel,

    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

    // Pageable pageable) {
    // Page<TrafficData> trafficData = sensorDataService.getTrafficData(
    // location,
    // congestionLevel,
    // from,
    // to,
    // pageable);
    // return ResponseEntity.ok(trafficData);
    // }
    @GetMapping("/traffic")
    @Operation(summary = "Get traffic sensor data", description = "Retrieves traffic sensor readings with optional filtering by location, congestion level, date range, sorting, and pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traffic data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters")
    })
    public ResponseEntity<Page<TrafficData>> getTrafficData(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) CongestionLevel congestionLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        Page<TrafficData> trafficData = sensorDataService.getTrafficData(
                location,
                congestionLevel,
                from,
                to,
                pageable);
        return ResponseEntity.ok(trafficData);
    }

    @GetMapping("/traffic/stats")
    @Operation(summary = "Get traffic dashboard statistics", description = "Returns aggregated traffic statistics for dashboard charts and summary cards.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traffic statistics retrieved successfully")
    })
    public ResponseEntity<TrafficStatsDto> getTrafficStats() {

        TrafficStatsDto stats = sensorDataService.getTrafficStats();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/traffic/trends")
    @Operation(summary = "Get traffic trend data", description = "Returns recent traffic density and average speed readings ordered by timestamp for dashboard charts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traffic trend data retrieved successfully")
    })
    public ResponseEntity<List<TrafficTrendDto>> getTrafficTrends() {
        return ResponseEntity.ok(sensorDataService.getTrafficTrends());
    }

    @GetMapping("/traffic/congestion-summary")
    @Operation(summary = "Get traffic congestion summary", description = "Returns count of traffic records grouped by congestion level for dashboard charts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traffic congestion summary retrieved successfully")
    })
    public ResponseEntity<Map<String, Long>> getTrafficCongestionSummary() {
        return ResponseEntity.ok(sensorDataService.getTrafficCongestionSummary());
    }
}
