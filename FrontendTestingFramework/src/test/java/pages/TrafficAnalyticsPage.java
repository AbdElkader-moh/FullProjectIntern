package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * TrafficAnalyticsPage — Page Object for /traffic-analytics
 *
 * DOM facts from traffic-analytics.html:
 *  Filter inputs   → #filterFrom, #filterTo, #filterLocation, #filterCongestion, #filterSort
 *  Apply button    → button.btn-primary (contains "Search")
 *  Reset button    → button.btn-secondary (contains "Clear All")
 *  Table rows      → .data-table tbody tr
 *  Record count    → .record-count
 *  Congestion badge→ .congestion-badge
 *  Active filters  → .filter-active-indicator
 *  Date error      → .filter-date-error
 *  Empty state     → .empty-state
 *  Error banner    → .error-banner
 *  Page title      → .page-title
 *  Pagination      → .pagination / .page-btn
 */
public class TrafficAnalyticsPage extends BasePage {

    // ── Filter panel ──────────────────────────────────────────────────────────
    private static final By FILTER_FROM        = By.id("filterFrom");
    private static final By FILTER_TO          = By.id("filterTo");
    private static final By FILTER_LOCATION    = By.id("filterLocation");
    private static final By FILTER_CONGESTION  = By.id("filterCongestion");
    private static final By FILTER_SORT        = By.id("filterSort");
    private static final By APPLY_BTN          = By.cssSelector("button.btn-primary");
    private static final By RESET_BTN          = By.cssSelector("button.btn-secondary");
    private static final By ACTIVE_FILTER_BADGE= By.cssSelector(".filter-active-indicator");
    private static final By DATE_ERROR         = By.cssSelector(".filter-date-error");

    // ── Results ───────────────────────────────────────────────────────────────
    private static final By TABLE_ROWS         = By.cssSelector(".data-table tbody tr");
    private static final By TABLE_LOADING      = By.cssSelector(".table-loading");
    private static final By TABLE_ERROR        = By.cssSelector(".error-banner");
    private static final By EMPTY_STATE        = By.cssSelector(".empty-state");
    private static final By RECORD_COUNT       = By.cssSelector(".record-count");
    private static final By CONGESTION_BADGES  = By.cssSelector(".congestion-badge");
    private static final By PAGE_TITLE         = By.cssSelector(".page-title");

    // ── Pagination ────────────────────────────────────────────────────────────
    private static final By PAGINATION         = By.cssSelector(".pagination");
    private static final By ACTIVE_PAGE_BTN    = By.cssSelector(".pagination-controls .page-btn.active");
    private static final By NEXT_PAGE_BTN      = By.xpath("//div[contains(@class,'pagination-controls')]//button[@aria-label='Next page']");

    // ── Nav ───────────────────────────────────────────────────────────────────
    private static final By BACK_LINK          = By.cssSelector("a.back-link");
    private static final By NAV_DASHBOARD      = By.xpath("//a[@routerLink='/traffic' and contains(@class,'nav-link')]");
    private static final By LOGOUT_BTN         = By.cssSelector("button.logout-btn");

    public TrafficAnalyticsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open traffic analytics page")
    public TrafficAnalyticsPage open() {
        navigateTo("/traffic-analytics");
        wait.waitForUrlToContain("/traffic-analytics");
        wait.waitForPresence(PAGE_TITLE);
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Enter location filter: {location}")
    public TrafficAnalyticsPage enterLocation(String location) {
        WebElement input = wait.waitForVisible(FILTER_LOCATION);
        input.clear();
        input.sendKeys(location);
        return this;
    }

    @Step("Select congestion level: {level}")
    public TrafficAnalyticsPage selectCongestionLevel(String level) {
        new Select(wait.waitForVisible(FILTER_CONGESTION)).selectByVisibleText(level);
        return this;
    }

    @Step("Select sort option: {sortValue}")
    public TrafficAnalyticsPage selectSort(String sortValue) {
        new Select(wait.waitForVisible(FILTER_SORT)).selectByValue(sortValue);
        return this;
    }

    @Step("Enter date from: {dateFrom}")
    public TrafficAnalyticsPage enterDateFrom(String dateFrom) {
        WebElement input = wait.waitForVisible(FILTER_FROM);
        input.clear();
        input.sendKeys(dateFrom);
        return this;
    }

    @Step("Enter date to: {dateTo}")
    public TrafficAnalyticsPage enterDateTo(String dateTo) {
        WebElement input = wait.waitForVisible(FILTER_TO);
        input.clear();
        input.sendKeys(dateTo);
        return this;
    }

    @Step("Click Apply Filters / Search button")
    public TrafficAnalyticsPage clickApply() {
        wait.waitForClickable(APPLY_BTN).click();
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Click Reset / Clear All button")
    public TrafficAnalyticsPage clickReset() {
        wait.waitForClickable(RESET_BTN).click();
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
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

    public boolean isOnAnalyticsPage()         { return urlContains("/traffic-analytics"); }
    public boolean isPageTitleDisplayed()      { return isDisplayed(PAGE_TITLE); }
    public boolean isDateErrorDisplayed()      { return isDisplayed(DATE_ERROR); }
    public boolean isEmptyStateDisplayed()     { return isDisplayed(EMPTY_STATE); }
    public boolean isTableErrorDisplayed()     { return isDisplayed(TABLE_ERROR); }
    public boolean hasPagination()             { return isDisplayed(PAGINATION); }

    public List<WebElement> getTableRows()     { return driver.findElements(TABLE_ROWS); }
    public int getTableRowCount()              { return getTableRows().size(); }
    public boolean hasResults()                { return getTableRowCount() > 0; }

    public String getRecordCountText() {
        try { return wait.waitForVisible(RECORD_COUNT).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getActiveFilterBadgeText() {
        try { return wait.waitForVisible(ACTIVE_FILTER_BADGE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isActiveFilterBadgeDisplayed() { return isDisplayed(ACTIVE_FILTER_BADGE); }
    public List<WebElement> getCongestionBadges()  { return driver.findElements(CONGESTION_BADGES); }
}
