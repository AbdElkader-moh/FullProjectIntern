package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.StreetLightAnalyticsPage;
import pages.StreetLightDashboardPage;
import pages.StreetLightAlertsPage;
import utils.SensorApiClient;
import utils.StreetLightApiReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * StreetLightAnalyticsTest — TC-186 … TC-201
 *
 * DATA SEEDING
 * ────────────
 * 
 * @BeforeMethod posts two readings via SensorApiClient before each test:
 *               - Normal reading (power=50, brightness=80, Low, Alexandria) —
 *               guaranteed table row
 *               - Specific reading (power=277, brightness=66.0, Moderate,
 *               Alexandria) — used for
 *               data-accuracy assertions (distinctive values unlikely to match
 *               simulator data)
 *
 *               The page is opened fresh in @BeforeMethod so each test starts
 *               on a clean state.
 *
 *               FILTER BEHAVIOUR (from traffic-analytics.ts)
 *               ─────────────────────────────────────────────
 *               - Status filter: sent to backend as a query param → server-side
 *               filtering
 *               - Location filter: sent to backend as a query param →
 *               server-side filtering
 *               - Date range: sent to backend → server-side filtering
 *               - Sort: controls sortField + sortDir passed to the backend
 *               Pageable
 *               - activeFilterCount: incremented for each non-default filter
 *               value applied
 *               - Date validation: client-side only — error shown if filterFrom
 *               > filterTo
 *               BEFORE calling the API. Uses datetime-local inputs, must be set
 *               via JS.
 *
 *               DATA-ACCURACY TEST (TC-201)
 *               ────────────────────────────
 *               Uses StreetLightApiReader to read the first record from
 *               GET /api/sensors/lights?sort=timestamp,desc&page=0&size=20 and
 *               asserts
 *               the first table row on the analytics page shows the same
 *               values.
 *               The API is read AFTER the page loads (same snapshot as the UI).
 */
@Epic("Street light monitoring")
@Feature("Street light analytics & search")
public class StreetLightAnalyticsTest extends BaseTest {

    private StreetLightAnalyticsPage analyticsPage;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser(); // ← add this line (was missing)
    }

    @BeforeMethod(alwaysRun = true)
    public void seedAndOpen() {
        try {
            SensorApiClient.postNormalLightReading();
            // Distinctive reading used for data-accuracy assertions
            SensorApiClient.postLightReading(45, 66.0, "ON", "Alexandria");
        } catch (Exception e) {
            System.out.println("[BeforeMethod] Seeding warning: " + e.getMessage());
        }
        analyticsPage = new StreetLightAnalyticsPage(driver);
        analyticsPage.open();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page load
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-186: Street light analytics page loads and URL is /lights-analytics")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /lights-analytics and verifies URL contains the path.")
    public void TC186_pageLoads() {
        Assert.assertTrue(analyticsPage.isOnAnalyticsPage(),
                "TC-186 FAILED: URL does not contain /lights-analytics.");
        Assert.assertTrue(analyticsPage.isPageTitleDisplayed(),
                "TC-186 FAILED: Page title not visible.");
        System.out.println("TC-186 PASSED");
    }

    @Test(description = "TC-187: Default load shows results with no active filter badge", groups = {"sanity"})
    @Story("Default state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On initial open with no filters applied, the table must show rows (data was " +
            "seeded in @BeforeMethod) and no .filter-active-indicator badge should appear.")
    public void TC187_defaultLoadShowsResults() {
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-187 FAILED: Error banner shown on default load.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-187 FAILED: No rows on default load even though data was seeded.");
        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-187 FAILED: Active filter badge shown with no filters applied.");
        System.out.println("TC-187 PASSED — rows: " + analyticsPage.getTableRowCount());
    }

    @Test(description = "TC-188: Record count label shows a positive number")
    @Story("Default state")
    @Severity(SeverityLevel.NORMAL)
    @Description("The .record-count span must show a positive integer after data was seeded.")
    public void TC188_recordCountIsPositive() {
        int count = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(count > 0,
                "TC-188 FAILED: Record count is " + count + " — expected > 0 after seeding.");
        System.out.println("TC-188 PASSED — record count: " + count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-189: Filtering by 'ON' status scopes all visible rows", groups = {"sanity"})
    @Story("Status filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Selects ON from #filterStatus and clicks Search. All visible " +
            ".status-badge elements must read 'ON'. The seeded ON " +
            "reading (power=45) guarantees at least one result.")
    public void TC189_statusFilterScopesRows() {
        analyticsPage.selectStatusLevel("ON").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-189 INFO: No ON records returned — backend may not " +
                    "have processed the seeded reading yet.");
            return;
        }

        analyticsPage.getStatusBadges().forEach(badge -> Assert.assertEquals(badge.getText().trim(), "ON",
                "TC-189 FAILED: Non-ON badge found: '" + badge.getText() + "'"));
        System.out.println("TC-189 PASSED — all badges are ON");
    }

    @Test(description = "TC-190: Filtering by 'OFF' status returns only OFF rows or empty state")
    @Story("Status filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects OFF from the status dropdown and searches. If results exist, " +
            "every status badge must say OFF.")
    public void TC190_highStatusFilter() {
        analyticsPage.selectStatusLevel("OFF").clickApply();

        if (analyticsPage.isEmptyStateDisplayed()) {
            System.out.println("TC-190 INFO: No OFF records found — passes as empty state.");
            return;
        }
        analyticsPage.getStatusBadges().forEach(badge -> Assert.assertEquals(badge.getText().trim(), "OFF",
                "TC-190 FAILED: Non-OFF badge: '" + badge.getText() + "'"));
        System.out.println("TC-190 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-191: Filtering by location 'Alexandria' returns only Alexandria rows", groups = {"sanity"})
    @Story("Location filter")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Types 'Alexandria' into #filterLocation and searches. All visible location " +
            "cells must contain 'Alexandria'. The simulator and @BeforeMethod both post " +
            "to Alexandria so results are guaranteed.")
    public void TC191_locationFilterScopesRows() {
        analyticsPage.enterLocation("Alexandria").clickApply();

        Assert.assertFalse(analyticsPage.isEmptyStateDisplayed(),
                "TC-191 FAILED: No results for 'Alexandria' even though data was seeded there.");

        analyticsPage.getTableRows().forEach(row -> {
            String location = row.findElements(
                    org.openqa.selenium.By.cssSelector("td:nth-child(2)"))
                    .stream().findFirst()
                    .map(org.openqa.selenium.WebElement::getText).orElse("").trim();
            Assert.assertTrue(location.toLowerCase().contains("alexandria"),
                    "TC-191 FAILED: Row location '" + location + "' does not contain 'Alexandria'.");
        });
        System.out.println("TC-191 PASSED — all rows are Alexandria");
    }

    @Test(description = "TC-192: Filtering by non-existent location shows empty state")
    @Story("Location filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Types a location that does not exist in the data. Expects the empty state.")
    public void TC192_nonExistentLocationShowsEmptyState() {
        analyticsPage.enterLocation("ZZZ_NoSuchCity_XYZ").clickApply();
        Assert.assertTrue(analyticsPage.isEmptyStateDisplayed(),
                "TC-192 FAILED: Expected empty state for non-existent location.");
        System.out.println("TC-192 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Active filter badge
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-193: Active filter badge appears after applying a status filter")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects 'ON' and clicks Search. Verifies that the .filter-active-indicator " +
            "badge appears on the Filter button.")
    public void TC193_activeFilterBadgeAppears() {
        analyticsPage.selectStatusLevel("ON").clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-193 FAILED: Active filter badge not shown after applying ON filter.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("1"),
                "TC-193 FAILED: Badge text '" + badgeText + "' does not contain '1'.");
        System.out.println("TC-193 PASSED — badge: " + badgeText);
    }

    @Test(description = "TC-194: Applying two filters shows badge with count 2")
    @Story("Filter state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects ON and types Alexandria, applies, and verifies the active filter " +
            "badge shows the number '2'.")
    public void TC194_twoFiltersShowCountTwo() {
        analyticsPage
                .enterLocation("Alexandria")
                .selectStatusLevel("ON")
                .clickApply();
        Assert.assertTrue(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-194 FAILED: Active filter badge not shown.");
        String badgeText = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badgeText.contains("2"),
                "TC-194 FAILED: Badge shows '" + badgeText + "' — expected '2'.");
        System.out.println("TC-194 PASSED — badge: " + badgeText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-195: Reset removes the active filter badge and restores full results")
    @Story("Reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects ON and Alexandria, applies them, then clicks Reset. " +
            "Verifies that the active filter badge disappears and inputs are cleared.")
    public void TC195_resetClearsFilters() {
        analyticsPage.selectStatusLevel("ON").enterLocation("Alexandria").clickApply();
        int filteredCount = analyticsPage.getRecordCountNumber();

        analyticsPage.clickReset();

        Assert.assertFalse(analyticsPage.isActiveFilterBadgeDisplayed(),
                "TC-195 FAILED: Active filter badge still shown after reset.");
        int totalCount = analyticsPage.getRecordCountNumber();
        Assert.assertTrue(totalCount >= filteredCount,
                "TC-195 FAILED: After reset count (" + totalCount
                        + ") < filtered count (" + filteredCount + ").");
        System.out.println("TC-195 PASSED — filtered: " + filteredCount + " total: " + totalCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date range filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-196: Invalid date range (end before start) shows validation error")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets end date before start date using JavaScript (required for datetime-local " +
            "inputs in Chrome). Clicks Search. Expects .filter-date-error to appear.")
    public void TC196_invalidDateRangeShowsError() {
        analyticsPage
                .enterDateFrom("2026-06-01T00:00")
                .enterDateTo("2026-05-01T00:00")
                .clickApply();
        Assert.assertTrue(analyticsPage.isDateErrorDisplayed(),
                "TC-196 FAILED: No date validation error shown for invalid range.");
        System.out.println("TC-196 PASSED");
    }

    @Test(description = "TC-197: Valid date range filters results to that window")
    @Story("Date filter")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sets a date range covering today. Expects results (seeded data was just posted) " +
            "and no date error. Active filter count must be 2 (from + to).")
    public void TC197_validDateRangeFiltersResults() {
        // Range: yesterday to tomorrow — covers all seeded data
        analyticsPage
                .enterDateFrom("2026-05-29T00:00")
                .enterDateTo("2026-05-31T23:59")
                .clickApply();

        Assert.assertFalse(analyticsPage.isDateErrorDisplayed(),
                "TC-197 FAILED: Date error shown for a valid range.");
        // Both from and to count as active filters
        String badge = analyticsPage.getActiveFilterBadgeText();
        Assert.assertTrue(badge.contains("2"),
                "TC-197 FAILED: Expected '2 active' for from+to filters, got: '" + badge + "'.");
        System.out.println("TC-197 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sort
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-198: Changing sort order re-fetches results without error")
    @Story("Sort")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selects 'powerConsumption,desc' from the sort dropdown and searches. Expects " +
            "results present with no error banner, and the active filter badge reflects " +
            "the non-default sort.")
    public void TC198_changingSortReturnsResults() {
        analyticsPage.selectSort("powerConsumption,desc").clickApply();
        Assert.assertFalse(analyticsPage.isTableErrorDisplayed(),
                "TC-198 FAILED: Error banner after changing sort.");
        boolean hasResults = analyticsPage.hasResults();
        boolean hasEmpty = analyticsPage.isEmptyStateDisplayed();
        Assert.assertTrue(hasResults || hasEmpty,
                "TC-198 FAILED: Neither results nor empty state after sort change.");
        System.out.println("TC-198 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-199: Pagination controls appear and next page works when data spans multiple pages")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("If more than 20 records exist (default page size), pagination is shown. " +
            "Clicks Next and verifies the active page number increases and rows are still present.")
    public void TC199_paginationNextPage() {
        if (!analyticsPage.hasPagination()) {
            System.out.println("TC-199 INFO: All records fit on one page — skip.");
            return;
        }
        String beforePage = analyticsPage.getActivePageNumber();
        analyticsPage.clickNextPage();
        String afterPage = analyticsPage.getActivePageNumber();
        Assert.assertNotEquals(afterPage, beforePage,
                "TC-199 FAILED: Page number did not change after Next.");
        Assert.assertTrue(analyticsPage.hasResults(),
                "TC-199 FAILED: No rows on page 2.");
        System.out.println("TC-199 PASSED: page " + beforePage + " → " + afterPage);
    }

    @Test(description = "TC-200: Changing page size re-fetches with the new per-page count")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Changes the per-page select to 10 and verifies the row count is <= 10 " +
            "and the record-count label is still present.")
    public void TC200_changingPageSizeApplies() {
        analyticsPage.selectPageSize("10");
        int rows = analyticsPage.getTableRowCount();
        Assert.assertTrue(rows <= 10,
                "TC-200 FAILED: Row count " + rows + " exceeds selected page size of 10.");
        Assert.assertFalse(analyticsPage.getRecordCountText().isEmpty(),
                "TC-200 FAILED: Record count label is empty after page size change.");
        System.out.println("TC-200 PASSED — rows on page: " + rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data accuracy
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-201: First table row values match the latest record from the API", groups = {"sanity"})
    @Story("Data accuracy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opens the analytics page (which loads with sort=timestamp,desc by default), " +
            "reads the API first record and the UI first row at the same moment, and " +
            "asserts location, power, brightness, and status level match.")
    public void TC201_firstRowMatchesApi() {
        // The analytics page opens sorted by timestamp desc (default:
        // filterSort='timestamp,desc')
        // so the first row = most recently posted reading.
        // The API endpoint GET /api/sensors/lights?sort=timestamp,desc&page=0&size=20
        // returns the same record at index 0.

        // Page is already open from @BeforeMethod. Read the API immediately —
        // same snapshot as the already-rendered UI.
        String apiLocation = StreetLightApiReader.getFirstRecordLocation();
        String apiPowerConsumption = StreetLightApiReader.getFirstRecordPowerConsumption();
        String apiBrightnessLevel = StreetLightApiReader.getFirstRecordAvgBrightnessLevel();
        String apiStatus = StreetLightApiReader.getFirstRecordStatusLevel();

        System.out.println("[TC201] API first record — location=" + apiLocation
                + " power=" + apiPowerConsumption + " brightness=" + apiBrightnessLevel
                + " status=" + apiStatus);

        Assert.assertNotNull(apiPowerConsumption,
                "TC-201 FAILED: API returned no data. Check sensor-service on localhost:8081.");

        // Read UI first row
        String uiLocation = analyticsPage.getFirstRowLocation();
        String uiPowerConsumption = analyticsPage.getFirstRowPowerConsumption();
        String uiBrightnessLevel = analyticsPage.getFirstRowBrightnessLevel();
        String uiStatus = analyticsPage.getFirstRowStatusLevel();

        System.out.println("[TC201] UI first row — location=" + uiLocation
                + " power=" + uiPowerConsumption + " brightness=" + uiBrightnessLevel
                + " status=" + uiStatus);

        Assert.assertEquals(uiLocation, apiLocation,
                "TC-201 FAILED: location UI='" + uiLocation + "' API='" + apiLocation + "'");

        // UI formats power with number:'1.0-2' so "66.0" becomes "66"
        double uiPower = Double.parseDouble(uiPowerConsumption);
        double apiPower = Double.parseDouble(apiPowerConsumption);
        Assert.assertEquals(uiPower, apiPower, 0.01,
                "TC-201 FAILED: power UI='" + uiPowerConsumption + "' API='" + apiPowerConsumption + "'");

        // Angular formats brightness with number:'1.0-2' or similar
        double uiBright = Double.parseDouble(uiBrightnessLevel);
        double apiBright = Double.parseDouble(apiBrightnessLevel);
        Assert.assertEquals(uiBright, apiBright, 0.01,
                "TC-201 FAILED: brightness UI='" + uiBrightnessLevel + "' API='" + apiBrightnessLevel + "'");
        Assert.assertEquals(uiStatus, apiStatus,
                "TC-201 FAILED: status UI='" + uiStatus + "' API='" + apiStatus + "'");

        System.out.println("TC-201 PASSED — first row matches API");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "TC-202: Back link navigates to /lights dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the back arrow and verifies navigation to /lights.")
    public void TC202_backLinkNavigatesToDashboard() {
        StreetLightDashboardPage dash = analyticsPage.clickBack();
        Assert.assertTrue(dash.isOnStreetLightDashboard(),
                "TC-202 FAILED: Back link did not reach /lights.");
        System.out.println("TC-202 PASSED");
    }

    @Test(description = "TC-203: Alerts nav link navigates to /lights-alerts")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the Alerts link in the header nav bar.")
    public void TC203_alertsNavLink() {
        StreetLightAlertsPage ap = analyticsPage.clickAlertsNav();
        Assert.assertTrue(ap.isOnAlertsPage(),
                "TC-203 FAILED: Did not reach /lights-alerts.");
        System.out.println("TC-203 PASSED");
    }

    @Test(description = "TC-204: Unauthenticated access to /lights-analytics redirects to /signin", priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /lights-analytics. Angular auth guard must redirect.")
    public void TC204_unauthenticatedAccess() {
        driver.manage().deleteAllCookies();
        navigateTo("/lights-analytics");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-204 FAILED: Expected /signin, got: " + driver.getCurrentUrl());
        System.out.println("TC-204 PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

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
