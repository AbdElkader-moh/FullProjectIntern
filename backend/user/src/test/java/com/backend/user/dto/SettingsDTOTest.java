package com.backend.user.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers every branch of SettingsDTO.validateThreshold():
 * - all 3 sensor types x their 2 metrics each, including both alias forms
 *   ("trafficDensity" vs "Traffic Density", etc.)
 * - boundary values (exact min / exact max / just outside each side)
 * - unknown metric per type, unknown type entirely
 * - null thresholdValue
 * - alertType validation (valid, case-insensitive, invalid)
 * - basic getter/setter/constructor coverage
 */
class SettingsDTOTest {

    private SettingsDTO dto(String type, String metric, Float threshold, String alertType) {
        return new SettingsDTO(null, type, metric, threshold, alertType);
    }

    // ---------- Traffic / trafficDensity (0-500) ----------

    // Sonar's S1612 ("replace lambda with method reference") is a false positive here:
    // assertDoesNotThrow has two overloads (Executable and ThrowingSupplier<T>), and a bare
    // void method reference like s::validateThreshold is ambiguous between them — it fails
    // to compile (confirmed via mvn test). The lambda block disambiguates to Executable.
    // Suppressed rather than "fixed" back into a build break; see java:S2143/JwtUtil
    // precedent in SonarQube-Fixes-JwtUtil-and-Duplicated-Literals-Log.md for the same pattern.
    @SuppressWarnings("java:S1612")
    @ParameterizedTest
    @CsvSource({
            "trafficDensity, 0",
            "trafficDensity, 500",
            "trafficDensity, 250",
            "Traffic Density, 0",
            "Traffic Density, 500"
    })
    void trafficDensity_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Traffic", metric, value, "above");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void trafficDensity_belowMin_throws() {
        SettingsDTO s = dto("Traffic", "trafficDensity", -1f, "above");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("Threshold for trafficDensity must be between 0 and 500.", ex.getMessage());
    }

    @Test
    void trafficDensity_aboveMax_throws() {
        SettingsDTO s = dto("Traffic", "trafficDensity", 501f, "above");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    // ---------- Traffic / avgSpeed (0-120) ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({
            "avgSpeed, 0",
            "avgSpeed, 120",
            "Avg Speed, 60",
            "Average Speed, 120"
    })
    void avgSpeed_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Traffic", metric, value, "below");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void avgSpeed_aboveMax_throws() {
        SettingsDTO s = dto("Traffic", "avgSpeed", 121f, "below");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    @Test
    void unknownTrafficMetric_throws() {
        SettingsDTO s = dto("Traffic", "notARealMetric", 10f, "above");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("Unknown Traffic metric: notARealMetric", ex.getMessage());
    }

    // ---------- Air / co (0-50) ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({
            "co, 0",
            "co, 50",
            "Carbon Monoxide, 25"
    })
    void co_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Air", metric, value, "above");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void co_belowMin_throws() {
        SettingsDTO s = dto("Air", "co", -0.1f, "above");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    // ---------- Air / ozone (0-300) ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({
            "ozone, 0",
            "ozone, 300",
            "Ozone, 150"
    })
    void ozone_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Air", metric, value, "below");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void ozone_aboveMax_throws() {
        SettingsDTO s = dto("Air", "ozone", 300.5f, "below");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    @Test
    void unknownAirMetric_throws() {
        SettingsDTO s = dto("Air", "smog", 10f, "above");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("Unknown Air metric: smog", ex.getMessage());
    }

    // ---------- Light / brightnessLevel (0-100) ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({
            "brightnessLevel, 0",
            "brightnessLevel, 100",
            "Brightness Level, 50"
    })
    void brightnessLevel_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Light", metric, value, "above");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void brightnessLevel_aboveMax_throws() {
        SettingsDTO s = dto("Light", "brightnessLevel", 101f, "above");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    // ---------- Light / powerConsumption (0-5000) ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({
            "powerConsumption, 0",
            "powerConsumption, 5000",
            "Power Consumption, 2500"
    })
    void powerConsumption_withinRange_validAliases_noException(String metric, float value) {
        SettingsDTO s = dto("Light", metric, value, "below");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    @Test
    void powerConsumption_belowMin_throws() {
        SettingsDTO s = dto("Light", "powerConsumption", -1f, "below");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    @Test
    void unknownLightMetric_throws() {
        SettingsDTO s = dto("Light", "flicker", 10f, "above");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("Unknown Light metric: flicker", ex.getMessage());
    }

    // ---------- Sensor type itself ----------

    @Test
    void unknownSensorType_throws() {
        SettingsDTO s = dto("Weather", "humidity", 10f, "above");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("Unknown sensor type: Weather", ex.getMessage());
    }

    // ---------- thresholdValue null ----------

    @Test
    void nullThresholdValue_throws() {
        SettingsDTO s = dto("Traffic", "trafficDensity", null, "above");
        assertThrows(IllegalArgumentException.class, s::validateThreshold);
    }

    // ---------- alertType validation ----------

    @Test
    void alertType_invalid_throws() {
        SettingsDTO s = dto("Traffic", "trafficDensity", 100f, "sideways");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, s::validateThreshold);
        assertEquals("alertType must be 'above' or 'below'.", ex.getMessage());
    }

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @ParameterizedTest
    @CsvSource({"above", "ABOVE", "Above", "below", "BELOW", "Below"})
    void alertType_caseInsensitive_valid(String alertType) {
        SettingsDTO s = dto("Traffic", "trafficDensity", 100f, alertType);
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    // ---------- overload delegating to instance fields ----------

    @SuppressWarnings("java:S1612") // see explanation above trafficDensity_withinRange_validAliases_noException
    @Test
    void validateThreshold_noArgOverload_delegatesToInstanceFields() {
        SettingsDTO s = dto("Air", "ozone", 100f, "above");
        assertDoesNotThrow(() -> { s.validateThreshold(); });
    }

    // ---------- getters / setters / constructors ----------

    @Test
    void noArgConstructor_andSetters_workAsExpected() {
        SettingsDTO s = new SettingsDTO();
        s.setId("id-1");
        s.setType("Air");
        s.setMetric("ozone");
        s.setThresholdValue(42f);
        s.setAlertType("above");

        assertEquals("id-1", s.getId());
        assertEquals("Air", s.getType());
        assertEquals("ozone", s.getMetric());
        assertEquals(42f, s.getThresholdValue());
        assertEquals("above", s.getAlertType());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        SettingsDTO s = new SettingsDTO("id-2", "Light", "brightnessLevel", 10f, "below");

        assertEquals("id-2", s.getId());
        assertEquals("Light", s.getType());
        assertEquals("brightnessLevel", s.getMetric());
        assertEquals(10f, s.getThresholdValue());
        assertEquals("below", s.getAlertType());
    }
}
