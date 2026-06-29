package utils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * StreetLightApiReader — reads traffic data from the backend API for data-accuracy tests.
 *
 * WHY THIS EXISTS
 * ───────────────
 * Three dashboard tests need to compare what the Angular UI displays against
 * what the backend actually returned. The flow is:
 *
 *   1. SensorApiClient.postTrafficReading(...) seeds a known reading.
 *   2. StreetLightApiReader fetches the API response and parses the value we just seeded.
 *   3. The test opens the dashboard and asserts the UI shows the same value.
 *
 * This is "source of truth" testing — the UI must accurately represent the API,
 * not just "show something".
 *
 * ENDPOINT REFERENCE (from SensorController.java)
 * ────────────────────────────────────────────────
 *   GET /api/sensors/light          → Page<TrafficData> (table source)
 *   GET /api/sensors/light/trends   → List<TrafficTrendDto> (line + bar chart source)
 *   GET /api/sensors/light/status-summary → Map<String,Long> (distribution chart)
 *
 * These endpoints have no @PreAuthorize in SensorController — they are open.
 * No auth header needed.
 *
 * URL: localhost:8081 (host-side port from docker-compose 8081:8081).
 */
public class StreetLightApiReader {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String BASE_URL =
            ConfigReader.get("app.sensor.url", "http://localhost:8081");

    private StreetLightApiReader() {}

    // ── Traffic table ─────────────────────────────────────────────────────────

    /**
     * Returns the raw JSON string from GET /api/sensors/light
     * with sort=timestamp,desc&page=0&size=10 (matches dashboard default).
     * The first record in content[] is the most recently posted reading.
     */
    public static String getLatestTrafficPageJson() {
        return get("/api/sensors/light?sort=timestamp%2Cdesc&page=0&size=20");
    }

    /**
     * Parses the location field of the first record in the traffic page response.
     * Uses simple string extraction — no JSON library dependency.
     */
    public static String getFirstRecordLocation() {
        String json = getLatestTrafficPageJson();
        // content is an array; first element's "location" field
        String content = extractAfter(json, "\"content\":[");
        return extractJsonString(content, "location");
    }

    /**
     * Parses powerConsumption of the first (most recent) record.
     * Returns the numeric value as a string (e.g. "450").
     */
    public static String getFirstRecordPowerConsumption() {
        String json = getLatestTrafficPageJson();
        String content = extractAfter(json, "\"content\":[");
        return extractJsonNumber(content, "powerConsumption");
    }

    /**
     * Parses brightnessLevel of the first (most recent) record.
     * Returns the value as a string — may include decimal (e.g. "15.0").
     */
    public static String getFirstRecordAvgBrightnessLevel() {
        String json = getLatestTrafficPageJson();
        String content = extractAfter(json, "\"content\":[");
        return extractJsonNumber(content, "brightnessLevel");
    }

    /**
     * Parses status of the first (most recent) record.
     * Returns one of: Low, Moderate, High, Severe.
     */
    public static String getFirstRecordStatusLevel() {
        String json = getLatestTrafficPageJson();
        String content = extractAfter(json, "\"content\":[");
        return extractJsonString(content, "status");
    }

    // ── Trends (line chart + bar chart source) ────────────────────────────────

    /**
     * Returns the raw JSON array from GET /api/sensors/light/trends.
     * The component reverses this array before rendering:
     *   this.trendData = (trends ?? []).slice().reverse()
     * So the LAST element in the API response is displayed as the FIRST (leftmost) dot.
     */
    public static String getTrendsJson() {
        return get("/api/sensors/light/trends");
    }

    /**
     * Returns the count of trend data points the API currently returns.
     * Used to verify the SVG has the right number of dots/bars rendered.
     */
    public static int getTrendDataPointCount() {
        String json = getTrendsJson();
        if (json == null || json.trim().equals("[]")) return 0;
        // Count opening braces at the top level — each { is one trend point
        int count = 0;
        for (int i = 0; i < json.length(); i++) {
            if (json.charAt(i) == '{') count++;
        }
        return count;
    }

    /**
     * Parses the powerConsumption of the last element in the trends array.
     * The component reverses the array so this value appears as the FIRST
     * dot on the left side of the power line chart.
     */
    public static String getNewestTrendPowerConsumption() {
        String json = getTrendsJson();
        if (json == null || json.trim().equals("[]")) return "";
        // Last occurrence of "powerConsumption" = last array element = newest
        int lastIdx = json.lastIndexOf("\"powerConsumption\"");
        if (lastIdx < 0) return "";
        return extractJsonNumber(json.substring(lastIdx), "powerConsumption");
    }

    /**
     * Parses the brightnessLevel of the last element in the trends array.
     * Same reversal logic — appears as the first bar on the brightness chart.
     */
    public static String getNewestTrendAvgBrightnessLevel() {
        String json = getTrendsJson();
        if (json == null || json.trim().equals("[]")) return "";
        int lastIdx = json.lastIndexOf("\"brightnessLevel\"");
        if (lastIdx < 0) return "";
        return extractJsonNumber(json.substring(lastIdx), "brightnessLevel");
    }


    // ── Status summary (distribution chart source) ────────────────────────

    /**
     * Returns the raw JSON from GET /api/sensors/light/status-summary.
     * Shape: {"Low":12,"Moderate":5,"High":3,"Severe":1}
     */
    public static String getStatusSummaryJson() {
        return get("/api/sensors/light/status-summary");
    }

    /**
     * Returns the count for a specific status level from the summary.
     * @param level "Low" | "Moderate" | "High" | "Severe"
     * @return the count as a string, or "0" if not present
     */
    public static String getStatusCount(String level) {
        String json = getStatusSummaryJson();
        if (json == null) return "0";
        String val = extractJsonNumber(json, level);
        return val != null ? val : "0";
    }

    /**
     * Returns true if the status summary contains at least one non-zero level.
     * Used as a prerequisite guard before asserting specific bar widths.
     */
    public static boolean hasStatusData() {
        String json = getStatusSummaryJson();
        if (json == null || json.trim().equals("{}")) return false;
        for (String level : new String[]{"Low", "Moderate", "High", "Severe"}) {
            String val = extractJsonNumber(json, level);
            if (val != null && !val.equals("0")) return true;
        }
        return false;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private static String get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response =
                    HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("[StreetLightApiReader] GET " + path
                        + " returned HTTP " + response.statusCode());
                return null;
            }
            return response.body();
        } catch (Exception e) {
            System.err.println("[StreetLightApiReader] GET " + path
                    + " failed: " + e.getMessage());
            return null;
        }
    }

    // ── Minimal JSON helpers (no external dependency) ─────────────────────────

    /** Returns the substring of json starting after the first occurrence of marker. */
    private static String extractAfter(String json, String marker) {
        if (json == null) return "";
        int idx = json.indexOf(marker);
        return idx >= 0 ? json.substring(idx + marker.length()) : json;
    }

    /** Extracts a JSON string value: "key":"value" → value */
    static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;
        // Skip whitespace after colon
        int i = colonIdx + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        int start = i + 1;
        int end   = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /** Extracts a JSON numeric value: "key":123 or "key":12.5 → "123" or "12.5" */
    static String extractJsonNumber(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;
        int i = colonIdx + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\n')) i++;
        int start = i;
        // Read until we hit a non-numeric character (comma, }, ], whitespace)
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']' || c == ' ' || c == '\n' || c == '\r') break;
            i++;
        }
        return i > start ? json.substring(start, i) : null;
    }
}
