package com.backend.sensor_data.controller;

import com.backend.sensor_data.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensor Data Ingestion", description = "Endpoints for receiving IoT sensor telemetry — traffic, air quality, and street lighting")
public class SensorController {
    private static final Logger logger = LoggerFactory.getLogger(SensorController.class);

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/traffic")
    @Transactional
    @Operation(summary = "Submit traffic sensor data", description = "Persists a traffic sensor reading and checks user-configured alert thresholds for trafficDensity and avgSpeed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Traffic data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveTrafficData(@Valid @RequestBody TrafficData data) {
        entityManager.persist(data);
        checkAlerts("Traffic", "trafficDensity", data.getTrafficDensity(), data.getLocation());
        checkAlerts("Traffic", "avgSpeed", data.getAvgSpeed(), data.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body("Traffic data saved successfully.");
    }

    @PostMapping("/air")
    @Transactional
    @Operation(summary = "Submit air pollution sensor data", description = "Persists an air quality sensor reading and checks user-configured alert thresholds for CO and ozone levels.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Air pollution data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveAirData(@Valid @RequestBody AirPollutionData data) {
        entityManager.persist(data);
        checkAlerts("Air", "co", data.getCo(), data.getLocation());
        checkAlerts("Air", "ozone", data.getOzone(), data.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body("Air pollution data saved successfully.");
    }

    @PostMapping("/light")
    @Transactional
    @Operation(summary = "Submit street light sensor data", description = "Persists a street light sensor reading and checks user-configured alert thresholds for brightnessLevel and powerConsumption.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Street light data saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields")
    })
    public ResponseEntity<?> receiveLightData(@Valid @RequestBody StreetLightData data) {
        entityManager.persist(data);
        checkAlerts("Light", "brightnessLevel", data.getBrightnessLevel(), data.getLocation());
        checkAlerts("Light", "powerConsumption", data.getPowerConsumption(), data.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body("Street light data saved successfully.");
    }

    private void checkAlerts(String type, String metric, Number value, String location) {
        String query = "SELECT threshold_value, alert_type, user_id FROM settings WHERE type = :type AND metric = :metric";
        List<Object[]> settings = entityManager.createNativeQuery(query)
            .setParameter("type", type)
            .setParameter("metric", metric)
            .getResultList();

        for (Object[] setting : settings) {
            float threshold = ((Number) setting[0]).floatValue();
            String alertType = (String) setting[1];
            Long userId = ((Number) setting[2]).longValue();
            
            boolean trigger = false;
            if ("above".equalsIgnoreCase(alertType) && value.floatValue() > threshold) { trigger = true; }
            if ("below".equalsIgnoreCase(alertType) && value.floatValue() < threshold) { trigger = true; }
            
            if (trigger) {
                logger.warn("[ALERT] {} {} ({}) exceeded threshold ({}) at Location: {}", type, metric, value, threshold, location);
                Notification notification = new Notification(userId, type, metric, value.floatValue(), threshold, alertType, location);
                entityManager.persist(notification);
            }
        }
    }
}
