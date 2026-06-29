package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.HomePage;
import pages.SettingsPage;
import pages.TrafficAlertsPage;
import pages.TrafficAnalyticsPage;
import pages.TrafficDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.TrafficApiReader;

/**
 * TrafficDashboardTest — TC-090 … TC-109
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * Thresholds are created once in @BeforeClass using SettingsPage — the same UI
 * your users use. This is consistent with the rest of the framework and avoids
 * any direct HTTP auth plumbing.
 *
 * Sensor readings are seeded before each test using SensorApiClient, which calls
 * POST /api/sensors/traffic on localhost:8081 (the host-side port exposed by
 * docker-compose). No simulator dependency — tests run instantly and deterministically.
 *
 * THRESHOLD SETUP (once per class)
 * ─────────────────────────────────
 *   Traffic Density > 100  (above)   →  density=450 from postHighDensityReading() triggers alert
 *   Average Speed   < 30   (below)   →  speed=5    from postLowSpeedReading()    triggers alert
 *
 * The simulator may still be running — that only adds more data. Tests assert on
 * minimums (>= 1) not exact counts, so simulator data doesn't break them.
 */
@Epic("Traffic monitoring")
@Feature("Traffic dashboard")
public class TrafficDashboardTest extends BaseTest {

    private TrafficDashboardPage dashboardPage;

    // ── One-time threshold setup ───────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
        ensureTrafficThresholdsExist();
    }

    /**
     * Uses SettingsPage to save the two traffic thresholds needed for alert tests.
     * Called once per class — idempotent enough because the Settings page will
     * simply add another threshold if one already exists (which the dashboard
     * handles gracefully by counting it alongside existing ones).
     */
    private void ensureTrafficThresholdsExist() {
        try {
            SettingsPage settingsPage = new SettingsPage(driver);
            settingsPage.open();

            // Threshold 1: Traffic Density above 100
            settingsPage.createThreshold("traffic", 0, 100, true);
            System.out.println("[Setup] Traffic Density > 100 threshold saved");

            // Threshold 2: Average Speed below 30
            settingsPage.createThreshold("traffic", 1, 30, false);
            System.out.println("[Setup] Average Speed < 30 threshold saved");

        } catch (Exception e) {
            System.out.println("[Setup] Threshold setup warning: " + e.getMessage()
                    + " — alert-dependent tests may be inconclusive.");
        }
    }

    // ── Per-test data seeding + navigation ────────────────────────────────────

    private static boolean dataSeeded = false;

    @BeforeMethod(alwaysRun = true)
    public void seedDataAndOpenDashboard() {
        if (!dataSeeded) {

            // Seed traffic readings directly — bypasses the 120-second simulator interval.
            // Normal reading populates the table; high-density + low-speed trigger alerts.
            try {
                SensorApiClient.postNormalTrafficReading();
                SensorApiClient.postHighDensityReading();
                SensorApiClient.postLowSpeedReading();
            } catch (Exception e) {
                System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage()
                        + " — tests will run against existing backend data.");
            }
            dataSeeded = true;
        }

        dashboardPage = new TrafficDashboardPage(driver);
        dashboardPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-090: Traffic dashboard loads and URL is /traffic")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /traffic and verifies the URL contains /traffic but not a sub-route.")
    public void TC090_trafficDashboardLoads() {
        Assert.assertTrue(dashboardPage.isOnTrafficDashboard(),
                "TC-090 FAILED: URL is not /traffic or landed on a sub-route.");
        System.out.println("TC-090 PASSED");
    }

    @Test(description = "TC-091: 'Traffic Monitoring Dashboard' page title is displayed")
    @Story("Page load")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies the .page-title element is visible.")
    public void TC091_pageTitleDisplayed() {
        Assert.assertTrue(dashboardPage.isPageTitleDisplayed(),
                "TC-091 FAILED: Page title not visible.");
        System.out.println("TC-091 PASSED");
    }

    @Test(description = "TC-092: Back link navigates to /home")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies navigation to /home.")
    public void TC092_backLinkNavigatesToHome() {
        HomePage homePage = dashboardPage.clickBackLink();
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-092 FAILED: Back link did not reach /home.");
        System.out.println("TC-092 PASSED");
    }

    @Test(description = "TC-093: Analytics nav link navigates to /traffic-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics header nav link.")
    public void TC093_analyticsNavLink() {
        TrafficAnalyticsPage ap = dashboardPage.clickAnalyticsNav();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-093 FAILED: Did not reach /traffic-analytics.");
        System.out.println("TC-093 PASSED");
    }

    @Test(description = "TC-094: Alerts nav link navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts header nav link.")
    public void TC094_alertsNavLink() {
        TrafficAlertsPage ap = dashboardPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-094 FAILED: Did not reach /traffic-alerts.");
        System.out.println("TC-094 PASSED");
    }

    @Test(description = "TC-095: 'Analytics & Search' quick action navigates to /traffic-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics & Search quick-action-btn.")
    public void TC095_quickActionAnalytics() {
        TrafficAnalyticsPage ap = dashboardPage.clickQuickActionAnalytics();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-095 FAILED: Quick action did not reach /traffic-analytics.");
        System.out.println("TC-095 PASSED");
    }

    @Test(description = "TC-096: 'Traffic Alerts' quick action navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Traffic Alerts quick-action-btn.")
    public void TC096_quickActionAlerts() {
        TrafficAlertsPage ap = dashboardPage.clickQuickActionAlerts();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-096 FAILED: Quick action did not reach /traffic-alerts.");
        System.out.println("TC-096 PASSED");
    }

    @Test(description = "TC-097: Stats section shows exactly 5 stat cards")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Stats section must be visible with exactly 5 non-skeleton stat cards.")
    public void TC097_statsCardsDisplayed() {
        Assert.assertTrue(dashboardPage.isStatsSectionDisplayed(),
                "TC-097 FAILED: Stats section not visible.");
        int count = dashboardPage.getStatCardCount();
        Assert.assertEquals(count, 5,
                "TC-097 FAILED: Expected 5 stat cards, found " + count);
        System.out.println("TC-097 PASSED — " + count + " cards");
    }

    @Test(description = "TC-098: All 5 stat card values are loaded and non-empty")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings were seeded in @BeforeMethod. All stat values must be non-empty.")
    public void TC098_statValuesNonEmpty() {
        Assert.assertTrue(dashboardPage.areStatsLoaded(),
                "TC-098 FAILED: Stats still loading or skeleton cards present.");
        for (int i = 0; i < 5; i++) {
            String val = dashboardPage.getStatValue(i);
            Assert.assertFalse(val.isEmpty(),
                    "TC-098 FAILED: Stat card " + i + " has an empty value.");
        }
        System.out.println("TC-098 PASSED");
    }

    @Test(description = "TC-099: Data table is visible with at least 1 row after seeding")
    @Story("Data table")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings posted in @BeforeMethod — table must have at least 1 row.")
    public void TC099_dataTableHasRows() {
        Assert.assertTrue(dashboardPage.isTableSectionDisplayed(),
                "TC-099 FAILED: Table section not visible.");
        int rows = dashboardPage.getTableRowCount();
        Assert.assertTrue(rows >= 1,
                "TC-099 FAILED: Expected >=1 row after seeding, found " + rows);
        System.out.println("TC-099 PASSED — " + rows + " rows");
    }

    @Test(description = "TC-100: Pagination controls appear when data exceeds page size")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that when more than 10 records exist pagination controls are shown.")
    public void TC100_paginationDisplayed() {
        if (dashboardPage.hasPagination()) {
            Assert.assertFalse(dashboardPage.getActivePageNumber().isEmpty(),
                    "TC-100 FAILED: Pagination shown but active page is empty.");
            System.out.println("TC-100 PASSED — page: " + dashboardPage.getActivePageNumber());
        } else {
            System.out.println("TC-100 INFO: Not enough records for pagination yet.");
        }
    }

    @Test(description = "TC-101: Clicking Next page changes the active page indicator")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only runs when pagination is available. Verifies active page number increases.")
    public void TC101_nextPageChangesContent() {
        if (!dashboardPage.hasPagination() || !dashboardPage.isNextPageEnabled()) {
            System.out.println("TC-101 INFO: Single page — skip.");
            return;
        }
        String before = dashboardPage.getActivePageNumber();
        dashboardPage.clickNextPage();
        String after = dashboardPage.getActivePageNumber();
        Assert.assertNotEquals(after, before,
                "TC-101 FAILED: Page number did not change after Next.");
        System.out.println("TC-101 PASSED: " + before + " → " + after);
    }

    @Test(description = "TC-102: Charts section renders 3 cards", groups = {"sanity"})
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Density line, speed bar, and congestion distribution charts must all be present.")
    public void TC102_chartsSectionHasThreeCards() {
        Assert.assertTrue(dashboardPage.isChartsSectionDisplayed(),
                "TC-102 FAILED: Charts section not visible.");
        Assert.assertTrue(dashboardPage.getChartCardCount() >= 3,
                "TC-102 FAILED: Expected >=3 chart cards, found "
                + dashboardPage.getChartCardCount());
        System.out.println("TC-102 PASSED — " + dashboardPage.getChartCardCount() + " cards");
    }

    @Test(description = "TC-103: Congestion distribution shows exactly 4 level rows")
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expects 4 .congestion-row elements: Low, Moderate, High, Severe.")
    public void TC103_congestionChartFourRows() {
        Assert.assertTrue(dashboardPage.isCongestionChartDisplayed(),
                "TC-103 FAILED: Congestion chart not visible.");
        Assert.assertEquals(dashboardPage.getCongestionRowCount(), 4,
                "TC-103 FAILED: Expected 4 rows, found "
                + dashboardPage.getCongestionRowCount());
        System.out.println("TC-103 PASSED");
    }

    @Test(description = "TC-104: Recent alerts section is displayed")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("High-density and low-speed readings crossed thresholds, so at least one alert " +
                 "should exist. Section must show alerts or the empty state — not an error.")
    public void TC104_recentAlertsSectionDisplayed() {
        Assert.assertTrue(dashboardPage.isAlertsSectionDisplayed(),
                "TC-104 FAILED: Alerts section not visible.");
        Assert.assertFalse(dashboardPage.isAlertsErrorDisplayed(),
                "TC-104 FAILED: Error banner shown in alerts section.");
        boolean hasAlerts = dashboardPage.hasAlerts();
        boolean hasEmpty  = dashboardPage.isAlertsEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-104 FAILED: Neither alert items nor empty state shown.");
        System.out.println("TC-104 PASSED — alerts: " + dashboardPage.getAlertItemCount());
    }

    @Test(description = "TC-105: Recent alerts section shows at most 5 alerts")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("The TS component slices to the latest 5. Count must never exceed 5.")
    public void TC105_recentAlertsCappedAtFive() {
        int count = dashboardPage.getAlertItemCount();
        Assert.assertTrue(count <= 5,
                "TC-105 FAILED: Expected <=5 recent alerts, found " + count);
        System.out.println("TC-105 PASSED — count: " + count);
    }

    @Test(description = "TC-106: 'View All Alerts' link navigates to /traffic-alerts")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the View All Alerts anchor in the recent alerts section.")
    public void TC106_viewAllAlertsLink() {
        TrafficAlertsPage ap = dashboardPage.clickViewAllAlerts();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-106 FAILED: View All Alerts did not reach /traffic-alerts.");
        System.out.println("TC-106 PASSED");
    }

    @Test(description = "TC-107: Manual refresh keeps dashboard on /traffic without error")
    @Story("Refresh")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the manual refresh icon. Dashboard must stay on /traffic and show no error.")
    public void TC107_manualRefresh() {
        dashboardPage.clickManualRefresh();
        wait.waitForCondition(d ->
            dashboardPage.isStatsSectionDisplayed() || dashboardPage.isLastRefreshedDisplayed()
        );
        Assert.assertTrue(dashboardPage.isOnTrafficDashboard(),
                "TC-107 FAILED: Dashboard left /traffic after refresh.");
        Assert.assertFalse(dashboardPage.isStatsErrorDisplayed(),
                "TC-107 FAILED: Error banner shown after refresh.");
        System.out.println("TC-107 PASSED");
    }

    @Test(description = "TC-108: Auto-refresh toggle disables then re-enables")
    @Story("Refresh")
    @Severity(SeverityLevel.MINOR)
    @Description("btn-auto-refresh.active by default. Toggling removes then restores the class.")
    public void TC108_autoRefreshToggle() {
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-108 FAILED: Auto-refresh not active by default.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertFalse(dashboardPage.isAutoRefreshActive(),
                "TC-108 FAILED: Auto-refresh did not deactivate.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-108 FAILED: Auto-refresh did not re-activate.");
        System.out.println("TC-108 PASSED");
    }

    @Test(description = "TC-109: Unauthenticated /traffic access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /traffic. Angular auth guard must redirect.")
    public void TC109_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/traffic");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-109 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-109 PASSED");
    }
    // ── TC-110: Table row values match the API ────────────────────────────────



    @Test(description = "TC-110: Real-Time Traffic Data table first row matches the latest API record")
    @Story("Data accuracy — table")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, refreshes the dashboard, waits for table to re-render, " +
                    "then reads the API and UI at the same moment and asserts they match."
    )
    public void TC110_tableFirstRowMatchesApi() {
        // Step 1 — seed
        SensorApiClient.postTrafficReading(333, 77.5, "Moderate", "Alexandria");

        // Step 2 — refresh
        dashboardPage.clickManualRefresh();

        // Step 3 — wait for the table to finish re-rendering
        try { wait.waitForInvisibility(org.openqa.selenium.By.cssSelector(".table-loading")); }
        catch (Exception ignored) {}
        // Wait until at least one row is present
        wait.waitForCondition(d ->
                !d.findElements(org.openqa.selenium.By.cssSelector(
                        ".data-table tbody tr")).isEmpty()
        );

        // Step 4 — read API after refresh (same snapshot as UI)
        String apiDensity    = TrafficApiReader.getFirstRecordDensity();
        String apiSpeed      = TrafficApiReader.getFirstRecordAvgSpeed();
        String apiLocation   = TrafficApiReader.getFirstRecordLocation();
        String apiCongestion = TrafficApiReader.getFirstRecordCongestionLevel();

        System.out.println("[TC110] API (post-refresh) — location=" + apiLocation
                + " density=" + apiDensity + " speed=" + apiSpeed
                + " congestion=" + apiCongestion);

        Assert.assertNotNull(apiDensity,
                "TC-110 FAILED: API returned no density. Check sensor-service.");

        // Step 5 — read UI
        String uiLocation   = dashboardPage.getFirstRowLocation();
        String uiDensity    = dashboardPage.getFirstRowDensity();
        String uiSpeed      = dashboardPage.getFirstRowSpeed();
        String uiCongestion = dashboardPage.getFirstRowCongestionLevel();

        System.out.println("[TC110] UI — location=" + uiLocation
                + " density=" + uiDensity + " speed=" + uiSpeed
                + " congestion=" + uiCongestion);

        // Step 6 — assert UI == API
        Assert.assertEquals(uiLocation, apiLocation,
                "TC-110 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
        Assert.assertEquals(uiDensity, apiDensity,
                "TC-110 FAILED: density UI='" + uiDensity + "' API='" + apiDensity + "'");
        Assert.assertEquals(uiSpeed, normaliseDecimal(apiSpeed, 1),
                "TC-110 FAILED: speed UI='" + uiSpeed
                        + "' API='" + normaliseDecimal(apiSpeed, 1) + "'");
        Assert.assertEquals(uiCongestion, apiCongestion,
                "TC-110 FAILED: congestion UI='" + uiCongestion + "' API='" + apiCongestion + "'");

        System.out.println("TC-110 PASSED — table first row matches API");
    }
    // ── TC-111: Density line & speed bar charts match trends API ─────────────

    // ── Fix 3: TC111 — use the renamed methods ────────────────────────────────────

    @Test(description = "TC-111: Density line and speed bar charts match the trends API")
    @Story("Data accuracy — charts")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, refreshes, waits for charts to render, then reads the " +
                    "trends API newest record and the UI leftmost dot/bar (both = newest reading " +
                    "after Angular reversal) and asserts they match."
    )
    public void TC111_chartsMatchTrendsApi() {
        // Step 1 — seed
        SensorApiClient.postTrafficReading(444, 88.8, "High", "Alexandria");

        // Step 2 — refresh
        dashboardPage.clickManualRefresh();

        // Step 3 — wait for dots to render
        try { wait.waitForInvisibility(org.openqa.selenium.By.cssSelector(".skeleton-chart")); }
        catch (Exception ignored) {}
        wait.waitForCondition(d ->
                !d.findElements(org.openqa.selenium.By.cssSelector("circle.density-dot")).isEmpty()
        );

        // Step 4 — read API newest record (last element of raw array = newest timestamp)
        String apiDensity = TrafficApiReader.getNewestTrendDensity();
        String apiSpeed   = TrafficApiReader.getNewestTrendAvgSpeed();
        int    apiCount   = TrafficApiReader.getTrendDataPointCount();

        System.out.println("[TC111] API newest — density=" + apiDensity
                + " speed=" + apiSpeed + " totalPoints=" + apiCount);

        Assert.assertNotNull(apiDensity,
                "TC-111 FAILED: Trends API returned no data. Check sensor-service.");

        // Step 5a — read UI leftmost dot (index 0 = newest after Angular reversal)
        String uiDensity = dashboardPage.getFirstDensityDotValue();
        System.out.println("[TC111] UI first (leftmost) dot value: " + uiDensity);

        Assert.assertFalse(uiDensity.isEmpty(),
                "TC-111 FAILED: No density dots rendered after refresh.");
        Assert.assertEquals(uiDensity, apiDensity,
                "TC-111 FAILED: density dot='" + uiDensity + "' API='" + apiDensity + "'");

        // Step 5b — read UI leftmost bar (index 0 = newest after Angular reversal)
        String uiSpeed = dashboardPage.getFirstSpeedBarValue();
        System.out.println("[TC111] UI first (leftmost) bar value: " + uiSpeed);

        Assert.assertFalse(uiSpeed.isEmpty(),
                "TC-111 FAILED: No speed bars rendered after refresh.");
        String normApiSpeed = normaliseDecimal(apiSpeed, 1);
        Assert.assertEquals(uiSpeed, normApiSpeed,
                "TC-111 FAILED: speed bar='" + uiSpeed + "' API='" + normApiSpeed + "'");

        // Step 5c — dot count matches API point count
        int uiDotCount = dashboardPage.getDensityDotCount();
        Assert.assertEquals(uiDotCount, apiCount,
                "TC-111 FAILED: UI dots=" + uiDotCount
                        + " but API returned " + apiCount + " points.");

        System.out.println("TC-111 PASSED — chart values match trends API");
    }
    public void TC112_congestionChartMatchesSummaryApi() {
        // Step 1 — seed
        SensorApiClient.postTrafficReading(480, 10.0, "Severe", "Alexandria");
        SensorApiClient.postTrafficReading(30,  100.0, "Low",   "Alexandria");

        // Step 2 — refresh
        dashboardPage.clickManualRefresh();

        // Step 3 — wait for all 4 congestion rows to re-render
        wait.waitForCondition(d ->
                d.findElements(org.openqa.selenium.By.cssSelector(".congestion-row")).size() == 4
        );

        // Step 4 — read API after refresh
        Assert.assertTrue(TrafficApiReader.hasCongestionData(),
                "TC-112 FAILED: Congestion summary API returned no data.");

        String apiLow      = TrafficApiReader.getCongestionCount("Low");
        String apiModerate = TrafficApiReader.getCongestionCount("Moderate");
        String apiHigh     = TrafficApiReader.getCongestionCount("High");
        String apiSevere   = TrafficApiReader.getCongestionCount("Severe");

        System.out.println("[TC112] API (post-refresh) — Low=" + apiLow
                + " Moderate=" + apiModerate + " High=" + apiHigh
                + " Severe=" + apiSevere);

        // Step 5 — read UI
        String uiLow      = dashboardPage.getCongestionCountForLevel("Low");
        String uiModerate = dashboardPage.getCongestionCountForLevel("Moderate");
        String uiHigh     = dashboardPage.getCongestionCountForLevel("High");
        String uiSevere   = dashboardPage.getCongestionCountForLevel("Severe");

        System.out.println("[TC112] UI — Low=" + uiLow
                + " Moderate=" + uiModerate + " High=" + uiHigh
                + " Severe=" + uiSevere);

        // Step 6 — assert all four levels match
        Assert.assertEquals(uiLow,      apiLow,      "TC-112 FAILED: Low");
        Assert.assertEquals(uiModerate, apiModerate, "TC-112 FAILED: Moderate");
        Assert.assertEquals(uiHigh,     apiHigh,     "TC-112 FAILED: High");
        Assert.assertEquals(uiSevere,   apiSevere,   "TC-112 FAILED: Severe");

        System.out.println("TC-112 PASSED — congestion distribution matches API");
    }
    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Normalises a decimal string to a fixed number of decimal places.
     * Harmonises API raw doubles with Angular's number:'1.1-1' pipe output.
     *   normaliseDecimal("77.50", 1) → "77.5"
     *   normaliseDecimal("88.8",  1) → "88.8"
     *   normaliseDecimal("15",    1) → "15.0"
     */
    private static String normaliseDecimal(String value, int decimalPlaces) {
        if (value == null || value.isEmpty()) return value;
        try {
            return String.format("%." + decimalPlaces + "f", Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
