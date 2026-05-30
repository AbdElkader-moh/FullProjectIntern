package pages;

import io.qameta.allure.Step;
import utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * TrafficDashboardPage — Page Object for /traffic
 *
 * DOM facts from traffic-dashboard.html:
 *
 *  Header nav      → .nav-link anchors (Home, Analytics, Alerts, Settings, Notifications, Profile)
 *  Quick actions   → .quick-action-btn (Analytics & Search, Traffic Alerts)
 *  Stats cards     → .stat-card (5 cards: Total Records, Avg Density, Avg Speed, + 2 more)
 *  Stats values    → .stat-value inside each .stat-card
 *  Stats labels    → .stat-label inside each .stat-card
 *  Table           → .data-table inside .table-section
 *  Table rows      → .data-table tbody tr
 *  Pagination      → .pagination / .page-btn
 *  Charts section  → .charts-section / .chart-card
 *  Congestion dist → .congestion-chart / .congestion-row
 *  Recent alerts   → .alerts-list / .alert-item
 *  Unread alert    → .alert-item.alert-unread
 *  Refresh button  → .btn-icon (manual refresh)
 *  Auto-refresh    → .btn-auto-refresh
 *  Syncing text    → .last-refreshed.syncing
 *  Last updated    → .last-refreshed (without .syncing)
 *  Error banner    → .error-banner
 *  Loading spinner → .spinner
 *  Back link       → a.back-link[routerLink="/home"]
 *  View all alerts → a.view-all-link[routerLink="/traffic-alerts"]
 */
public class TrafficDashboardPage extends BasePage {

    // ── Header nav ────────────────────────────────────────────────────────────
    private static final By BRAND_LINK         = By.cssSelector("a.brand-link");
    private static final By NAV_HOME           = By.xpath("//a[@routerLink='/home' and contains(@class,'nav-link')]");
    private static final By NAV_ANALYTICS      = By.xpath("//a[@routerLink='/traffic-analytics']");
    private static final By NAV_ALERTS         = By.xpath("//a[@routerLink='/traffic-alerts' and contains(@class,'nav-link')]");
    private static final By NAV_SETTINGS       = By.xpath("//a[@routerLink='/settings' and contains(@class,'nav-link')]");
    private static final By NAV_NOTIFICATIONS  = By.xpath("//a[@routerLink='/notifications' and contains(@class,'nav-link')]");
    private static final By NAV_PROFILE        = By.xpath("//a[@routerLink='/profile' and contains(@class,'nav-link')]");
    private static final By LOGOUT_BTN         = By.cssSelector("button.logout-btn");
    private static final By NOTIF_BADGE        = By.cssSelector(".notification-link .badge");

    // ── Page header ───────────────────────────────────────────────────────────
    private static final By PAGE_TITLE         = By.cssSelector(".page-title");
    private static final By PAGE_SUBTITLE      = By.cssSelector(".page-subtitle");
    private static final By BACK_LINK          = By.cssSelector("a.back-link");

    // ── Refresh controls ──────────────────────────────────────────────────────
    private static final By MANUAL_REFRESH_BTN = By.cssSelector("button.btn-icon");
    private static final By AUTO_REFRESH_BTN   = By.cssSelector("button.btn-auto-refresh");
    private static final By LAST_REFRESHED     = By.cssSelector(".last-refreshed:not(.syncing)");
    private static final By SYNCING_INDICATOR  = By.cssSelector(".last-refreshed.syncing");

    // ── Quick actions ─────────────────────────────────────────────────────────
    private static final By QUICK_ACTION_ANALYTICS = By.xpath("//a[contains(@class,'quick-action-btn') and @routerLink='/traffic-analytics']");
    private static final By QUICK_ACTION_ALERTS    = By.xpath("//a[contains(@class,'quick-action-btn') and @routerLink='/traffic-alerts']");

    // ── Stats ─────────────────────────────────────────────────────────────────
    private static final By STATS_SECTION      = By.cssSelector(".stats-section");
    private static final By STAT_CARDS         = By.cssSelector(".stat-card:not(.skeleton)");
    private static final By STAT_VALUES        = By.cssSelector(".stat-card:not(.skeleton) .stat-value");
    private static final By STAT_LABELS        = By.cssSelector(".stat-card:not(.skeleton) .stat-label");
    private static final By STATS_ERROR        = By.cssSelector(".error-banner");
    private static final By STATS_SKELETON     = By.cssSelector(".stat-card.skeleton");

    // ── Table ─────────────────────────────────────────────────────────────────
    private static final By TABLE_SECTION      = By.cssSelector(".table-section");
    private static final By TABLE_ROWS         = By.cssSelector(".data-table tbody tr");
    private static final By TABLE_LOADING      = By.cssSelector(".table-loading");
    private static final By TABLE_EMPTY        = By.cssSelector(".empty-state");
    private static final By TABLE_ERROR        = By.cssSelector(".table-section .error-banner");
    private static final By RECORD_COUNT       = By.cssSelector(".record-count");
    private static final By PAGINATION         = By.cssSelector(".pagination");
    private static final By PAGE_BTNS          = By.cssSelector(".pagination-controls .page-btn");
    private static final By ACTIVE_PAGE_BTN    = By.cssSelector(".pagination-controls .page-btn.active");
    private static final By NEXT_PAGE_BTN      = By.xpath("//div[contains(@class,'pagination-controls')]//button[@aria-label='Next page']");
    private static final By PREV_PAGE_BTN      = By.xpath("//div[contains(@class,'pagination-controls')]//button[@aria-label='Previous page']");
    private static final By FIRST_PAGE_BTN     = By.xpath("//div[contains(@class,'pagination-controls')]//button[@aria-label='First page']");

    // ── Charts ────────────────────────────────────────────────────────────────
    private static final By CHARTS_SECTION     = By.cssSelector(".charts-section");
    private static final By CHART_CARDS        = By.cssSelector(".chart-card");
    private static final By CONGESTION_CHART   = By.cssSelector(".congestion-chart");
    private static final By CONGESTION_ROWS    = By.cssSelector(".congestion-row");
    private static final By CHART_EMPTY        = By.cssSelector(".chart-empty");
    private static final By CHART_ERROR        = By.cssSelector(".chart-empty.chart-error");
    private static final By DENSITY_SVG        = By.cssSelector(".density-line");
    private static final By SPEED_BARS         = By.cssSelector(".speed-bar");

    // ── Recent alerts ─────────────────────────────────────────────────────────
    private static final By ALERTS_SECTION     = By.cssSelector(".alerts-section");
    private static final By ALERTS_LIST        = By.cssSelector(".alerts-list");
    private static final By ALERT_ITEMS        = By.cssSelector(".alert-item");
    private static final By UNREAD_ALERT_ITEMS = By.cssSelector(".alert-item.alert-unread");
    private static final By ALERTS_EMPTY       = By.cssSelector(".alerts-empty");
    private static final By ALERTS_ERROR       = By.cssSelector(".alerts-section .error-banner");
    private static final By VIEW_ALL_ALERTS    = By.cssSelector("a.view-all-link");

    // ── Constructor ───────────────────────────────────────────────────────────

    public TrafficDashboardPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Step("Open traffic dashboard page")
    public TrafficDashboardPage open() {
        navigateTo("/traffic");
        wait.waitForUrlToContain("/traffic");
        waitForInitialLoad();
        return this;
    }

    /** Waits for at least the page title and stats section to be present */
    private void waitForInitialLoad() {
        wait.waitForPresence(PAGE_TITLE);
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        try { wait.waitForInvisibility(By.cssSelector(".spinner")); } catch (Exception ignored) {}
    }

    @Step("Click Back link to go to /home")
    public HomePage clickBackLink() {
        wait.waitForClickable(BACK_LINK).click();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    @Step("Click Analytics nav link")
    public TrafficAnalyticsPage clickAnalyticsNav() {
        wait.waitForClickable(NAV_ANALYTICS).click();
        wait.waitForUrlToContain("/traffic-analytics");
        return new TrafficAnalyticsPage(driver);
    }

    @Step("Click Alerts nav link")
    public TrafficAlertsPage clickAlertsNav() {
        wait.waitForClickable(NAV_ALERTS).click();
        wait.waitForUrlToContain("/traffic-alerts");
        return new TrafficAlertsPage(driver);
    }

    @Step("Click Home nav link")
    public HomePage clickHomeNav() {
        wait.waitForClickable(NAV_HOME).click();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    @Step("Click Analytics & Search quick action button")
    public TrafficAnalyticsPage clickQuickActionAnalytics() {
        wait.waitForClickable(QUICK_ACTION_ANALYTICS).click();
        wait.waitForUrlToContain("/traffic-analytics");
        return new TrafficAnalyticsPage(driver);
    }

    @Step("Click Traffic Alerts quick action button")
    public TrafficAlertsPage clickQuickActionAlerts() {
        wait.waitForClickable(QUICK_ACTION_ALERTS).click();
        wait.waitForUrlToContain("/traffic-alerts");
        return new TrafficAlertsPage(driver);
    }

    @Step("Click View All Alerts link")
    public TrafficAlertsPage clickViewAllAlerts() {
        wait.waitForClickable(VIEW_ALL_ALERTS).click();
        wait.waitForUrlToContain("/traffic-alerts");
        return new TrafficAlertsPage(driver);
    }

    @Step("Click manual refresh button")
    public TrafficDashboardPage clickManualRefresh() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(MANUAL_REFRESH_BTN).click(), "click manual refresh");
        return this;
    }

    @Step("Click auto-refresh toggle button")
    public TrafficDashboardPage clickAutoRefreshToggle() {
        wait.waitForClickable(AUTO_REFRESH_BTN).click();
        return this;
    }

    @Step("Click logout button")
    public void logout() {
        wait.waitForClickable(LOGOUT_BTN).click();
        wait.waitForUrlToContain("/signin");
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Step("Go to next page")
    public TrafficDashboardPage clickNextPage() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(NEXT_PAGE_BTN).click(), "click next page");
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Go to previous page")
    public TrafficDashboardPage clickPreviousPage() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(PREV_PAGE_BTN).click(), "click previous page");
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    @Step("Go to first page")
    public TrafficDashboardPage clickFirstPage() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(FIRST_PAGE_BTN).click(), "click first page");
        try { wait.waitForInvisibility(TABLE_LOADING); } catch (Exception ignored) {}
        return this;
    }

    // ── State checks ──────────────────────────────────────────────────────────

    public boolean isOnTrafficDashboard()        { return urlContains("/traffic") && !urlContains("/traffic-"); }
    public boolean isPageTitleDisplayed()        { return isDisplayed(PAGE_TITLE); }
    public boolean isStatsSectionDisplayed()     { return isDisplayed(STATS_SECTION); }
    public boolean isChartsSectionDisplayed()    { return isDisplayed(CHARTS_SECTION); }
    public boolean isAlertsSectionDisplayed()    { return isDisplayed(ALERTS_SECTION); }
    public boolean isTableSectionDisplayed()     { return isDisplayed(TABLE_SECTION); }
    public boolean isLastRefreshedDisplayed()    { return isDisplayed(LAST_REFRESHED); }
    public boolean isSyncingIndicatorDisplayed() { return isDisplayed(SYNCING_INDICATOR); }
    public boolean isAutoRefreshActive()         {
        try {
            WebElement btn = wait.waitForVisible(AUTO_REFRESH_BTN);
            return btn.getAttribute("class") != null && btn.getAttribute("class").contains("active");
        } catch (Exception e) { return false; }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public List<WebElement> getStatCards()   { return driver.findElements(STAT_CARDS); }
    public List<WebElement> getStatValues()  { return driver.findElements(STAT_VALUES); }
    public int getStatCardCount()            { return getStatCards().size(); }
    public boolean areStatsLoaded()          { return !getStatCards().isEmpty() && driver.findElements(STATS_SKELETON).isEmpty(); }
    public boolean isStatsErrorDisplayed()   { return isDisplayed(STATS_ERROR); }

    /** Returns the text of a stat value card by its 0-based index */
    public String getStatValue(int index) {
        List<WebElement> values = getStatValues();
        if (index >= values.size()) return "";
        return values.get(index).getText().trim();
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    public List<WebElement> getTableRows()   { return driver.findElements(TABLE_ROWS); }
    public int getTableRowCount()            { return getTableRows().size(); }
    public boolean isTableEmpty()            { return isDisplayed(TABLE_EMPTY); }
    public boolean isTableErrorDisplayed()   { return isDisplayed(TABLE_ERROR); }
    public boolean isTableLoadingDisplayed() { return isDisplayed(TABLE_LOADING); }
    public boolean hasPagination()           { return isDisplayed(PAGINATION); }
    public boolean isNextPageEnabled()       {
        try {
            WebElement btn = driver.findElement(NEXT_PAGE_BTN);
            return btn.isEnabled() && (btn.getAttribute("disabled") == null);
        } catch (Exception e) { return false; }
    }

    public String getActivePageNumber() {
        try { return wait.waitForVisible(ACTIVE_PAGE_BTN).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    public List<WebElement> getChartCards()       { return driver.findElements(CHART_CARDS); }
    public int getChartCardCount()                { return getChartCards().size(); }
    public boolean isCongestionChartDisplayed()   { return isDisplayed(CONGESTION_CHART); }
    public List<WebElement> getCongestionRows()   { return driver.findElements(CONGESTION_ROWS); }
    public int getCongestionRowCount()            { return getCongestionRows().size(); }
    public boolean isDensityLineRendered()        { return isDisplayed(DENSITY_SVG); }
    public boolean areSpeedBarsRendered()         { return !driver.findElements(SPEED_BARS).isEmpty(); }
    public boolean isChartErrorDisplayed()        { return isDisplayed(CHART_ERROR); }

    // ── Recent alerts ─────────────────────────────────────────────────────────

    public List<WebElement> getAlertItems()       { return driver.findElements(ALERT_ITEMS); }
    public int getAlertItemCount()                { return getAlertItems().size(); }
    public boolean hasAlerts()                    { return getAlertItemCount() > 0; }
    public boolean isAlertsEmptyStateDisplayed()  { return isDisplayed(ALERTS_EMPTY); }
    public boolean isAlertsErrorDisplayed()       { return isDisplayed(ALERTS_ERROR); }
    public int getUnreadAlertCount()              { return driver.findElements(UNREAD_ALERT_ITEMS).size(); }

    public boolean isNotifBadgeDisplayed() {
        return !driver.findElements(NOTIF_BADGE).isEmpty();
    }

    /** Clicks on the first alert item to navigate to /traffic-alerts */
    @Step("Click first alert item")
    public TrafficAlertsPage clickFirstAlert() {
        List<WebElement> items = getAlertItems();
        if (!items.isEmpty()) {
            RetryHelper.retryVoid(() -> items.get(0).click(), "click first alert item");
            wait.waitForUrlToContain("/traffic-alerts");
        }
        return new TrafficAlertsPage(driver);
    }
}
