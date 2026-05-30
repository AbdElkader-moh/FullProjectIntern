package utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * SensorApiClient — calls the sensor-service REST endpoints directly.
 *
 * WHY THIS EXISTS
 * ───────────────
 * The simulator runs on a fixed interval (traffic every 120 s in docker-compose).
 * Tests cannot wait that long and cannot control the values it sends.
 * Calling POST /api/sensors/traffic directly seeds exact, known values instantly.
 *
 * URL NOTE (docker-compose architecture)
 * ──────────────────────────────────────
 * The test machine is OUTSIDE the Docker bridge network (project-net).
 * Internal hostnames like "sensor-service" are only reachable from within Docker.
 * From the test machine, the sensor service is exposed via the host port mapping:
 *
 *   sensor-service container port 8081  →  host port 8081
 *
 * So all calls go to http://localhost:8081, NOT http://sensor-service:8081.
 *
 * Configured via config.properties: app.sensor.url=http://localhost:8081
 * Override with -Dapp.sensor.url=... if your port mapping differs.
 *
 * NO AUTH REQUIRED
 * ────────────────
 * Looking at SensorController.java: POST /api/sensors/traffic has no
 * @PreAuthorize or security annotation — it is an open endpoint designed
 * for the simulator to call without credentials. No Authorization header needed.
 */
public class SensorApiClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Sensor service base URL — host-side port from docker-compose (8081:8081). */
    private static final String BASE_URL =
            ConfigReader.get("app.sensor.url", "http://localhost:8081");

    private SensorApiClient() {}

    // ── Traffic ───────────────────────────────────────────────────────────────

    /**
     * POST /api/sensors/traffic
     *
     * Sends a traffic reading. The backend compares each metric against any
     * saved thresholds for the user and creates a notification alert if
     * a threshold is crossed.
     *
     * The simulator uses location "Alexandria" — tests use the same location
     * so filter tests that look for "Alexandria" work correctly.
     *
     * @param trafficDensity  vehicles/hr  (valid range 0–500 per backend spec)
     * @param avgSpeed        km/h         (valid range 0–120 per backend spec)
     * @param congestionLevel "Low" | "Moderate" | "High" | "Severe"
     * @param location        must match the location used in the saved threshold
     */
    public static void postTrafficReading(int trafficDensity,
                                          double avgSpeed,
                                          String congestionLevel,
                                          String location) {
        String body = String.format(
            "{\"location\":\"%s\",\"trafficDensity\":%d," +
            "\"avgSpeed\":%.2f,\"congestionLevel\":\"%s\"}",
            location, trafficDensity, avgSpeed, congestionLevel
        );
        post("/api/sensors/traffic", body);
        System.out.println("[SensorAPI] Traffic posted — density=" + trafficDensity
                + " speed=" + avgSpeed + " congestion=" + congestionLevel
                + " loc=" + location);
    }

    /**
     * Sends a high-density reading at the simulator's location (Alexandria).
     * density=450 exceeds the threshold of 100 set by SettingsPage in @BeforeClass,
     * so this reading will trigger a Traffic Density alert.
     */
    public static void postHighDensityReading() {
        postTrafficReading(450, 15.0, "Severe", "Alexandria");
    }

    /**
     * Sends a low-speed reading at Alexandria.
     * avgSpeed=5 is below the Average Speed threshold of 30 set in @BeforeClass,
     * so this reading will trigger an Average Speed alert.
     */
    public static void postLowSpeedReading() {
        postTrafficReading(200, 5.0, "High", "Alexandria");
    }

    /**
     * Sends a normal-range reading that should NOT trigger any threshold alert.
     * Useful to populate the dashboard table without adding alert noise.
     */
    public static void postNormalTrafficReading() {
        postTrafficReading(50, 80.0, "Low", "Alexandria");
    }

    // ── Light ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/sensors/light
     *
     * @param brightnessLevel   0–100
     * @param powerConsumption  0–5000 W
     * @param status            "ON" | "OFF"
     * @param location          city/area name (simulator uses "Smouha")
     */
    public static void postLightReading(int brightnessLevel,
                                        double powerConsumption,
                                        String status,
                                        String location) {
        String body = String.format(
            "{\"location\":\"%s\",\"brightnessLevel\":%d," +
            "\"powerConsumption\":%.2f,\"status\":\"%s\"}",
            location, brightnessLevel, powerConsumption, status
        );
        post("/api/sensors/light", body);
        System.out.println("[SensorAPI] Light posted — brightness=" + brightnessLevel
                + " power=" + powerConsumption + " loc=" + location);
    }

    // ── Air ───────────────────────────────────────────────────────────────────

    /**
     * POST /api/sensors/air
     *
     * @param co     Carbon Monoxide ppm  (valid range 0–50)
     * @param ozone  Ozone ppb            (valid range 0–300)
     * @param location city name (simulator uses "Cairo")
     */
    public static void postAirReading(double co, double ozone, String location) {
        String body = String.format(
            "{\"location\":\"%s\",\"pm2_5\":12.5,\"pm10\":25.0," +
            "\"co\":%.2f,\"no2\":20.0,\"so2\":15.0,\"ozone\":%.2f," +
            "\"pollutionLevel\":\"Moderate\"}",
            location, co, ozone
        );
        post("/api/sensors/air", body);
        System.out.println("[SensorAPI] Air posted — co=" + co
                + " ozone=" + ozone + " loc=" + location);
    }

    // ── Core HTTP ─────────────────────────────────────────────────────────────

    private static void post(String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response =
                    HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                System.err.println("[SensorAPI] POST " + path
                        + " returned HTTP " + status
                        + " body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[SensorAPI] POST " + path + " failed: " + e.getMessage());
            throw new RuntimeException("SensorApiClient.post(" + path + ") failed", e);
        }
    }
}
