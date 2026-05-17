package com.internship.tests;

import com.internship.base.BaseTest;
import com.internship.pages.SettingsPage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * SettingsTest — Alert Thresholds (/settings)   TC-037 … TC-060
 *
 * ── FRONTEND BUG DOCUMENTED ──────────────────────────────────────────────────
 *
 *   The UI displays "(0 - 100)" as the threshold range hint for EVERY metric,
 *   regardless of which sensor is selected. This is a hardcoded display bug.
 *
 *   Backend validation enforces the REAL ranges from the API spec:
 *
 *   ┌──────────────────────┬────────────┬──────────────────────────────────────┐
 *   │ Sensor               │ Metric     │ Backend range   (UI shows "(0-100)") │
 *   ├──────────────────────┼────────────┼──────────────────────────────────────┤
 *   │ Traffic              │ Density    │ 0 – 500  ← UI hint is WRONG          │
 *   │ Traffic              │ Avg Speed  │ 0 – 120  ← UI hint is WRONG          │
 *   │ Air Quality          │ CO         │ 0 – 50   ← UI hint correct by chance │
 *   │ Air Quality          │ Ozone      │ 0 – 300  ← UI hint is WRONG          │
 *   │ Street Light         │ Brightness │ 0 – 100  ← UI hint correct           │
 *   │ Street Light         │ Power      │ 0 – 5000 ← UI hint is WRONG          │
 *   └──────────────────────┴────────────┴──────────────────────────────────────┘
 *
 *   Tests validate backend behaviour (save accepted / rejected) because that is
 *   what matters for system correctness. A separate BUG tag marks tests that
 *   also expose the frontend display defect.
 *
 * ── ORIGINAL TC-043 / TC-044 BUG ─────────────────────────────────────────────
 *
 *   Original code used max=100 for Traffic Density (copied from the wrong UI hint).
 *   TC-043 submitted 99 thinking it was near-max; TC-044 submitted 101 expecting
 *   rejection — but 101 is valid (real max is 500), so the test was testing nothing.
 *   Both are corrected to use backend-accurate values.
 */
public class SettingsTest extends BaseTest {

    private SettingsPage settingsPage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
    }

    @BeforeMethod(alwaysRun = true)
    public void openSettings() {
        settingsPage = new SettingsPage(driver);
        settingsPage.open();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-037  Page load
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-037: Settings page loads at /settings")
    public void TC037_settingsPageLoads() {
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
            "TC-037 FAILED: URL does not contain /settings");
        System.out.println("TC-037 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-038 / TC-039 / TC-040  Sensor selector labels
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-038: Traffic sensor label is clickable and page stays at /settings")
    public void TC038_trafficSensorSelectable() {
        settingsPage.selectSensorType("traffic");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
            "TC-038 FAILED: Navigated away after Traffic sensor selection");
        System.out.println("TC-038 PASSED");
    }

    @Test(description = "TC-039: Air Quality sensor label is clickable")
    public void TC039_airQualitySensorSelectable() {
        settingsPage.selectSensorType("air quality");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
            "TC-039 FAILED: Navigated away after Air Quality sensor selection");
        System.out.println("TC-039 PASSED");
    }

    @Test(description = "TC-040: Street Light sensor label is clickable")
    public void TC040_streetLightSensorSelectable() {
        settingsPage.selectSensorType("street light");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
            "TC-040 FAILED: Navigated away after Street Light sensor selection");
        System.out.println("TC-040 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-041  Save a valid threshold → Active Thresholds count increases
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-041: Saving a valid threshold increases Active Thresholds count")
    public void TC041_saveValidThreshold() {
        int countBefore = settingsPage.getActiveThresholdCount();
        // Traffic Density mid-range value — well within 0–500
        settingsPage.createThreshold("traffic", 0, 250, true);
        int countAfter = settingsPage.getActiveThresholdCount();
        Assert.assertTrue(countAfter > countBefore,
            "TC-041 FAILED: Count did not increase. Before=" + countBefore + " After=" + countAfter);
        System.out.println("TC-041 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-042  Min boundary (0) — Traffic Density
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-042: Value 0 (min boundary) is accepted for Traffic Density")
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

    // ════════════════════════════════════════════════════════════════════════
    // TC-043  Near-max valid — Traffic Density  [FIXED: max=500, not 100]
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-043: Value 499 (near-max, Traffic Density 0–500) is accepted — BUG: UI shows wrong range (0-100)")
    public void TC043_trafficDensityNearMaxAccepted() {
        /*
         * BUG EXPOSED: UI hint reads "(0 - 100)" but backend accepts up to 500.
         * This test confirms backend accepts 499 even though the UI hint is wrong.
         * Original test used 99 — only because it copied the wrong UI hint.
         */
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(0)
                    .enterThresholdValue("499", "traffic", 0)
                    .clickBelow()
                    .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
            "TC-043 FAILED: Error shown for 499 (valid — backend max for Traffic Density is 500). " +
            "NOTE: UI hint incorrectly shows (0-100). This is a frontend display bug.");
        System.out.println("TC-043 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-044  Above-max rejection — Traffic Density  [FIXED: max=500, not 100]
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-044: Value 501 (above max 500, Traffic Density) is rejected by backend — BUG: UI shows wrong range (0-100)")
    public void TC044_trafficDensityAboveMaxRejected() {
        /*
         * BUG EXPOSED: UI hint says "(0 - 100)" — the actual backend max is 500.
         * Original test used 101 which is VALID (backend allows up to 500),
         * so the original TC-044 was a false green — passing for the wrong reason.
         * 501 is the correct value that should be rejected by the backend.
         */
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(0)
                    .enterThresholdValue("501", "traffic", 0)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
            "TC-044 FAILED: No error for 501 (above backend max 500 for Traffic Density). " +
            "NOTE: UI hint shows (0-100) which is wrong — backend max is 500.");
        System.out.println("TC-044 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-045  Above / Below toggles are interactive
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-045: Above and Below toggles are clickable without page navigation")
    public void TC045_aboveBelowToggle() {
        settingsPage.selectSensorType("traffic");
        settingsPage.clickAbove();
        settingsPage.clickBelow();
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
            "TC-045 FAILED: Page navigated away during toggle interaction");
        System.out.println("TC-045 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-046  Delete an active threshold
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-046: A saved threshold can be deleted and count decreases")
    public void TC046_deleteActiveThreshold() {
        settingsPage.createThreshold("traffic", 0, 100, true);
        int countBefore = settingsPage.getActiveThresholdCount();
        System.out.println("[TC046] Before delete: " + countBefore);
        if (countBefore == 0) Assert.fail("TC-046 FAILED: No thresholds available to delete");
        settingsPage.deleteFirstActiveThreshold();
        int countAfter = settingsPage.getActiveThresholdCount();
        System.out.println("[TC046] After delete: " + countAfter);
        Assert.assertTrue(countAfter < countBefore,
            "TC-046 FAILED: Count did not decrease. Before=" + countBefore + " After=" + countAfter);
        System.out.println("TC-046 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-047 / TC-048  Traffic — Average Speed  (backend: 0 – 120)
    // BUG: UI shows "(0 - 100)" — wrong for this metric
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-047: Average Speed value 120 (backend max) is accepted — BUG: UI shows wrong range (0-100)")
    public void TC047_avgSpeedMaxBoundaryAccepted() {
        /*
         * BUG: UI hint reads "(0 - 100)" but backend max for Average Speed is 120.
         * This test verifies backend accepts 120 despite the wrong UI hint.
         */
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(1)
                    .enterThresholdValue("120", "traffic", 1)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
            "TC-047 FAILED: Error for 120 (backend max for Average Speed 0–120). " +
            "UI hint incorrectly shows (0-100).");
        System.out.println("TC-047 PASSED");
    }

    @Test(description = "TC-048: Average Speed value 121 (above backend max 120) is rejected")
    public void TC048_avgSpeedAboveMaxRejected() {
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(1)
                    .enterThresholdValue("121", "traffic", 1)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
            "TC-048 FAILED: No error for 121 (above backend max 120 for Average Speed)");
        System.out.println("TC-048 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-049 / TC-050  Air Quality — Carbon Monoxide  (backend: 0 – 50 ppm)
    // UI shows "(0 - 100)" — wrong, but CO max is 50 so the UI hint is harmless here
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-049: Carbon Monoxide value 50 (backend max, 0–50 ppm) is accepted")
    public void TC049_carbonMonoxideMaxBoundaryAccepted() {
        settingsPage.selectSensorType("air quality")
                    .selectMetricByIndex(0)
                    .enterThresholdValue("50", "air quality", 0)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
            "TC-049 FAILED: Error for 50 (backend max for Carbon Monoxide 0–50 ppm)");
        System.out.println("TC-049 PASSED");
    }

    @Test(description = "TC-050: Carbon Monoxide value 51 (above backend max 50 ppm) is rejected")
    public void TC050_carbonMonoxideAboveMaxRejected() {
        /*
         * This is the value visible in Active Thresholds in the screenshots ("above 51").
         * It proves the backend rejected it (or it was saved before validation was enforced).
         * This test confirms 51 IS rejected.
         */
        settingsPage.selectSensorType("air quality")
                    .selectMetricByIndex(0)
                    .enterThresholdValue("51", "air quality", 0)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
            "TC-050 FAILED: No error for 51 (above backend max 50 for Carbon Monoxide ppm)");
        System.out.println("TC-050 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-051 / TC-052  Air Quality — Ozone  (backend: 0 – 300 ppb)
    // BUG: UI shows "(0 - 100)" — wrong for this metric
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-051: Ozone value 300 (backend max, 0–300 ppb) is accepted — BUG: UI shows wrong range (0-100)")
    public void TC051_ozoneMaxBoundaryAccepted() {
        settingsPage.selectSensorType("air quality")
                    .selectMetricByIndex(1)
                    .enterThresholdValue("300", "air quality", 1)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
            "TC-051 FAILED: Error for 300 (backend max for Ozone 0–300 ppb). " +
            "UI hint incorrectly shows (0-100).");
        System.out.println("TC-051 PASSED");
    }

    @Test(description = "TC-052: Ozone value 301 (above backend max 300 ppb) is rejected")
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

    // ════════════════════════════════════════════════════════════════════════
    // TC-053 / TC-054  Street Light — Brightness Level  (backend: 0 – 100)
    // UI shows "(0 - 100)" — correct for this metric
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-053: Brightness Level value 100 (backend max, 0–100) is accepted")
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

    // ════════════════════════════════════════════════════════════════════════
    // TC-055 / TC-056  Street Light — Power Consumption  (backend: 0 – 5000)
    // BUG: UI shows "(0 - 100)" — wrong for this metric
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-055: Power Consumption value 5000 (backend max, 0–5000) is accepted — BUG: UI shows wrong range (0-100)")
    public void TC055_powerConsumptionMaxBoundaryAccepted() {
        settingsPage.selectSensorType("street light")
                    .selectMetricByIndex(1)
                    .enterThresholdValue("5000", "street light", 1)
                    .clickAbove()
                    .clickSaveThreshold();
        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
            "TC-055 FAILED: Error for 5000 (backend max for Power Consumption 0–5000). " +
            "UI hint incorrectly shows (0-100).");
        System.out.println("TC-055 PASSED");
    }

    @Test(description = "TC-056: Power Consumption value 5001 (above backend max 5000) is rejected")
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

    // ════════════════════════════════════════════════════════════════════════
    // TC-057  Traffic sensor label receives Angular "active" class
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-057: Selecting Traffic sensor applies Angular 'active' CSS class to its label")
    public void TC057_trafficSensorLabelActivated() {
        settingsPage.selectSensorType("traffic");
        Assert.assertTrue(settingsPage.isTrafficSensorSelected(),
            "TC-057 FAILED: Traffic label does not carry 'active' class after selection");
        System.out.println("TC-057 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-058  Min boundary (0) accepted across all 6 metrics
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-058: Value 0 (min) is accepted for every sensor/metric combination")
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
                "TC-058 FAILED: Error for value 0 on " + name +
                " [sensor=" + sensor + " index=" + index + "]");
            System.out.println("[TC058] 0 accepted for: " + name);
        }
        System.out.println("TC-058 PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-059  Success message appears after valid save
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-059: .alert-success is shown after saving a valid threshold")
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

    // ════════════════════════════════════════════════════════════════════════
    // TC-060  Below-direction threshold saves without error
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "TC-060: A 'Below' direction threshold saves successfully")
    public void TC060_belowDirectionThresholdSaves() {
        // Ozone mid-range, Below direction
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
