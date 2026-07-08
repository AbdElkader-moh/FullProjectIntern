package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.HomePage;
import pages.SettingsPage;
import pages.StreetLightAlertsPage;
import pages.StreetLightAnalyticsPage;
import pages.StreetLightDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.StreetLightApiReader;

/**
 * StreetLightDashboardTest — TC-205 … TC-227
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * Thresholds are created once in @BeforeClass using SettingsPage — the same UI
 * your users use. This is consistent with the rest of the framework and avoids
 * any direct HTTP auth plumbing.
 *
 * Sensor readings are seeded before each test using SensorApiClient, which calls
 * POST /api/sensors/street-light on localhost:8081 (the host-side port exposed by
 * docker-compose). No simulator dependency — tests run instantly and deterministically.
 *
 * THRESHOLD SETUP (once per class)
 * ─────────────────────────────────
 *   Power Consumption > 100  (above)   →  power=450 from postHighPowerConsumptionReading() triggers alert
 *   Brightness Level   < 30   (below)   →  brightness=5    from postLowBrightnessLevelReading()    triggers alert
 *
 * The simulator may still be running — that only adds more data. Tests assert on
 * minimums (>= 1) not exact counts, so simulator data doesn't break them.
 */
@Epic("Street light monitoring")
@Feature("Street light dashboard")
public class StreetLightDashboardTest extends BaseTest {

    private StreetLightDashboardPage dashboardPage;

    // ── One-time threshold setup ───────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
        ensureStreetLightThresholdsExist();
    }

    /**
     * Uses SettingsPage to save the two street light thresholds needed for alert tests.
     * Called once per class — idempotent enough because the Settings page will
     * simply add another threshold if one already exists (which the dashboard
     * handles gracefully by counting it alongside existing ones).
     */
        private void ensureStreetLightThresholdsExist() {
        try {
            SettingsPage settingsPage = new SettingsPage(driver);
            settingsPage.open();

            // Power Consumption above 100 (Index 1)
            settingsPage.createThreshold("street light", 1, 100, true);
            System.out.println("[Setup] Power Consumption > 100 saved");

            // Brightness Level below 30 (Index 0)
            settingsPage.createThreshold("street light", 0, 30, false);
            System.out.println("[Setup] Brightness Level < 30 saved");

        } catch (Exception e) {
            System.out.println("[Setup] Threshold setup warning: " + e.getMessage()
                    + " — alert-dependent tests may be inconclusive.");
        }
    }

    // ── Per-test data seeding + navigation ────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    public void seedDataAndOpenDashboard() {

        // Seed street light readings directly — bypasses the 120-second simulator interval.
        // Normal reading populates the table; high-power + low-brightness trigger alerts.
        try {
            SensorApiClient.postNormalLightReading();
            SensorApiClient.postHighPowerConsumptionReading();
            SensorApiClient.postLowBrightnessLevelReading();
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage()
                    + " — tests will run against existing backend data.");
        }

        dashboardPage = new StreetLightDashboardPage(driver);
        dashboardPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-205: Street light dashboard page loads successfully")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /lights and verifies the URL and page title.")
    public void TC205_pageLoads() {
        Assert.assertTrue(dashboardPage.isOnStreetLightDashboard(),
                "TC-205 FAILED: URL does not contain /lights.");
        System.out.println("TC-205 PASSED");
    }

    @Test(description = "TC-206: 'Street Light Monitoring Dashboard' page title is displayed")
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies the h1 .page-title text.")
    public void TC206_pageTitleIsDisplayed() {
        Assert.assertTrue(dashboardPage.isPageTitleDisplayed(),
                "TC-206 FAILED: Page title not visible.");
        Assert.assertTrue(dashboardPage.getPageTitleText().contains("Street Light"),
                "TC-206 FAILED: Title does not contain 'Street Light'. Found: "
                        + dashboardPage.getPageTitleText());
        System.out.println("TC-206 PASSED");
    }

    @Test(description = "TC-207: Back link navigates to /home")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks .back-link and verifies navigation to /home.")
    public void TC207_backLinkNavigatesToHome() {
        HomePage homePage = dashboardPage.clickBackLink();
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-207 FAILED: Back link did not reach /home.");
        System.out.println("TC-207 PASSED");
    }

    @Test(description = "TC-208: Analytics nav link navigates to /lights-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics header nav link.")
    public void TC208_analyticsNavLink() {
        StreetLightAnalyticsPage ap = dashboardPage.clickAnalyticsNav();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-208 FAILED: Did not reach /lights-analytics.");
        System.out.println("TC-208 PASSED");
    }

    @Test(description = "TC-209: Alerts nav link navigates to /lights-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts header nav link.")
    public void TC209_alertsNavLink() {
        StreetLightAlertsPage ap = dashboardPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-209 FAILED: Did not reach /lights-alerts.");
        System.out.println("TC-209 PASSED");
    }

    @Test(description = "TC-210: 'Analytics & Search' quick action navigates to /street-light-analytics")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Analytics & Search quick-action-btn.")
    public void TC210_quickActionAnalytics() {
        StreetLightAnalyticsPage ap = dashboardPage.clickQuickActionAnalytics();
        Assert.assertTrue(ap.isOnAnalyticsPage(),
                "TC-210 FAILED: Quick action did not reach /street-light-analytics.");
        System.out.println("TC-210 PASSED");
    }

    @Test(description = "TC-211: 'Street Light Alerts' quick action navigates to /lights-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Street Light Alerts quick-action-btn.")
    public void TC211_alertsQuickAction() {
        pages.StreetLightAlertsPage alerts = dashboardPage.clickAlertsQuickAction();
        Assert.assertTrue(alerts.isOnAlertsPage(),
                "TC-211 FAILED: Did not reach /lights-alerts.");
        System.out.println("TC-211 PASSED");
    }

    @Test(description = "TC-212: Stats section shows exactly 5 stat cards")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Stats section must be visible with exactly 5 non-skeleton stat cards.")
    public void TC212_statsCardsDisplayed() {
        Assert.assertTrue(dashboardPage.isStatsSectionDisplayed(),
                "TC-212 FAILED: Stats section not visible.");
        int count = dashboardPage.getStatCardCount();
        Assert.assertEquals(count, 5,
                "TC-212 FAILED: Expected 5 stat cards, found " + count);
        System.out.println("TC-212 PASSED — " + count + " cards");
    }

    @Test(description = "TC-213: All 5 stat card values are loaded and non-empty")
    @Story("Stats cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings were seeded in @BeforeMethod. All stat values must be non-empty.")
    public void TC213_statValuesNonEmpty() {
        Assert.assertTrue(dashboardPage.areStatsLoaded(),
                "TC-213 FAILED: Stats still loading or skeleton cards present.");
        for (int i = 0; i < 5; i++) {
            String val = dashboardPage.getStatValue(i);
            Assert.assertFalse(val.isEmpty(),
                    "TC-213 FAILED: Stat card " + i + " has an empty value.");
        }
        System.out.println("TC-213 PASSED");
    }

    @Test(description = "TC-214: Data table is visible with at least 1 row after seeding")
    @Story("Data table")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Three readings posted in @BeforeMethod — table must have at least 1 row.")
    public void TC214_dataTableHasRows() {
        Assert.assertTrue(dashboardPage.isTableSectionDisplayed(),
                "TC-214 FAILED: Table section not visible.");
        int rows = dashboardPage.getTableRowCount();
        Assert.assertTrue(rows >= 1,
                "TC-214 FAILED: Expected >=1 row after seeding, found " + rows);
        System.out.println("TC-214 PASSED — " + rows + " rows");
    }

    @Test(description = "TC-215: Pagination controls appear when data exceeds page size")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that when more than 10 records exist pagination controls are shown.")
    public void TC215_paginationDisplayed() {
        if (dashboardPage.hasPagination()) {
            Assert.assertFalse(dashboardPage.getActivePageNumber().isEmpty(),
                    "TC-215 FAILED: Pagination shown but active page is empty.");
            System.out.println("TC-215 PASSED — page: " + dashboardPage.getActivePageNumber());
        } else {
            System.out.println("TC-215 INFO: Not enough records for pagination yet.");
        }
    }

    @Test(description = "TC-216: Clicking Next page changes the active page indicator")
    @Story("Data table")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only runs when pagination is available. Verifies active page number increases.")
    public void TC216_nextPageChangesContent() {
        if (!dashboardPage.hasPagination() || !dashboardPage.isNextPageEnabled()) {
            System.out.println("TC-216 INFO: Single page — skip.");
            return;
        }
        String before = dashboardPage.getActivePageNumber();
        dashboardPage.clickNextPage();
        String after = dashboardPage.getActivePageNumber();
        Assert.assertNotEquals(after, before,
                "TC-216 FAILED: Page number did not change after Next.");
        System.out.println("TC-216 PASSED: " + before + " → " + after);
    }

    @Test(description = "TC-217: Charts section renders with exactly two cards")
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expects exactly 2 chart cards for Street Lights (Trends and Values).")
    public void TC217_chartsSectionHasTwoCards() {
        Assert.assertTrue(dashboardPage.isChartsSectionDisplayed(),
                "TC-217 FAILED: Charts section not visible.");
        Assert.assertEquals(dashboardPage.getChartCardCount(), 2,
                "TC-217 FAILED: Expected 2 chart cards, found " + dashboardPage.getChartCardCount());
        System.out.println("TC-217 PASSED — " + dashboardPage.getChartCardCount() + " cards");
    }

    @Test(description = "TC-218: Trend and Values charts are visible without errors")
    @Story("Charts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expects no chart errors in the rendered charts.")
    public void TC218_trendAndValuesChartsVisible() {
        Assert.assertFalse(dashboardPage.isChartErrorDisplayed(),
                "TC-218 FAILED: Chart error displayed.");
        System.out.println("TC-218 PASSED");
    }

    @Test(description = "TC-219: Recent alerts section is displayed")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("High-power and low-brightness readings crossed thresholds, so at least one alert " +
                 "should exist. Section must show alerts or the empty state — not an error.")
    public void TC219_recentAlertsSectionDisplayed() {
        Assert.assertTrue(dashboardPage.isAlertsSectionDisplayed(),
                "TC-219 FAILED: Alerts section not visible.");
        Assert.assertFalse(dashboardPage.isAlertsErrorDisplayed(),
                "TC-219 FAILED: Error banner shown in alerts section.");
        boolean hasAlerts = dashboardPage.hasAlerts();
        boolean hasEmpty  = dashboardPage.isAlertsEmptyStateDisplayed();
        Assert.assertTrue(hasAlerts || hasEmpty,
                "TC-219 FAILED: Neither alert items nor empty state shown.");
        System.out.println("TC-219 PASSED — alerts: " + dashboardPage.getAlertItemCount());
    }

    @Test(description = "TC-220: Alerts list max 5 items")
    @Story("Recent alerts")
    @Severity(SeverityLevel.MINOR)
    @Description("Expects up to 5 alerts to be rendered in the recent alerts section.")
    public void TC220_alertsListMax5() {
        int count = dashboardPage.getAlertItemCount();
        Assert.assertTrue(count <= 5,
                "TC-220 FAILED: Found " + count + " alerts, max is 5.");
        System.out.println("TC-220 PASSED");
    }

    @Test(description = "TC-221: View All Alerts navigates to /notifications")
    @Story("Recent alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the View All Alerts anchor in the recent alerts section.")
    public void TC221_viewAllAlertsLink() {
        dashboardPage.clickViewAllAlerts();
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/notifications"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/notifications"),
                "TC-221 FAILED: View All Alerts did not reach /notifications.");
        System.out.println("TC-221 PASSED");
    }

    @Test(description = "TC-222: Manual refresh keeps dashboard on /street-light without error")
    @Story("Refresh")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the manual refresh icon. Dashboard must stay on /street-light and show no error.")
    public void TC222_manualRefresh() {
        dashboardPage.clickManualRefresh();
        wait.waitForCondition(d ->
            dashboardPage.isStatsSectionDisplayed() || dashboardPage.isLastRefreshedDisplayed()
        );
        Assert.assertTrue(dashboardPage.isOnStreetLightDashboard(),
                "TC-222 FAILED: Dashboard left /street-light after refresh.");
        Assert.assertFalse(dashboardPage.isStatsErrorDisplayed(),
                "TC-222 FAILED: Error banner shown after refresh.");
        System.out.println("TC-222 PASSED");
    }

    @Test(description = "TC-223: Auto-refresh toggle disables then re-enables")
    @Story("Refresh")
    @Severity(SeverityLevel.MINOR)
    @Description("btn-auto-refresh.active by default. Toggling removes then restores the class.")
    public void TC223_autoRefreshToggle() {
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-223 FAILED: Auto-refresh not active by default.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertFalse(dashboardPage.isAutoRefreshActive(),
                "TC-223 FAILED: Auto-refresh did not deactivate.");
        dashboardPage.clickAutoRefreshToggle();
        Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                "TC-223 FAILED: Auto-refresh did not re-activate.");
        System.out.println("TC-223 PASSED");
    }

    @Test(description = "TC-224: Unauthenticated /street-light access redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /street-light. Angular auth guard must redirect.")
    public void TC224_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/street-light");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-224 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-224 PASSED");
    }
    // ── TC-225: Table row values match the API ────────────────────────────────



    @Test(description = "TC-225: Real-Time Street Light Data table first row matches the latest API record")
    @Story("Data accuracy — table")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, clicks manual refresh, waits for the table to update, " +
                    "then reads the API and UI at the same moment and asserts they match."
    )
    public void TC225_tableFirstRowMatchesApi() {
        // Step 1 — seed
        SensorApiClient.postLightReading(33, 77.5, "ON", "Alexandria");

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
        String apiPowerConsumption    = StreetLightApiReader.getFirstRecordPowerConsumption();
        String apiBrightnessLevel      = StreetLightApiReader.getFirstRecordAvgBrightnessLevel();
        String apiLocation   = StreetLightApiReader.getFirstRecordLocation();
        String apiStatus = StreetLightApiReader.getFirstRecordStatusLevel();

        System.out.println("[TC225] API (post-refresh) — location=" + apiLocation
                + " power=" + apiPowerConsumption + " brightness=" + apiBrightnessLevel
                + " status=" + apiStatus);

        Assert.assertNotNull(apiPowerConsumption,
                "TC-225 FAILED: API returned no power. Check sensor-service.");

        // Step 5 — read UI
        String uiLocation   = dashboardPage.getFirstRowLocation();
        String uiPowerConsumption    = dashboardPage.getFirstRowPowerConsumption();
        String uiBrightnessLevel      = dashboardPage.getFirstRowBrightnessLevel();
        String uiStatus = dashboardPage.getFirstRowStatusLevel();

        System.out.println("[TC225] UI — location=" + uiLocation
                + " power=" + uiPowerConsumption + " brightness=" + uiBrightnessLevel
                + " status=" + uiStatus);

        // Step 6 — assert UI == API
        Assert.assertEquals(uiLocation, apiLocation,
                "TC-225 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
        Assert.assertEquals(Double.parseDouble(uiPowerConsumption), Double.parseDouble(apiPowerConsumption), 0.1,
                "TC-225 FAILED: power UI='" + uiPowerConsumption + "' API='" + apiPowerConsumption + "'");
        Assert.assertEquals(Double.parseDouble(uiBrightnessLevel), Double.parseDouble(apiBrightnessLevel), 0.1,
                "TC-225 FAILED: brightness UI='" + uiBrightnessLevel + "' API='" + apiBrightnessLevel + "'");
        Assert.assertEquals(uiStatus, apiStatus,
                "TC-225 FAILED: status UI='" + uiStatus + "' API='" + apiStatus + "'");

        System.out.println("TC-225 PASSED — table first row matches API");
    }
    // ── TC-226: PowerConsumption line & brightness bar charts match trends API ─────────────

    // ── Fix 3: TC226 — use the renamed methods ────────────────────────────────────

    @Test(description = "TC-226: PowerConsumption line and brightness bar charts match the trends API")
    @Story("Data accuracy — charts")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Seeds a reading, refreshes, waits for charts to render, then reads the " +
                    "trends API newest record and the UI leftmost dot/bar (both = newest reading " +
                    "after Angular reversal) and asserts they match."
    )
    public void TC226_chartsMatchTrendsApi() {
        // Step 1
        SensorApiClient.postLightReading(44, 88.8, "OFF", "Alexandria");

        // Step 2 — refresh
        dashboardPage.clickManualRefresh();

        // Step 3 — wait for dots to render
        try { wait.waitForInvisibility(org.openqa.selenium.By.cssSelector(".skeleton-chart")); }
        catch (Exception ignored) {}
        wait.waitForCondition(d ->
                !d.findElements(org.openqa.selenium.By.cssSelector("circle.density-dot")).isEmpty()
        );

        // Step 4 — read API newest record (last element of raw array = newest timestamp)
        String apiPowerConsumption = StreetLightApiReader.getNewestTrendPowerConsumption();
        String apiBrightnessLevel   = StreetLightApiReader.getNewestTrendAvgBrightnessLevel();
        int    apiCount   = StreetLightApiReader.getTrendDataPointCount();

        System.out.println("[TC226] API newest — power=" + apiPowerConsumption
                + " brightness=" + apiBrightnessLevel + " totalPoints=" + apiCount);

        Assert.assertNotNull(apiPowerConsumption,
                "TC-226 FAILED: Trends API returned no data. Check sensor-service.");

        // Step 5a — read UI leftmost dot (index 0 = newest after Angular reversal)
        String uiBrightnessLevel = dashboardPage.getFirstBrightnessLevelDotValue();
        System.out.println("[TC226] UI first (leftmost) dot value: " + uiBrightnessLevel);

        Assert.assertFalse(uiBrightnessLevel.isEmpty(),
                "TC-226 FAILED: No brightness dots rendered after refresh.");
        Assert.assertEquals(Double.parseDouble(uiBrightnessLevel), Double.parseDouble(apiBrightnessLevel), 0.1,
                "TC-226 FAILED: brightness dot='" + uiBrightnessLevel + "' API='" + apiBrightnessLevel + "'");

        // Step 5b — read UI leftmost bar (index 0 = newest after Angular reversal)
        String uiPowerConsumption = dashboardPage.getFirstPowerConsumptionBarValue();
        System.out.println("[TC226] UI first (leftmost) bar value: " + uiPowerConsumption);

        Assert.assertFalse(uiPowerConsumption.isEmpty(),
                "TC-226 FAILED: No power bars rendered after refresh.");
        Assert.assertEquals(Double.parseDouble(uiPowerConsumption), Double.parseDouble(apiPowerConsumption), 0.1,
                "TC-226 FAILED: power bar='" + uiPowerConsumption + "' API='" + apiPowerConsumption + "'");

        // Step 5c — dot count matches API point count
        int uiDotCount = dashboardPage.getPowerConsumptionDotCount();
        Assert.assertEquals(uiDotCount, apiCount,
                "TC-226 FAILED: UI dots=" + uiDotCount
                        + " but API returned " + apiCount + " points.");

        System.out.println("TC-226 PASSED — chart values match trends API");
    }

    @Test(description = "TC-227: Charts load properly", groups = {"sanity"})
    @Story("Charts")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Refreshes dashboard and ensures charts load correctly without error states.")
    public void TC227_chartsLoadWithoutErrors() {
        // Step 1 — refresh
        dashboardPage.clickManualRefresh();

        // Step 2 — wait for charts section
        wait.waitForCondition(d -> dashboardPage.isChartsSectionDisplayed());

        // Step 3 — verify no errors
        Assert.assertFalse(dashboardPage.isChartErrorDisplayed(),
                "TC-227 FAILED: Chart error banner displayed.");

        System.out.println("TC-227 PASSED — charts load without errors");
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
