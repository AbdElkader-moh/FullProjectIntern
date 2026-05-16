package com.internship.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigReader: Central configuration loader.
 * Priority order: System property > Environment variable > config.properties
 */
public class ConfigReader {

    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties. " +
                    "Ensure the file exists in the project root.", e);
        }
    }

    private ConfigReader() {}

    /**
     * Retrieve a config value.
     * Priority: System property > Env variable > config.properties
     */
    public static String get(String key) {
        // 1. Java system property (-D flags or Maven)
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        // 2. Environment variable (dots/dashes → underscores, uppercased)
        String envKey = key.replace(".", "_").replace("-", "_").toUpperCase();
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) return value;

        // 3. config.properties
        value = properties.getProperty(key);
        if (value != null) return value.trim();

        throw new RuntimeException("Missing config key: '" + key + "'. " +
                "Set it in config.properties, as a system property (-D" + key + "=value), " +
                "or as environment variable " + envKey);
    }

    /** Returns defaultValue instead of throwing when the key is absent. */
    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ── Existing getters (unchanged) ─────────────────────────────────────────

    public static String getAppUrl()           { return get("app.url"); }
    public static String getEmail()            { return get("app.email"); }
    public static String getPassword()         { return get("app.password"); }
    public static String getBrowser()          { return get("browser"); }
    public static boolean isHeadless()         { return Boolean.parseBoolean(get("headless")); }
    public static int getExplicitWait()        { return Integer.parseInt(get("explicit.wait")); }
    public static int getPageLoadTimeout()     { return Integer.parseInt(get("page.load.timeout")); }
    public static String getReportsDir()       { return get("reports.dir"); }
    public static String getScreenshotsDir()   { return get("screenshots.dir"); }
    public static String getSignupImagePath()  { return get("signup.image.path"); }
    public static String getDuplicateEmail()   { return get("signup.duplicate.email"); }

    // ── New getters for notification/retry/polling keys ──────────────────────

    /** Seconds — how long to wait for the notification bell to be clickable. */
    public static int getNotificationBellTimeout() {
        return getInt("notification.bell.timeout", 20);
    }

    /** Seconds — how long to poll for a new notification to appear. */
    public static int getNotificationAppearanceTimeout() {
        return getInt("notification.appearance.timeout", 60);
    }

    /** Seconds — how long to wait for .unread class to disappear after mark-as-read. */
    public static int getNotificationReadStateTimeout() {
        return getInt("notification.read.state.timeout", 15);
    }

    /** Seconds — how long to wait for mark-all-read button. */
    public static int getMarkAllReadTimeout() {
        return getInt("mark.all.read.timeout", 10);
    }

    /** Seconds — max time to wait for simulator to produce a notification. */
    public static int getSimulatorDelayTimeout() {
        return getInt("simulator.delay.timeout", 90);
    }

    /** Milliseconds — poll interval when waiting for simulator notification. */
    public static int getSimulatorPollIntervalMs() {
        return getInt("simulator.poll.interval.ms", 3000);
    }

    /** How many times to retry a flaky action before failing. */
    public static int getRetryAttempts() {
        return getInt("retry.attempts", 3);
    }

    /** Milliseconds — sleep between retry attempts. */
    public static int getRetryIntervalMs() {
        return getInt("retry.interval.ms", 1000);
    }

    /** Milliseconds — WebDriverWait polling cadence. */
    public static int getPollingIntervalMs() {
        return getInt("polling.interval.ms", 500);
    }
}
