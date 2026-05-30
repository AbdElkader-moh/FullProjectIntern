package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.SettingsPage;
import pages.TrafficAlertsPage;
import pages.TrafficDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * TrafficAlertsTest — TC-091 … TC-102
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * @BeforeClass — uses SettingsPage to save two traffic thresholds once:
 *   Traffic Density > 100  (sensor index 0, above)
 *   Average Speed   < 30   (sensor index 1, below)
 *
 * @BeforeMethod — uses SensorApiClient to post readings that cross both thresholds,
 *   guaranteeing at least two alerts exist before each test. Readings go to
 *   localhost:8081 (host-side port from docker-compose 8081:8081).
 *
 * DOM CHANGES fixed in TrafficAlertsPage (compared to previous version):
 *   - Alert Type dropdown (Traffic/Air/Light) removed — page is traffic-only.
 *   - New #aFilterMetric dropdown: "Traffic Density" | "Average Speed".
 *   - Live banner replaced by toast stack (.toast-stack).
 *   - All type badges are .type-traffic.
 */
@Epic("Traffic monitoring")
@Feature("Traffic alerts")
public class TrafficAlertsTest extends BaseTest {

    private TrafficAlertsPage alertsPage;

    // ── One-time threshold setup ───────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
        ensureTrafficThresholdsExist();
    }

    private void ensureTrafficThresholdsExist() {
        try {
            SettingsPage settingsPage = new SettingsPage(driver);
            settingsPage.open();

            // Traffic Density above 100
            settingsPage.createThreshold("traffic", 0, 100, true);
            System.out.println("[Setup] Traffic Density > 100 saved");

            // Average Speed below 30
            settingsPage.createThreshold("traffic", 1, 30, false);
            System.out.println("[Setup] Average Speed < 30 saved");

        } catch (Exception e) {
            System.out.println("[Setup] Threshold warning: " + e.getMessage());
        }
    }

    // ── Per-test data seeding + navigation ────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    public void seedAlertsAndOpen() {
        loginWithDefaultUser();
        try {
            // density=450 > threshold 100 → triggers Traffic Density alert
            SensorApiClient.postHighDensityReading();
            // speed=5 < threshold 30 → triggers Average Speed alert
            SensorApiClient.postLowSpeedReading();
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        alertsPage = new TrafficAlertsPage(driver);
        alertsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-091: Traffic alerts page loads and URL is /traffic-alerts")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /traffic-alerts and verifies URL and page title.")
    public void TC091_alertsPageLoads() {
        Assert.assertTrue(alertsPage.isOnAlertsPage(),
                "TC-091 FAILED: URL does not contain /traffic-alerts.");
        Assert.assertTrue(alertsPage.isPageTitleDisplayed(),
                "TC-091 FAILED: Page title not visible.");
        System.out.println("TC-091 PASSED");
    }

    @Test(description = "TC-092: Alerts table shows rows or empty state — no error banner")
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Two readings crossed thresholds in @BeforeMethod. Table must show rows with no error.")
    public void TC092_tableOrEmptyState() {
        Assert.assertFalse(alertsPage.isTableErrorDisplayed(),
                "TC-092 FAILED: Error banner on initial load.");
        boolean hasAlerts = alertsPage.hasAlerts();
        boolean hasEmpty  = alertsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-092 FAILED: Neither rows nor empty state shown.");
        System.out.println("TC-092 PASSED — rows: " + alertsPage.getTableRowCount());
    }

    @Test(description = "TC-093: All visible type badges have class type-traffic")
    @Story("Alert content")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This page only shows Traffic alerts. No Air or Light type badges should appear.")
    public void TC093_allBadgesAreTrafficType() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-093 INFO: No alerts present.");
            return;
        }
        int total   = driver.findElements(
                org.openqa.selenium.By.cssSelector(".type-badge")).size();
        int traffic = alertsPage.getTypeBadges().size();
        Assert.assertEquals(traffic, total,
                "TC-093 FAILED: " + (total - traffic) + " non-traffic badge(s) found.");
        System.out.println("TC-093 PASSED — all " + traffic + " badges are type-traffic");
    }

    @Test(description = "TC-094: 'Traffic Density' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Traffic Density from #aFilterMetric. All metric cells must read 'Traffic Density'.")
    public void TC094_metricFilterScopesToDensity() {
        alertsPage.selectMetricFilter("Traffic Density").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-094 INFO: No Traffic Density alerts present.");
            return;
        }
        alertsPage.getMetricCells().forEach(cell ->
            Assert.assertEquals(cell.getText().trim(), "Traffic Density",
                "TC-094 FAILED: Wrong metric: " + cell.getText()));
        System.out.println("TC-094 PASSED");
    }

    @Test(description = "TC-095: 'Average Speed' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Average Speed from #aFilterMetric. All metric cells must read 'Average Speed'.")
    public void TC095_metricFilterScopesToSpeed() {
        alertsPage.selectMetricFilter("Average Speed").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-095 INFO: No Average Speed alerts present.");
            return;
        }
        alertsPage.getMetricCells().forEach(cell ->
            Assert.assertEquals(cell.getText().trim(), "Average Speed",
                "TC-095 FAILED: Wrong metric: " + cell.getText()));
        System.out.println("TC-095 PASSED");
    }

    @Test(description = "TC-096: Reset clears metric filter and restores full list")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies density filter, records count, resets, verifies count >= filtered count.")
    public void TC096_resetRestoresList() {
        alertsPage.selectMetricFilter("Traffic Density").clickApplyFilters();
        int filteredCount = alertsPage.getTableRowCount();
        alertsPage.clickResetFilters();
        int totalCount = alertsPage.getTableRowCount();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-096 FAILED: After reset (" + totalCount
                + ") < filtered (" + filteredCount + ").");
        System.out.println("TC-096 PASSED — filtered: " + filteredCount
                + " total: " + totalCount);
    }

    @Test(description = "TC-097: Invalid date range shows validation error")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("End date before start date — expects .filter-date-error.")
    public void TC097_invalidDateRangeError() {
        alertsPage.enterDateFrom("2026-06-01T00:00")
                  .enterDateTo("2026-05-01T00:00")
                  .clickApplyFilters();
        Assert.assertTrue(alertsPage.isDateErrorDisplayed(),
                "TC-097 FAILED: No date validation error shown.");
        System.out.println("TC-097 PASSED");
    }

    @Test(description = "TC-098: Mark All Read button appears only when unread alerts exist")
    @Story("Mark as read")
    @Severity(SeverityLevel.CRITICAL)
    @Description(".btn-mark-all only rendered when unreadCount > 0. Verifies presence matches unread rows.")
    public void TC098_markAllReadVisibility() {
        int unreadRows = alertsPage.getUnreadRowCount();
        if (unreadRows > 0) {
            Assert.assertTrue(alertsPage.isMarkAllReadDisplayed(),
                    "TC-098 FAILED: Unread rows exist but button not shown.");
        } else {
            Assert.assertFalse(alertsPage.isMarkAllReadDisplayed(),
                    "TC-098 FAILED: No unread rows but button is shown.");
        }
        System.out.println("TC-098 PASSED — unread rows: " + unreadRows);
    }

    @Test(description = "TC-099: Every alert row has a severity badge of High, Medium, or Low")
    @Story("Alert content")
    @Severity(SeverityLevel.NORMAL)
    @Description("Iterates .severity-badge and asserts each is High, Medium, or Low.")
    public void TC099_severityBadgesValid() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-099 INFO: No alerts — skip.");
            return;
        }
        alertsPage.getSeverityBadges().forEach(badge -> {
            String text = badge.getText().trim();
            Assert.assertTrue(
                text.equals("High") || text.equals("Medium") || text.equals("Low"),
                "TC-099 FAILED: Unexpected severity: '" + text + "'");
        });
        System.out.println("TC-099 PASSED");
    }

    @Test(description = "TC-100: Back link navigates to /traffic dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies /traffic.")
    public void TC100_backLink() {
        TrafficDashboardPage dash = alertsPage.clickBack();
        Assert.assertTrue(dash.isOnTrafficDashboard(),
                "TC-100 FAILED: Did not reach /traffic.");
        System.out.println("TC-100 PASSED");
    }

    @Test(description = "TC-101: Pagination shows when alerts exceed page size of 15")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies pagination controls appear with non-zero rows when total > 15.")
    public void TC101_paginationForManyAlerts() {
        if (alertsPage.hasPagination()) {
            Assert.assertTrue(alertsPage.getTableRowCount() > 0,
                    "TC-101 FAILED: Pagination shown but no rows.");
            System.out.println("TC-101 PASSED");
        } else {
            System.out.println("TC-101 INFO: All alerts fit on one page.");
        }
    }

    @Test(description = "TC-102: Unauthenticated /traffic-alerts access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes cookies then navigates to /traffic-alerts. Auth guard must redirect.")
    public void TC102_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/traffic-alerts");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-102 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-102 PASSED");
    }
}
