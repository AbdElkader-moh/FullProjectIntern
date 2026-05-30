package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigReader — infrastructure configuration only.
 *
 * What lives here: browser, URLs, timeouts, directories, report names.
 * What does NOT live here anymore: test credentials and test input values.
 *   → Those now come from test_data.xlsx via ExcelDataReader / TestDataProvider.
 *
 * Priority order (highest → lowest):
 *   1. Java system property  (-Dkey=value  or  Maven -D flags)
 *   2. Environment variable  (KEY_WITH_DOTS_AS_UNDERSCORES_UPPERCASED)
 *   3. config.properties     (file in the project root)
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

    // ── Core lookup ───────────────────────────────────────────────────────────

    /**
     * Retrieves a config value.
     * Priority: system property → env variable → config.properties → throws.
     */
    public static String get(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        String envKey = key.replace(".", "_").replace("-", "_").toUpperCase();
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) return value;

        value = properties.getProperty(key);
        if (value != null) return value.trim();

        throw new RuntimeException("Missing config key: '" + key + "'. " +
                "Set it in config.properties, as -D" + key + "=value, " +
                "or as environment variable " + envKey);
    }

    /** Returns {@code defaultValue} instead of throwing when the key is absent. */
    public static String get(String key, String defaultValue) {
        try { return get(key); } catch (RuntimeException e) { return defaultValue; }
    }

    private static int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    // ── Infrastructure getters ────────────────────────────────────────────────

    public static String getAppUrl()           { return get("app.url"); }
    public static String getBrowser()          { return get("browser"); }
    public static boolean isHeadless()         { return Boolean.parseBoolean(get("headless")); }
    public static int getExplicitWait()        { return Integer.parseInt(get("explicit.wait")); }
    public static int getPageLoadTimeout()     { return Integer.parseInt(get("page.load.timeout")); }
    public static String getReportsDir()       { return get("reports.dir"); }
    public static String getScreenshotsDir()   { return get("screenshots.dir"); }
    public static String getSignupImagePath()  { return get("signup.image.path"); }

    /**
     * Kept for backward-compatibility with any test that still calls
     * ConfigReader.getEmail() / .getPassword().
     *
     * @deprecated  Use TestDataProvider.getEmail() / getPassword() instead.
     *              Those delegate to ExcelDataReader, keeping credentials in Excel.
     */
    @Deprecated
    public static String getEmail()          { return get("app.email"); }
    @Deprecated
    public static String getPassword()       { return get("app.password"); }
    @Deprecated
    public static String getDuplicateEmail() { return get("signup.duplicate.email"); }

    // ── Notification / polling timeouts ───────────────────────────────────────

    public static int getNotificationBellTimeout()        { return getInt("notification.bell.timeout", 20); }
    public static int getNotificationAppearanceTimeout()  { return getInt("notification.appearance.timeout", 60); }
    public static int getNotificationReadStateTimeout()   { return getInt("notification.read.state.timeout", 15); }
    public static int getMarkAllReadTimeout()             { return getInt("mark.all.read.timeout", 10); }
    public static int getSimulatorDelayTimeout()          { return getInt("simulator.delay.timeout", 90); }
    public static int getSimulatorPollIntervalMs()        { return getInt("simulator.poll.interval.ms", 3000); }

    // ── Retry / polling ───────────────────────────────────────────────────────

    public static int getRetryAttempts()      { return getInt("retry.attempts", 3); }
    public static int getRetryIntervalMs()    { return getInt("retry.interval.ms", 1000); }
    public static int getPollingIntervalMs()  { return getInt("polling.interval.ms", 500); }
}
