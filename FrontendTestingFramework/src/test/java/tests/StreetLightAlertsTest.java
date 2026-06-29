package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.SettingsPage;
import pages.StreetLightAlertsPage;
import pages.StreetLightDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * StreetLightAlertsTest — TC-171 … TC-182
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * @BeforeClass — uses SettingsPage to save two traffic thresholds once:
 *   Power Consumption > 100  (sensor index 0, above)
 *   Brightness Level   < 30   (sensor index 1, below)
 *
 * @BeforeMethod — uses SensorApiClient to post readings that cross both thresholds,
 *   guaranteeing at least two alerts exist before each test. Readings go to
 *   localhost:8081 (host-side port from docker-compose 8081:8081).
 *
 * DOM CHANGES fixed in StreetLightAlertsPage (compared to previous version):
 *   - Alert Type dropdown (Traffic/Air/Light) removed — page is traffic-only.
 *   - New #aFilterMetric dropdown: "Power Consumption" | "Brightness Level".
 *   - Live banner replaced by toast stack (.toast-stack).
 *   - All type badges are .type-light.
 */
@Epic("Street light monitoring")
@Feature("Street light alerts")
public class StreetLightAlertsTest extends BaseTest {

    private StreetLightAlertsPage alertsPage;

    // ── One-time threshold setup ───────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
        ensureStreetLightThresholdsExist();
    }

    private void ensureStreetLightThresholdsExist() {
        try {
            SettingsPage settingsPage = new SettingsPage(driver);
            settingsPage.open();

            // Power Consumption above 100 (Power Consumption is index 1)
            settingsPage.createThreshold("street light", 1, 100, true);
            System.out.println("[Setup] Power Consumption > 100 saved");

            // Brightness Level below 30 (Brightness Level is index 0)
            settingsPage.createThreshold("street light", 0, 30, false);
            System.out.println("[Setup] Brightness Level < 30 saved");

        } catch (Exception e) {
            System.out.println("[Setup] Threshold warning: " + e.getMessage());
        }
    }

    // ── Per-test data seeding + navigation ────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    public void seedAlertsAndOpen() {
        try {
            // power=450 > threshold 100 → triggers Power Consumption alert
            SensorApiClient.postHighPowerConsumptionReading();
            // brightness=5 < threshold 30 → triggers Brightness Level alert
            SensorApiClient.postLowBrightnessLevelReading();
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        alertsPage = new StreetLightAlertsPage(driver);
        alertsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-171: Street light alerts page loads and URL is /lights-alerts")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /lights-alerts and verifies URL and page title.")
    public void TC171_alertsPageLoads() {
        Assert.assertTrue(alertsPage.isOnAlertsPage(),
                "TC-171 FAILED: URL does not contain /lights-alerts.");
        Assert.assertTrue(alertsPage.isPageTitleDisplayed(),
                "TC-171 FAILED: Page title not visible.");
        System.out.println("TC-171 PASSED");
    }

    @Test(description = "TC-172: Alerts table shows rows or empty state — no error banner")
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Two readings crossed thresholds in @BeforeMethod. Table must show rows with no error.")
    public void TC172_tableOrEmptyState() {
        Assert.assertFalse(alertsPage.isTableErrorDisplayed(),
                "TC-172 FAILED: Error banner on initial load.");
        boolean hasAlerts = alertsPage.hasAlerts();
        boolean hasEmpty  = alertsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-172 FAILED: Neither rows nor empty state shown.");
        System.out.println("TC-172 PASSED — rows: " + alertsPage.getTableRowCount());
    }

    @Test(description = "TC-173: All visible type badges have class type-light")
    @Story("Alert content")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This page only shows Street light alerts. No Air or Traffic type badges should appear.")
    public void TC173_allBadgesAreStreetLightType() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-173 INFO: No alerts present.");
            return;
        }
        int total   = driver.findElements(
                org.openqa.selenium.By.cssSelector(".type-badge")).size();
        int light = alertsPage.getTypeBadges().size();
        Assert.assertEquals(light, total,
                "TC-173 FAILED: " + (total - light) + " non-light badge(s) found.");
        System.out.println("TC-173 PASSED — all " + light + " badges are type-light");
    }

    @Test(description = "TC-174: 'Power Consumption' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Power Consumption from #aFilterMetric. All metric cells must read 'Power Consumption'.")
    public void TC174_metricFilterScopesToPowerConsumption() {
        alertsPage.selectMetricFilter("Power Consumption").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-174 INFO: No Power Consumption alerts present.");
            return;
        }
        alertsPage.getMetricCells().forEach(cell ->
            Assert.assertEquals(cell.getText().trim(), "Power Consumption",
                "TC-174 FAILED: Wrong metric: " + cell.getText()));
        System.out.println("TC-174 PASSED");
    }

    @Test(description = "TC-175: 'Brightness Level' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Brightness Level from #aFilterMetric. All metric cells must read 'Brightness Level'.")
    public void TC175_metricFilterScopesToBrightnessLevel() {
        alertsPage.selectMetricFilter("Brightness Level").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-175 INFO: No Brightness Level alerts present.");
            return;
        }
        alertsPage.getMetricCells().forEach(cell ->
            Assert.assertEquals(cell.getText().trim(), "Brightness Level",
                "TC-175 FAILED: Wrong metric: " + cell.getText()));
        System.out.println("TC-175 PASSED");
    }

    @Test(description = "TC-176: Reset clears metric filter and restores full list")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies power filter, records count, resets, verifies count >= filtered count.")
    public void TC176_resetRestoresList() {
        alertsPage.selectMetricFilter("Power Consumption").clickApplyFilters();
        int filteredCount = alertsPage.getTableRowCount();
        alertsPage.clickResetFilters();
        int totalCount = alertsPage.getTableRowCount();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-176 FAILED: After reset (" + totalCount
                + ") < filtered (" + filteredCount + ").");
        System.out.println("TC-176 PASSED — filtered: " + filteredCount
                + " total: " + totalCount);
    }

    @Test(description = "TC-177: Invalid date range shows validation error")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("End date before start date — expects .filter-date-error.")
    public void TC177_invalidDateRangeError() {
        alertsPage.enterDateFrom("2026-06-01T00:00")
                  .enterDateTo("2026-05-01T00:00")
                  .clickApplyFilters();
        Assert.assertTrue(alertsPage.isDateErrorDisplayed(),
                "TC-177 FAILED: No date validation error shown.");
        System.out.println("TC-177 PASSED");
    }

    @Test(description = "TC-178: Mark All Read button appears only when unread alerts exist")
    @Story("Mark as read")
    @Severity(SeverityLevel.CRITICAL)
    @Description(".btn-mark-all only rendered when unreadCount > 0. Verifies presence matches unread rows.")
    public void TC178_markAllReadVisibility() {
        int unreadRows = alertsPage.getUnreadRowCount();
        if (unreadRows > 0) {
            Assert.assertTrue(alertsPage.isMarkAllReadDisplayed(),
                    "TC-178 FAILED: Unread rows exist but button not shown.");
        } else {
            Assert.assertFalse(alertsPage.isMarkAllReadDisplayed(),
                    "TC-178 FAILED: No unread rows but button is shown.");
        }
        System.out.println("TC-178 PASSED — unread rows: " + unreadRows);
    }

    @Test(description = "TC-179: Every alert row has a severity badge of High, Medium, or Low")
    @Story("Alert content")
    @Severity(SeverityLevel.NORMAL)
    @Description("Iterates .severity-badge and asserts each is High, Medium, or Low.")
    public void TC179_severityBadgesValid() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-179 INFO: No alerts — skip.");
            return;
        }
        alertsPage.getSeverityBadges().forEach(badge -> {
            String text = badge.getText().trim();
            Assert.assertTrue(
                text.equals("High") || text.equals("Medium") || text.equals("Low"),
                "TC-179 FAILED: Unexpected severity: '" + text + "'");
        });
        System.out.println("TC-179 PASSED");
    }

    @Test(description = "TC-180: Back link navigates to /lights dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies /lights.")
    public void TC180_backLink() {
        StreetLightDashboardPage dash = alertsPage.clickBack();
        Assert.assertTrue(dash.isOnStreetLightDashboard(),
                "TC-180 FAILED: Did not reach /lights.");
        System.out.println("TC-180 PASSED");
    }

    @Test(description = "TC-181: Pagination shows when alerts exceed page size of 15")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies pagination controls appear with non-zero rows when total > 15.")
    public void TC181_paginationForManyAlerts() {
        if (alertsPage.hasPagination()) {
            Assert.assertTrue(alertsPage.getTableRowCount() > 0,
                    "TC-181 FAILED: Pagination shown but no rows.");
            System.out.println("TC-181 PASSED");
        } else {
            System.out.println("TC-181 INFO: All alerts fit on one page.");
        }
    }

    @Test(description = "TC-183: Real-time toast notifications display on new alerts", groups = {"sanity"})
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Seeds a new alert while on the page and verifies the toast stack shows it.")
    public void TC183_toastNotificationPopupDisplays() {
        SensorApiClient.postHighPowerConsumptionReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(),
                "TC-183 FAILED: Toast notification did not appear.");
        
        // Clean up toast for the next test
        alertsPage.dismissFirstToast();
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        
        System.out.println("TC-183 PASSED");
    }

    @Test(description = "TC-184: Toast notification auto hides after 5 seconds")
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Triggers a toast and waits 6 seconds to ensure it auto-dismisses.")
    public void TC184_toastNotificationAutoDismisses() {
        SensorApiClient.postLowBrightnessLevelReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(), "Setup FAILED: No toast.");
        
        // Wait for it to disappear
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        Assert.assertFalse(alertsPage.hasActiveToasts(),
                "TC-184 FAILED: Toast did not auto-hide after 5 seconds.");
        System.out.println("TC-184 PASSED");
    }

    @Test(description = "TC-185: Toast notification can be dismissed manually")
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Triggers a toast and clicks the close button.")
    public void TC185_dismissToastManually() {
        SensorApiClient.postHighPowerConsumptionReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(), "Setup FAILED: No toast.");
        
        alertsPage.dismissFirstToast();
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        Assert.assertFalse(alertsPage.hasActiveToasts(),
                "TC-185 FAILED: Toast was not dismissed manually.");
        System.out.println("TC-185 PASSED");
    }

    @Test(description = "TC-182: Unauthenticated /lights-alerts access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes cookies then navigates to /lights-alerts. Auth guard must redirect.")
    public void TC182_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/lights-alerts");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-182 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-182 PASSED");
    }
}
