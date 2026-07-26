package com.backend.sensor_data.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.service.SensorDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensor Data Ingestion", description = "Endpoints for receiving IoT sensor telemetry — traffic, air quality, and street lighting")
public class SensorController {

    private final SensorDataService sensorDataService;
    private static final String ERROR_KEY = "error";
    private static final String DETAILS_KEY = "details";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String LOCATION_FIELD = "location";
    private static final String TIMEST_FIELD = "timestamp";

    public SensorController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    // ---------------------------------------------------------------
    // Shared helpers — pagination + sort validation used by all three
    // "list" endpoints below (getTrafficData, getAirData, getLightData).
    // Extracted here instead of a formal pattern since all three call
    // sites live in this one class; see report notes on Template Method.
    // ---------------------------------------------------------------
    /**
     * Validates page/size params. Returns an error ResponseEntity if invalid,
     * or null if the params are valid and processing should continue.
     */
    private ResponseEntity<Object> validatePagination(int page, int size) {
        if (page < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, VALIDATION_FAILED, DETAILS_KEY, "Page must be >= 0"));
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, VALIDATION_FAILED, DETAILS_KEY, "Page size must be greater than 0"));
        }
        return null;
    }

    /**
     * Parses a "field,dir" sort param against an allowed field set. Throws
     * IllegalArgumentException on an unrecognized field, which callers catch
     * and turn into a 400 response.
     */
    private Sort buildSort(String sort, Set<String> allowedSortFields) {
        return buildSort(sort, allowedSortFields, Map.of());
    }

    private Sort buildSort(String sort, Set<String> allowedSortFields, Map<String, String> fieldAliases) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        if (!allowedSortFields.contains(field)) {
            throw new IllegalArgumentException("Invalid sort field: " + field);
        }
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        String resolvedField = fieldAliases.getOrDefault(field, field);
        return Sort.by(dir, resolvedField);
    }

    @PostMapping("/traffic")
    @Operation(summary = "Submit traffic sensor data", description = "Persists a traffic sensor reading and checks user-configured alert thresholds for trafficDensity and avgSpeed.")
    @ApiResponse(responseCode = "201", description = "Traffic data saved successfully")
    @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    public ResponseEntity<String> receiveTrafficData(@RequestBody TrafficDataDto data) {
        sensorDataService.saveTrafficData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Traffic data saved successfully.");
    }

    @PostMapping("/air")
    @Operation(summary = "Submit air pollution sensor data", description = "Persists an air quality sensor reading and checks user-configured alert thresholds for CO and ozone levels.")
    @ApiResponse(responseCode = "201", description = "Air pollution data saved successfully")
    @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    public ResponseEntity<String> receiveAirData(@RequestBody AirPollutionDataDto data) {
        sensorDataService.saveAirPollutionData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Air pollution data saved successfully.");
    }

    @PostMapping("/light")
    @Operation(summary = "Submit street light sensor data", description = "Persists a street light sensor reading and checks user-configured alert thresholds for brightnessLevel and powerConsumption.")
    @ApiResponse(responseCode = "201", description = "Street light data saved successfully")
    @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    public ResponseEntity<String> receiveLightData(@RequestBody StreetLightDataDto data) {
        sensorDataService.saveStreetLightData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Street light data saved successfully.");
    }

    private static final Set<String> TRAFFIC_SORT_FIELDS = Set.of(
            TIMEST_FIELD, LOCATION_FIELD, "trafficDensity", "avgSpeed", "congestionLevel");

    @GetMapping("/traffic")
    @Operation(summary = "Get traffic sensor data", description = "Retrieves traffic sensor readings with optional filtering by location, congestion level, date range, sorting, and pagination.")
    @ApiResponse(responseCode = "200", description = "Traffic data retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters")
    public ResponseEntity<Object> getTrafficData(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) CongestionLevel congestionLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        ResponseEntity<Object> paginationError = validatePagination(page, size);
        if (paginationError != null) {
            return paginationError;
        }

        Sort sortObj;
        try {
            sortObj = buildSort(sort, TRAFFIC_SORT_FIELDS);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, VALIDATION_FAILED, DETAILS_KEY, ex.getMessage()));
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<TrafficData> trafficData = sensorDataService.getTrafficData(
                location, congestionLevel, from, to, pageable);
        return ResponseEntity.ok(trafficData);
    }

    @GetMapping("/traffic/stats")
    @Operation(summary = "Get traffic dashboard statistics", description = "Returns aggregated traffic statistics for dashboard charts and summary cards.")
    @ApiResponse(responseCode = "200", description = "Traffic statistics retrieved successfully")
    public ResponseEntity<TrafficStatsDto> getTrafficStats() {
        return ResponseEntity.ok(sensorDataService.getTrafficStats());
    }

    @GetMapping("/traffic/trends")
    @Operation(summary = "Get traffic trend data", description = "Returns recent traffic density and average speed readings ordered by timestamp for dashboard charts.")
    @ApiResponse(responseCode = "200", description = "Traffic trend data retrieved successfully")
    public ResponseEntity<List<TrafficTrendDto>> getTrafficTrends() {
        return ResponseEntity.ok(sensorDataService.getTrafficTrends());
    }

    @GetMapping("/traffic/congestion-summary")
    @Operation(summary = "Get traffic congestion summary", description = "Returns count of traffic records grouped by congestion level for dashboard charts.")
    @ApiResponse(responseCode = "200", description = "Traffic congestion summary retrieved successfully")
    public ResponseEntity<Map<String, Long>> getTrafficCongestionSummary() {
        return ResponseEntity.ok(sensorDataService.getTrafficCongestionSummary());
    }

    private static final Set<String> AIR_SORT_FIELDS = Set.of(
            TIMEST_FIELD, LOCATION_FIELD, "co", "ozone", "pollutionLevel", "pm2_5", "pm10", "no2", "so2");

    private static final Map<String, String> AIR_SORT_FIELD_ALIASES = Map.of(
        "pm2_5", "pm25");

    @GetMapping("/air")
    @Operation(summary = "Get air pollution sensor data", description = "Retrieves air pollution readings with optional filtering by location, date range, sorting, and pagination.")
    @ApiResponse(responseCode = "200", description = "Air pollution data retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters")
    public ResponseEntity<Object> getAirData(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) PollutionLevel pollutionLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        ResponseEntity<Object> paginationError = validatePagination(page, size);
        if (paginationError != null) {
            return paginationError;
        }

        Sort sortObj;
        try {
            sortObj = buildSort(sort, AIR_SORT_FIELDS, AIR_SORT_FIELD_ALIASES);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, VALIDATION_FAILED, DETAILS_KEY, ex.getMessage()));
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(sensorDataService.getAirData(location, pollutionLevel, from, to, pageable));
    }

    @GetMapping("/air/stats")
    @Operation(summary = "Get air pollution dashboard statistics", description = "Returns aggregated air pollution statistics for dashboard charts and summary cards.")
    @ApiResponse(responseCode = "200", description = "Air pollution statistics retrieved successfully")
    public ResponseEntity<AirStatsDto> getAirStats() {
        return ResponseEntity.ok(sensorDataService.getAirStats());
    }

    @GetMapping("/air/trends")
    @Operation(summary = "Get air pollution trend data", description = "Returns recent CO and ozone readings ordered by timestamp for dashboard charts.")
    @ApiResponse(responseCode = "200", description = "Air pollution trend data retrieved successfully")
    public ResponseEntity<List<AirTrendDto>> getAirTrends() {
        return ResponseEntity.ok(sensorDataService.getAirTrends());
    }

    private static final Set<String> LIGHT_SORT_FIELDS = Set.of(
            TIMEST_FIELD, LOCATION_FIELD, "brightnessLevel", "powerConsumption", "status");

    @GetMapping("/light")
    @Operation(summary = "Get street light sensor data", description = "Retrieves street light readings with optional filtering by location, status, date range, sorting, and pagination.")
    @ApiResponse(responseCode = "200", description = "Street light data retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters")
    public ResponseEntity<Object> getLightData(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        ResponseEntity<Object> paginationError = validatePagination(page, size);
        if (paginationError != null) {
            return paginationError;
        }

        Sort sortObj;
        try {
            sortObj = buildSort(sort, LIGHT_SORT_FIELDS);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, VALIDATION_FAILED, DETAILS_KEY, ex.getMessage()));
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(sensorDataService.getLightData(location, status, from, to, pageable));
    }

    @GetMapping("/light/stats")
    @Operation(summary = "Get street light dashboard statistics", description = "Returns aggregated street light statistics for dashboard charts and summary cards.")
    @ApiResponse(responseCode = "200", description = "Street light statistics retrieved successfully")
    public ResponseEntity<LightStatsDto> getLightStats() {
        return ResponseEntity.ok(sensorDataService.getLightStats());
    }

    @GetMapping("/light/trends")
    @Operation(summary = "Get street light trend data", description = "Returns recent brightness and power consumption readings ordered by timestamp for dashboard charts.")
    @ApiResponse(responseCode = "200", description = "Street light trend data retrieved successfully")
    public ResponseEntity<List<LightTrendDto>> getLightTrends() {
        return ResponseEntity.ok(sensorDataService.getLightTrends());
    }
}
