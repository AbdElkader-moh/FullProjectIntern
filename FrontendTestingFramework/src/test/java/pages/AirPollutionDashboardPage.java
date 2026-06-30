package pages;

import io.qameta.allure.Step;
import utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import java.util.List;

/**
 * AirPollutionDashboardPage — Page Object for /air
 *
 * This file REPLACES the previous AirPollutionDashboardPage.java entirely.
 * New additions (marked NEW) support the three data-accuracy tests:
 *
 * TC-081: Table row values match the API
 * TC-082: CarbonMonoxide line chart dots match the trends API
 * TC-083: Pollutionlevel distribution bars match the pollution-summary API
 *
 * DOM facts from traffic-dashboard.html:
 * ── Table ──
 * First row cells: td.col-location, td.col-co .co-value,
 * td.col-ozone .ozone-value, td .pollution-badge
 * ── Charts ──
 * CarbonMonoxide dots: circle.co-dot (each has a <title> child = "HH:MM: 450
 * veh/hr")
 * Ozone bars: rect.ozone-bar (each has a <title> child = "HH:MM: 15.0 km/h")
 * Pollutionlevel rows: .pollution-row (each has .pollution-level-name +
 * .pollution-count)
 */
public class AirPollutionDashboardPage extends BasePage {

    // ── Header nav ────────────────────────────────────────────────────────────
    private static final By BRAND_LINK = By.cssSelector("a.brand-link");
    private static final By NAV_HOME = By.xpath("//a[@href='/home' and contains(@class,'nav-link')]");
    private static final By NAV_ANALYTICS = By.cssSelector(".analytics-btn");
    private static final By NAV_ALERTS = By.cssSelector(".alerts-btn");
    private static final By NAV_SETTINGS = By.xpath("//a[@href='/settings' and contains(@class,'nav-link')]");
    private static final By NAV_NOTIFICATIONS = By
            .xpath("//a[@href='/notifications' and contains(@class,'nav-link')]");
    private static final By LOGOUT_BTN = By.cssSelector("button.logout-btn");
    private static final By NOTIF_BADGE = By.cssSelector(".notification-link .badge");

    // ── Page header ───────────────────────────────────────────────────────────
    private static final By PAGE_TITLE = By.cssSelector(".page-title");
    private static final By BACK_LINK = By.cssSelector("a.back-link");

    // ── Refresh controls ──────────────────────────────────────────────────────
    private static final By MANUAL_REFRESH_BTN = By.cssSelector("button.btn-icon");
    private static final By AUTO_REFRESH_BTN = By.cssSelector("button.btn-auto-refresh");
    private static final By LAST_REFRESHED = By.cssSelector(".last-refreshed:not(.syncing)");
    private static final By SYNCING_INDICATOR = By.cssSelector(".last-refreshed.syncing");

    // ── Quick actions ─────────────────────────────────────────────────────────
    private static final By QUICK_ACTION_ANALYTICS = By
            .xpath("//a[contains(@class,'dashboard-action-btn') and @href='/air-analytics']");
    private static final By QUICK_ACTION_ALERTS = By
            .xpath("//a[contains(@class,'dashboard-action-btn') and @href='/air-alerts']");

    // ── Stats ─────────────────────────────────────────────────────────────────
    private static final By STATS_SECTION = By.cssSelector(".stats-section");
    private static final By STAT_CARDS = By.cssSelector(".stat-card:not(.skeleton)");
    private static final By STAT_VALUES = By.cssSelector(".stat-card:not(.skeleton) .stat-value");
    private static final By STATS_ERROR = By.cssSelector(".error-banner");
    private static final By STATS_SKELETON = By.cssSelector(".stat-card.skeleton");

    // ── Table ─────────────────────────────────────────────────────────────────
    private static final By TABLE_SECTION = By.cssSelector(".table-section");
    private static final By TABLE_ROWS = By.cssSelector(".data-table tbody tr");
    private static final By TABLE_LOADING = By.cssSelector(".table-loading");
    private static final By TABLE_EMPTY = By.cssSelector(".empty-state");
    private static final By TABLE_ERROR = By.cssSelector(".table-section .error-banner");
    private static final By PAGINATION = By.cssSelector(".pagination");
    private static final By ACTIVE_PAGE_BTN = By.cssSelector(".pagination-controls .page-btn.active");
    private static final By NEXT_PAGE_BTN = By
            .xpath("//div[contains(@class,'pagination-controls')]//button[contains(text(),'›')]");

    // ── NEW: First table row cell selectors ───────────────────────────────────
    /** Location cell of the first data row */
    private static final By FIRST_ROW_LOCATION = By.cssSelector(".data-table tbody tr:first-child td:nth-child(2)");
    /** CarbonMonoxide value span of the first row */
    private static final By FIRST_ROW_CO = By.cssSelector(".data-table tbody tr:first-child td:nth-child(3)");
    /** Ozone value span of the first row */
    private static final By FIRST_ROW_OZONE = By.cssSelector(".data-table tbody tr:first-child td:nth-child(4)");
    /** Pollutionlevel badge of the first row */
    private static final By FIRST_ROW_POLLUTION = By
            .cssSelector(".data-table tbody tr:first-child td:nth-child(9) .congestion-badge");

    // ── Charts ────────────────────────────────────────────────────────────────
    private static final By CHARTS_SECTION = By.cssSelector(".charts-section");
    private static final By CHART_CARDS = By.cssSelector(".chart-card");
    private static final By CONGESTION_CHART = By.cssSelector(".pollution-chart");
    private static final By CONGESTION_ROWS = By.cssSelector(".pollution-row");
    private static final By CHART_ERROR = By.cssSelector(".chart-empty.chart-error");

    // ── NEW: Chart element selectors ──────────────────────────────────────────
    /**
     * SVG density dots — each carries a <title> tooltip like "17:44: 450 veh/hr"
     */
    private static final By DENSITY_DOTS = By.cssSelector("circle.density-dot");
    /** SVG speed bars — each carries a <title> tooltip like "17:44: 15.0 km/h" */
    private static final By SPEED_BARS = By.cssSelector("rect.speed-bar");
    /**
     * Pollutionlevel count cells inside each row.
     * DOM: .pollution-row > .pollution-count (text = "3")
     */
    private static final By CONGESTION_COUNTS = By.cssSelector(".pollution-row .pollution-count");
    /**
     * Pollutionlevel level name labels.
     * DOM: .pollution-row > .pollution-label-col > .pollution-level-name (text =
     * "High")
     */
    private static final By CONGESTION_LEVEL_NAMES = By.cssSelector(".pollution-row .pollution-level-name");

    // ── Recent alerts ─────────────────────────────────────────────────────────
    private static final By ALERTS_SECTION = By.cssSelector(".alerts-section");
    private static final By ALERT_ITEMS = By.cssSelector(".alert-item");
    private static final By ALERTS_EMPTY = By.cssSelector(".alerts-empty");
    private static final By ALERTS_ERROR = By.cssSelector(".alerts-section .error-banner");
    private static final By VIEW_ALL_ALERTS = By.cssSelector("a.view-all-link");

    // ── Constructor ───────────────────────────────────────────────────────────

    public AirPollutionDashboardPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Step("Open traffic dashboard page")
    public AirPollutionDashboardPage open() {
        navigateTo("/air");
        wait.waitForUrlToContain("/air");
        waitForInitialLoad();
        return this;
    }

    private void waitForInitialLoad() {
        wait.waitForPresence(PAGE_TITLE);
        try {
            wait.waitForInvisibility(TABLE_LOADING);
        } catch (Exception ignored) {
        }
        try {
            wait.waitForInvisibility(By.cssSelector(".spinner"));
        } catch (Exception ignored) {
        }
    }

    @Step("Click Back link to /home")
    public HomePage clickBackLink() {
        wait.waitForClickable(BACK_LINK).click();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    @Step("Click Analytics nav link")
    public AirPollutionAnalyticsPage clickAnalyticsNav() {
        wait.waitForClickable(NAV_ANALYTICS).click();
        wait.waitForUrlToContain("/air-analytics");
        return new AirPollutionAnalyticsPage(driver);
    }

    @Step("Click Alerts nav link")
    public AirPollutionAlertsPage clickAlertsNav() {
        wait.waitForClickable(NAV_ALERTS).click();
        wait.waitForUrlToContain("/air-alerts");
        return new AirPollutionAlertsPage(driver);
    }

    @Step("Click Home nav link")
    public HomePage clickHomeNav() {
        wait.waitForClickable(NAV_HOME).click();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    @Step("Click Analytics & Search quick action")
    public AirPollutionAnalyticsPage clickQuickActionAnalytics() {
        wait.waitForClickable(QUICK_ACTION_ANALYTICS).click();
        wait.waitForUrlToContain("/air-analytics");
        return new AirPollutionAnalyticsPage(driver);
    }

    @Step("Click Traffic Alerts quick action")
    public AirPollutionAlertsPage clickQuickActionAlerts() {
        wait.waitForClickable(QUICK_ACTION_ALERTS).click();
        wait.waitForUrlToContain("/air-alerts");
        return new AirPollutionAlertsPage(driver);
    }

    @Step("Click View All Alerts link")
    public void clickViewAllAlerts() {
        wait.waitForClickable(VIEW_ALL_ALERTS).click();
    }

    @Step("Click manual refresh button")
    public AirPollutionDashboardPage clickManualRefresh() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(MANUAL_REFRESH_BTN).click(), "manual refresh");
        return this;
    }

    @Step("Click auto-refresh toggle")
    public AirPollutionDashboardPage clickAutoRefreshToggle() {
        boolean wasActive = isAutoRefreshActive();
        wait.waitForClickable(AUTO_REFRESH_BTN).click();
        wait.waitForCondition(d -> {
            try {
                String cls = d.findElement(AUTO_REFRESH_BTN).getAttribute("class");
                boolean isActive = cls != null && cls.contains("active");
                return isActive != wasActive;
            } catch (Exception e) { return false; }
        });
        return this;
    }

    @Step("Click Next page")
    public AirPollutionDashboardPage clickNextPage() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(NEXT_PAGE_BTN).click(), "next page");
        try {
            wait.waitForInvisibility(TABLE_LOADING);
        } catch (Exception ignored) {
        }
        return this;
    }

    @Step("Click logout")
    public void logout() {
        wait.waitForClickable(LOGOUT_BTN).click();
        wait.waitForUrlToContain("/signin");
    }

    // ── General state checks ──────────────────────────────────────────────────

    public boolean isOnAirDashboard() {
        return urlContains("/air") && !urlContains("/air-");
    }

    public boolean isPageTitleDisplayed() {
        return isDisplayed(PAGE_TITLE);
    }

    public boolean isStatsSectionDisplayed() {
        return isDisplayed(STATS_SECTION);
    }

    public boolean isChartsSectionDisplayed() {
        return isDisplayed(CHARTS_SECTION);
    }

    public boolean isAlertsSectionDisplayed() {
        return isDisplayed(ALERTS_SECTION);
    }

    public boolean isTableSectionDisplayed() {
        return isDisplayed(TABLE_SECTION);
    }

    public boolean isLastRefreshedDisplayed() {
        return isDisplayed(LAST_REFRESHED);
    }

    public boolean isStatsErrorDisplayed() {
        return isDisplayed(STATS_ERROR);
    }

    public boolean isAlertsErrorDisplayed() {
        return isDisplayed(ALERTS_ERROR);
    }

    public boolean isAutoRefreshActive() {
        try {
            String cls = wait.waitForVisible(AUTO_REFRESH_BTN).getAttribute("class");
            return cls != null && cls.contains("active");
        } catch (Exception e) {
            return false;
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public List<WebElement> getStatCards() {
        return driver.findElements(STAT_CARDS);
    }

    public int getStatCardCount() {
        return getStatCards().size();
    }

    public boolean areStatsLoaded() {
        return !getStatCards().isEmpty() && driver.findElements(STATS_SKELETON).isEmpty();
    }

    public String getStatValue(int index) {
        List<WebElement> values = driver.findElements(STAT_VALUES);
        return index < values.size() ? values.get(index).getText().trim() : "";
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    public List<WebElement> getTableRows() {
        return driver.findElements(TABLE_ROWS);
    }

    public int getTableRowCount() {
        return getTableRows().size();
    }

    public boolean isTableEmpty() {
        return isDisplayed(TABLE_EMPTY);
    }

    public boolean isTableErrorDisplayed() {
        return isDisplayed(TABLE_ERROR);
    }

    public boolean hasPagination() {
        return isDisplayed(PAGINATION);
    }

    public boolean isNextPageEnabled() {
        try {
            WebElement btn = driver.findElement(NEXT_PAGE_BTN);
            return btn.isEnabled() && btn.getAttribute("disabled") == null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getActivePageNumber() {
        try {
            return wait.waitForVisible(ACTIVE_PAGE_BTN).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ── NEW: First table row cell readers ─────────────────────────────────────

    /**
     * Returns the location text of the first data row.
     * Waits until at least one row is present before reading.
     */
    @Step("Read first table row location")
    public String getFirstRowLocation() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_LOCATION).getText().trim();
    }

    /**
     * Returns the traffic co value from the first row as displayed.
     * The span contains only the number (e.g. "450"), not the "veh/hr" unit.
     */
    @Step("Read first table row CO value")
    public String getFirstRowCarbonMonoxide() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_CO).getText().trim().replaceAll("[^0-9.]", "");
    }

    /**
     * Returns the average ozone value from the first row as displayed.
     * Angular renders this with number:'1.1-1' pipe, so "15.0" not "15".
     */
    @Step("Read first table row ozone value")
    public String getFirstRowOzone() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_OZONE).getText().trim().replaceAll("[^0-9.]", "");
    }

    /**
     * Returns the pollution level badge text from the first row.
     * One of: Low, Moderate, High, Severe.
     */
    @Step("Read first table row pollution level")
    public String getFirstRowPollutionLevel() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_POLLUTION).getText().trim();
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    public List<WebElement> getChartCards() {
        return driver.findElements(CHART_CARDS);
    }

    public int getChartCardCount() {
        return getChartCards().size();
    }

    public boolean isPollutionlevelChartDisplayed() {
        return isDisplayed(CONGESTION_CHART);
    }

    public boolean isChartErrorDisplayed() {
        return isDisplayed(CHART_ERROR);
    }

    // ── NEW: CarbonMonoxide line chart readers
    // ───────────────────────────────────────

    /**
     * Returns all rendered co-dot elements.
     * Count should match the number of trend data points returned by the API.
     */
    public List<WebElement> getCarbonMonoxideDots() {
        return driver.findElements(DENSITY_DOTS);
    }

    public int getCarbonMonoxideDotCount() {
        return getCarbonMonoxideDots().size();
    }

    /**
     * Returns the tooltip text of the last co dot (rightmost on the chart).
     * The component reverses the API array, so the last dot = the latest reading.
     * Format: "HH:MM: 450 veh/hr"
     */
    @Step("Read last co dot tooltip")
    public String getLastCarbonMonoxideDotTooltip() {
        List<WebElement> dots = getCarbonMonoxideDots();
        if (dots.isEmpty())
            return "";
        WebElement lastDot = dots.get(dots.size() - 1);
        try {
            // Read the <title> child's textContent directly via JS
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript(
                            "var t = arguments[0].querySelector('title');" +
                                    "return t ? t.textContent : '';",
                            lastDot);
            return result != null ? result.toString().trim() : "";
        } catch (Exception e) {
            System.out.println("[AirPollutionDashboardPage] Could not read co dot tooltip: "
                    + e.getMessage());
            return "";
        }
    }

    /**
     * Returns the co number from the last dot's tooltip.
     * Tooltip format: "HH:MM: 450 veh/hr" → extracts "450"
     */
    public String getLastCarbonMonoxideDotValue() {
        String tooltip = getLastCarbonMonoxideDotTooltip();
        if (tooltip.isEmpty())
            return "";
        // Format: "17:44: 450 veh/hr" — split on ": " and take second part
        String[] parts = tooltip.split(": ");
        if (parts.length < 2)
            return "";
        // Second part: "450 veh/hr" — take the number before the space
        return parts[1].split(" ")[0].trim();
    }

    // ── NEW: Average ozone bar chart readers ──────────────────────────────────

    /**
     * Returns all rendered ozone-bar rect elements.
     */
    public List<WebElement> getOzoneBars() {
        return driver.findElements(SPEED_BARS);
    }

    public int getOzoneBarCount() {
        return getOzoneBars().size();
    }

    /**
     * Returns the tooltip text of the last ozone bar (rightmost = latest reading).
     * Format: "HH:MM: 15.0 km/h"
     */
    @Step("Read last ozone bar tooltip")
    public String getLastOzoneBarTooltip() {
        List<WebElement> bars = getOzoneBars();
        if (bars.isEmpty())
            return "";
        WebElement lastBar = bars.get(bars.size() - 1);
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript(
                            "var t = arguments[0].querySelector('title');" +
                                    "return t ? t.textContent : '';",
                            lastBar);
            return result != null ? result.toString().trim() : "";
        } catch (Exception e) {
            System.out.println("[AirPollutionDashboardPage] Could not read ozone bar tooltip: "
                    + e.getMessage());
            return "";
        }
    }

    /**
     * Returns the ozone number from the last bar's tooltip.
     * Format "17:44: 15.0 km/h" → "15.0"
     */
    public String getLastOzoneBarValue() {
        String tooltip = getLastOzoneBarTooltip();
        if (tooltip.isEmpty())
            return "";
        String[] parts = tooltip.split(": ");
        if (parts.length < 2)
            return "";
        return parts[1].split(" ")[0].trim();
    }

    // ── NEW: Pollutionlevel distribution chart readers
    // ────────────────────────────

    public List<WebElement> getPollutionlevelRows() {
        return driver.findElements(CONGESTION_ROWS);
    }

    public int getPollutionlevelRowCount() {
        return getPollutionlevelRows().size();
    }

    /**
     * Returns the count text for a specific pollution level row.
     * 
     * @param level "Low" | "Moderate" | "High" | "Severe"
     * @return the count string (e.g. "3"), or "" if the level row is not found
     */
    @Step("Read pollution count for level: {level}")
    public String getPollutionlevelCountForLevel(String level) {
        List<WebElement> names = driver.findElements(CONGESTION_LEVEL_NAMES);
        List<WebElement> counts = driver.findElements(CONGESTION_COUNTS);
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).getText().trim().equalsIgnoreCase(level)) {
                return i < counts.size() ? counts.get(i).getText().trim() : "";
            }
        }
        return "";
    }

    public String getFirstCarbonMonoxideDotValue() {
        String tooltip = getFirstCarbonMonoxideDotTooltip();
        if (tooltip.isEmpty())
            return "";
        String[] parts = tooltip.split(": ");
        if (parts.length < 2)
            return "";
        return parts[1].split(" ")[0].trim();
    }

    public String getFirstCarbonMonoxideDotTooltip() {
        List<WebElement> dots = getCarbonMonoxideDots();
        if (dots.isEmpty())
            return "";
        WebElement firstDot = dots.get(0); // index 0 = leftmost = newest
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript(
                            "var t = arguments[0].querySelector('title');" +
                                    "return t ? t.textContent : '';",
                            firstDot);
            return result != null ? result.toString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the ozone value from the FIRST (leftmost) ozone bar = newest reading.
     * Tooltip format: "HH:MM: 88.8 km/h" → extracts "88.8"
     */
    public String getFirstOzoneBarValue() {
        String tooltip = getFirstOzoneBarTooltip();
        if (tooltip.isEmpty())
            return "";
        String[] parts = tooltip.split(": ");
        if (parts.length < 2)
            return "";
        return parts[1].split(" ")[0].trim();
    }

    public String getFirstOzoneBarTooltip() {
        List<WebElement> bars = getOzoneBars();
        if (bars.isEmpty())
            return "";
        WebElement firstBar = bars.get(0); // index 0 = leftmost = newest
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript(
                            "var t = arguments[0].querySelector('title');" +
                                    "return t ? t.textContent : '';",
                            firstBar);
            return result != null ? result.toString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
    // ── Recent alerts ─────────────────────────────────────────────────────────

    public List<WebElement> getAlertItems() {
        return driver.findElements(ALERT_ITEMS);
    }

    public int getAlertItemCount() {
        return getAlertItems().size();
    }

    public boolean hasAlerts() {
        return getAlertItemCount() > 0;
    }

    public boolean isAlertsEmptyStateDisplayed() {
        return isDisplayed(ALERTS_EMPTY);
    }
}
