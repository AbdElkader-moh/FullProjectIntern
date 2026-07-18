package tests;
import base.BaseTest;
import io.qameta.allure.*;
import pages.SettingsPage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Core pages")
@Feature("Settings — alert thresholds")
public class SettingsTest extends BaseTest {

    private SettingsPage settingsPage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void openSettings() {
        loginWithDefaultUser();
        settingsPage = new SettingsPage(driver);
        settingsPage.open();
    }

    // ── Page load ─────────────────────────────────────────────────────────────

    @Test(description = "TC-037: Settings page loads at /settings", groups = {"sanity"})
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /settings and verifies the URL contains the expected path.")
    public void TC037_settingsPageLoads() {
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-037 FAILED: URL does not contain /settings");
        System.out.println("TC-037 PASSED");
    }

    // ── Sensor selector ───────────────────────────────────────────────────────

    @Test(description = "TC-038: Traffic sensor label is clickable and page stays at /settings")
    @Story("Sensor selection")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Traffic radio-option label and verifies the page does not navigate away.")
    public void TC038_trafficSensorSelectable() {
        settingsPage.selectSensorType("traffic");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-038 FAILED: Navigated away after Traffic sensor selection");
        System.out.println("TC-038 PASSED");
    }

    @Test(description = "TC-039: Air Quality sensor label is clickable")
    @Story("Sensor selection")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Air Quality radio-option label and verifies the page stays at /settings.")
    public void TC039_airQualitySensorSelectable() {
        settingsPage.selectSensorType("air quality");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-039 FAILED: Navigated away after Air Quality sensor selection");
        System.out.println("TC-039 PASSED");
    }

    @Test(description = "TC-040: Street Light sensor label is clickable")
    @Story("Sensor selection")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Street Light radio-option label and verifies the page stays at /settings.")
    public void TC040_streetLightSensorSelectable() {
        settingsPage.selectSensorType("street light");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-040 FAILED: Navigated away after Street Light sensor selection");
        System.out.println("TC-040 PASSED");
    }

    // ── Valid save ────────────────────────────────────────────────────────────

    @Test(description = "TC-041: Saving a valid threshold increases Active Thresholds count", groups = {"sanity"})
    @Story("Save threshold")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Saves a mid-range Traffic Density threshold and verifies the active thresholds count increases by at least 1.")
    public void TC041_saveValidThreshold() {
        int countBefore = settingsPage.getActiveThresholdCount();
        settingsPage.createThreshold("traffic", 0, 250, true);
        int countAfter = settingsPage.getActiveThresholdCount();
        Assert.assertTrue(countAfter > countBefore,
                "TC-041 FAILED: Count did not increase. Before=" + countBefore + " After=" + countAfter);
        System.out.println("TC-041 PASSED");
    }

    // ── Traffic Density (0–500) ───────────────────────────────────────────────

    @Test(description = "TC-042: Value 0 (min boundary) is accepted for Traffic Density")
    @Story("Boundary values — Traffic Density")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters the minimum value (0) for Traffic Density. Backend range is 0–500. Expects no error message.")
    public void TC042_trafficDensityMinBoundaryAccepted() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(0)
                .enterThresholdValue("0", "traffic", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-042 FAILED: Error shown for value 0 (min boundary, Traffic Density 0–500)");
        System.out.println("TC-042 PASSED");
    }

    @Test(description = "TC-043: Value 499 (near-max, Traffic Density 0–500) is accepted — BUG: UI shows wrong range (0-100)")
    @Story("Boundary values — Traffic Density")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 499 for Traffic Density. Backend accepts up to 500. UI hint incorrectly shows (0-100) — known frontend display bug. Expects no error.")
    public void TC043_trafficDensityNearMaxAccepted() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(0)
                .enterThresholdValue("499", "traffic", 0)
                .clickBelow()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-043 FAILED: Error shown for 499 (valid — backend max for Traffic Density is 500). "
                        + "NOTE: UI hint incorrectly shows (0-100). This is a frontend display bug.");
        System.out.println("TC-043 PASSED");
    }

    @Test(description = "TC-044: Value 501 (above max 500, Traffic Density) is rejected by backend — BUG: UI shows wrong range (0-100)")
    @Story("Boundary values — Traffic Density")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 501 for Traffic Density. Backend max is 500. UI hint incorrectly shows (0-100) — known frontend display bug. Expects rejection error.")
    public void TC044_trafficDensityAboveMaxRejected() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(0)
                .enterThresholdValue("501", "traffic", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-044 FAILED: No error for 501 (above backend max 500 for Traffic Density).");
        System.out.println("TC-044 PASSED");
    }

    @Test(description = "TC-045: Value -1 (below min 0, Traffic Density) is rejected")
    @Story("Boundary values — Traffic Density")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters -1 for Traffic Density. Expects a validation error since the minimum is 0.")
    public void TC045_trafficDensityBelowMinRejected() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(0)
                .enterThresholdValue("-1", "traffic", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-045 FAILED: No error for -1 (below min 0 for Traffic Density)");
        System.out.println("TC-045 PASSED");
    }

    // ── Average Speed (0–120) ────────────────────────────────────────────────

    @Test(description = "TC-046: Value 120 (max boundary, Average Speed 0–120) is accepted — BUG: UI shows (0-100)")
    @Story("Boundary values — Average Speed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters the maximum value (120) for Average Speed. Backend range is 0–120. Expects no error.")
    public void TC046_avgSpeedMaxBoundaryAccepted() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(1)
                .enterThresholdValue("120", "traffic", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-046 FAILED: Error for 120 (backend max for Avg Speed 0–120). "
                        + "UI hint incorrectly shows (0-100).");
        System.out.println("TC-046 PASSED");
    }

    @Test(description = "TC-047: Value 121 (above max 120, Average Speed) is rejected")
    @Story("Boundary values — Average Speed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 121 for Average Speed. Backend max is 120. Expects a rejection error.")
    public void TC047_avgSpeedAboveMaxRejected() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(1)
                .enterThresholdValue("121", "traffic", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-047 FAILED: No error for 121 (above backend max 120 for Avg Speed)");
        System.out.println("TC-047 PASSED");
    }

    // ── Carbon Monoxide (0–50) ───────────────────────────────────────────────

    @Test(description = "TC-048: CO value 50 (backend max, 0–50 ppm) is accepted")
    @Story("Boundary values — Carbon Monoxide")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters the maximum value (50) for Carbon Monoxide. Backend range is 0–50 ppm. Expects no error.")
    public void TC048_coMaxBoundaryAccepted() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(0)
                .enterThresholdValue("50", "air quality", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-048 FAILED: Error for 50 (backend max for CO 0–50 ppm)");
        System.out.println("TC-048 PASSED");
    }

    @Test(description = "TC-049: CO value 51 (above backend max 50 ppm) is rejected")
    @Story("Boundary values — Carbon Monoxide")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 51 for Carbon Monoxide. Backend max is 50 ppm. Expects a rejection error.")
    public void TC049_coAboveMaxRejected() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(0)
                .enterThresholdValue("51", "air quality", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-049 FAILED: No error for 51 (above backend max 50 for CO ppm)");
        System.out.println("TC-049 PASSED");
    }

    // ── Ozone (0–300) ────────────────────────────────────────────────────────

    @Test(description = "TC-050: Ozone value 0 (min boundary) is accepted")
    @Story("Boundary values — Ozone")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters the minimum value (0) for Ozone. Backend range is 0–300 ppb. Expects no error.")
    public void TC050_ozoneMinBoundaryAccepted() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(1)
                .enterThresholdValue("0", "air quality", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-050 FAILED: Error for 0 (min boundary for Ozone 0–300 ppb)");
        System.out.println("TC-050 PASSED");
    }

    @Test(description = "TC-051: Ozone value 300 (backend max, 0–300 ppb) is accepted — BUG: UI shows (0-100)")
    @Story("Boundary values — Ozone")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 300 for Ozone. Backend max is 300 ppb. UI hint incorrectly shows (0-100). Expects no error.")
    public void TC051_ozoneMaxBoundaryAccepted() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(1)
                .enterThresholdValue("300", "air quality", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-051 FAILED: Error for 300 (backend max for Ozone 0–300 ppb). "
                        + "UI hint incorrectly shows (0-100).");
        System.out.println("TC-051 PASSED");
    }

    @Test(description = "TC-052: Ozone value 301 (above backend max 300 ppb) is rejected")
    @Story("Boundary values — Ozone")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 301 for Ozone. Backend max is 300 ppb. Expects a rejection error.")
    public void TC052_ozoneAboveMaxRejected() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(1)
                .enterThresholdValue("301", "air quality", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-052 FAILED: No error for 301 (above backend max 300 for Ozone ppb)");
        System.out.println("TC-052 PASSED");
    }

    // ── Brightness Level (0–100) ─────────────────────────────────────────────

    @Test(description = "TC-053: Brightness Level value 100 (backend max, 0–100) is accepted")
    @Story("Boundary values — Brightness Level")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 100 for Brightness Level. Backend range is 0–100. UI hint is correct for this metric. Expects no error.")
    public void TC053_brightnessMaxBoundaryAccepted() {
        settingsPage.selectSensorType("street light")
                .selectMetricByIndex(0)
                .enterThresholdValue("100", "street light", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-053 FAILED: Error for 100 (backend max for Brightness Level 0–100)");
        System.out.println("TC-053 PASSED");
    }

    @Test(description = "TC-054: Brightness Level value 101 (above backend max 100) is rejected")
    @Story("Boundary values — Brightness Level")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 101 for Brightness Level. Backend max is 100. Expects a rejection error.")
    public void TC054_brightnessAboveMaxRejected() {
        settingsPage.selectSensorType("street light")
                .selectMetricByIndex(0)
                .enterThresholdValue("101", "street light", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-054 FAILED: No error for 101 (above backend max 100 for Brightness Level)");
        System.out.println("TC-054 PASSED");
    }

    // ── Power Consumption (0–5000) ────────────────────────────────────────────

    @Test(description = "TC-055: Power Consumption value 5000 (backend max, 0–5000) is accepted — BUG: UI shows (0-100)")
    @Story("Boundary values — Power Consumption")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 5000 for Power Consumption. Backend max is 5000. UI hint incorrectly shows (0-100). Expects no error.")
    public void TC055_powerConsumptionMaxBoundaryAccepted() {
        settingsPage.selectSensorType("street light")
                .selectMetricByIndex(1)
                .enterThresholdValue("5000", "street light", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-055 FAILED: Error for 5000 (backend max for Power Consumption 0–5000). "
                        + "UI hint incorrectly shows (0-100).");
        System.out.println("TC-055 PASSED");
    }

    @Test(description = "TC-056: Power Consumption value 5001 (above backend max 5000) is rejected")
    @Story("Boundary values — Power Consumption")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters 5001 for Power Consumption. Backend max is 5000. Expects a rejection error.")
    public void TC056_powerConsumptionAboveMaxRejected() {
        settingsPage.selectSensorType("street light")
                .selectMetricByIndex(1)
                .enterThresholdValue("5001", "street light", 1)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-056 FAILED: No error for 5001 (above backend max 5000 for Power Consumption)");
        System.out.println("TC-056 PASSED");
    }

    // ── Active class + all-metrics min ────────────────────────────────────────

    @Test(description = "TC-057: Selecting Traffic sensor applies Angular 'active' CSS class to its label")
    @Story("Sensor selection")
    @Severity(SeverityLevel.MINOR)
    @Description("Selects the Traffic sensor and verifies its label receives the Angular 'active' CSS class.")
    public void TC057_trafficSensorLabelActivated() {
        settingsPage.selectSensorType("traffic");
        Assert.assertTrue(settingsPage.isTrafficSensorSelected(),
                "TC-057 FAILED: Traffic label does not carry 'active' class after selection");
        System.out.println("TC-057 PASSED");
    }

    @Test(description = "TC-058: Value 0 (min) is accepted for every sensor/metric combination")
    @Story("Boundary values — all metrics")
    @Severity(SeverityLevel.NORMAL)
    @Description("Iterates all 6 sensor/metric combinations and submits value 0 for each. Expects no error in any case since 0 is the global minimum.")
    public void TC058_minBoundaryAllMetrics() {
        String[][] combos = {
                {"traffic",      "0", "Traffic Density"},
                {"traffic",      "1", "Average Speed"},
                {"air quality",  "0", "Carbon Monoxide"},
                {"air quality",  "1", "Ozone"},
                {"street light", "0", "Brightness Level"},
                {"street light", "1", "Power Consumption"}
        };
        for (String[] c : combos) {
            String sensor = c[0];
            int    index  = Integer.parseInt(c[1]);
            String name   = c[2];
            settingsPage.open();
            settingsPage.selectSensorType(sensor)
                    .selectMetricByIndex(index)
                    .enterThresholdValue("0", sensor, index)
                    .clickAbove()
                    .clickSaveThreshold();
            Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                    "TC-058 FAILED: Error for value 0 on " + name
                            + " [sensor=" + sensor + " index=" + index + "]");
            System.out.println("[TC058] 0 accepted for: " + name);
        }
        System.out.println("TC-058 PASSED");
    }

    @Test(description = "TC-059: .alert-success is shown after saving a valid threshold")
    @Story("Save threshold")
    @Severity(SeverityLevel.NORMAL)
    @Description("Saves a valid Traffic Density threshold and verifies the .alert-success message appears on screen.")
    public void TC059_successMessageShownAfterValidSave() {
        settingsPage.selectSensorType("traffic")
                .selectMetricByIndex(0)
                .enterThresholdValue("200", "traffic", 0)
                .clickAbove()
                .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isSuccessMessageDisplayed(),
                "TC-059 FAILED: No .alert-success after saving valid threshold (Traffic Density 200)");
        System.out.println("TC-059 PASSED");
    }

    @Test(description = "TC-060: A 'Below' direction threshold saves successfully")
    @Story("Save threshold")
    @Severity(SeverityLevel.NORMAL)
    @Description("Saves an Ozone threshold with the 'Below' direction selected. Expects no error — direction choice should not affect acceptance.")
    public void TC060_belowDirectionThresholdSaves() {
        settingsPage.selectSensorType("air quality")
                .selectMetricByIndex(1)
                .enterThresholdValue("150", "air quality", 1)
                .clickBelow()
                .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-060 FAILED: Error for valid Below-direction Ozone threshold (value=150, range 0–300)");
        System.out.println("TC-060 PASSED");
    }
}