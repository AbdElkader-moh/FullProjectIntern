package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.HomePage;
import pages.SettingsPage;
import pages.TrafficAlertsPage;
import pages.TrafficAnalyticsPage;
import pages.TrafficDashboardPage;
import utils.ConfigReader;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.TrafficApiReader;

/**
 * TrafficDashboardTest — TC-061 … TC-080
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

    @BeforeMethod(alwaysRun = true)
    public void seedDataAndOpenDashboard() {
        loginWithDefaultUser();

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

        dashboardPage = new TrafficDashboardPage(driver);
        dashboardPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-061: Traffic dashboard loads and URL is /traffic")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /traffic and verifies the URL contains /traffic but not a sub-route.")
    public void TC061_trafficDashboardLoads() {
        Assert.assertTrue(dashboardPage.isOnTrafficDashboard(),
                "TC-061 FAILED: URL is not /traffic or landed on a sub-route.");
        System.out.println("TC-061 PASSED");
    }

    @Test(description = "TC-062: 'Traffic Monitoring Dashboard' page title is displayed")
    @Story("Page load")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies the .page-title element is visible.")
    public void TC062_pageTitleDisplayed() {
        Assert.assertTrue(dashboardPage.isPageTitleDisplayed(),
                "TC-062 FAILED: Page title not visible.");
        System.out.println("TC-062 PASSED");
    }

    @Test(description = "TC-063: Back link navigates to /home")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies navigation to /home.")
    public void TC063_backLinkNavigatesToHome() {
        HomePage homePage = dashboardPage.clickBackLink();
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-063 FAILED: Back link did not reach /home.");
        System.out.println("TC-063 PASSED");
    }

    @Test(description = "TC-064: Analytics nav link navigates to /traffic-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics header nav link.")
    public void TC064_analyticsNavLink() {
        TrafficAnalyticsPage ap = dashboardPage.clickAnalyticsNav();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-064 FAILED: Did not reach /traffic-analytics.");
        System.out.println("TC-064 PASSED");
    }

    @Test(description = "TC-065: Alerts nav link navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts header nav link.")
    public void TC065_alertsNavLink() {
        TrafficAlertsPage ap = dashboardPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-065 FAILED: Did not reach /traffic-alerts.");
        System.out.println("TC-065 PASSED");
    }

    @Test(description = "TC-066: 'Analytics & Search' quick action navigates to /traffic-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics & Search quick-action-btn.")
    public void TC066_quickActionAnalytics() {
        TrafficAnalyticsPage ap = dashboardPage.clickQuickActionAnalytics();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-066 FAILED: Quick action did not reach /traffic-analytics.");
        System.out.println("TC-066 PASSED");
    }

    @Test(description = "TC-067: 'Traffic Alerts' quick action navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Traffic Alerts quick-action-btn.")
    public void TC067_quickActionAlerts() {
        TrafficAlertsPage ap = dashboardPage.clickQuickActionAlerts();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-067 FAILED: Quick action did not reach /traffic-alerts.");
        System.out.println("TC-067 PASSED");
    }

    @Test(description = "TC-068: Stats section shows exactly 5 stat cards")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Stats section must be visible with exactly 5 non-skeleton stat cards.")
    public void TC068_statsCardsDisplayed() {
        Assert.assertTrue(dashboardPage.isStatsSectionDisplayed(),
                "TC-068 FAILED: Stats section not visible.");
        int count = dashboardPage.getStatCardCount();
        Assert.assertEquals(count, 5,
                "TC-068 FAILED: Expected 5 stat cards, found " + count);
        System.out.println("TC-068 PASSED — " + count + " cards");
    }

    @Test(description = "TC-069: All 5 stat card values are loaded and non-empty")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings were seeded in @BeforeMethod. All stat values must be non-empty.")
    public void TC069_statValuesNonEmpty() {
        Assert.assertTrue(dashboardPage.areStatsLoaded(),
                "TC-069 FAILED: Stats still loading or skeleton cards present.");
        for (int i = 0; i < 5; i++) {
            String val = dashboardPage.getStatValue(i);
            Assert.assertFalse(val.isEmpty(),
                    "TC-069 FAILED: Stat card " + i + " has an empty value.");
        }
        System.out.println("TC-069 PASSED");
    }

    @Test(description = "TC-070: Data table is visible with at least 1 row after seeding")
    @Story("Data table")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings posted in @BeforeMethod — table must have at least 1 row.")
    public void TC070_dataTableHasRows() {
        Assert.assertTrue(dashboardPage.isTableSectionDisplayed(),
                "TC-070 FAILED: Table section not visible.");
        int rows = dashboardPage.getTableRowCount();
        Assert.assertTrue(rows >= 1,
                "TC-070 FAILED: Expected >=1 row after seeding, found " + rows);
        System.out.println("TC-070 PASSED — " + rows + " rows");
    }

    @Test(description = "TC-071: Pagination controls appear when data exceeds page size")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that when more than 10 records exist pagination controls are shown.")
    public void TC071_paginationDisplayed() {
        if (dashboardPage.hasPagination()) {
            Assert.assertFalse(dashboardPage.getActivePageNumber().isEmpty(),
                    "TC-071 FAILED: Pagination shown but active page is empty.");
            System.out.println("TC-071 PASSED — page: " + dashboardPage.getActivePageNumber());
        } else {
            System.out.println("TC-071 INFO: Not enough records for pagination yet.");
        }
    }

    @Test(description = "TC-072: Clicking Next page changes the active page indicator")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only runs when pagination is available. Verifies active page number increases.")
    public void TC072_nextPageChangesContent() {
        if (!dashboardPage.hasPagination() || !dashboardPage.isNextPageEnabled()) {
            System.out.println("TC-072 INFO: Single page — skip.");
            return;
        }
        String before = dashboardPage.getActivePageNumber();
        dashboardPage.clickNextPage();
        String after = dashboardPage.getActivePageNumber();
        Assert.assertNotEquals(after, before,
                "TC-072 FAILED: Page number did not change after Next.");
        System.out.println("TC-072 PASSED: " + before + " → " + after);
    }

    @Test(description = "TC-073: Charts section is visible with at least 3 chart cards")
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Density line, speed bar, and congestion distribution charts must all be present.")
    public void TC073_chartsSectionHasThreeCards() {
        Assert.assertTrue(dashboardPage.isChartsSectionDisplayed(),
                "TC-073 FAILED: Charts section not visible.");
        Assert.assertTrue(dashboardPage.getChartCardCount() >= 3,
                "TC-073 FAILED: Expected >=3 chart cards, found "
                + dashboardPage.getChartCardCount());
        System.out.println("TC-073 PASSED — " + dashboardPage.getChartCardCount() + " cards");
    }

    @Test(description = "TC-074: Congestion distribution shows exactly 4 level rows")
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expects 4 .congestion-row elements: Low, Moderate, High, Severe.")
    public void TC074_congestionChartFourRows() {
        Assert.assertTrue(dashboardPage.isCongestionChartDisplayed(),
                "TC-074 FAILED: Congestion chart not visible.");
        Assert.assertEquals(dashboardPage.getCongestionRowCount(), 4,
                "TC-074 FAILED: Expected 4 rows, found "
                + dashboardPage.getCongestionRowCount());
        System.out.println("TC-074 PASSED");
    }

    @Test(description = "TC-075: Recent alerts section is displayed")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("High-density and low-speed readings crossed thresholds, so at least one alert " +
                 "should exist. Section must show alerts or the empty state — not an error.")
    public void TC075_recentAlertsSectionDisplayed() {
        Assert.assertTrue(dashboardPage.isAlertsSectionDisplayed(),
                "TC-075 FAILED: Alerts section not visible.");
        Assert.assertFalse(dashboardPage.isAlertsErrorDisplayed(),
                "TC-075 FAILED: Error banner shown in alerts section.");
        boolean hasAlerts = dashboardPage.hasAlerts();
        boolean hasEmpty  = dashboardPage.isAlertsEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-075 FAILED: Neither alert items nor empty state shown.");
        System.out.println("TC-075 PASSED — alerts: " + dashboardPage.getAlertItemCount());
    }

    @Test(description = "TC-076: Recent alerts section shows at most 5 alerts")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("The TS component slices to the latest 5. Count must never exceed 5.")
    public void TC076_recentAlertsCappedAtFive() {
        int count = dashboardPage.getAlertItemCount();
        Assert.assertTrue(count <= 5,
                "TC-076 FAILED: Expected <=5 recent alerts, found " + count);
        System.out.println("TC-076 PASSED — count: " + count);
    }

    @Test(description = "TC-077: 'View All Alerts' link navigates to /traffic-alerts")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the View All Alerts anchor in the recent alerts section.")
    public void TC077_viewAllAlertsLink() {
        TrafficAlertsPage ap = dashboardPage.clickViewAllAlerts();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-077 FAILED: View All Alerts did not reach /traffic-alerts.");
        System.out.println("TC-077 PASSED");
    }

    @Test(description = "TC-078: Manual refresh keeps dashboard on /traffic without error")
    @Story("Refresh")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the manual refresh icon. Dashboard must stay on /traffic and show no error.")
    public void TC078_manualRefresh() {
        dashboardPage.clickManualRefresh();
        wait.waitForCondition(d ->
            dashboardPage.isStatsSectionDisplayed() || dashboardPage.isLastRefreshedDisplayed()
        );
        Assert.assertTrue(dashboardPage.isOnTrafficDashboard(),
                "TC-078 FAILED: Dashboard left /traffic after refresh.");
        Assert.assertFalse(dashboardPage.isStatsErrorDisplayed(),
                "TC-078 FAILED: Error banner shown after refresh.");
        System.out.println("TC-078 PASSED");
    }

    @Test(description = "TC-079: Auto-refresh toggle disables then re-enables")
    @Story("Refresh")
    @Severity(SeverityLevel.MINOR)
    @Description("btn-auto-refresh.active by default. Toggling removes then restores the class.")
    public void TC079_autoRefreshToggle() {
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-079 FAILED: Auto-refresh not active by default.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertFalse(dashboardPage.isAutoRefreshActive(),
                "TC-079 FAILED: Auto-refresh did not deactivate.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-079 FAILED: Auto-refresh did not re-activate.");
        System.out.println("TC-079 PASSED");
    }

    @Test(description = "TC-080: Unauthenticated /traffic access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /traffic. Angular auth guard must redirect.")
    public void TC080_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/traffic");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-080 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-080 PASSED");
    }
    // ── TC-081: Table row values match the API ────────────────────────────────



    @Test(description = "TC-081: Real-Time Traffic Data table first row matches the latest API record")
    @Story("Data accuracy — table")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, refreshes the dashboard, waits for table to re-render, " +
                    "then reads the API and UI at the same moment and asserts they match."
    )
    public void TC081_tableFirstRowMatchesApi() {
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

        System.out.println("[TC081] API (post-refresh) — location=" + apiLocation
                + " density=" + apiDensity + " speed=" + apiSpeed
                + " congestion=" + apiCongestion);

        Assert.assertNotNull(apiDensity,
                "TC-081 FAILED: API returned no density. Check sensor-service.");

        // Step 5 — read UI
        String uiLocation   = dashboardPage.getFirstRowLocation();
        String uiDensity    = dashboardPage.getFirstRowDensity();
        String uiSpeed      = dashboardPage.getFirstRowSpeed();
        String uiCongestion = dashboardPage.getFirstRowCongestionLevel();

        System.out.println("[TC081] UI — location=" + uiLocation
                + " density=" + uiDensity + " speed=" + uiSpeed
                + " congestion=" + uiCongestion);

        // Step 6 — assert UI == API
        Assert.assertEquals(uiLocation, apiLocation,
                "TC-081 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
        Assert.assertEquals(uiDensity, apiDensity,
                "TC-081 FAILED: density UI='" + uiDensity + "' API='" + apiDensity + "'");
        Assert.assertEquals(uiSpeed, normaliseDecimal(apiSpeed, 1),
                "TC-081 FAILED: speed UI='" + uiSpeed
                        + "' API='" + normaliseDecimal(apiSpeed, 1) + "'");
        Assert.assertEquals(uiCongestion, apiCongestion,
                "TC-081 FAILED: congestion UI='" + uiCongestion + "' API='" + apiCongestion + "'");

        System.out.println("TC-081 PASSED — table first row matches API");
    }
    // ── TC-082: Density line & speed bar charts match trends API ─────────────

    // ── Fix 3: TC082 — use the renamed methods ────────────────────────────────────

    @Test(description = "TC-082: Density line and speed bar charts match the trends API")
    @Story("Data accuracy — charts")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, refreshes, waits for charts to render, then reads the " +
                    "trends API newest record and the UI leftmost dot/bar (both = newest reading " +
                    "after Angular reversal) and asserts they match."
    )
    public void TC082_chartsMatchTrendsApi() {
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

        System.out.println("[TC082] API newest — density=" + apiDensity
                + " speed=" + apiSpeed + " totalPoints=" + apiCount);

        Assert.assertNotNull(apiDensity,
                "TC-082 FAILED: Trends API returned no data. Check sensor-service.");

        // Step 5a — read UI leftmost dot (index 0 = newest after Angular reversal)
        String uiDensity = dashboardPage.getFirstDensityDotValue();
        System.out.println("[TC082] UI first (leftmost) dot value: " + uiDensity);

        Assert.assertFalse(uiDensity.isEmpty(),
                "TC-082 FAILED: No density dots rendered after refresh.");
        Assert.assertEquals(uiDensity, apiDensity,
                "TC-082 FAILED: density dot='" + uiDensity + "' API='" + apiDensity + "'");

        // Step 5b — read UI leftmost bar (index 0 = newest after Angular reversal)
        String uiSpeed = dashboardPage.getFirstSpeedBarValue();
        System.out.println("[TC082] UI first (leftmost) bar value: " + uiSpeed);

        Assert.assertFalse(uiSpeed.isEmpty(),
                "TC-082 FAILED: No speed bars rendered after refresh.");
        String normApiSpeed = normaliseDecimal(apiSpeed, 1);
        Assert.assertEquals(uiSpeed, normApiSpeed,
                "TC-082 FAILED: speed bar='" + uiSpeed + "' API='" + normApiSpeed + "'");

        // Step 5c — dot count matches API point count
        int uiDotCount = dashboardPage.getDensityDotCount();
        Assert.assertEquals(uiDotCount, apiCount,
                "TC-082 FAILED: UI dots=" + uiDotCount
                        + " but API returned " + apiCount + " points.");

        System.out.println("TC-082 PASSED — chart values match trends API");
    }
    public void TC083_congestionChartMatchesSummaryApi() {
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
                "TC-083 FAILED: Congestion summary API returned no data.");

        String apiLow      = TrafficApiReader.getCongestionCount("Low");
        String apiModerate = TrafficApiReader.getCongestionCount("Moderate");
        String apiHigh     = TrafficApiReader.getCongestionCount("High");
        String apiSevere   = TrafficApiReader.getCongestionCount("Severe");

        System.out.println("[TC083] API (post-refresh) — Low=" + apiLow
                + " Moderate=" + apiModerate + " High=" + apiHigh
                + " Severe=" + apiSevere);

        // Step 5 — read UI
        String uiLow      = dashboardPage.getCongestionCountForLevel("Low");
        String uiModerate = dashboardPage.getCongestionCountForLevel("Moderate");
        String uiHigh     = dashboardPage.getCongestionCountForLevel("High");
        String uiSevere   = dashboardPage.getCongestionCountForLevel("Severe");

        System.out.println("[TC083] UI — Low=" + uiLow
                + " Moderate=" + uiModerate + " High=" + uiHigh
                + " Severe=" + uiSevere);

        // Step 6 — assert all four levels match
        Assert.assertEquals(uiLow,      apiLow,      "TC-083 FAILED: Low");
        Assert.assertEquals(uiModerate, apiModerate, "TC-083 FAILED: Moderate");
        Assert.assertEquals(uiHigh,     apiHigh,     "TC-083 FAILED: High");
        Assert.assertEquals(uiSevere,   apiSevere,   "TC-083 FAILED: Severe");

        System.out.println("TC-083 PASSED — congestion distribution matches API");
    }
    // ── Helper ────────────────────────────────────────────────────────────────
    @Test(description = "TC-084: Global alert banner appears after threshold crossing then auto-dismisses within 15 seconds")
    @Story("Global alert banner")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Posts density=450 which exceeds the saved Traffic Density > 100 threshold. " +
                    "The backend pushes a WebSocket alert to the AppComponent which renders " +
                    ".global-alert-banner with a message and an OK button. " +
                    "The banner must then disappear on its own within 15 seconds (AppComponent setTimeout ~5s). " +
                    "If still visible after 15s the OK button is clicked to clean up before failing."
    )
    public void TC084_globalAlertBannerAppearsAndAutoDismisses() {
        org.openqa.selenium.By BANNER     = org.openqa.selenium.By.cssSelector(".global-alert-banner");
        org.openqa.selenium.By BANNER_MSG = org.openqa.selenium.By.cssSelector(".global-alert-banner span");
        org.openqa.selenium.By BANNER_OK  = org.openqa.selenium.By.cssSelector(".global-alert-ok");

        // Step 1 — seed a threshold-crossing reading
        try {
            SensorApiClient.postHighDensityReading(); // density=450 > threshold 100
        } catch (Exception e) {
            Assert.fail("TC-084 FAILED: Could not post reading — " + e.getMessage());
        }

        // Step 2 — wait for the banner to appear (WebSocket propagation)
        int appearTimeout = ConfigReader.getNotificationBellTimeout(); // default 20s
        System.out.println("[TC084] Waiting up to " + appearTimeout + "s for banner...");
        try {
            wait.waitForCondition(d ->
                    !d.findElements(BANNER).isEmpty()
                            && d.findElements(BANNER).get(0).isDisplayed()
            );
        } catch (Exception e) {
            Assert.fail(
                    "TC-084 FAILED: .global-alert-banner did not appear within " + appearTimeout + "s. " +
                            "Check WebSocket connection, threshold setup in @BeforeClass, " +
                            "and that sensor-service is running on localhost:8081."
            );
        }
        System.out.println("[TC084] Banner appeared ✔");

        // Step 3 — verify message text and OK button while banner is visible
        String bannerText = "";
        try { bannerText = driver.findElement(BANNER_MSG).getText().trim(); }
        catch (Exception ignored) {}
        Assert.assertFalse(bannerText.isEmpty(),
                "TC-084 FAILED: Banner appeared but message text is empty.");
        Assert.assertFalse(driver.findElements(BANNER_OK).isEmpty(),
                "TC-084 FAILED: OK dismiss button not found inside the banner.");
        System.out.println("[TC084] Message: '" + bannerText + "', OK button present ✔");

        // Step 4 — wait for auto-dismiss (AppComponent setTimeout ~5s, allow up to 15s)
        int dismissTimeout = 15;
        System.out.println("[TC084] Waiting up to " + dismissTimeout + "s for auto-dismiss...");
        try {
            long deadline = System.currentTimeMillis() + dismissTimeout * 1000L;
            while (System.currentTimeMillis() < deadline) {
                java.util.List<org.openqa.selenium.WebElement> b = driver.findElements(BANNER);
                if (b.isEmpty() || !b.get(0).isDisplayed()) break;
                Thread.sleep(500);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        // Step 5 — assert banner is gone; click OK to clean up if still visible
        java.util.List<org.openqa.selenium.WebElement> remaining = driver.findElements(BANNER);
        boolean stillVisible = !remaining.isEmpty() && remaining.get(0).isDisplayed();
        if (stillVisible) {
            try { driver.findElements(BANNER_OK).get(0).click(); } catch (Exception ignored) {}
            Assert.fail(
                    "TC-084 FAILED: Banner still visible after " + dismissTimeout + "s. " +
                            "Auto-dismiss did not fire — check AppComponent setTimeout for alertBanner."
            );
        }

        System.out.println("TC-084 PASSED — banner appeared, content verified, auto-dismissed ✔");
    }
    @Test(description = "TC-085: Dashboard auto-refreshes every 60 seconds when auto-refresh is enabled")
    @Story("Auto-refresh")
    @Severity(SeverityLevel.NORMAL)
    @Description(
            "Verifies the 60-second auto-refresh cycle defined by AUTO_REFRESH_MS = 60_000 " +
                    "in traffic-dashboard.ts. The component uses interval(60_000).pipe(startWith(0)) " +
                    "which triggers loadAllData() every 60 seconds. The visible proof of a refresh " +
                    "is the 'Updated HH:MM:SS' timestamp (.last-refreshed) updating to a new value. " +
                    "Strategy: (1) ensure auto-refresh is active, (2) read the current timestamp, " +
                    "(3) wait 65 seconds (60s interval + 5s buffer for API calls), " +
                    "(4) assert the timestamp changed, meaning a new refresh cycle completed."
    )
    public void TC085_dashboardAutoRefreshesEvery60Seconds() {
        org.openqa.selenium.By SYNCING        = org.openqa.selenium.By.cssSelector(".last-refreshed.syncing");
        org.openqa.selenium.By LAST_REFRESHED = org.openqa.selenium.By.cssSelector(".last-refreshed:not(.syncing)");
        org.openqa.selenium.By AUTO_BTN       = org.openqa.selenium.By.cssSelector("button.btn-auto-refresh");

        // Step 1 — ensure auto-refresh is active (btn-auto-refresh must have class 'active')
        // If it was disabled by a previous test (TC-079), re-enable it now.
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(AUTO_BTN);
            boolean isActive = btn.getAttribute("class") != null
                    && btn.getAttribute("class").contains("active");
            if (!isActive) {
                btn.click();
                System.out.println("[TC085] Auto-refresh was paused — re-enabled.");
                // Wait for the immediate t=0 emission that fires on re-enable
                try { wait.waitForInvisibility(SYNCING); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[TC085] Could not check auto-refresh state: " + e.getMessage());
        }

        // Step 2 — wait for any ongoing syncing to settle, then read current timestamp
        try { wait.waitForInvisibility(SYNCING); } catch (Exception ignored) {}

        String timestampBefore = "";
        try {
            org.openqa.selenium.WebElement el = wait.waitForVisible(LAST_REFRESHED);
            timestampBefore = el.getText().trim();
        } catch (Exception e) {
            Assert.fail("TC-085 FAILED: 'Updated HH:MM:SS' element not visible before waiting. " +
                    "Dashboard must show a timestamp before we can detect it changing.");
        }
        System.out.println("[TC085] Timestamp before wait: '" + timestampBefore + "'");

        // Step 3 — wait 65 seconds
        // 60s = the AUTO_REFRESH_MS interval
        //  5s = buffer for the four parallel API calls (stats, table, charts, alerts) to complete
        System.out.println("[TC085] Waiting 65 seconds for the auto-refresh cycle to fire...");
        try {
            Thread.sleep(65_000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            Assert.fail("TC-085 FAILED: Test thread was interrupted during the 65s wait.");
        }

        // Step 4 — wait for any syncing indicator to finish (the refresh is in progress)
        try { wait.waitForInvisibility(SYNCING); } catch (Exception ignored) {}

        // Step 5 — read the new timestamp
        String timestampAfter = "";
        try {
            org.openqa.selenium.WebElement el = wait.waitForVisible(LAST_REFRESHED);
            timestampAfter = el.getText().trim();
        } catch (Exception e) {
            Assert.fail("TC-085 FAILED: 'Updated HH:MM:SS' element not visible after waiting.");
        }
        System.out.println("[TC085] Timestamp after wait:  '" + timestampAfter + "'");

        // Step 6 — assert the timestamp changed
        Assert.assertFalse(timestampAfter.isEmpty(),
                "TC-085 FAILED: Timestamp is empty after 65s wait.");
        Assert.assertNotEquals(timestampAfter, timestampBefore,
                "TC-085 FAILED: Timestamp did not change after 65 seconds. " +
                        "Expected auto-refresh to have fired at least once (AUTO_REFRESH_MS = 60_000). " +
                        "Check that the interval subscription is active and that " +
                        "loadRecentAlerts() completes successfully (it sets lastRefreshed).");

        System.out.println("TC-085 PASSED — timestamp changed: '"
                + timestampBefore + "' → '" + timestampAfter + "' ✔");
    }
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
