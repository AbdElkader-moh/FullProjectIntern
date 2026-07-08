package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * AirPollutionAlertsPage — Page Object for /air-alerts
 *
 * DOM facts from UPDATED air-alerts.html + air-alerts.ts:
 *
 *  IMPORTANT CHANGES from previous version:
 *   - The "Alert Type" dropdown (Traffic/Air/Light) is GONE.
 *     The page now only shows Air pollution alerts (filtered server-side).
 *   - New filter: "Alert Classification" (#aFilterMetric) with options
 *     "Carbon Monoxide" and "Ozone" (from metricOptions array in TS).
 *   - Live banner is now a TOAST STACK (.toast-stack) not .live-alert-banner.
 *     Individual toasts: .toast, with progress bar .toast-progress.
 *     Toasts auto-dismiss after 5 s and support manual close via .toast-close.
 *   - Type badge always has class .type-air (no .type-air or .type-light).
 *
 *  Unchanged:
 *   - .page-title, .page-subtitle, .btn-mark-all
 *   - .data-table tbody tr, tr.unread-row
 *   - .severity-badge, .status-unread, .status-read
 *   - .action-btn.action-read, .action-btn.action-delete
 *   - .pagination, .page-btn
 *   - .empty-state, .error-banner, .table-loading
 *   - #aFilterFrom, #aFilterTo, #aFilterLocation, #aFilterSort
 *   - a.back-link[routerLink="/air"]
 */
public class AirPollutionAlertsPage extends BasePage {

    // ── Page header ───────────────────────────────────────────────────────────
    private static final By PAGE_TITLE         = By.cssSelector(".page-title");
    private static final By PAGE_SUBTITLE      = By.cssSelector(".page-subtitle");
    private static final By MARK_ALL_BTN       = By.cssSelector("button.btn-mark-all");
    private static final By BACK_LINK          = By.cssSelector("a.back-link");

    // ── Toast stack (replaces the old single live-alert-banner) ───────────────
    private static final By TOAST_STACK        = By.cssSelector(".toast-stack");
    private static final By TOASTS             = By.cssSelector(".toast");
    private static final By TOAST_CLOSE_BTNS   = By.cssSelector(".toast-close");

    // ── Filter panel ──────────────────────────────────────────────────────────
    private static final By FILTER_FROM        = By.id("aFilterFrom");
    private static final By FILTER_TO          = By.id("aFilterTo");
    private static final By FILTER_LOCATION    = By.id("aFilterLocation");

    /**
     * NEW: metric classification filter — replaces the old alert-type dropdown.
     * Options: "" (all), "Carbon Monoxide", "Ozone"
     */
    private static final By FILTER_METRIC      = By.id("aFilterMetric");
    private static final By FILTER_SORT        = By.id("aFilterSort");
    private static final By APPLY_BTN          = By.cssSelector("button.btn-primary");
    private static final By RESET_BTN          = By.cssSelector("button.btn-secondary");
    private static final By CLEAR_FILTERS_BTN  = By.cssSelector("button.clear-filters-btn");
    private static final By DATE_ERROR         = By.cssSelector(".filter-date-error");

    // ── Table ─────────────────────────────────────────────────────────────────
    private static final By TABLE_ROWS         = By.cssSelector(".data-table tbody tr");
    private static final By UNREAD_ROWS        = By.cssSelector(".data-table tbody tr.unread-row");
    private static final By STATUS_UNREAD      = By.cssSelector(".status-unread");
    private static final By STATUS_READ        = By.cssSelector(".status-read");
    /**
     * Type badges are now always .type-air (page is air-pollution-only).
     */
    private static final By TYPE_BADGES        = By.cssSelector(".type-badge.type-air");
    private static final By METRIC_CELLS       = By.cssSelector("td.col-metric");
    private static final By SEVERITY_BADGES    = By.cssSelector(".severity-badge");
    private static final By MARK_READ_BTNS     = By.cssSelector(".action-btn.action-read");
    private static final By DELETE_BTNS        = By.cssSelector(".action-btn.action-delete");
    private static final By TABLE_LOADING      = By.cssSelector(".table-loading");
    private static final By EMPTY_STATE        = By.cssSelector(".empty-state");
    private static final By TABLE_ERROR        = By.cssSelector(".error-banner");
    private static final By RECORD_COUNT       = By.cssSelector(".record-count");

    // ── Pagination ────────────────────────────────────────────────────────────
    private static final By PAGINATION         = By.cssSelector(".pagination");
    private static final By ACTIVE_PAGE_BTN    = By.cssSelector(".pagination-controls .page-btn.active");
    private static final By NEXT_PAGE_BTN      = By.xpath(
            "//div[contains(@class,'pagination-controls')]//button[@aria-label='Next page']");

    // ── Nav ───────────────────────────────────────────────────────────────────
    private static final By LOGOUT_BTN         = By.cssSelector("button.logout-btn");

    public AirPollutionAlertsPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Step("Open air alerts page")
    public AirPollutionAlertsPage open() {
        navigateTo("/air-alerts");
        wait.waitForUrlToContain("/air-alerts");
        wait.waitForPresence(PAGE_TITLE);
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Click Back link to air pollution dashboard")
    public AirPollutionDashboardPage clickBack() {
        wait.waitForClickable(BACK_LINK).click();
        wait.waitForUrlToContain("/air");
        return new AirPollutionDashboardPage(driver);
    }

    @Step("Click logout button")
    public void logout() {
        wait.waitForClickable(LOGOUT_BTN).click();
        wait.waitForUrlToContain("/signin");
    }

    // ── Filter actions ────────────────────────────────────────────────────────

    @Step("Select metric classification filter: {metric}")
    public AirPollutionAlertsPage selectMetricFilter(String metric) {
        new Select(wait.waitForVisible(FILTER_METRIC)).selectByVisibleText(metric);
        return this;
    }

    @Step("Select location filter: {location}")
    public AirPollutionAlertsPage selectLocation(String location) {
        new Select(wait.waitForVisible(FILTER_LOCATION)).selectByVisibleText(location);
        return this;
    }

    @Step("Select sort order: {sortValue}")
    public AirPollutionAlertsPage selectSort(String sortValue) {
        new Select(wait.waitForVisible(FILTER_SORT)).selectByValue(sortValue);
        return this;
    }

// In AirPollutionAlertsPage, replace enterDateFrom and enterDateTo with:

    @Step("Enter date from: {dateFrom}")
    public AirPollutionAlertsPage enterDateFrom(String dateFrom) {
        WebElement input = wait.waitForPresence(FILTER_FROM);
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                        input, dateFrom
                );
        return this;
    }

    @Step("Enter date to: {dateTo}")
    public AirPollutionAlertsPage enterDateTo(String dateTo) {
        WebElement input = wait.waitForPresence(FILTER_TO);
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                        input, dateTo
                );
        return this;
    }

    @Step("Click Apply Filters button")
    public AirPollutionAlertsPage clickApplyFilters() {
        wait.waitForClickable(APPLY_BTN).click();
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Click Reset filters button")
    public AirPollutionAlertsPage clickResetFilters() {
        wait.waitForClickable(RESET_BTN).click();
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    // ── Alert actions ─────────────────────────────────────────────────────────

    @Step("Click Mark All Read button")
    public AirPollutionAlertsPage clickMarkAllRead() {
        wait.waitForClickable(MARK_ALL_BTN).click();
        return this;
    }

    @Step("Click Mark Read on first unread alert")
    public AirPollutionAlertsPage clickMarkReadOnFirst() {
        List<WebElement> btns = driver.findElements(MARK_READ_BTNS);
        if (!btns.isEmpty()) {
            utils.RetryHelper.retryVoid(() -> btns.get(0).click(), "click mark read");
        }
        return this;
    }

    @Step("Click Delete on first alert")
    public AirPollutionAlertsPage clickDeleteFirst() {
        List<WebElement> btns = driver.findElements(DELETE_BTNS);
        if (!btns.isEmpty()) {
            utils.RetryHelper.retryVoid(() -> btns.get(0).click(), "click delete");
        }
        return this;
    }

    /** Dismisses the first visible toast by clicking its close button. */
    @Step("Dismiss first toast notification")
    public AirPollutionAlertsPage dismissFirstToast() {
        List<WebElement> closeBtns = driver.findElements(TOAST_CLOSE_BTNS);
        if (!closeBtns.isEmpty()) {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtns.get(0));
        }
        return this;
    }

    // ── State checks ──────────────────────────────────────────────────────────

    public boolean isOnAlertsPage()             { return urlContains("/air-alerts"); }
    public boolean isPageTitleDisplayed()       { return isDisplayed(PAGE_TITLE); }
    public boolean isMarkAllReadDisplayed()     { return isDisplayed(MARK_ALL_BTN); }
    public boolean isToastStackPresent()        { return isDisplayed(TOAST_STACK); }
    public boolean isDateErrorDisplayed()       { return isDisplayed(DATE_ERROR); }
    public boolean isEmptyStateDisplayed()      { return isDisplayed(EMPTY_STATE); }
    public boolean isTableErrorDisplayed()      { return isDisplayed(TABLE_ERROR); }
    public boolean hasPagination()              { return isDisplayed(PAGINATION); }
    public boolean isClearFiltersBtnDisplayed() { return isDisplayed(CLEAR_FILTERS_BTN); }

    public List<WebElement> getTableRows()      { return driver.findElements(TABLE_ROWS); }
    public int getTableRowCount()               { return getTableRows().size(); }
    public boolean hasAlerts()                  { return getTableRowCount() > 0; }

    public List<WebElement> getUnreadRows()     { return driver.findElements(UNREAD_ROWS); }
    public int getUnreadRowCount()              { return getUnreadRows().size(); }

    /** Returns all visible toast elements (max 5 stacked at once). */
    public List<WebElement> getToasts()         { return driver.findElements(TOASTS); }
    public int getToastCount()                  { return getToasts().size(); }
    public boolean hasActiveToasts()            { return getToastCount() > 0; }

    public List<WebElement> getTypeBadges()     { return driver.findElements(TYPE_BADGES); }
    public List<WebElement> getSeverityBadges() { return driver.findElements(SEVERITY_BADGES); }
    public List<WebElement> getMetricCells()    { return driver.findElements(METRIC_CELLS); }

    public String getPageSubtitleText() {
        try { return wait.waitForVisible(PAGE_SUBTITLE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getRecordCountText() {
        try { return wait.waitForVisible(RECORD_COUNT).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    /**
     * Waits up to timeoutSeconds for at least one alert row to appear.
     * Used after seeding data via SensorApiClient.
     */
    public boolean waitForAlertToAppear(int timeoutSeconds) {
        try {
            wait.waitForCondition(d ->
                    !d.findElements(TABLE_ROWS).isEmpty() && d.findElements(EMPTY_STATE).isEmpty()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
