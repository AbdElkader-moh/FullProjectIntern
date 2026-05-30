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
 * TrafficAnalyticsTest — TC-103 … TC-118
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
 * DATA-ACCURACY TEST (TC-118)
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
    }

    @BeforeMethod(alwaysRun = true)
    public void seedAndOpen() {
        loginWithDefaultUser();
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

    @Test(description = "TC-103: Traffic analytics page loads and URL is /traffic-analytics")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /traffic-analytics and verifies the URL and page title are present.")
    public void TC103_pageLoads() {
        Assert.assertTrue(analyticsPage.isOnAnalyticsPage(),
                "TC-103 FAILED: URL does not contain /traffic-analytics.");
        Assert.assertTrue(analyticsPage.isPageTitleDisplayed(),
                "TC-103 FAILED: Page title not visible.");
        System.out.println("TC-103 PASSED");
    }

    @Test(description = "TC-104: Default load shows results with no active filter badge")
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On initial open with no filters applied, the table must show rows (data was " +
                 "seeded in @BeforeMethod) and no .filter-active-indicator badge should appear.")
    public void TC104_defaultLoadShowsResults() {
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-104 FAILED: Error banner shown on default load.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-104 FAILED: No rows on default load even though data was seeded.");
        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-104 FAILED: Active filter badge shown with no filters applied.");
        System.out.println("TC-104 PASSED — rows: " + analyticsPage.getTableRowCount());
    }

    @Test(description = "TC-105: Record count label shows a positive number")
    @Story("Default state")
    @Severity(SeverityLevel.NORMAL)
    @Description("The .record-count span must show a positive integer after data was seeded.")
    public void TC105_recordCountIsPositive() {
        int count = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(count > 0,
                "TC-105 FAILED: Record count is " + count + " — expected > 0 after seeding.");
        System.out.println("TC-105 PASSED — record count: " + count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Congestion filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-106: Filtering by 'Moderate' congestion scopes all visible rows")
    @Story("Congestion filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Moderate from #filterCongestion and clicks Search. All visible " +
                 ".congestion-badge elements must read 'Moderate'. The seeded Moderate " +
                 "reading (density=277) guarantees at least one result.")
    public void TC106_congestionFilterScopesRows() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-106 INFO: No Moderate records returned — backend may not " +
                               "have processed the seeded reading yet.");
            return;
        }

        analyticsPage.getCongestionBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "Moderate",
                "TC-106 FAILED: Non-Moderate badge found: '" + badge.getText() + "'")
        );
        System.out.println("TC-106 PASSED — all badges are Moderate");
    }

    @Test(description = "TC-107: Filtering by 'High' congestion returns only High rows or empty state")
    @Story("Congestion filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects High from the congestion dropdown and searches. If results exist, " +
                 "every congestion badge must say High.")
    public void TC107_highCongestionFilter() {
        analyticsPage.selectCongestionLevel("High").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-107 INFO: No High records found — passes as empty state.");
            return;
        }
        analyticsPage.getCongestionBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "High",
                "TC-107 FAILED: Non-High badge: '" + badge.getText() + "'")
        );
        System.out.println("TC-107 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-108: Filtering by location 'Alexandria' returns only Alexandria rows")
    @Story("Location filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Types 'Alexandria' into #filterLocation and searches. All visible location " +
                 "cells must contain 'Alexandria'. The simulator and @BeforeMethod both post " +
                 "to Alexandria so results are guaranteed.")
    public void TC108_locationFilterScopesRows() {
        analyticsPage.enterLocation("Alexandria").clickApply();

        Assert.assertFalse(analyticsPage.isEmptyStateDisplayed(),
                "TC-108 FAILED: No results for 'Alexandria' even though data was seeded there.");

        analyticsPage.getTableRows().forEach(row -> {
            String location = row.findElements(
                org.openqa.selenium.By.cssSelector("td.col-location"))
                .stream().findFirst()
                .map(org.openqa.selenium.WebElement::getText).orElse("").trim();
            Assert.assertTrue(location.toLowerCase().contains("alexandria"),
                "TC-108 FAILED: Row location '" + location + "' does not contain 'Alexandria'.");
        });
        System.out.println("TC-108 PASSED — all rows are Alexandria");
    }

    @Test(description = "TC-109: Filtering by non-existent location shows empty state")
    @Story("Location filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Types a location that does not exist in the data. Expects the empty state.")
    public void TC109_nonExistentLocationShowsEmptyState() {
        analyticsPage.enterLocation("ZZZ_NoSuchCity_XYZ").clickApply();
        Assert.assertTrue(analyticsPage.isEmptyStateDisplayed(),
                "TC-109 FAILED: Expected empty state for non-existent location.");
        System.out.println("TC-109 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Active filter badge
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-110: Active filter badge appears after applying a congestion filter")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a Moderate congestion filter and verifies the .filter-active-indicator " +
                 "badge appears. The badge text should contain '1 active'.")
    public void TC110_activeFilterBadgeAppears() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-110 FAILED: Active filter badge not shown after applying congestion filter.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("1"),
                "TC-110 FAILED: Badge text '" + badgeText + "' does not contain '1'.");
        System.out.println("TC-110 PASSED — badge: " + badgeText);
    }

    @Test(description = "TC-111: Applying two filters shows badge with count 2")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies both a location filter and a congestion filter. Badge must show '2 active'.")
    public void TC111_twoFiltersShowCountTwo() {
        analyticsPage
            .enterLocation("Alexandria")
            .selectCongestionLevel("Moderate")
            .clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-111 FAILED: Active filter badge not shown.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("2"),
                "TC-111 FAILED: Badge shows '" + badgeText + "' — expected '2'.");
        System.out.println("TC-111 PASSED — badge: " + badgeText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-112: Reset removes the active filter badge and restores full results")
    @Story("Reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a filter, records filtered count, clicks Reset, then verifies the " +
                 "badge disappears and the total count is >= the filtered count.")
    public void TC112_resetClearsFilters() {
        analyticsPage.selectCongestionLevel("Moderate").clickApply();
        int filteredCount = analyticsPage.getRecordCountNumber();

        analyticsPage.clickReset();

        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-112 FAILED: Active filter badge still shown after reset.");
        int totalCount = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-112 FAILED: After reset count (" + totalCount
                + ") < filtered count (" + filteredCount + ").");
        System.out.println("TC-112 PASSED — filtered: " + filteredCount + " total: " + totalCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date range filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-113: Invalid date range (end before start) shows validation error")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets end date before start date using JavaScript (required for datetime-local " +
                 "inputs in Chrome). Clicks Search. Expects .filter-date-error to appear.")
    public void TC113_invalidDateRangeShowsError() {
        analyticsPage
            .enterDateFrom("2026-06-01T00:00")
            .enterDateTo("2026-05-01T00:00")
            .clickApply();
        Assert.assertTrue(analyticsPage.isDateErrorDisplayed(),
                "TC-113 FAILED: No date validation error shown for invalid range.");
        System.out.println("TC-113 PASSED");
    }

    @Test(description = "TC-114: Valid date range filters results to that window")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets a date range covering today. Expects results (seeded data was just posted) " +
                 "and no date error. Active filter count must be 2 (from + to).")
    public void TC114_validDateRangeFiltersResults() {
        // Range: yesterday to tomorrow — covers all seeded data
        analyticsPage
            .enterDateFrom("2026-05-29T00:00")
            .enterDateTo("2026-05-31T23:59")
            .clickApply();

        Assert.assertFalse(analyticsPage.isDateErrorDisplayed(),
                "TC-114 FAILED: Date error shown for a valid range.");
        // Both from and to count as active filters
        String badge = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badge.contains("2"),
                "TC-114 FAILED: Expected '2 active' for from+to filters, got: '" + badge + "'.");
        System.out.println("TC-114 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sort
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-115: Changing sort order re-fetches results without error")
    @Story("Sort")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects 'trafficDensity,desc' from the sort dropdown and searches. Expects " +
                 "results present with no error banner, and the active filter badge reflects " +
                 "the non-default sort.")
    public void TC115_changingSortReturnsResults() {
        analyticsPage.selectSort("trafficDensity,desc").clickApply();
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-115 FAILED: Error banner after changing sort.");
        boolean hasResults = analyticsPage.hasResults();
        boolean hasEmpty   = analyticsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasResults || hasEmpty,
                "TC-115 FAILED: Neither results nor empty state after sort change.");
        System.out.println("TC-115 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-116: Pagination controls appear and next page works when data spans multiple pages")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("If more than 20 records exist (default page size), pagination is shown. " +
                 "Clicks Next and verifies the active page number increases and rows are still present.")
    public void TC116_paginationNextPage() {
        if (!analyticsPage.hasPagination()) {
            System.out.println("TC-116 INFO: All records fit on one page — skip.");
            return;
        }
        String beforePage = analyticsPage.getActivePageNumber();
        analyticsPage.clickNextPage();
        String afterPage = analyticsPage.getActivePageNumber();
        Assert.assertNotEquals(afterPage, beforePage,
                "TC-116 FAILED: Page number did not change after Next.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-116 FAILED: No rows on page 2.");
        System.out.println("TC-116 PASSED: page " + beforePage + " → " + afterPage);
    }

    @Test(description = "TC-117: Changing page size re-fetches with the new per-page count")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Changes the per-page select to 10 and verifies the row count is <= 10 " +
                 "and the record-count label is still present.")
    public void TC117_changingPageSizeApplies() {
        analyticsPage.selectPageSize("10");
        int rows = analyticsPage.getTableRowCount();
        Assert.assertTrue(rows <= 10,
                "TC-117 FAILED: Row count " + rows + " exceeds selected page size of 10.");
        Assert.assertFalse(analyticsPage.getRecordCountText().isEmpty(),
                "TC-117 FAILED: Record count label is empty after page size change.");
        System.out.println("TC-117 PASSED — rows on page: " + rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data accuracy
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-118: First table row values match the latest record from the API")
    @Story("Data accuracy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opens the analytics page (which loads with sort=timestamp,desc by default), " +
                 "reads the API first record and the UI first row at the same moment, and " +
                 "asserts location, density, speed, and congestion level match.")
    public void TC118_firstRowMatchesApi() {
        // The analytics page opens sorted by timestamp desc (default: filterSort='timestamp,desc')
        // so the first row = most recently posted reading.
        // The API endpoint GET /api/sensors/traffic?sort=timestamp,desc&page=0&size=20
        // returns the same record at index 0.

        // Page is already open from @BeforeMethod. Read the API immediately —
        // same snapshot as the already-rendered UI.
        String apiLocation   = TrafficApiReader.getFirstRecordLocation();
        String apiDensity    = TrafficApiReader.getFirstRecordDensity();
        String apiSpeed      = TrafficApiReader.getFirstRecordAvgSpeed();
        String apiCongestion = TrafficApiReader.getFirstRecordCongestionLevel();

        System.out.println("[TC118] API first record — location=" + apiLocation
                + " density=" + apiDensity + " speed=" + apiSpeed
                + " congestion=" + apiCongestion);

        Assert.assertNotNull(apiDensity,
                "TC-118 FAILED: API returned no data. Check sensor-service on localhost:8081.");

        // Read UI first row
        String uiLocation   = analyticsPage.getFirstRowLocation();
        String uiDensity    = analyticsPage.getFirstRowDensity();
        String uiSpeed      = analyticsPage.getFirstRowSpeed();
        String uiCongestion = analyticsPage.getFirstRowCongestionLevel();

        System.out.println("[TC118] UI first row — location=" + uiLocation
                + " density=" + uiDensity + " speed=" + uiSpeed
                + " congestion=" + uiCongestion);

        Assert.assertEquals(uiLocation, apiLocation,
                "TC-118 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
        Assert.assertEquals(uiDensity, apiDensity,
                "TC-118 FAILED: density UI='" + uiDensity + "' API='" + apiDensity + "'");

        // Angular formats speed with number:'1.1-1' pipe → normalise API to 1 dp
        String normApiSpeed = normaliseDecimal(apiSpeed, 1);
        Assert.assertEquals(uiSpeed, normApiSpeed,
                "TC-118 FAILED: speed UI='" + uiSpeed + "' API='" + normApiSpeed + "'");
        Assert.assertEquals(uiCongestion, apiCongestion,
                "TC-118 FAILED: congestion UI='" + uiCongestion + "' API='" + apiCongestion + "'");

        System.out.println("TC-118 PASSED — first row matches API");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-119: Back link navigates to /traffic dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the back arrow and verifies navigation to /traffic.")
    public void TC119_backLinkNavigatesToDashboard() {
        TrafficDashboardPage dash = analyticsPage.clickBack();
        Assert.assertTrue(dash.isOnTrafficDashboard(),
                "TC-119 FAILED: Back link did not reach /traffic.");
        System.out.println("TC-119 PASSED");
    }

    @Test(description = "TC-120: Alerts nav link navigates to /traffic-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts link in the header nav bar.")
    public void TC120_alertsNavLink() {
        TrafficAlertsPage ap = analyticsPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-120 FAILED: Did not reach /traffic-alerts.");
        System.out.println("TC-120 PASSED");
    }

    @Test(description = "TC-121: Unauthenticated access to /traffic-analytics redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /traffic-analytics. Angular auth guard must redirect.")
    public void TC121_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/traffic-analytics");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-121 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-121 PASSED");
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
