package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * StreetLightAnalyticsPage — Page Object for /street-light-analytics
 *
 * DOM facts from street-light-analytics.html:
 *
 *  Filter inputs:
 *    #filterFrom       datetime-local  (Start Date / Time)
 *    #filterLocation   text            (location search + datalist)
 *    #filterTo         datetime-local  (End Date / Time)
 *    #filterStatus select          (All Levels | Low | Moderate | High | Severe)
 *    #filterSort       select          (sort options from SORT_OPTIONS in TS)
 *
 *  Filter actions:
 *    button.btn-primary   → Search / Apply
 *    button.btn-secondary → Clear All / Reset
 *
 *  Filter state:
 *    .filter-active-indicator  → "N active" badge (only when activeFilterCount > 0)
 *    .filter-date-error        → validation message (only when end < start)
 *
 *  Results section:
 *    .record-count             → "N records found"
 *    .data-table tbody tr      → data rows
 *    td.col-location           → location cell
 *    td.col-power .power-value → power number
 *    td.col-brightness .brightness-value → brightness number (formatted 1.1-1)
 *    td .status-badge      → status level
 *    .table-loading            → loading spinner
 *    .empty-state              → no results state
 *    .error-banner             → API error
 *    #pageSize select          → per-page control
 *    .pagination               → pagination wrapper
 *    .page-btn.active          → current page button
 *
 *  Navigation:
 *    a.back-link[routerLink="/street-light"]
 *    a[routerLink="/home"].nav-link
 *    a[routerLink="/street-light-alerts"].nav-link
 *    button.logout-btn
 *
 *  IMPORTANT — datetime-local inputs:
 *    Chrome breaks these into sub-fields. sendKeys() types unreliably.
 *    All date setters use JavaScript to set value + dispatch input/change events
 *    so Angular's [(ngModel)] binding picks up the new value.
 */
public class StreetLightAnalyticsPage extends BasePage {

    // ── Filter panel ──────────────────────────────────────────────────────────
    private static final By FILTER_FROM           = By.id("from");
    private static final By FILTER_TO             = By.id("to");
    private static final By FILTER_LOCATION       = By.id("location");
    private static final By FILTER_CONGESTION     = By.id("status");
    private static final By FILTER_SORT           = By.id("filterSort");
    private static final By APPLY_BTN             = By.cssSelector("button.btn-primary");
    private static final By RESET_BTN             = By.cssSelector("button.btn-secondary");
    private static final By ACTIVE_FILTER_BADGE   = By.cssSelector(".filter-active-indicator");
    private static final By DATE_ERROR            = By.cssSelector(".filter-date-error");

    // ── Results section ───────────────────────────────────────────────────────
    private static final By RECORD_COUNT          = By.cssSelector(".record-count");
    private static final By TABLE_ROWS            = By.cssSelector(".data-table tbody tr");
    private static final By TABLE_LOADING         = By.cssSelector(".table-loading");
    private static final By TABLE_ERROR           = By.cssSelector(".error-banner");
    private static final By EMPTY_STATE           = By.cssSelector(".empty-state");
    private static final By CONGESTION_BADGES     = By.cssSelector(".congestion-badge");
    private static final By PAGE_TITLE            = By.cssSelector(".page-title");
    private static final By PAGE_SIZE_SELECT      = By.id("pageSize");

    // ── First row cell selectors ──────────────────────────────────────────────
    private static final By FIRST_ROW_LOCATION    = By.cssSelector(".data-table tbody tr:first-child td:nth-child(2)");
    private static final By FIRST_ROW_BRIGHTNESS  = By.cssSelector(".data-table tbody tr:first-child td:nth-child(3)");
    private static final By FIRST_ROW_POWER       = By.cssSelector(".data-table tbody tr:first-child td:nth-child(4)");
    private static final By FIRST_ROW_CONGESTION  = By.cssSelector(".data-table tbody tr:first-child td:nth-child(5) .congestion-badge");

    // ── Pagination ────────────────────────────────────────────────────────────
    private static final By PAGINATION            = By.cssSelector(".pagination");
    private static final By ACTIVE_PAGE_BTN       = By.cssSelector(".pagination-controls .page-btn.active");
    private static final By NEXT_PAGE_BTN         = By.xpath("//div[contains(@class,'pagination-controls')]//button[contains(text(),'›')]");
    private static final By PREV_PAGE_BTN         = By.xpath("//div[contains(@class,'pagination-controls')]//button[contains(text(),'‹')]");
    private static final By FIRST_PAGE_BTN        = By.xpath("//div[contains(@class,'pagination-controls')]//button[contains(text(),'«')]");
    private static final By PAGINATION_INFO       = By.cssSelector(".pagination-info");

    // ── Navigation ────────────────────────────────────────────────────────────
    private static final By BACK_LINK             = By.cssSelector("a.back-link");
    private static final By NAV_DASHBOARD         = By.xpath("//a[@href='/lights' and contains(@class,'nav-link')]");
    private static final By NAV_ALERTS            = By.xpath("//a[@href='/lights-alerts' and contains(@class,'nav-link')]");
    private static final By LOGOUT_BTN            = By.cssSelector("button.logout-btn");

    // ── Constructor ───────────────────────────────────────────────────────────

    public StreetLightAnalyticsPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Step("Open street light analytics page")
    public StreetLightAnalyticsPage open() {
        navigateTo("/lights-analytics");
        wait.waitForUrlToContain("/lights-analytics");
        wait.waitForPresence(PAGE_TITLE);
        waitForResults();
        return this;
    }

    private void waitForResults() {
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
    }

    @Step("Click Back link to street light dashboard")
    public StreetLightDashboardPage clickBack() {
        wait.waitForClickable(BACK_LINK).click();
        wait.waitForUrlMatches(".*\\/lights$");
        return new StreetLightDashboardPage(driver);
    }

    @Step("Click Dashboard nav link")
    public StreetLightDashboardPage clickDashboardNav() {
        wait.waitForClickable(NAV_DASHBOARD).click();
        wait.waitForUrlMatches(".*\\/lights$");
        return new StreetLightDashboardPage(driver);
    }

    @Step("Click Alerts nav link")
    public StreetLightAlertsPage clickAlertsNav() {
        wait.waitForClickable(NAV_ALERTS).click();
        wait.waitForUrlToContain("/lights-alerts");
        return new StreetLightAlertsPage(driver);
    }

    @Step("Click logout")
    public void logout() {
        wait.waitForClickable(LOGOUT_BTN).click();
        wait.waitForUrlToContain("/signin");
    }

    // ── Filter actions ────────────────────────────────────────────────────────

    /**
     * Sets a datetime-local input via JavaScript.
     *
     * WHY JS: Chrome's datetime-local input is split into sub-fields.
     * sendKeys() types into them in an undefined order producing garbled values.
     * Setting value + dispatching input/change events lets Angular's ngModel
     * pick up the new value reliably.
     *
     * @param value format: "YYYY-MM-DDTHH:MM" (e.g. "2026-06-01T00:00")
     */
    private void setDateInput(By locator, String value) {
        WebElement input = wait.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input',  {bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                input, value
        );
    }

    @Step("Enter start date: {dateFrom}")
    public StreetLightAnalyticsPage enterDateFrom(String dateFrom) {
        setDateInput(FILTER_FROM, dateFrom);
        return this;
    }

    @Step("Enter end date: {dateTo}")
    public StreetLightAnalyticsPage enterDateTo(String dateTo) {
        setDateInput(FILTER_TO, dateTo);
        return this;
    }

    @Step("Enter location filter: {location}")
    public StreetLightAnalyticsPage enterLocation(String location) {
        WebElement input = wait.waitForVisible(FILTER_LOCATION);
        input.clear();
        input.sendKeys(location);
        return this;
    }

    @Step("Select status level: {level}")
    public StreetLightAnalyticsPage selectStatusLevel(String level) {
        new Select(wait.waitForVisible(FILTER_CONGESTION)).selectByVisibleText(level);
        return this;
    }

    @Step("Select sort option: {sortValue}")
    public StreetLightAnalyticsPage selectSort(String sortValue) {
        new Select(wait.waitForVisible(FILTER_SORT)).selectByValue(sortValue);
        return this;
    }

    @Step("Select page size: {size}")
    public StreetLightAnalyticsPage selectPageSize(String size) {
        new Select(wait.waitForVisible(PAGE_SIZE_SELECT)).selectByVisibleText(size);
        waitForResults();
        return this;
    }

    @Step("Click Search / Apply button")
    public StreetLightAnalyticsPage clickApply() {
        wait.waitForClickable(APPLY_BTN).click();
        waitForResults();
        return this;
    }

    @Step("Click Clear All / Reset button")
    public StreetLightAnalyticsPage clickReset() {
        wait.waitForClickable(RESET_BTN).click();
        waitForResults();
        return this;
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Step("Click Next page")
    public StreetLightAnalyticsPage clickNextPage() {
        wait.waitForClickable(NEXT_PAGE_BTN).click();
        waitForResults();
        return this;
    }

    @Step("Click Previous page")
    public StreetLightAnalyticsPage clickPreviousPage() {
        wait.waitForClickable(PREV_PAGE_BTN).click();
        waitForResults();
        return this;
    }

    @Step("Click First page")
    public StreetLightAnalyticsPage clickFirstPage() {
        wait.waitForClickable(FIRST_PAGE_BTN).click();
        waitForResults();
        return this;
    }

    // ── State checks ──────────────────────────────────────────────────────────

    public boolean isOnAnalyticsPage()          { return urlContains("/lights-analytics"); }
    public boolean isPageTitleDisplayed()       { return isDisplayed(PAGE_TITLE); }
    public boolean isDateErrorDisplayed()       { return isDisplayed(DATE_ERROR); }
    public boolean isEmptyStateDisplayed()      { return isDisplayed(EMPTY_STATE); }
    public boolean isTableErrorDisplayed()      { return isDisplayed(TABLE_ERROR); }
    public boolean isTableLoadingDisplayed()    { return isDisplayed(TABLE_LOADING); }
    public boolean hasPagination()              { return isDisplayed(PAGINATION); }
    public boolean isActiveFilterBadgeDisplayed(){ return isDisplayed(ACTIVE_FILTER_BADGE); }
    public boolean isNextPageEnabled() {
        try {
            WebElement btn = driver.findElement(NEXT_PAGE_BTN);
            return btn.isEnabled() && btn.getAttribute("disabled") == null;
        } catch (Exception e) { return false; }
    }

    // ── Data readers ──────────────────────────────────────────────────────────

    public List<WebElement> getTableRows()       { return driver.findElements(TABLE_ROWS); }
    public int getTableRowCount()                { return getTableRows().size(); }
    public boolean hasResults()                  { return getTableRowCount() > 0; }
    public List<WebElement> getStatusBadges(){ return driver.findElements(CONGESTION_BADGES); }

    public String getRecordCountText() {
        try { return wait.waitForVisible(RECORD_COUNT).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public int getRecordCountNumber() {
        String text = getRecordCountText();
        // Format: "123 records found" or "1 record found"
        try { return Integer.parseInt(text.split(" ")[0].replace(",", "")); }
        catch (Exception e) { return -1; }
    }

    public String getActiveFilterBadgeText() {
        try { return wait.waitForVisible(ACTIVE_FILTER_BADGE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getActivePageNumber() {
        try { return wait.waitForVisible(ACTIVE_PAGE_BTN).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getPaginationInfoText() {
        try { return wait.waitForVisible(PAGINATION_INFO).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    // ── First-row cell readers ────────────────────────────────────────────────

    @Step("Read first row location")
    public String getFirstRowLocation() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_LOCATION).getText().trim();
    }

    @Step("Read first row power")
    public String getFirstRowPowerConsumption() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        // The API suffix 'kWh' is sometimes returned by the UI if suffix is configured.
        String text = wait.waitForVisible(FIRST_ROW_POWER).getText().trim();
        return text.replace(" kWh", "").replace(",", "");
    }

    @Step("Read first row brightness")
    public String getFirstRowBrightnessLevel() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        String text = wait.waitForVisible(FIRST_ROW_BRIGHTNESS).getText().trim();
        return text.replace("%", "");
    }

    @Step("Read first row status level")
    public String getFirstRowStatusLevel() {
        wait.waitForPresenceOfAll(TABLE_ROWS);
        return wait.waitForVisible(FIRST_ROW_CONGESTION).getText().trim();
    }
}