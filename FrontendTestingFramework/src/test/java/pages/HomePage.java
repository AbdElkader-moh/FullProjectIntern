package pages;

import io.qameta.allure.Step;
import utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class HomePage extends BasePage {

    private static final By AVATAR = By.xpath(
            "//a[contains(@class,'btn-profile') and .//span[normalize-space()='Profile']]");
    private static final By NOTIFICATION_BELL = By.xpath(
            "//a[@aria-label='Notifications'] | //a[.//span[normalize-space()='Notifications']]");
    private static final By SEARCH_INPUT = By.xpath(
            "//input[@type='search' or @placeholder[contains(.,'Search') or contains(.,'search')]]");
    private static final By FILTER_BUTTONS = By.cssSelector(".filter-btn, [class*='filter']");
    private static final By CARDS          = By.cssSelector(".card, [class*='card']");
    private static final By LOADING        = By.cssSelector(".loading, [class*='loading']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    @Step("Open home page")
    public HomePage open() {
        navigateTo("/home");
        waitForPageToLoad();
        return this;
    }

    private void waitForPageToLoad() {
        wait.waitForCondition(d -> {
            String url = d.getCurrentUrl();
            return url.contains("/home") || url.contains("/signin")
                    || url.contains("/profile") || url.contains("/dashboard")
                    || url.equals(url);
        });
        if (!driver.getCurrentUrl().contains("/home")) {
            navigateTo("/home");
            wait.waitForCondition(d -> d.getCurrentUrl().contains("/home"));
        }
        try { wait.waitForInvisibility(LOADING); } catch (Exception ignored) {}
    }

    @Step("Click notification bell")
    public NotificationsPage clickNotificationBell() {
        RetryHelper.retryVoid(() -> {
            WebElement bell = wait.waitForNotificationBell(NOTIFICATION_BELL);
            bell.click();
        }, "click notification bell");
        wait.waitForUrlToContain("/notifications");
        return new NotificationsPage(driver);
    }

    @Step("Click profile avatar")
    public ProfilePage clickProfileAvatar() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(AVATAR).click(), "click avatar");
        wait.waitForUrlToContain("/profile");
        return new ProfilePage(driver);
    }

    @Step("Navigate to profile page")
    public ProfilePage navigateToProfile() {
        navigateTo("/profile");
        wait.waitForUrlToContain("/profile");
        return new ProfilePage(driver);
    }

    @Step("Navigate to settings page")
    public SettingsPage navigateToSettings() {
        navigateTo("/settings");
        wait.waitForUrlToContain("/settings");
        return new SettingsPage(driver);
    }

    @Step("Navigate to notifications page")
    public NotificationsPage navigateToNotifications() {
        navigateTo("/notifications");
        wait.waitForUrlToContain("/notifications");
        return new NotificationsPage(driver);
    }

    @Step("Type in search bar: {query}")
    public HomePage typeInSearch(String query) {
        WebElement searchInput = wait.waitForVisible(SEARCH_INPUT);
        searchInput.clear();
        searchInput.sendKeys(query);
        return this;
    }

    public boolean isNotificationBellDisplayed() {
        try {
            WebElement bell = wait.waitForNotificationBell(NOTIFICATION_BELL);
            return bell.isDisplayed() && bell.isEnabled();
        } catch (Exception e) {
            System.out.println("[HOME] Bell check failed: " + e.getMessage());
            return false;
        }
    }

    public boolean isProfileAvatarDisplayed() { return isDisplayed(AVATAR); }
    public boolean isOnHomePage()             { return urlContains("/home"); }
    public boolean isSearchBarDisplayed()     { return isDisplayed(SEARCH_INPUT); }

    public List<WebElement> getCards()         { return wait.waitForPresenceOfAll(CARDS); }
    public List<WebElement> getFilterButtons() { return driver.findElements(FILTER_BUTTONS); }

    public boolean hasCards() {
        try { return !getCards().isEmpty(); } catch (Exception e) { return false; }
    }

    public HomePage clickFirstFilterButton() {
        List<WebElement> filters = getFilterButtons();
        if (!filters.isEmpty()) click(filters.get(0));
        return this;
    }
}