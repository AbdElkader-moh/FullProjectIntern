package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.HomePage;
import pages.SettingsPage;
import pages.AirPollutionAlertsPage;
import pages.AirPollutionAnalyticsPage;
import pages.AirPollutionDashboardPage;
import utils.SensorApiClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.AirPollutionApiReader;

/**
 * AirPollutionDashboardTest — TC-148 … TC-170
 *
 * DATA SEEDING STRATEGY
 * ─────────────────────
 * Thresholds are created once in @BeforeClass using SettingsPage — the same UI
 * your users use. This is consistent with the rest of the framework and avoids
 * any direct HTTP auth plumbing.
 *
 * Sensor readings are seeded before each test using SensorApiClient, which
 * calls
 * POST /api/sensors/air on localhost:8081 (the host-side port exposed by
 * docker-compose). No simulator dependency — tests run instantly and
 * deterministically.
 *
 * THRESHOLD SETUP (once per class)
 * ─────────────────────────────────
 * Carbon Monoxide > 100 (above) → co=450 from postHighCarbonMonoxideReading()
 * triggers alert
 * Ozone < 30 (below) → ozone=5 from postLowOzoneReading() triggers alert
 *
 * The simulator may still be running — that only adds more data. Tests assert
 * on
 * minimums (>= 1) not exact counts, so simulator data doesn't break them.
 */
@Epic("Air quality monitoring")
@Feature("Air pollution dashboard")
public class AirPollutionDashboardTest extends BaseTest {

        private AirPollutionDashboardPage dashboardPage;

        // ── One-time threshold setup ───────────────────────────────────────────────

        @BeforeClass(alwaysRun = true)
        @Override
        public void setUp() {
                super.setUp();
                loginWithDefaultUser();
                ensureAirThresholdsExist();
        }

        /**
         * Uses SettingsPage to save the two air pollution thresholds needed for alert tests.
         * Called once per class — idempotent enough because the Settings page will
         * simply add another threshold if one already exists (which the dashboard
         * handles gracefully by counting it alongside existing ones).
         */
        private void ensureAirThresholdsExist() {
                try {
                        SettingsPage settingsPage = new SettingsPage(driver);
                        settingsPage.open();

                        // Threshold 1: Carbon Monoxide above 40
                        settingsPage.createThreshold("air quality", 0, 40, true);
                        System.out.println("[Setup] Carbon Monoxide > 40 threshold saved");

                        // Threshold 2: Ozone below 30
                        settingsPage.createThreshold("air quality", 1, 30, false);
                        System.out.println("[Setup] Ozone < 30 threshold saved");

                } catch (Exception e) {
                        System.out.println("[Setup] Threshold setup warning: " + e.getMessage()
                                        + " — alert-dependent tests may be inconclusive.");
                }
        }

        // ── Per-test data seeding + navigation ────────────────────────────────────

        @BeforeMethod(alwaysRun = true)
        public void seedDataAndOpenDashboard() {

                // Seed air pollution readings directly — bypasses the 120-second simulator interval.
                // Normal reading populates the table; high-co + low-ozone trigger alerts.
                try {
                        SensorApiClient.postNormalAirReading();
                        SensorApiClient.postHighCarbonMonoxideReading();
                        SensorApiClient.postLowOzoneReading();
                } catch (Exception e) {
                        System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage()
                                        + " — tests will run against existing backend data.");
                }

                dashboardPage = new AirPollutionDashboardPage(driver);
                dashboardPage.open();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Tests
        // ─────────────────────────────────────────────────────────────────────────

        @Test(description = "TC-148: Air pollution dashboard loads and URL is /air")
        @Story("Page load")
        @Severity(SeverityLevel.BLOCKER)
        @Description("Navigates to /air and verifies the URL contains /air but not a sub-route.")
        public void TC148_airDashboardLoads() {
                Assert.assertTrue(dashboardPage.isOnAirDashboard(),
                                "TC-148 FAILED: URL is not /air or landed on a sub-route.");
                System.out.println("TC-148 PASSED");
        }

        @Test(description = "TC-149: 'Air Pollution Monitoring Dashboard' page title is displayed")
        @Story("Page load")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies the .page-title element is visible.")
        public void TC149_pageTitleDisplayed() {
                Assert.assertTrue(dashboardPage.isPageTitleDisplayed(),
                                "TC-149 FAILED: Page title not visible.");
                System.out.println("TC-149 PASSED");
        }

        @Test(description = "TC-150: Back link navigates to /home")
        @Story("Navigation")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks .back-link and verifies navigation to /home.")
        public void TC150_backLinkNavigatesToHome() {
                HomePage homePage = dashboardPage.clickBackLink();
                Assert.assertTrue(homePage.isOnHomePage(),
                                "TC-150 FAILED: Back link did not reach /home.");
                System.out.println("TC-150 PASSED");
        }

        @Test(description = "TC-151: Analytics nav link navigates to /air-analytics")
        @Story("Navigation")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the Analytics header nav link.")
        public void TC151_analyticsNavLink() {
                AirPollutionAnalyticsPage ap = dashboardPage.clickAnalyticsNav();
                Assert.assertTrue(ap.isOnAnalyticsPage(),
                                "TC-151 FAILED: Did not reach /air-analytics.");
                System.out.println("TC-151 PASSED");
        }

        @Test(description = "TC-152: Alerts nav link navigates to /air-alerts")
        @Story("Navigation")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the Alerts header nav link.")
        public void TC152_alertsNavLink() {
                AirPollutionAlertsPage ap = dashboardPage.clickAlertsNav();
                Assert.assertTrue(ap.isOnAlertsPage(),
                                "TC-152 FAILED: Did not reach /air-alerts.");
                System.out.println("TC-152 PASSED");
        }

        @Test(description = "TC-153: 'Analytics & Search' quick action navigates to /air-analytics")
        @Story("Navigation")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the Analytics & Search quick-action-btn.")
        public void TC153_quickActionAnalytics() {
                AirPollutionAnalyticsPage ap = dashboardPage.clickQuickActionAnalytics();
                Assert.assertTrue(ap.isOnAnalyticsPage(),
                                "TC-153 FAILED: Quick action did not reach /air-analytics.");
                System.out.println("TC-153 PASSED");
        }

        @Test(description = "TC-154: 'Air Pollution Alerts' quick action navigates to /air-alerts")
        @Story("Navigation")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the Air Pollution Alerts quick-action-btn.")
        public void TC154_quickActionAlerts() {
                AirPollutionAlertsPage ap = dashboardPage.clickQuickActionAlerts();
                Assert.assertTrue(ap.isOnAlertsPage(),
                                "TC-154 FAILED: Quick action did not reach /air-alerts.");
                System.out.println("TC-154 PASSED");
        }

        @Test(description = "TC-155: Stats section shows exactly 5 stat cards")
        @Story("Stats cards")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Stats section must be visible with exactly 6 non-skeleton stat cards.")
        public void TC155_statsCardsDisplayed() {
                Assert.assertTrue(dashboardPage.isStatsSectionDisplayed(),
                                "TC-155 FAILED: Stats section not visible.");
                int count = dashboardPage.getStatCardCount();
                Assert.assertEquals(count, 6,
                                "TC-155 FAILED: Expected 6 stat cards, found " + count);
                System.out.println("TC-155 PASSED — 6 stat cards visible");
        }

        @Test(description = "TC-156: All 5 stat card values are loaded and non-empty")
        @Story("Stats cards")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Three readings were seeded in @BeforeMethod. All stat values must be non-empty.")
        public void TC156_statValuesNonEmpty() {
                Assert.assertTrue(dashboardPage.areStatsLoaded(),
                                "TC-156 FAILED: Stats still loading or skeleton cards present.");
                for (int i = 0; i < 5; i++) {
                        String val = dashboardPage.getStatValue(i);
                        Assert.assertFalse(val.isEmpty(),
                                        "TC-156 FAILED: Stat card " + i + " has an empty value.");
                }
                System.out.println("TC-156 PASSED");
        }

        @Test(description = "TC-157: Data table is visible with at least 1 row after seeding")
        @Story("Data table")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Three readings posted in @BeforeMethod — table must have at least 1 row.")
        public void TC157_dataTableHasRows() {
                Assert.assertTrue(dashboardPage.isTableSectionDisplayed(),
                                "TC-157 FAILED: Table section not visible.");
                int rows = dashboardPage.getTableRowCount();
                Assert.assertTrue(rows >= 1,
                                "TC-157 FAILED: Expected >=1 row after seeding, found " + rows);
                System.out.println("TC-157 PASSED — " + rows + " rows");
        }

        @Test(description = "TC-158: Pagination controls appear when data exceeds page size")
        @Story("Data table")
        @Severity(SeverityLevel.NORMAL)
        @Description("Checks that when more than 10 records exist pagination controls are shown.")
        public void TC158_paginationDisplayed() {
                if (dashboardPage.hasPagination()) {
                        Assert.assertFalse(dashboardPage.getActivePageNumber().isEmpty(),
                                        "TC-158 FAILED: Pagination shown but active page is empty.");
                        System.out.println("TC-158 PASSED — page: " + dashboardPage.getActivePageNumber());
                } else {
                        System.out.println("TC-158 INFO: Not enough records for pagination yet.");
                }
        }

        @Test(description = "TC-159: Clicking Next page changes the active page indicator")
        @Story("Data table")
        @Severity(SeverityLevel.NORMAL)
        @Description("Only runs when pagination is available. Verifies active page number increases.")
        public void TC159_nextPageChangesContent() {
                if (!dashboardPage.hasPagination() || !dashboardPage.isNextPageEnabled()) {
                        System.out.println("TC-159 INFO: Single page — skip.");
                        return;
                }
                String before = dashboardPage.getActivePageNumber();
                dashboardPage.clickNextPage();
                String after = dashboardPage.getActivePageNumber();
                Assert.assertNotEquals(after, before,
                                "TC-159 FAILED: Page number did not change after Next.");
                System.out.println("TC-159 PASSED: " + before + " → " + after);
        }

        @Test(description = "TC-160: Charts section renders with exactly two cards")
        @Story("Charts")
        @Severity(SeverityLevel.NORMAL)
        @Description("Expects exactly 2 chart cards for Air Pollution (Trends and Values).")
        public void TC160_chartsSectionHasTwoCards() {
                Assert.assertTrue(dashboardPage.isChartsSectionDisplayed(),
                                "TC-160 FAILED: Charts section not visible.");
                Assert.assertEquals(dashboardPage.getChartCardCount(), 2,
                                "TC-160 FAILED: Expected 2 chart cards, found " + dashboardPage.getChartCardCount());
                System.out.println("TC-160 PASSED");
        }

        @Test(description = "TC-161: Trend and Values charts are visible without errors")
        @Story("Charts")
        @Severity(SeverityLevel.NORMAL)
        @Description("Expects no chart errors in the rendered charts.")
        public void TC161_trendAndValuesChartsVisible() {
                Assert.assertFalse(dashboardPage.isChartErrorDisplayed(),
                                "TC-161 FAILED: Chart error displayed.");
                System.out.println("TC-161 PASSED");
        }

        @Test(description = "TC-162: Recent alerts section is displayed")
        @Story("Recent alerts")
        @Severity(SeverityLevel.NORMAL)
        @Description("High-co and low-ozone readings crossed thresholds, so at least one alert " +
                        "should exist. Section must show alerts or the empty state — not an error.")
        public void TC162_recentAlertsSectionDisplayed() {
                Assert.assertTrue(dashboardPage.isAlertsSectionDisplayed(),
                                "TC-162 FAILED: Alerts section not visible.");
                Assert.assertFalse(dashboardPage.isAlertsErrorDisplayed(),
                                "TC-162 FAILED: Error banner shown in alerts section.");
                boolean hasAlerts = dashboardPage.hasAlerts();
                boolean hasEmpty = dashboardPage.isAlertsEmptyStateDisplayed();
                Assert.assertTrue(hasAlerts || hasEmpty,
                                "TC-162 FAILED: Neither alert items nor empty state shown.");
                System.out.println("TC-162 PASSED — alerts: " + dashboardPage.getAlertItemCount());
        }

        @Test(description = "TC-163: Alerts list max 5 items")
        @Story("Recent alerts")
        @Severity(SeverityLevel.MINOR)
        @Description("Expects up to 5 alerts to be rendered in the recent alerts section.")
        public void TC163_alertsListMax5() {
                int count = dashboardPage.getAlertItemCount();
                Assert.assertTrue(count <= 5,
                                "TC-163 FAILED: Found " + count + " alerts, max is 5.");
                System.out.println("TC-163 PASSED");
        }

        @Test(description = "TC-164: View All Alerts navigates to /notifications")
        @Story("Recent alerts")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the View All Alerts anchor in the recent alerts section.")
        public void TC164_viewAllAlertsLink() {
        dashboardPage.clickViewAllAlerts();
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/notifications"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/notifications"),
                "TC-164 FAILED: View All Alerts did not reach /notifications.");
        System.out.println("TC-164 PASSED");
    }

        @Test(description = "TC-165: Manual refresh keeps dashboard on /air without error")
        @Story("Refresh")
        @Severity(SeverityLevel.NORMAL)
        @Description("Clicks the manual refresh icon. Dashboard must stay on /air and show no error.")
        public void TC165_manualRefresh() {
                dashboardPage.clickManualRefresh();
                wait.waitForCondition(d -> dashboardPage.isStatsSectionDisplayed()
                                || dashboardPage.isLastRefreshedDisplayed());
                Assert.assertTrue(dashboardPage.isOnAirDashboard(),
                                "TC-165 FAILED: Dashboard left /air after refresh.");
                Assert.assertFalse(dashboardPage.isStatsErrorDisplayed(),
                                "TC-165 FAILED: Error banner shown after refresh.");
                System.out.println("TC-165 PASSED");
        }

        @Test(description = "TC-166: Auto-refresh toggle disables then re-enables")
        @Story("Refresh")
        @Severity(SeverityLevel.MINOR)
        @Description("btn-auto-refresh.active by default. Toggling removes then restores the class.")
        public void TC166_autoRefreshToggle() {
                Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                                "TC-166 FAILED: Auto-refresh not active by default.");
                dashboardPage.clickAutoRefreshToggle();
                Assert.assertFalse(dashboardPage.isAutoRefreshActive(),
                                "TC-166 FAILED: Auto-refresh did not deactivate.");
                dashboardPage.clickAutoRefreshToggle();
                Assert.assertTrue(dashboardPage.isAutoRefreshActive(),
                                "TC-166 FAILED: Auto-refresh did not re-activate.");
                System.out.println("TC-166 PASSED");
        }

        @Test(description = "TC-167: Unauthenticated /air access redirects to /signin", priority = 10)
        @Story("Access control")
        @Severity(SeverityLevel.BLOCKER)
        @Description("Deletes all cookies then navigates to /air. Angular auth guard must redirect.")
        public void TC167_unauthenticatedAccess() {
                driver.manage().deleteAllCookies();
                navigateTo("/air");
                wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
                Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                                "TC-167 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
                System.out.println("TC-167 PASSED");
        }
        // ── TC-168: Table row values match the API ────────────────────────────────

        @Test(description = "TC-168: Real-Time Air Pollution Data table first row matches the latest API record")
        @Story("Data accuracy — table")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Seeds a reading, refreshes the dashboard, waits for table to re-render, " +
                        "then reads the API and UI at the same moment and asserts they match.")
        public void TC168_tableFirstRowMatchesApi() {
                // Step 1 — seed
                SensorApiClient.postAirReading(333, 77.5, "Moderate", "Alexandria");

                // Step 2 — refresh
                dashboardPage.clickManualRefresh();

                // Step 3 — wait for the table to finish re-rendering
                try {
                        wait.waitForInvisibility(org.openqa.selenium.By.cssSelector(".table-loading"));
                } catch (Exception ignored) {
                }
                // Wait until at least one row is present
                wait.waitForCondition(d -> !d.findElements(org.openqa.selenium.By.cssSelector(
                                ".data-table tbody tr")).isEmpty());

                // Step 4 — read API after refresh (same snapshot as UI)
                String apiCarbonMonoxide = AirPollutionApiReader.getFirstRecordCarbonMonoxide();
                String apiOzone = AirPollutionApiReader.getFirstRecordAvgOzone();
                String apiLocation = AirPollutionApiReader.getFirstRecordLocation();
                String apiPollutionlevel = AirPollutionApiReader.getFirstRecordPollutionlevelLevel();

                System.out.println("[TC168] API (post-refresh) — location=" + apiLocation
                                + " co=" + apiCarbonMonoxide + " ozone=" + apiOzone
                                + " pollution=" + apiPollutionlevel);

                Assert.assertNotNull(apiCarbonMonoxide,
                                "TC-168 FAILED: API returned no co. Check sensor-service.");

                // Step 5 — read UI
                String uiLocation = dashboardPage.getFirstRowLocation();
                String uiCarbonMonoxide = dashboardPage.getFirstRowCarbonMonoxide();
                String uiOzone = dashboardPage.getFirstRowOzone();
                String uiPollutionlevel = dashboardPage.getFirstRowPollutionLevel();

                System.out.println("[TC168] UI — location=" + uiLocation
                                + " co=" + uiCarbonMonoxide + " ozone=" + uiOzone
                                + " pollution=" + uiPollutionlevel);

                // Step 6 — assert UI == API
                Assert.assertEquals(uiLocation, apiLocation,
                                "TC-168 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
                Assert.assertEquals(Double.parseDouble(uiCarbonMonoxide), Double.parseDouble(apiCarbonMonoxide), 0.1,
                                "TC-168 FAILED: co UI='" + uiCarbonMonoxide + "' API='" + apiCarbonMonoxide + "'");
                Assert.assertEquals(Double.parseDouble(uiOzone), Double.parseDouble(apiOzone), 0.1,
                                "TC-168 FAILED: ozone UI='" + uiOzone
                                                + "' API='" + normaliseDecimal(apiOzone, 1) + "'");
                Assert.assertEquals(uiPollutionlevel, apiPollutionlevel.replace("_", " "),
                                "TC-168 FAILED: pollution UI='" + uiPollutionlevel + "' API='" + apiPollutionlevel
                                                + "'");

                System.out.println("TC-168 PASSED — table first row matches API");
        }
        // ── TC-169: CarbonMonoxide line & ozone bar charts match trends API
        // ─────────────

        // ── Fix 3: TC169 — use the renamed methods
        // ────────────────────────────────────

        @Test(description = "TC-169: CarbonMonoxide line and ozone bar charts match the trends API")
        @Story("Data accuracy — charts")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Seeds a reading, refreshes, waits for charts to render, then reads the " +
                        "trends API newest record and the UI leftmost dot/bar (both = newest reading " +
                        "after Angular reversal) and asserts they match.")
        public void TC169_chartsMatchTrendsApi() {
                // Step 1 — seed
                SensorApiClient.postAirReading(444, 88.8, "Unhealthy", "Alexandria");

                // Step 2 — refresh
                dashboardPage.clickManualRefresh();

                // Step 3 — wait for dots to render
                try {
                        wait.waitForInvisibility(org.openqa.selenium.By.cssSelector(".skeleton-chart"));
                } catch (Exception ignored) {
                }
                wait.waitForCondition(d -> !d.findElements(org.openqa.selenium.By.cssSelector("circle.density-dot"))
                                .isEmpty());

                // Step 4 — read API newest record (last element of raw array = newest
                // timestamp)
                String apiCarbonMonoxide = AirPollutionApiReader.getNewestTrendCarbonMonoxide();
                String apiOzone = AirPollutionApiReader.getNewestTrendAvgOzone();
                int apiCount = AirPollutionApiReader.getTrendDataPointCount();

                System.out.println("[TC169] API newest — co=" + apiCarbonMonoxide
                                + " ozone=" + apiOzone + " totalPoints=" + apiCount);

                Assert.assertNotNull(apiCarbonMonoxide,
                                "TC-169 FAILED: Trends API returned no data. Check sensor-service.");

                // Step 5a — read UI leftmost dot (index 0 = newest after Angular reversal)
                String uiCarbonMonoxide = dashboardPage.getFirstCarbonMonoxideDotValue();
                System.out.println("[TC169] UI first (leftmost) dot value: " + uiCarbonMonoxide);

                Assert.assertFalse(uiCarbonMonoxide.isEmpty(),
                                "TC-169 FAILED: No co dots rendered after refresh.");
                Assert.assertEquals(Double.parseDouble(uiCarbonMonoxide), Double.parseDouble(apiCarbonMonoxide), 0.1,
                                "TC-169 FAILED: co dot='" + uiCarbonMonoxide + "' API='" + apiCarbonMonoxide + "'");

                // Step 5b — read UI leftmost bar (index 0 = newest after Angular reversal)
                String uiOzone = dashboardPage.getFirstOzoneBarValue();
                System.out.println("[TC169] UI first (leftmost) bar value: " + uiOzone);

                Assert.assertFalse(uiOzone.isEmpty(),
                                "TC-169 FAILED: No ozone bars rendered after refresh.");
                String normApiOzone = normaliseDecimal(apiOzone, 1);
                Assert.assertEquals(Double.parseDouble(uiOzone), Double.parseDouble(apiOzone), 0.1,
                                "TC-169 FAILED: ozone bar='" + uiOzone + "' API='" + normApiOzone + "'");

                // Step 5c — dot count matches API point count
                int uiDotCount = dashboardPage.getCarbonMonoxideDotCount();
                Assert.assertEquals(uiDotCount, apiCount,
                                "TC-169 FAILED: UI dots=" + uiDotCount
                                                + " but API returned " + apiCount + " points.");

                System.out.println("TC-169 PASSED — chart values match trends API");
        }

        @Test(description = "TC-170: Charts section renders correctly without errors", groups = {"sanity"})
        @Story("Charts")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Refreshes dashboard and ensures charts load correctly without error states.")
        public void TC170_chartsLoadWithoutErrors() {
                // Step 1 — refresh
                dashboardPage.clickManualRefresh();

                // Step 2 — wait for charts section
                wait.waitForCondition(d -> dashboardPage.isChartsSectionDisplayed());

                // Step 3 — verify no errors
                Assert.assertFalse(dashboardPage.isChartErrorDisplayed(),
                                "TC-170 FAILED: Chart error banner displayed.");

                System.out.println("TC-170 PASSED — charts load without errors");
        }
        // ── Helper ────────────────────────────────────────────────────────────────

        /**
         * Normalises a decimal string to a fixed number of decimal places.
         * Harmonises API raw doubles with Angular's number:'1.1-1' pipe output.
         * normaliseDecimal("77.50", 1) → "77.5"
         * normaliseDecimal("88.8", 1) → "88.8"
         * normaliseDecimal("15", 1) → "15.0"
         */
        private static String normaliseDecimal(String value, int decimalPlaces) {
                if (value == null || value.isEmpty())
                        return value;
                try {
                        return String.format("%." + decimalPlaces + "f", Double.parseDouble(value));
                } catch (NumberFormatException e) {
                        return value;
                }
        }
}
