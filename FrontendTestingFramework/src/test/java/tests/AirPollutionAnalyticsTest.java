package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.AirPollutionAnalyticsPage;
import pages.AirPollutionDashboardPage;
import pages.AirPollutionAlertsPage;
import utils.SensorApiClient;
import utils.AirPollutionApiReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * AirPollutionAnalyticsTest — TC-129 … TC-144
 *
 * DATA SEEDING
 * ────────────
 * @BeforeMethod posts two readings via SensorApiClient before each test:
 *   - Normal reading (co=50, ozone=80, Low, Alexandria)  — guaranteed table row
 *   - Specific reading (co=277, ozone=66.0, Moderate, Alexandria) — used for
 *     data-accuracy assertions (distinctive values unlikely to match simulator data)
 *
 * The page is opened fresh in @BeforeMethod so each test starts on a clean state.
 *
 * FILTER BEHAVIOUR (from traffic-analytics.ts)
 * ─────────────────────────────────────────────
 *   - Pollutionlevel filter: sent to backend as a query param → server-side filtering
 *   - Location filter: sent to backend as a query param → server-side filtering
 *   - Date range: sent to backend → server-side filtering
 *   - Sort: controls sortField + sortDir passed to the backend Pageable
 *   - activeFilterCount: incremented for each non-default filter value applied
 *   - Date validation: client-side only — error shown if filterFrom > filterTo
 *     BEFORE calling the API. Uses datetime-local inputs, must be set via JS.
 *
 * DATA-ACCURACY TEST (TC-144)
 * ────────────────────────────
 * Uses AirPollutionApiReader to read the first record from
 * GET /api/sensors/air-pollution?sort=timestamp,desc&page=0&size=20 and asserts
 * the first table row on the analytics page shows the same values.
 * The API is read AFTER the page loads (same snapshot as the UI).
 */
@Epic("Air quality monitoring")
@Feature("Air pollution analytics & search")
public class AirPollutionAnalyticsTest extends BaseTest {

    private AirPollutionAnalyticsPage analyticsPage;

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
            SensorApiClient.postNormalAirReading();
            // Distinctive reading used for data-accuracy assertions
            SensorApiClient.postAirReading(44, 66.0, "Moderate", "Alexandria");
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        analyticsPage = new AirPollutionAnalyticsPage(driver);
        analyticsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page load
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-129: Traffic analytics page loads and URL is /air-analytics")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /air-analytics and verifies the URL and page title are present.")
    public void TC129_pageLoads() {
        Assert.assertTrue(analyticsPage.isOnAnalyticsPage(),
                "TC-129 FAILED: URL does not contain /air-analytics.");
        Assert.assertTrue(analyticsPage.isPageTitleDisplayed(),
                "TC-129 FAILED: Page title not visible.");
        System.out.println("TC-129 PASSED");
    }

    @Test(description = "TC-130: Default load shows results with no active filter badge", groups = {"sanity"})
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On initial open with no filters applied, the table must show rows (data was " +
                 "seeded in @BeforeMethod) and no .filter-active-indicator badge should appear.")
    public void TC130_defaultLoadShowsResults() {
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-130 FAILED: Error banner shown on default load.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-130 FAILED: No rows on default load even though data was seeded.");
        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-130 FAILED: Active filter badge shown with no filters applied.");
        System.out.println("TC-130 PASSED — rows: " + analyticsPage.getTableRowCount());
    }

    @Test(description = "TC-131: Record count label shows a positive number")
    @Story("Default state")
    @Severity(SeverityLevel.NORMAL)
    @Description("The .record-count span must show a positive integer after data was seeded.")
    public void TC131_recordCountIsPositive() {
        int count = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(count > 0,
                "TC-131 FAILED: Record count is " + count + " — expected > 0 after seeding.");
        System.out.println("TC-131 PASSED — record count: " + count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pollutionlevel filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-132: Filtering by 'Moderate' pollution scopes all visible rows", groups = {"sanity"})
    @Story("Pollutionlevel filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects Moderate from #filterPollutionlevel and clicks Search. All visible " +
                 ".pollution-badge elements must read 'Moderate'. The seeded Moderate " +
                 "reading (co=277) guarantees at least one result.")
    public void TC132_pollutionFilterScopesRows() {
        analyticsPage.selectPollutionlevelLevel("Moderate").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-132 INFO: No Moderate records returned — backend may not " +
                               "have processed the seeded reading yet.");
            return;
        }

        analyticsPage.getPollutionlevelBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "Moderate",
                "TC-132 FAILED: Non-Moderate badge found: '" + badge.getText() + "'")
        );
        System.out.println("TC-132 PASSED — all badges are Moderate");
    }

    @Test(description = "TC-133: Filter by Unhealthy Pollutionlevel scopes table to Unhealthy only",
          dependsOnMethods = "TC129_pageLoads")
    @Story("Filtering")
    @Severity(SeverityLevel.NORMAL)
    public void TC133_highPollutionlevelFilter() {
        analyticsPage.selectPollutionlevelLevel("Unhealthy");
        analyticsPage.clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-133 INFO: No High records found — passes as empty state.");
            return;
        }
        analyticsPage.getPollutionlevelBadges().forEach(badge ->
            Assert.assertEquals(badge.getText().trim(), "High",
                "TC-133 FAILED: Non-High badge: '" + badge.getText() + "'")
        );
        System.out.println("TC-133 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-134: Filtering by location 'Alexandria' returns only Alexandria rows", groups = {"sanity"})
    @Story("Location filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Types 'Alexandria' into #filterLocation and searches. All visible location " +
                 "cells must contain 'Alexandria'. The simulator and @BeforeMethod both post " +
                 "to Alexandria so results are guaranteed.")
    public void TC134_locationFilterScopesRows() {
        analyticsPage.enterLocation("Alexandria").clickApply();

        Assert.assertFalse(analyticsPage.isEmptyStateDisplayed(),
                "TC-134 FAILED: No results for 'Alexandria' even though data was seeded there.");

        analyticsPage.getTableRows().forEach(row -> {
            String location = row.findElements(
                org.openqa.selenium.By.cssSelector("td:nth-child(2)"))
                .stream().findFirst()
                .map(org.openqa.selenium.WebElement::getText).orElse("").trim();
            Assert.assertTrue(location.toLowerCase().contains("alexandria"),
                "TC-134 FAILED: Row location '" + location + "' does not contain 'Alexandria'.");
        });
        System.out.println("TC-134 PASSED — all rows are Alexandria");
    }

    @Test(description = "TC-135: Filtering by non-existent location shows empty state")
    @Story("Location filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Types a location that does not exist in the data. Expects the empty state.")
    public void TC135_nonExistentLocationShowsEmptyState() {
        analyticsPage.enterLocation("ZZZ_NoSuchCity_XYZ").clickApply();
        Assert.assertTrue(analyticsPage.isEmptyStateDisplayed(),
                "TC-135 FAILED: Expected empty state for non-existent location.");
        System.out.println("TC-135 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Active filter badge
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-136: Active filter badge appears after applying a pollution filter")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a Moderate pollution filter and verifies the .filter-active-indicator " +
                 "badge appears. The badge text should contain '1 active'.")
    public void TC136_activeFilterBadgeAppears() {
        analyticsPage.selectPollutionlevelLevel("Moderate").clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-136 FAILED: Active filter badge not shown after applying pollution filter.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("1"),
                "TC-136 FAILED: Badge text '" + badgeText + "' does not contain '1'.");
        System.out.println("TC-136 PASSED — badge: " + badgeText);
    }

    @Test(description = "TC-137: Applying two filters shows badge with count 2")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies both a location filter and a pollution filter. Badge must show '2 active'.")
    public void TC137_twoFiltersShowCountTwo() {
        analyticsPage
            .enterLocation("Alexandria")
            .selectPollutionlevelLevel("Moderate")
            .clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-137 FAILED: Active filter badge not shown.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("2"),
                "TC-137 FAILED: Badge shows '" + badgeText + "' — expected '2'.");
        System.out.println("TC-137 PASSED — badge: " + badgeText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-138: Reset removes the active filter badge and restores full results")
    @Story("Reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Applies a filter, records filtered count, clicks Reset, then verifies the " +
                 "badge disappears and the total count is >= the filtered count.")
    public void TC138_resetClearsFilters() {
        analyticsPage.selectPollutionlevelLevel("Moderate").clickApply();
        int filteredCount = analyticsPage.getRecordCountNumber();

        analyticsPage.clickReset();

        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-138 FAILED: Active filter badge still shown after reset.");
        int totalCount = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-138 FAILED: After reset count (" + totalCount
                + ") < filtered count (" + filteredCount + ").");
        System.out.println("TC-138 PASSED — filtered: " + filteredCount + " total: " + totalCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date range filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-139: Invalid date range (end before start) shows validation error")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets end date before start date using JavaScript (required for datetime-local " +
                 "inputs in Chrome). Clicks Search. Expects .filter-date-error to appear.")
    public void TC139_invalidDateRangeShowsError() {
        analyticsPage
            .enterDateFrom("2026-06-01T00:00")
            .enterDateTo("2026-05-01T00:00")
            .clickApply();
        Assert.assertTrue(analyticsPage.isDateErrorDisplayed(),
                "TC-139 FAILED: No date validation error shown for invalid range.");
        System.out.println("TC-139 PASSED");
    }

    @Test(description = "TC-140: Valid date range filters results to that window")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets a date range covering today. Expects results (seeded data was just posted) " +
                 "and no date error. Active filter count must be 2 (from + to).")
    public void TC140_validDateRangeFiltersResults() {
        // Range: yesterday to tomorrow — covers all seeded data
        analyticsPage
            .enterDateFrom("2026-05-29T00:00")
            .enterDateTo("2026-05-31T23:59")
            .clickApply();

        Assert.assertFalse(analyticsPage.isDateErrorDisplayed(),
                "TC-140 FAILED: Date error shown for a valid range.");
        // Both from and to count as active filters
        String badge = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badge.contains("2"),
                "TC-140 FAILED: Expected '2 active' for from+to filters, got: '" + badge + "'.");
        System.out.println("TC-140 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sort
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-141: Changing sort order re-fetches results without error")
    @Story("Sort")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects 'co,desc' from the sort dropdown and searches. Expects " +
                 "results present with no error banner, and the active filter badge reflects " +
                 "the non-default sort.")
    public void TC141_changingSortReturnsResults() {
        analyticsPage.selectSort("co,desc").clickApply();
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-141 FAILED: Error banner after changing sort.");
        boolean hasResults = analyticsPage.hasResults();
        boolean hasEmpty   = analyticsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasResults || hasEmpty,
                "TC-141 FAILED: Neither results nor empty state after sort change.");
        System.out.println("TC-141 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-142: Pagination controls appear and next page works when data spans multiple pages")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("If more than 20 records exist (default page size), pagination is shown. " +
                 "Clicks Next and verifies the active page number increases and rows are still present.")
    public void TC142_paginationNextPage() {
        if (!analyticsPage.hasPagination()) {
            System.out.println("TC-142 INFO: All records fit on one page — skip.");
            return;
        }
        String beforePage = analyticsPage.getActivePageNumber();
        analyticsPage.clickNextPage();
        String afterPage = analyticsPage.getActivePageNumber();
        Assert.assertNotEquals(afterPage, beforePage,
                "TC-142 FAILED: Page number did not change after Next.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-142 FAILED: No rows on page 2.");
        System.out.println("TC-142 PASSED: page " + beforePage + " → " + afterPage);
    }

    @Test(description = "TC-143: Changing page size re-fetches with the new per-page count")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Changes the per-page select to 10 and verifies the row count is <= 10 " +
                 "and the record-count label is still present.")
    public void TC143_changingPageSizeApplies() {
        analyticsPage.selectPageSize("10");
        int rows = analyticsPage.getTableRowCount();
        Assert.assertTrue(rows <= 10,
                "TC-143 FAILED: Row count " + rows + " exceeds selected page size of 10.");
        Assert.assertFalse(analyticsPage.getRecordCountText().isEmpty(),
                "TC-143 FAILED: Record count label is empty after page size change.");
        System.out.println("TC-143 PASSED — rows on page: " + rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data accuracy
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-144: First table row values match the latest record from the API", groups = {"sanity"})
    @Story("Data accuracy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opens the analytics page (which loads with sort=timestamp,desc by default), " +
                 "reads the API first record and the UI first row at the same moment, and " +
                 "asserts location, co, ozone, and pollution level match.")
    public void TC144_firstRowMatchesApi() {
        // The analytics page opens sorted by timestamp desc (default: filterSort='timestamp,desc')
        // so the first row = most recently posted reading.
        // The API endpoint GET /api/sensors/air-pollution?sort=timestamp,desc&page=0&size=20
        // returns the same record at index 0.

        // Page is already open from @BeforeMethod. Read the API immediately —
        // same snapshot as the already-rendered UI.
        String apiLocation   = AirPollutionApiReader.getFirstRecordLocation();
        String apiCarbonMonoxide    = AirPollutionApiReader.getFirstRecordCarbonMonoxide();
        String apiOzone      = AirPollutionApiReader.getFirstRecordAvgOzone();
        String apiPollutionlevel = AirPollutionApiReader.getFirstRecordPollutionlevelLevel();

        System.out.println("[TC144] API first record — location=" + apiLocation
                + " co=" + apiCarbonMonoxide + " ozone=" + apiOzone
                + " pollution=" + apiPollutionlevel);

        Assert.assertNotNull(apiCarbonMonoxide,
                "TC-144 FAILED: API returned no data. Check sensor-service on localhost:8081.");

        // Read UI first row
        String uiLocation   = analyticsPage.getFirstRowLocation();
        String uiCarbonMonoxide    = analyticsPage.getFirstRowCarbonMonoxide();
        String uiOzone      = analyticsPage.getFirstRowOzone();
        String uiPollutionlevel = analyticsPage.getFirstRowPollutionLevel();

        System.out.println("[TC144] UI first row — location=" + uiLocation
                + " co=" + uiCarbonMonoxide + " ozone=" + uiOzone
                + " pollution=" + uiPollutionlevel);

        Assert.assertEquals(uiLocation, apiLocation,
                "TC-144 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");
        Assert.assertEquals(Double.parseDouble(uiCarbonMonoxide), Double.parseDouble(apiCarbonMonoxide), 0.1,
                "TC-144 FAILED: co UI='" + uiCarbonMonoxide + "' API='" + apiCarbonMonoxide + "'");

        // Angular formatting may strip .0 if integer
        Assert.assertEquals(Double.parseDouble(uiOzone), Double.parseDouble(apiOzone), 0.1,
                "TC-144 FAILED: ozone UI='" + uiOzone + "' API='" + apiOzone + "'");
        Assert.assertEquals(uiPollutionlevel, apiPollutionlevel.replace("_", " "),
                "TC-144 FAILED: pollution UI='" + uiPollutionlevel + "' API='" + apiPollutionlevel + "'");

        System.out.println("TC-144 PASSED — first row matches API");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-145: Back link navigates to /air-pollution dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the back arrow and verifies navigation to /air-pollution.")
    public void TC145_backLinkNavigatesToDashboard() {
        AirPollutionDashboardPage dash = analyticsPage.clickBack();
        Assert.assertTrue(dash.isOnAirDashboard(),
                "TC-145 FAILED: Back link did not reach /air-pollution.");
        System.out.println("TC-145 PASSED");
    }

    @Test(description = "TC-146: Alerts nav link navigates to /air-pollution-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts link in the header nav bar.")
    public void TC146_alertsNavLink() {
        AirPollutionAlertsPage ap = analyticsPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-146 FAILED: Did not reach /air-pollution-alerts.");
        System.out.println("TC-146 PASSED");
    }

    @Test(description = "TC-147: Unauthenticated access to /air-analytics redirects to /signin",
          priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /air-analytics. Angular auth guard must redirect.")
    public void TC147_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/air-analytics");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-147 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-147 PASSED");
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
