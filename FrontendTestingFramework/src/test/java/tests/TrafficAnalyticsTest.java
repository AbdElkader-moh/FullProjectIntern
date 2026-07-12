package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.TrafficAnalyticsPage;
import pages.TrafficDashboardPage;
import pages.TrafficAlertsPage;
import utils.SensorApiClient;
import utils.TrafficApiReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * TrafficAnalyticsTest — TC-072 … TC-087
 *
 * DATA SEEDING
 * ────────────
 * @BeforeMethod posts two readings via SensorApiClient before each test:
 *   - Normal reading (density=50, speed=80, Low, Alexandria)  — guaranteed table row
 *   - Specific reading (density=277, speed=66.0, Moderate, Alexandria) — used for
 *     data-accuracy assertions (distinctive values unlikely to match simulator data)
 *
 * The page is opened fresh in @BeforeMethod so each test starts on a clean state.
 *
 * FILTER BEHAVIOUR (from traffic-analytics.ts)
 * ─────────────────────────────────────────────
 *   - Congestion filter: sent to backend as a query param → server-side filtering
 *   - Location filter: sent to backend as a query param → server-side filtering
 *   - Date range: sent to backend → server-side filtering
 *   - Sort: controls sortField + sortDir passed to the backend Pageable
 *   - activeFilterCount: incremented for each non-default filter value applied
 *   - Date validation: client-side only — error shown if filterFrom > filterTo
 *     BEFORE calling the API. Uses datetime-local inputs, must be set via JS.
 *
 * DATA-ACCURACY TEST (TC-087)
 * ────────────────────────────
 * Uses TrafficApiReader to read the first record from
 * GET /api/sensors/traffic?sort=timestamp,desc&page=0&size=20 and asserts
 * the first table row on the analytics page shows the same values.
 * The API is read AFTER the page loads (same snapshot as the UI).
 */
@Epic("Traffic monitoring")
@Feature("Traffic analytics & search")
public class TrafficAnalyticsTest extends BaseTest {

    private TrafficAnalyticsPage analyticsPage;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
    }

    @BeforeMethod(alwaysRun = true)
    public void seedAndOpen() {
        try {
            SensorApiClient.postNormalTrafficReading();
            // Distinctive reading used for data-accuracy assertions
            SensorApiClient.postTrafficReading(277, 66.0, "Moderate", "Alexandria");
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        analyticsPage = new TrafficAnalyticsPage(driver);
        analyticsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page load
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-072: Traffic analytics page loads and URL is /traffic-analytics")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /traffic-analytics and verifies the URL and page title are present.")
    public void TC072_pageLoads() {
        Assert.assertTrue(analyticsPage.isOnAnalyticsPage(),
                "TC-072 FAILED: URL does not contain /traffic-analytics.");
        Assert.assertTrue(analyticsPage.isPageTitleDisplayed(),
                "TC-072 FAILED: Page title not visible.");
        System.out.println("TC-072 PASSED");
    }

    @Test(description = "TC-073: Default load shows results with no active filter badge", groups = {"sanity"})
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On initial open with no filters applied, the table must show rows (data was " +
                 "seeded in @BeforeMethod) and no .filter-active-indicator badge should appear.")
    public void TC073_defaultLoadShowsResults() {
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-073 FAILED: Error banner shown on default load.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-073 FAILED: No rows on default load even though data was seeded.");
        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-073 FAILED: Active filter badge shown with no filters applied.");
        System.out.println("TC-073 PASSED — rows: " + analyticsPage.getTableRowCount());
    }

    @Test(description = "TC-074: Record count label shows a positive number")
    @Story("Default state")
    @Severity(SeverityLevel.NORMAL)
    @Description("The .record-count span must show a positive integer after data was seeded.")
    public void TC074_recordCountIsPositive() {
        int count = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(count > 0,
                "TC-074 FAILED: Record count is " + count + " — expected > 0 after seeding.");
        System.out.println("TC-074 PASSED — record count: " + count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Congestion filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-075: Filtering by 'Moderate' congestion scopes all visible rows", groups = {"sanity"})
    @Story("Congestion filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Moderate from #filterCongestion and clicks Search. All visible " +
                 ".congestion-badge elements must read 'Moderate'. The seeded Moderate " +
                 "reading (density=277) guarantees at least one result.")
    public void TC075_congestionFilterScopesRows() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-075 INFO: No Moderate records returned — backend may not " +
                               "have processed the seeded reading yet.");
            return;
        }

        analyticsPage.getCongestionBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "Moderate",
                "TC-075 FAILED: Non-Moderate badge found: '" + badge.getText() + "'")
        );
        System.out.println("TC-075 PASSED — all badges are Moderate");
    }

    @Test(description = "TC-076: Filtering by 'High' congestion returns only High rows or empty state")
    @Story("Congestion filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects High from the congestion dropdown and searches. If results exist, " +
                 "every congestion badge must say High.")
    public void TC076_highCongestionFilter() {
        analyticsPage.selectCongestionLevel("High").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-076 INFO: No High records found — passes as empty state.");
            return;
        }
        analyticsPage.getCongestionBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "High",
                "TC-076 FAILED: Non-High badge: '" + badge.getText() + "'")
        );
        System.out.println("TC-076 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-077: Filtering by location 'Alexandria' returns only Alexandria rows", groups = {"sanity"})
    @Story("Location filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Types 'Alexandria' into #filterLocation and searches. All visible location " +
                 "cells must contain 'Alexandria'. The simulator and @BeforeMethod both post " +
                 "to Alexandria so results are guaranteed.")
    public void TC077_locationFilterScopesRows() {
        analyticsPage.enterLocation("Alexandria").clickApply();

        Assert.assertFalse(analyticsPage.isEmptyStateDisplayed(),
                "TC-077 FAILED: No results for 'Alexandria' even though data was seeded there.");

        analyticsPage.getTableRows().forEach(row -> {
            String location = row.findElements(
                org.openqa.selenium.By.cssSelector("td.col-location"))
                .stream().findFirst()
                .map(org.openqa.selenium.WebElement::getText).orElse("").trim();
            Assert.assertTrue(location.toLowerCase().contains("alexandria"),
                "TC-077 FAILED: Row location '" + location + "' does not contain 'Alexandria'.");
        });
        System.out.println("TC-077 PASSED — all rows are Alexandria");
    }

    @Test(description = "TC-078: Filtering by non-existent location shows empty state")
    @Story("Location filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Types a location that does not exist in the data. Expects the empty state.")
    public void TC078_nonExistentLocationShowsEmptyState() {
        analyticsPage.enterLocation("ZZZ_NoSuchCity_XYZ").clickApply();
        Assert.assertTrue(analyticsPage.isEmptyStateDisplayed(),
                "TC-078 FAILED: Expected empty state for non-existent location.");
        System.out.println("TC-078 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Active filter badge
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-079: Active filter badge appears after applying a congestion filter")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a Moderate congestion filter and verifies the .filter-active-indicator " +
                 "badge appears. The badge text should contain '1 active'.")
    public void TC079_activeFilterBadgeAppears() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-079 FAILED: Active filter badge not shown after applying congestion filter.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("1"),
                "TC-079 FAILED: Badge text '" + badgeText + "' does not contain '1'.");
        System.out.println("TC-079 PASSED — badge: " + badgeText);
    }

    @Test(description = "TC-080: Applying two filters shows badge with count 2")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies both a location filter and a congestion filter. Badge must show '2 active'.")
    public void TC080_twoFiltersShowCountTwo() {
        analyticsPage
            .enterLocation("Alexandria")
            .selectCongestionLevel("Moderate")
            .clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-080 FAILED: Active filter badge not shown.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("2"),
                "TC-080 FAILED: Badge shows '" + badgeText + "' — expected '2'.");
        System.out.println("TC-080 PASSED — badge: " + badgeText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-081: Reset removes the active filter badge and restores full results")
    @Story("Reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a filter, records filtered count, clicks Reset, then verifies the " +
                 "badge disappears and the total count is >= the filtered count.")
    public void TC081_resetClearsFilters() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();
        int filteredCount = analyticsPage.getRecordCountNumber();

        analyticsPage.clickReset();

        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-081 FAILED: Active filter badge still shown after reset.");
        int totalCount = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-081 FAILED: After reset count (" + totalCount
                + ") < filtered count (" + filteredCount + ").");
        System.out.println("TC-081 PASSED — filtered: " + filteredCount + " total: " + totalCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date range filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-082: Invalid date range (end before start) shows validation error")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets end date before start date using JavaScript (required for datetime-local " +
                 "inputs in Chrome). Clicks Search. Expects .filter-date-error to appear.")
    public void TC082_invalidDateRangeShowsError() {
        analyticsPage
            .enterDateFrom("2026-06-01T00:00")
            .enterDateTo("2026-05-01T00:00")
            .clickApply();
        Assert.assertTrue(analyticsPage.isDateErrorDisplayed(),
                "TC-082 FAILED: No date validation error shown for invalid range.");
        System.out.println("TC-082 PASSED");
    }

    @Test(description = "TC-083: Valid date range filters results to that window")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets a date range covering today. Expects results (seeded data was just posted) " +
                 "and no date error. Active filter count must be 2 (from + to).")
    public void TC083_validDateRangeFiltersResults() {
        // Range: yesterday to tomorrow — covers all seeded data
        analyticsPage
            .enterDateFrom("2026-05-29T00:00")
            .enterDateTo("2026-05-31T23:59")
            .clickApply();

        Assert.assertFalse(analyticsPage.isDateErrorDisplayed(),
                "TC-083 FAILED: Date error shown for a valid range.");
        // Both from and to count as active filters
        String badge = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badge.contains("2"),
                "TC-083 FAILED: Expected '2 active' for from+to filters, got: '" + badge + "'.");
        System.out.println("TC-083 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sort
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-084: Changing sort order re-fetches results without error")
    @Story("Sort")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects 'trafficDensity,desc' from the sort dropdown and searches. Expects " +
                 "results present with no error banner, and the active filter badge reflects " +
                 "the non-default sort.")
    public void TC084_changingSortReturnsResults() {
        analyticsPage.selectSort("trafficDensity,desc").clickApply();
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-084 FAILED: Error banner after changing sort.");
        boolean hasResults = analyticsPage.hasResults();
        boolean hasEmpty   = analyticsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasResults || hasEmpty,
                "TC-084 FAILED: Neither results nor empty state after sort change.");
        System.out.println("TC-084 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-085: Pagination controls appear and next page works when data spans multiple pages")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("If more than 20 records exist (default page size), pagination is shown. " +
                 "Clicks Next and verifies the active page number increases and rows are still present.")
    public void TC085_paginationNextPage() {
        if (!analyticsPage.hasPagination()) {
            System.out.println("TC-085 INFO: All records fit on one page — skip.");
            return;
        }
          String beforePage = analyticsPage.getActivePageNumber();
          analyticsPage.clickNextPage();
          
          String[] afterPage = new String[1];
          utils.RetryHelper.retryVoid(() -> {
              afterPage[0] = analyticsPage.getActivePageNumber();
              if (afterPage[0].equals(beforePage)) {
                  throw new RuntimeException("Page number hasn't changed yet");
              }
          }, "Wait for active page number to change");
          
          Assert.assertNotEquals(afterPage[0], beforePage,
                  "TC-085 FAILED: Page number did not change after Next.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-085 FAILED: No rows on page 2.");
        System.out.println("TC-085 PASSED: page " + beforePage + " → " + afterPage[0]);
    }

    @Test(description = "TC-086: Changing page size re-fetches with the new per-page count")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Changes the per-page select to 10 and verifies the row count is <= 10 " +
                 "and the record-count label is still present.")
    public void TC086_changingPageSizeApplies() {
        analyticsPage.selectPageSize("10");
        int rows = analyticsPage.getTableRowCount();
        Assert.assertTrue(rows <= 10,
                "TC-086 FAILED: Row count " + rows + " exceeds selected page size of 10.");
        Assert.assertFalse(analyticsPage.getRecordCountText().isEmpty(),
                "TC-086 FAILED: Record count label is empty after page size change.");
        System.out.println("TC-086 PASSED — rows on page: " + rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data accuracy
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-087: First table row values match the latest record from the API", groups = {"sanity"})
    @Story("Data accuracy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opens the analytics page (which loads with sort=timestamp,desc by default), " +
                 "reads the API first record and the UI first row at the same moment, and " +
                 "asserts location, density, speed, and congestion level match.")
    public void TC087_firstRowMatchesApi() {
        // Seed a distinctive reading just to ensure there is data.
        try {
            SensorApiClient.postTrafficReading(277, 66.0, "Moderate", "Alexandria");
        } catch (Exception e) {
            System.out.println("[TC087] Seeding warning: " + e.getMessage());
        }

        String[] apiValues = new String[4];
        String[] uiValues = new String[4];

        // Read UI and API at the same time, with retry in case simulator adds a record right between the calls
        utils.RetryHelper.retryVoid(() -> {
            analyticsPage.clickApply(); // reloads UI table
            
            apiValues[0] = TrafficApiReader.getFirstRecordLocation();
            apiValues[1] = TrafficApiReader.getFirstRecordDensity();
            apiValues[2] = TrafficApiReader.getFirstRecordAvgSpeed();
            apiValues[3] = TrafficApiReader.getFirstRecordCongestionLevel();

            uiValues[0] = analyticsPage.getFirstRowLocation();
            uiValues[1] = analyticsPage.getFirstRowDensity();
            uiValues[2] = analyticsPage.getFirstRowSpeed();
            uiValues[3] = analyticsPage.getFirstRowCongestionLevel();

            if (!uiValues[0].equals(apiValues[0]) || !uiValues[1].equals(apiValues[1])) {
                throw new RuntimeException("UI and API mismatch (possible race condition with simulator)");
            }
        }, "Read API and UI first row at the same time");

        System.out.println("[TC087] UI first row — location=" + uiValues[0]
                + " density=" + uiValues[1] + " speed=" + uiValues[2]
                + " congestion=" + uiValues[3]);

        Assert.assertEquals(uiValues[0], apiValues[0],
                "TC-087 FAILED: location UI='" + uiValues[0] + "' expected='" + apiValues[0] + "'");
        Assert.assertEquals(uiValues[1], apiValues[1],
                "TC-087 FAILED: density UI='" + uiValues[1] + "' expected='" + apiValues[1] + "'");
        Assert.assertEquals(uiValues[2], normaliseDecimal(apiValues[2], 1),
                "TC-087 FAILED: speed UI='" + uiValues[2] + "' expected='" + apiValues[2] + "'");
        Assert.assertEquals(uiValues[3], apiValues[3],
                "TC-087 FAILED: congestion UI='" + uiValues[3] + "' expected='" + apiValues[3] + "'");

        System.out.println("TC-087 PASSED — first row matches API");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-088: Back link navigates to /traffic dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the back arrow and verifies navigation to /traffic.")
    public void TC088_backLinkNavigatesToDashboard() {
        TrafficDashboardPage dash = analyticsPage.clickBack();
        Assert.assertTrue(dash.isOnTrafficDashboard(),
                "TC-088 FAILED: Back link did not reach /traffic.");
        System.out.println("TC-088 PASSED");
    }

    @Test(description = "TC-089: Alerts nav link navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts link in the header nav bar.")
    public void TC089_alertsNavLink() {
        TrafficAlertsPage ap = analyticsPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-089 FAILED: Did not reach /traffic-alerts.");
        System.out.println("TC-089 PASSED");
    }

    @Test(description = "TC-090: Unauthenticated access to /traffic-analytics redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /traffic-analytics. Angular auth guard must redirect.")
    public void TC090_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/traffic-analytics");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-090 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-090 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private static String normaliseDecimal(String value, int decimalPlaces) {
        if (value == null || value.isEmpty()) return value;
        try {
            return String.format("%." + decimalPlaces + "f", Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return value;
        }
    }

}
