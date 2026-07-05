package com.backend.sensor_data.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.dto.AirStatsDto;
import com.backend.sensor_data.dto.AirTrendDto;
import com.backend.sensor_data.dto.LightStatsDto;
import com.backend.sensor_data.dto.LightTrendDto;
import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.dto.TrafficStatsDto;
import com.backend.sensor_data.dto.TrafficTrendDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.service.SensorDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensor Data Ingestion", description = "Endpoints for receiving IoT sensor telemetry — traffic, air quality, and street lighting")
public class SensorController {

    private final SensorDataService sensorDataService;

    public SensorController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    // -------------------------------------------------------------------------
    // Traffic — POST
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Air — POST
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Light — POST
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Traffic — GET (existing, unchanged)
    // -------------------------------------------------------------------------

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
        return ResponseEntity.ok(sensorDataService.getTrafficData(location, congestionLevel, from, to, pageable));
    }

    @GetMapping("/traffic/stats")
    @Operation(summary = "Get traffic dashboard statistics", description = "Returns aggregated traffic statistics for dashboard charts and summary cards.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Traffic statistics retrieved successfully")
    })
    public ResponseEntity<TrafficStatsDto> getTrafficStats() {
        return ResponseEntity.ok(sensorDataService.getTrafficStats());
    }

    @GetMapping("/traffic/trends")
    @Operation(summary = "Get traffic trend data", description = "Returns the 50 most recent traffic density and average speed readings ordered by timestamp descending for dashboard charts.")
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

    // -------------------------------------------------------------------------
    // Air — GET (fully annotated)
    // -------------------------------------------------------------------------

    @GetMapping("/air")
    @Operation(
        summary = "Get air pollution sensor data",
        description = "Retrieves paginated air pollution sensor readings. Supports optional filtering by location (case-insensitive, partial match) and date range. Supports sorting via the `sort` query parameter (e.g. `sort=co,desc`)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Air pollution data retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AirPollutionDataDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter or pagination parameters — e.g. size=0, invalid date format, or unknown sort field",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\": \"Invalid request\", \"details\": \"...\"}"))
        )
    })
    public ResponseEntity<Page<AirPollutionData>> getAirData(
            @Parameter(description = "Filter by sensor location — case-insensitive partial match", example = "downtown")
            @RequestParam(required = false) String location,

            @Parameter(description = "Filter records from this datetime (inclusive). Format: `yyyy-MM-dd'T'HH:mm:ss`", example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "Filter records up to this datetime (inclusive). Format: `yyyy-MM-dd'T'HH:mm:ss`", example = "2026-06-30T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "Pagination and sorting — page number (0-based), size, and optional sort field+direction. Valid sort fields: `timestamp`, `co`, `ozone`, `pm2_5`, `pm10`, `no2`, `so2`, `location`", example = "page=0&size=20&sort=timestamp,desc")
            Pageable pageable) {
        return ResponseEntity.ok(sensorDataService.getAirData(location, from, to, pageable));
    }

    @GetMapping("/air/stats")
    @Operation(
        summary = "Get air pollution dashboard statistics",
        description = "Returns a single aggregated statistics object for the air pollution dashboard. Includes averages, min/max values for CO and ozone, total alert count, and a breakdown of record counts per pollution level."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Air pollution statistics retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AirStatsDto.class))
        )
    })
    public ResponseEntity<AirStatsDto> getAirStats() {
        return ResponseEntity.ok(sensorDataService.getAirStats());
    }

    @GetMapping("/air/trends")
    @Operation(
        summary = "Get air pollution trend data",
        description = "Returns the 50 most recent air pollution readings ordered by timestamp descending. Each record contains a timestamp, CO level, and ozone level — intended for time-series dashboard charts."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Air pollution trend data retrieved successfully — array of up to 50 records",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AirTrendDto.class)))
        )
    })
    public ResponseEntity<List<AirTrendDto>> getAirTrends() {
        return ResponseEntity.ok(sensorDataService.getAirTrends());
    }

    // -------------------------------------------------------------------------
    // Light — GET (fully annotated)
    // -------------------------------------------------------------------------

    @GetMapping("/light")
    @Operation(
        summary = "Get street light sensor data",
        description = "Retrieves paginated street light sensor readings. Supports optional filtering by location (case-insensitive, partial match), operational status, and date range. Supports sorting via the `sort` query parameter (e.g. `sort=brightnessLevel,asc`)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Street light data retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StreetLightDataDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter or pagination parameters — e.g. size=0, invalid date format, unknown sort field, or invalid status value",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\": \"Invalid request\", \"details\": \"...\"}"))
        )
    })
    public ResponseEntity<Page<StreetLightData>> getLightData(
            @Parameter(description = "Filter by sensor location — case-insensitive partial match", example = "corniche")
            @RequestParam(required = false) String location,

            @Parameter(description = "Filter by operational status of the street light", schema = @Schema(allowableValues = {"ON", "OFF"}), example = "ON")
            @RequestParam(required = false) Status status,

            @Parameter(description = "Filter records from this datetime (inclusive). Format: `yyyy-MM-dd'T'HH:mm:ss`", example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "Filter records up to this datetime (inclusive). Format: `yyyy-MM-dd'T'HH:mm:ss`", example = "2026-06-30T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "Pagination and sorting — page number (0-based), size, and optional sort field+direction. Valid sort fields: `timestamp`, `brightnessLevel`, `powerConsumption`, `status`, `location`", example = "page=0&size=20&sort=timestamp,desc")
            Pageable pageable) {
        return ResponseEntity.ok(sensorDataService.getLightData(location, status, from, to, pageable));
    }

    @GetMapping("/light/stats")
    @Operation(
        summary = "Get street light dashboard statistics",
        description = "Returns a single aggregated statistics object for the street light dashboard. Includes average brightness and power consumption, peak power consumption, lowest brightness, total alert count, and a breakdown of ON/OFF record counts."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Street light statistics retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LightStatsDto.class))
        )
    })
    public ResponseEntity<LightStatsDto> getLightStats() {
        return ResponseEntity.ok(sensorDataService.getLightStats());
    }

    @GetMapping("/light/trends")
    @Operation(
        summary = "Get street light trend data",
        description = "Returns the 50 most recent street light readings ordered by timestamp descending. Each record contains a timestamp, brightness level, and power consumption — intended for time-series dashboard charts."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Street light trend data retrieved successfully — array of up to 50 records",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LightTrendDto.class)))
        )
    })
    public ResponseEntity<List<LightTrendDto>> getLightTrends() {
        return ResponseEntity.ok(sensorDataService.getLightTrends());
    }
}