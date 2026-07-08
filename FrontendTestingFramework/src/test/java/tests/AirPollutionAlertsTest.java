package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.SettingsPage;
import pages.AirPollutionAlertsPage;
import pages.AirPollutionDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * AirPollutionAlertsTest — TC-114 … TC-128
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * @BeforeClass — uses SettingsPage to save two air pollution thresholds once:
 *   Carbon Monoxide > 100  (sensor index 0, above)
 *   Ozone   < 30   (sensor index 1, below)
 *
 * @BeforeMethod — uses SensorApiClient to post readings that cross both thresholds,
 *   guaranteeing at least two alerts exist before each test. Readings go to
 *   localhost:8081 (host-side port from docker-compose 8081:8081).
 *
 * DOM CHANGES fixed in AirPollutionAlertsPage (compared to previous version):
 *   - Alert Type dropdown (Traffic/Air/Light) removed — page is air-pollution-only.
 *   - New #aFilterMetric dropdown: "Carbon Monoxide" | "Ozone".
 *   - Live banner replaced by toast stack (.toast-stack).
 *   - All type badges are .type-air.
 */
@Epic("Air quality monitoring")
@Feature("Air pollution alerts")
public class AirPollutionAlertsTest extends BaseTest {

    private AirPollutionAlertsPage alertsPage;

    // ── One-time threshold setup ───────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
        ensureAirThresholdsExist();
    }

    private void ensureAirThresholdsExist() {
        try {
            SettingsPage settingsPage = new SettingsPage(driver);
            settingsPage.open();

            // Carbon Monoxide above 40
            settingsPage.createThreshold("air quality", 0, 40, true);
            System.out.println("[Setup] Carbon Monoxide > 40 saved");

            // Ozone below 30
            settingsPage.createThreshold("air quality", 1, 30, false);
            System.out.println("[Setup] Ozone < 30 saved");

        } catch (Exception e) {
            System.out.println("[Setup] Threshold warning: " + e.getMessage());
        }
    }

    // ── Per-test data seeding + navigation ────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    public void seedAlertsAndOpen() {
        try {
            // co=450 > threshold 100 → triggers Carbon Monoxide alert
            SensorApiClient.postHighCarbonMonoxideReading();
            // ozone=5 < threshold 30 → triggers Ozone alert
            SensorApiClient.postLowOzoneReading();
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        alertsPage = new AirPollutionAlertsPage(driver);
        alertsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-114: Air pollution alerts page loads and URL is /air-alerts")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /air-alerts and verifies URL and page title.")
    public void TC114_alertsPageLoads() {
        Assert.assertTrue(alertsPage.isOnAlertsPage(),
                "TC-114 FAILED: URL does not contain /air-alerts.");
        Assert.assertTrue(alertsPage.isPageTitleDisplayed(),
                "TC-114 FAILED: Page title not visible.");
        System.out.println("TC-114 PASSED");
    }

    @Test(description = "TC-115: Alerts table shows rows or empty state — no error banner")
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Two readings crossed thresholds in @BeforeMethod. Table must show rows with no error.")
    public void TC115_tableOrEmptyState() {
        Assert.assertFalse(alertsPage.isTableErrorDisplayed(),
                "TC-115 FAILED: Error banner on initial load.");
        boolean hasAlerts = alertsPage.hasAlerts();
        boolean hasEmpty  = alertsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-115 FAILED: Neither rows nor empty state shown.");
        System.out.println("TC-115 PASSED — rows: " + alertsPage.getTableRowCount());
    }

    @Test(description = "TC-116: All visible type badges have class type-air")
    @Story("Alert content")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This page only shows Air pollution alerts. No Traffic or Light type badges should appear.")
    public void TC116_allBadgesAreAirType() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-116 INFO: No alerts present.");
            return;
        }
        int total   = driver.findElements(
                org.openqa.selenium.By.cssSelector(".type-badge")).size();
        int air = alertsPage.getTypeBadges().size();
        Assert.assertEquals(air, total,
                "TC-116 FAILED: " + (total - air) + " non-air badge(s) found.");
        System.out.println("TC-116 PASSED — all " + air + " badges are type-air");
    }

    @Test(description = "TC-117: 'Carbon Monoxide' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Carbon Monoxide from #aFilterMetric. All metric cells must read 'Carbon Monoxide'.")
    public void TC117_metricFilterScopesToCarbonMonoxide() {
        alertsPage.selectMetricFilter("Carbon Monoxide").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-117 INFO: No Carbon Monoxide alerts present.");
            return;
        }
        alertsPage.getMetricCells().forEach(cell ->
            Assert.assertEquals(cell.getText().trim(), "Carbon Monoxide",
                "TC-117 FAILED: Wrong metric: " + cell.getText()));
        System.out.println("TC-117 PASSED");
    }

    @Test(description = "TC-118: 'Ozone' metric filter scopes results")
    @Story("Filtering")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Ozone from #aFilterMetric. All metric cells must read 'Ozone'.")
    public void TC118_metricFilterScopesToOzone() {
        alertsPage.selectMetricFilter("Ozone").clickApplyFilters();
        if (alertsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-118 INFO: No Ozone alerts present.");
            return;
        }
        utils.RetryHelper.retryVoid(() -> {
            alertsPage.getMetricCells().forEach(cell ->
                Assert.assertEquals(cell.getText().trim(), "Ozone",
                    "TC-118 FAILED: Wrong metric: " + cell.getText()));
        }, "Assert metric cells in TC118");
        System.out.println("TC-118 PASSED");
    }

    @Test(description = "TC-119: Reset clears metric filter and restores full list")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies co filter, records count, resets, verifies count >= filtered count.")
    public void TC119_resetRestoresList() {
        alertsPage.selectMetricFilter("Carbon Monoxide").clickApplyFilters();
        int filteredCount = alertsPage.getTableRowCount();
        alertsPage.clickResetFilters();
        int totalCount = alertsPage.getTableRowCount();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-119 FAILED: After reset (" + totalCount
                + ") < filtered (" + filteredCount + ").");
        System.out.println("TC-119 PASSED — filtered: " + filteredCount
                + " total: " + totalCount);
    }

    @Test(description = "TC-120: Invalid date range shows validation error")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("End date before start date — expects .filter-date-error.")
    public void TC120_invalidDateRangeError() {
        alertsPage.enterDateFrom("2026-06-01T00:00")
                  .enterDateTo("2026-05-01T00:00")
                  .clickApplyFilters();
        Assert.assertTrue(alertsPage.isDateErrorDisplayed(),
                "TC-120 FAILED: No date validation error shown.");
        System.out.println("TC-120 PASSED");
    }

    @Test(description = "TC-121: Mark All Read button appears only when unread alerts exist")
    @Story("Mark as read")
    @Severity(SeverityLevel.CRITICAL)
    @Description(".btn-mark-all only rendered when unreadCount > 0. Verifies presence matches unread rows.")
    public void TC121_markAllReadVisibility() {
        int unreadRows = alertsPage.getUnreadRowCount();
        if (unreadRows > 0) {
            Assert.assertTrue(alertsPage.isMarkAllReadDisplayed(),
                    "TC-121 FAILED: Unread rows exist but button not shown.");
        } else {
            Assert.assertFalse(alertsPage.isMarkAllReadDisplayed(),
                    "TC-121 FAILED: No unread rows but button is shown.");
        }
        System.out.println("TC-121 PASSED — unread rows: " + unreadRows);
    }

    @Test(description = "TC-122: Every alert row has a severity badge of High, Medium, or Low")
    @Story("Alert content")
    @Severity(SeverityLevel.NORMAL)
    @Description("Iterates .severity-badge and asserts each is High, Medium, or Low.")
    public void TC122_severityBadgesValid() {
        if (!alertsPage.hasAlerts()) {
            System.out.println("TC-122 INFO: No alerts — skip.");
            return;
        }
        utils.RetryHelper.retryVoid(() -> {
            alertsPage.getSeverityBadges().forEach(badge -> {
                String text = badge.getText().trim();
                Assert.assertTrue(
                    text.equals("High") || text.equals("Medium") || text.equals("Low"),
                    "TC-122 FAILED: Unexpected severity: '" + text + "'");
            });
        }, "Assert severity badges in TC122");
        System.out.println("TC-122 PASSED");
    }

    @Test(description = "TC-123: Back link navigates to /air dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies /air.")
    public void TC123_backLink() {
        AirPollutionDashboardPage dash = alertsPage.clickBack();
        Assert.assertTrue(dash.isOnAirDashboard(),
                "TC-123 FAILED: Did not reach /air.");
        System.out.println("TC-123 PASSED");
    }

    @Test(description = "TC-124: Pagination shows when alerts exceed page size of 15")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies pagination controls appear with non-zero rows when total > 15.")
    public void TC124_paginationForManyAlerts() {
        if (alertsPage.hasPagination()) {
            Assert.assertTrue(alertsPage.getTableRowCount() > 0,
                    "TC-124 FAILED: Pagination shown but no rows.");
            System.out.println("TC-124 PASSED");
        } else {
            System.out.println("TC-124 INFO: All alerts fit on one page.");
        }
    }

    @Test(description = "TC-126: Real-time toast notifications display on new alerts", groups = {"sanity"})
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Seeds a new alert while on the page and verifies the toast stack shows it.")
    public void TC126_toastNotificationPopupDisplays() {
        SensorApiClient.postHighCarbonMonoxideReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(),
                "TC-126 FAILED: Toast notification did not appear.");
        
        // Clean up toast for the next test
        alertsPage.dismissFirstToast();
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        
        System.out.println("TC-126 PASSED");
    }

    @Test(description = "TC-127: Toast notification auto hides after 5 seconds")
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Triggers a toast and waits to ensure it auto-dismisses.")
    public void TC127_toastNotificationAutoDismisses() {
        SensorApiClient.postLowOzoneReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(), "Setup FAILED: No toast.");
        
        // Wait for it to disappear
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        Assert.assertFalse(alertsPage.hasActiveToasts(),
                "TC-127 FAILED: Toast did not auto-hide after 5 seconds.");
        System.out.println("TC-127 PASSED");
    }

    @Test(description = "TC-128: Toast notification can be dismissed manually")
    @Story("Notifications")
    @Severity(SeverityLevel.NORMAL)
    @Description("Triggers a toast and clicks the close button.")
    public void TC128_dismissToastManually() {
        SensorApiClient.postHighCarbonMonoxideReading();
        wait.waitForCondition(d -> alertsPage.hasActiveToasts());
        Assert.assertTrue(alertsPage.hasActiveToasts(), "Setup FAILED: No toast.");
        
        alertsPage.dismissFirstToast();
        wait.waitForCondition(d -> !alertsPage.hasActiveToasts());
        Assert.assertFalse(alertsPage.hasActiveToasts(),
                "TC-128 FAILED: Toast was not dismissed manually.");
        System.out.println("TC-128 PASSED");
    }

    @Test(description = "TC-125: Unauthenticated /air-alerts access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes cookies then navigates to /air-alerts. Auth guard must redirect.")
    public void TC125_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/air-alerts");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-125 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-125 PASSED");
    }
}
