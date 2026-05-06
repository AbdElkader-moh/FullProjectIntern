package com.backend.sensor_data.controller;

import com.backend.sensor_data.entity.*;
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
public class SensorController {
    private static final Logger logger = LoggerFactory.getLogger(SensorController.class);

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/traffic")
    @Transactional
    public ResponseEntity<?> receiveTrafficData(@Valid @RequestBody TrafficData data) {
        entityManager.persist(data);
        checkAlerts("Traffic", "trafficDensity", data.getTrafficDensity(), data.getLocation());
        checkAlerts("Traffic", "avgSpeed", data.getAvgSpeed(), data.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body("Traffic data saved successfully.");
    }

    @PostMapping("/air")
    @Transactional
    public ResponseEntity<?> receiveAirData(@Valid @RequestBody AirPollutionData data) {
        entityManager.persist(data);
        checkAlerts("Air", "co", data.getCo(), data.getLocation());
        checkAlerts("Air", "ozone", data.getOzone(), data.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body("Air pollution data saved successfully.");
    }

    @PostMapping("/light")
    @Transactional
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
