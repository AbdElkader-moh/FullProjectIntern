package pages;


import io.qameta.allure.Step;

import utils.ConfigReader;
import utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class NotificationsPage extends BasePage {

    private static final By PAGE_HEADING             = By.xpath("//h1[contains(text(),'Notifications')] | //h2[contains(text(),'Notifications')]");
    private static final By ALL_CAUGHT_UP_MSG        = By.xpath("//*[contains(text(),'All caught up')]");
    private static final By NOTIFICATION_ITEMS       = By.cssSelector(".notif-item");
    private static final By UNREAD_NOTIFICATION_ITEMS= By.cssSelector(".notif-item.unread");
    private static final By UNREAD_DOT               = By.cssSelector(".unread-dot");
    private static final By BACK_BTN                 = By.cssSelector("button.btn-back");
    private static final By MARK_ALL_READ_BTN        = By.cssSelector("button.mark-all-btn");
    private static final By LOADING_SPINNER          = By.cssSelector(".loading");
    private static final By PAGE_SUBTITLE            = By.cssSelector(".page-subtitle");

    public NotificationsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open notifications page")
    public NotificationsPage open() {
        navigateTo("/notifications");
        wait.waitForVisible(PAGE_HEADING);
        try { wait.waitForInvisibility(LOADING_SPINNER); } catch (Exception ignored) {}
        return this;
    }

    @Step("Click Back button")
    public HomePage clickBack() {
        RetryHelper.retryVoid(() -> wait.waitForClickable(BACK_BTN).click(), "click Back button");
        wait.waitForUrlToContainAny("/home", "/");
        return new HomePage(driver);
    }

    @Step("Click notification item at index {index}")
    public NotificationsPage clickNotificationItem(int index) {
        List<WebElement> items = getNotificationItems();
        if (index < items.size()) {
            RetryHelper.retryVoid(() ->
                            wait.waitForClickable(items.get(index)).click(),
                    "click notification item " + index);
        }
        return this;
    }

    @Step("Mark first unread notification as read")
    public boolean markFirstNotificationAsRead() {
        List<WebElement> unread = driver.findElements(UNREAD_NOTIFICATION_ITEMS);
        if (unread.isEmpty()) {
            System.out.println("[NOTIF] No unread notifications to mark");
            return false;
        }
        WebElement first = unread.get(0);
        RetryHelper.retryVoid(() -> wait.waitForClickable(first).click(),
                "click first unread notification");
        boolean removed = wait.waitForClassAbsent(
                first, "unread", ConfigReader.getNotificationReadStateTimeout());
        System.out.println("[NOTIF] Mark-as-read result: " + (removed ? "SUCCESS" : "TIMEOUT"));
        return removed;
    }

    @Step("Click 'Mark all as read' button")
    public boolean markAllNotificationsAsRead() {
        WebElement btn = wait.waitForClickable(MARK_ALL_READ_BTN);
        btn.click();
        boolean gone = wait.waitForUnreadIndicatorToDisappear(UNREAD_DOT);
        System.out.println("[NOTIF] Mark-all-read result: " + (gone ? "SUCCESS" : "TIMEOUT"));
        return gone;
    }

    @Step("Wait for at least {minimumCount} notification(s) to appear")
    public boolean waitForNotificationsToAppear(int minimumCount) {
        return wait.waitForNotificationToAppear(NOTIFICATION_ITEMS, minimumCount);
    }

    public int getUnreadCount() {
        try {
            WebElement subtitle = wait.waitForVisibleOrNull(PAGE_SUBTITLE, 3);
            if (subtitle != null) {
                String text = subtitle.getText().trim();
                if (text.contains("unread")) {
                    return Integer.parseInt(text.split("\\s+")[0]);
                }
            }
        } catch (StaleElementReferenceException | NumberFormatException e) {
            System.out.println("[NOTIF] Could not parse subtitle count: " + e.getMessage());
        }
        return driver.findElements(UNREAD_NOTIFICATION_ITEMS).size();
    }

    public boolean hasNotificationMatching(String... substrings) {
        List<WebElement> items = driver.findElements(NOTIFICATION_ITEMS);
        for (WebElement item : items) {
            try {
                String text = item.getText().toLowerCase();
                boolean match = true;
                for (String s : substrings) {
                    if (!text.contains(s.toLowerCase())) { match = false; break; }
                }
                if (match) return true;
            } catch (StaleElementReferenceException e) {
                items = driver.findElements(NOTIFICATION_ITEMS);
            }
        }
        return false;
    }

    public boolean isOnNotificationsPage()         { return urlContains("/notifications"); }
    public boolean isPageHeadingDisplayed()        { return isDisplayed(PAGE_HEADING); }
    public boolean isAllCaughtUpMessageDisplayed() { return isDisplayed(ALL_CAUGHT_UP_MSG); }
    public boolean isMarkAllReadButtonDisplayed()  { return isDisplayed(MARK_ALL_READ_BTN); }
    public boolean isNoAlertsMessageDisplayed()    { return isDisplayed(By.xpath("//*[contains(text(),'No alerts')]")); }
    public boolean isEmptyStateDisplayed()         { return isAllCaughtUpMessageDisplayed() || isNoAlertsMessageDisplayed(); }
    public List<WebElement> getNotificationItems() { return driver.findElements(NOTIFICATION_ITEMS); }
    public int getNotificationCount()              { return getNotificationItems().size(); }
    public boolean hasNotifications()              { return getNotificationCount() > 0; }
}