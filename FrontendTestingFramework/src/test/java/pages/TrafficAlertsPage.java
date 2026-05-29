package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * TrafficAlertsPage — Page Object for /traffic-alerts
 *
 * DOM facts from traffic-alerts.html:
 *  Page title      → .page-title (h1)
 *  Subtitle        → .page-subtitle (shows count + unread)
 *  Mark all btn    → button.btn-mark-all (only rendered when unreadCount > 0)
 *  Filter inputs   → #aFilterFrom, #aFilterTo, #aFilterLocation, #aFilterType, #aFilterSort
 *  Apply button    → button.btn-primary
 *  Reset button    → button.btn-secondary
 *  Clear all badge → button.clear-filters-btn
 *  Date error      → .filter-date-error
 *  Table rows      → .data-table tbody tr
 *  Unread rows     → tr.unread-row
 *  Status cells    → .status-unread / .status-read
 *  Type badges     → .type-badge
 *  Severity badges → .severity-badge
 *  Mark read btn   → .action-btn.action-read (per row)
 *  Delete btn      → .action-btn.action-delete (per row)
 *  Live banner     → .live-alert-banner
 *  Empty state     → .empty-state
 *  Error banner    → .error-banner
 *  Pagination      → .pagination / .page-btn
 */
public class TrafficAlertsPage extends BasePage {

    // ── Page header ───────────────────────────────────────────────────────────
    private static final By PAGE_TITLE         = By.cssSelector(".page-title");
    private static final By PAGE_SUBTITLE      = By.cssSelector(".page-subtitle");
    private static final By MARK_ALL_BTN       = By.cssSelector("button.btn-mark-all");
    private static final By BACK_LINK          = By.cssSelector("a.back-link");

    // ── Live banner ───────────────────────────────────────────────────────────
    private static final By LIVE_BANNER        = By.cssSelector(".live-alert-banner");

    // ── Filter panel ──────────────────────────────────────────────────────────
    private static final By FILTER_FROM        = By.id("aFilterFrom");
    private static final By FILTER_TO          = By.id("aFilterTo");
    private static final By FILTER_LOCATION    = By.id("aFilterLocation");
    private static final By FILTER_TYPE        = By.id("aFilterType");
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
    private static final By TYPE_BADGES        = By.cssSelector(".type-badge");
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
    private static final By NEXT_PAGE_BTN      = By.xpath("//div[contains(@class,'pagination-controls')]//button[@aria-label='Next page']");

    // ── Nav ───────────────────────────────────────────────────────────────────
    private static final By NAV_DASHBOARD      = By.xpath("//a[@routerLink='/traffic' and contains(@class,'nav-link')]");
    private static final By LOGOUT_BTN         = By.cssSelector("button.logout-btn");

    public TrafficAlertsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open traffic alerts page")
    public TrafficAlertsPage open() {
        navigateTo("/traffic-alerts");
        wait.waitForUrlToContain("/traffic-alerts");
        wait.waitForPresence(PAGE_TITLE);
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    // ── Filter actions ────────────────────────────────────────────────────────

    @Step("Select alert type filter: {type}")
    public TrafficAlertsPage selectAlertType(String type) {
        new Select(wait.waitForVisible(FILTER_TYPE)).selectByVisibleText(type);
        return this;
    }

    @Step("Select location filter: {location}")
    public TrafficAlertsPage selectLocation(String location) {
        new Select(wait.waitForVisible(FILTER_LOCATION)).selectByVisibleText(location);
        return this;
    }

    @Step("Select sort order: {sortValue}")
    public TrafficAlertsPage selectSort(String sortValue) {
        new Select(wait.waitForVisible(FILTER_SORT)).selectByValue(sortValue);
        return this;
    }

    @Step("Enter date from: {dateFrom}")
    public TrafficAlertsPage enterDateFrom(String dateFrom) {
        WebElement input = wait.waitForVisible(FILTER_FROM);
        input.clear();
        input.sendKeys(dateFrom);
        return this;
    }

    @Step("Enter date to: {dateTo}")
    public TrafficAlertsPage enterDateTo(String dateTo) {
        WebElement input = wait.waitForVisible(FILTER_TO);
        input.clear();
        input.sendKeys(dateTo);
        return this;
    }

    @Step("Click Apply Filters button")
    public TrafficAlertsPage clickApplyFilters() {
        wait.waitForClickable(APPLY_BTN).click();
        return this;
    }

    @Step("Click Reset filters button")
    public TrafficAlertsPage clickResetFilters() {
        wait.waitForClickable(RESET_BTN).click();
        return this;
    }

    // ── Alert actions ─────────────────────────────────────────────────────────

    @Step("Click Mark All Read button")
    public TrafficAlertsPage clickMarkAllRead() {
        wait.waitForClickable(MARK_ALL_BTN).click();
        return this;
    }

    @Step("Click Mark Read on first unread alert")
    public TrafficAlertsPage clickMarkReadOnFirst() {
        List<WebElement> btns = driver.findElements(MARK_READ_BTNS);
        if (!btns.isEmpty()) btns.get(0).click();
        return this;
    }

    @Step("Click Delete on first alert")
    public TrafficAlertsPage clickDeleteFirst() {
        List<WebElement> btns = driver.findElements(DELETE_BTNS);
        if (!btns.isEmpty()) btns.get(0).click();
        return this;
    }

    @Step("Click Back link to traffic dashboard")
    public TrafficDashboardPage clickBack() {
        wait.waitForClickable(BACK_LINK).click();
        wait.waitForUrlToContain("/traffic");
        return new TrafficDashboardPage(driver);
    }

    @Step("Click logout button")
    public void logout() {
        wait.waitForClickable(LOGOUT_BTN).click();
        wait.waitForUrlToContain("/signin");
    }

    // ── State checks ──────────────────────────────────────────────────────────

    public boolean isOnAlertsPage()             { return urlContains("/traffic-alerts"); }
    public boolean isPageTitleDisplayed()       { return isDisplayed(PAGE_TITLE); }
    public boolean isMarkAllReadDisplayed()     { return isDisplayed(MARK_ALL_BTN); }
    public boolean isLiveBannerDisplayed()      { return isDisplayed(LIVE_BANNER); }
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

    public List<WebElement> getTypeBadges()     { return driver.findElements(TYPE_BADGES); }
    public List<WebElement> getSeverityBadges() { return driver.findElements(SEVERITY_BADGES); }

    public String getPageSubtitleText() {
        try { return wait.waitForVisible(PAGE_SUBTITLE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getRecordCountText() {
        try { return wait.waitForVisible(RECORD_COUNT).getText().trim(); }
        catch (Exception e) { return ""; }
    }
}
