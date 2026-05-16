package com.internship.tests;

import com.internship.base.BaseTest;
import com.internship.pages.SettingsPage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * SettingsTest – Alert Thresholds (/settings) — TC-037 … TC-046
 *
 * All TC038–TC046 failures traced to ONE root cause:
 *   The sensor type selectors are <label class="radio-option"> elements wrapping
 *   <input type="radio"> — NOT <button> elements. Every previous locator searched
 *   for //button[contains(.,'Traffic')] which found zero elements in the DOM.
 *
 * Threshold constraint confirmed from settings.html:
 *   <span class="constraint-hint">({{ currentConstraint.min }} – {{ currentConstraint.max }})</span>
 *   Screenshot showed "(0 - 100)" for Traffic Density.
 *   TC043 uses 99 (valid, within range).
 *   TC044 uses 101 (above range — expects rejection).
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

    // ── TC-037 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-037: Settings/Alert Thresholds page loads and URL is /settings")
    public void TC037_settingsPageLoads() {
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-037 FAILED: Settings page did not load at /settings.");
        System.out.println("TC-037 PASSED");
    }

    // ── TC-038 ────────────────────────────────────────────────────────────────

    /**
     * Root cause fix: selectSensorType("traffic") now clicks the
     * <label class="radio-option"> containing the Traffic radio input,
     * not a non-existent <button>.
     */
    @Test(description = "TC-038: Traffic sensor type label is selectable")
    public void TC038_trafficSensorSelectable() {
        settingsPage.selectSensorType("traffic");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-038 FAILED: Page navigated away after Traffic sensor selection.");
        System.out.println("TC-038 PASSED");
    }

    // ── TC-039 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-039: Air Quality sensor type label is selectable")
    public void TC039_airQualitySensorSelectable() {
        settingsPage.selectSensorType("air quality");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-039 FAILED: Navigated away after Air Quality sensor selection.");
        System.out.println("TC-039 PASSED");
    }

    // ── TC-040 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-040: Street Light sensor type label is selectable")
    public void TC040_streetLightSensorSelectable() {
        settingsPage.selectSensorType("street light");
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-040 FAILED: Navigated away after Street Light sensor selection.");
        System.out.println("TC-040 PASSED");
    }

    // ── TC-041 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-041: A valid threshold (50) is saved and appears in Active Thresholds")
    public void TC041_saveValidThreshold() {
        int countBefore = settingsPage.getActiveThresholdCount();

        // Traffic Density index=0, value=50 — safely within (0–100)
        settingsPage.createThreshold("traffic", 0, 50, true);

        int countAfter = settingsPage.getActiveThresholdCount();
        Assert.assertTrue(countAfter > countBefore,
                "TC-041 FAILED: Active threshold count did not increase. Before="
                        + countBefore + " After=" + countAfter);
        System.out.println("TC-041 PASSED");
    }

    // ── TC-042 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-042: Threshold value 0 (minimum boundary) is accepted")
    public void TC042_thresholdMinBoundary() {
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(0)  // Traffic Density (0–100)
                    .enterThresholdValue("0")
                    .clickAbove()
                    .clickSaveThreshold();

        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-042 FAILED: Unexpected error for value 0 (min boundary).");
        System.out.println("TC-042 PASSED");
    }

    // ── TC-043 ────────────────────────────────────────────────────────────────

    /**
     * Screenshot + HTML both confirm range is (0–100) for Traffic Density.
     * 99 is the last valid value below the max.
     */
    @Test(description = "TC-043: Threshold value 99 (near-max boundary, range 0-100) is accepted")
    public void TC043_thresholdMaxBoundary() {
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(0)   // Traffic Density (0–100)
                    .enterThresholdValue("99")
                    .clickBelow()
                    .clickSaveThreshold();

        Assert.assertFalse(settingsPage.isThresholdErrorDisplayed(),
                "TC-043 FAILED: Unexpected error for value 99 (max is 100).");
        System.out.println("TC-043 PASSED");
    }

    // ── TC-044 ────────────────────────────────────────────────────────────────

    /**
     * 101 is above the confirmed max of 100 for Traffic Density.
     * The HTML binds [max]="currentConstraint.max" on the input —
     * Angular validates on submit and sets errorMessage → shows .alert-error.
     */
    @Test(description = "TC-044: Threshold value 101 (above max of 100) is rejected with error")
    public void TC044_thresholdAboveMaxRejected() {
        settingsPage.selectSensorType("traffic")
                    .selectMetricByIndex(0)    // Traffic Density — max 100
                    .enterThresholdValue("101")
                    .clickAbove()
                    .clickSaveThreshold();

        Assert.assertTrue(settingsPage.isThresholdErrorDisplayed(),
                "TC-044 FAILED: Expected .alert-error for value 101 (above max 100).");
        System.out.println("TC-044 PASSED");
    }

    // ── TC-045 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-045: Above and Below toggle buttons are interactive")
    public void TC045_aboveBelowToggle() {
        settingsPage.selectSensorType("traffic");
        settingsPage.clickAbove();
        settingsPage.clickBelow();
        Assert.assertTrue(settingsPage.isOnSettingsPage(),
                "TC-045 FAILED: Page navigated away during toggle interaction.");
        System.out.println("TC-045 PASSED");
    }

    // ── TC-046 ────────────────────────────────────────────────────────────────

    /**
     * The delete button real DOM:
     *   <button class="delete-btn" title="Delete">✕</button>
     * SettingsPage.deleteFirstActiveThreshold() uses By.cssSelector(".delete-btn")
     * and waits for the async API call to reduce the row count.
     */
    @Test(description = "TC-046: An active threshold can be deleted from the active list")
    public void TC046_deleteActiveThreshold() {
        // Ensure there is at least one threshold to delete
        settingsPage.createThreshold("traffic", 0, 30, true);

        int countBefore = settingsPage.getActiveThresholdCount();
        System.out.println("[TC046] Before delete: " + countBefore);

        if (countBefore > 0) {
            settingsPage.deleteFirstActiveThreshold();
            int countAfter = settingsPage.getActiveThresholdCount();
            System.out.println("[TC046] After delete: " + countAfter);
            Assert.assertTrue(countAfter < countBefore,
                    "TC-046 FAILED: Count did not decrease. Before="
                  + countBefore + " After=" + countAfter);
        } else {
            Assert.fail("TC-046 FAILED: No active thresholds found to delete.");
        }
        System.out.println("TC-046 PASSED");
    }
}
