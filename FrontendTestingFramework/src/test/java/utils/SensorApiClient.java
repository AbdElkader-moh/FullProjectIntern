package utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import data.TestDataProvider;

public class SensorApiClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String BASE_URL = ConfigReader.get("app.sensor.url", "http://localhost:8081");

    private SensorApiClient() {
    }

    // ── Traffic ───────────────────────────────────────────────────────────────

    public static void postTrafficReading(int trafficDensity,
            double avgSpeed,
            String congestionLevel,
            String location) {
        String body = String.format(
                "{\"location\":\"%s\",\"trafficDensity\":%d," +
                        "\"avgSpeed\":%.2f,\"congestionLevel\":\"%s\"}",
                location, trafficDensity, avgSpeed, congestionLevel);
        post("/api/sensors/traffic", body);
        System.out.println("[SensorAPI] Traffic posted — density=" + trafficDensity
                + " speed=" + avgSpeed + " congestion=" + congestionLevel
                + " loc=" + location);
    }

    public static void postHighDensityReading() {
        postTrafficReading(450, 15.0, "Severe", "Alexandria");
    }

    public static void postLowSpeedReading() {
        postTrafficReading(200, 5.0, "High", "Alexandria");
    }

    public static void postNormalTrafficReading() {
        postTrafficReading(50, 80.0, "Low", "Alexandria");
    }

    // ── Light ─────────────────────────────────────────────────────────────────

    public static void postLightReading(int brightnessLevel,
            double powerConsumption,
            String status,
            String location) {
        String body = String.format(
                "{\"location\":\"%s\",\"brightnessLevel\":%d," +
                        "\"powerConsumption\":%.2f,\"status\":\"%s\"}",
                location, brightnessLevel, powerConsumption, status);
        post("/api/sensors/light", body);
        System.out.println("[SensorAPI] Light posted — brightness=" + brightnessLevel
                + " power=" + powerConsumption + " status=" + status + " loc=" + location);
    }

    public static void postHighPowerConsumptionReading() {
        postLightReading(10, 4500.0, "ON", "Alexandria");
    }

    public static void postLowBrightnessLevelReading() {
        postLightReading(5, 500.0, "OFF", "Alexandria");
    }

    public static void postNormalLightReading() {
        postLightReading(80, 1200.0, "ON", "Alexandria");
    }

    // ── Air ───────────────────────────────────────────────────────────────────

    public static void postAirReading(double co, double ozone, String pollutionLevel, String location) {
        String body = String.format(
                "{\"location\":\"%s\",\"pm2_5\":12.5,\"pm10\":25.0," +
                        "\"co\":%.2f,\"no2\":20.0,\"so2\":15.0,\"ozone\":%.2f," +
                        "\"pollutionLevel\":\"%s\"}",
                location, co, ozone, pollutionLevel);
        post("/api/sensors/air", body);
        System.out.println("[SensorAPI] Air posted — co=" + co
                + " ozone=" + ozone + " pollutionLevel=" + pollutionLevel + " loc=" + location);
    }

    public static void postAirReading(double co, double ozone, String location) {
        postAirReading(co, ozone, "Moderate", location);
    }

    public static void postHighCarbonMonoxideReading() {
        postAirReading(45.0, 100.0, "Very_Unhealthy", "Alexandria");
    }

    public static void postLowOzoneReading() {
        postAirReading(10.0, 5.0, "Unhealthy", "Alexandria");
    }

    public static void postNormalAirReading() {
        postAirReading(15.0, 50.0, "Good", "Alexandria");
    }

    // ── Core HTTP ─────────────────────────────────────────────────────────────

    private static final String AUTH_URL = ConfigReader.get("app.url", "http://localhost:4200");
    private static String jwtToken = null;

    private static synchronized void authenticateIfNeeded() {
        if (jwtToken != null) return;
        
        try {
            String email = TestDataProvider.getEmail();
            String password = TestDataProvider.getPassword();
            String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_URL + "/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();
                    
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                jwtToken = response.headers().firstValue("Authorization").orElse(null);
                System.out.println("[SensorAPI] Authenticated successfully, JWT cached.");
            } else {
                System.err.println("[SensorAPI] Failed to authenticate: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[SensorAPI] Authentication error: " + e.getMessage());
        }
    }

    private static void post(String path, String jsonBody) {
        authenticateIfNeeded();
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15));
            
            if (jwtToken != null && !jwtToken.isEmpty()) {
                reqBuilder.header("Authorization", jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken);
            }
            
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

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
