package com.internship.pages;

import com.internship.utils.ConfigReader;
import com.internship.utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * NotificationsPage: Page Object for /notifications
 *
 * Changes from original:
 *  - BACK_BTN: fixed from broken XPath text() to button.btn-back CSS (TC051 fix)
 *  - NOTIFICATION_ITEMS: corrected to .notif-item (matches real Angular HTML)
 *  - Added: mark-as-read methods (Issue 2)
 *  - Added: notification appearance polling (Issue 3 & 4)
 *  - Added: loading spinner wait in open()
 */
public class NotificationsPage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────────────────

    private static final By PAGE_HEADING =
            By.xpath("//h1[contains(text(),'Notifications')] | //h2[contains(text(),'Notifications')]");

    private static final By ALL_CAUGHT_UP_MSG =
            By.xpath("//*[contains(text(),'All caught up')]");

    /**
     * Fixed from original .notification-item / .alert-item guesses.
     * Real Angular HTML confirmed in notifications.html: <div class="notif-item">
     */
    private static final By NOTIFICATION_ITEMS =
            By.cssSelector(".notif-item");

    private static final By UNREAD_NOTIFICATION_ITEMS =
            By.cssSelector(".notif-item.unread");

    private static final By UNREAD_DOT =
            By.cssSelector(".unread-dot");

    /**
     * TC051 fix.
     *
     * BROKEN original:
     *   By.xpath("//button[contains(text(),'Back')] | //a[contains(text(),'Back')]")
     *
     * Why it broke: XPath text() only matches DIRECT text-node children.
     * The DOM from notifications.html is:
     *   <button class="btn-back">
     *     <svg/>
     *     <span>Back</span>   ← text is inside child <span>, NOT the button
     *   </button>
     * So contains(text(),'Back') on <button> is always false → TimeoutException.
     *
     * Fix: target by the stable CSS class confirmed in notifications.html line 4.
     */
    private static final By BACK_BTN =
            By.cssSelector("button.btn-back");

    /** Rendered only when unreadCount > 0. */
    private static final By MARK_ALL_READ_BTN =
            By.cssSelector("button.mark-all-btn");

    /** Angular loading div shown during async data fetch. */
    private static final By LOADING_SPINNER =
            By.cssSelector(".loading");

    private static final By PAGE_SUBTITLE =
            By.cssSelector(".page-subtitle");

    // ── Constructor ───────────────────────────────────────────────────────────

    public NotificationsPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public NotificationsPage open() {
        navigateTo("/notifications");
        wait.waitForVisible(PAGE_HEADING);
        // Wait for Angular to finish loading notifications from backend
        try { wait.waitForInvisibility(LOADING_SPINNER); }
        catch (Exception ignored) {}
        return this;
    }

    /** TC051 — now uses fixed button.btn-back CSS selector. */
    public HomePage clickBack() {
        RetryHelper.retryVoid(() ->
                wait.waitForClickable(BACK_BTN).click(),
                "click Back button");
        wait.waitForUrlToContainAny("/home", "/");
        return new HomePage(driver);
    }

    public NotificationsPage clickNotificationItem(int index) {
        List<WebElement> items = getNotificationItems();
        if (index < items.size()) {
            RetryHelper.retryVoid(() ->
                    wait.waitForClickable(items.get(index)).click(),
                    "click notification item " + index);
        }
        return this;
    }

    // ── Issue 2 — Mark as Read ────────────────────────────────────────────────

    /**
     * Clicks the first unread notification to mark it as read.
     * Angular removes the .unread class and .unread-dot on click (markRead(n)).
     *
     * @return true if the .unread class was removed within the configured timeout
     */
    public boolean markFirstNotificationAsRead() {
        List<WebElement> unread = driver.findElements(UNREAD_NOTIFICATION_ITEMS);
        if (unread.isEmpty()) {
            System.out.println("[NOTIF] No unread notifications to mark");
            return false;
        }
        WebElement first = unread.get(0);
        RetryHelper.retryVoid(() ->
                wait.waitForClickable(first).click(),
                "click first unread notification");

        boolean removed = wait.waitForClassAbsent(
                first, "unread", ConfigReader.getNotificationReadStateTimeout());
        System.out.println("[NOTIF] Mark-as-read result: " + (removed ? "SUCCESS" : "TIMEOUT"));
        return removed;
    }

    /**
     * Clicks "Mark all as read" and waits for all .unread-dot elements to vanish.
     *
     * @return true if all unread indicators disappeared
     */
    public boolean markAllNotificationsAsRead() {
        WebElement btn = wait.waitForClickable(MARK_ALL_READ_BTN);
        btn.click();
        boolean gone = wait.waitForUnreadIndicatorToDisappear(UNREAD_DOT);
        System.out.println("[NOTIF] Mark-all-read result: " + (gone ? "SUCCESS" : "TIMEOUT"));
        return gone;
    }

    // ── Issue 3 & 4 — Polling + Appearance Verification ──────────────────────

    /**
     * Polls until at least minimumCount notifications appear in the list.
     * Uses notification.appearance.timeout and simulator.poll.interval.ms from config.
     *
     * @return true if condition met within timeout
     */
    public boolean waitForNotificationsToAppear(int minimumCount) {
        return wait.waitForNotificationToAppear(NOTIFICATION_ITEMS, minimumCount);
    }

    /**
     * Returns the current unread count by reading the page subtitle text.
     * Falls back to counting .notif-item.unread elements if subtitle is absent.
     */
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

    /**
     * Scans all visible .notif-item elements for a notification whose text
     * contains ALL the given substrings (case-insensitive).
     */
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
                items = driver.findElements(NOTIFICATION_ITEMS); // re-query and continue
            }
        }
        return false;
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public boolean isOnNotificationsPage()        { return urlContains("/notifications"); }
    public boolean isPageHeadingDisplayed()       { return isDisplayed(PAGE_HEADING); }
    public boolean isAllCaughtUpMessageDisplayed(){ return isDisplayed(ALL_CAUGHT_UP_MSG); }
    public boolean isMarkAllReadButtonDisplayed() { return isDisplayed(MARK_ALL_READ_BTN); }

    public boolean isNoAlertsMessageDisplayed() {
        return isDisplayed(By.xpath("//*[contains(text(),'No alerts')]"));
    }

    public boolean isEmptyStateDisplayed() {
        return isAllCaughtUpMessageDisplayed() || isNoAlertsMessageDisplayed();
    }

    public List<WebElement> getNotificationItems() {
        return driver.findElements(NOTIFICATION_ITEMS);
    }

    public int getNotificationCount()  { return getNotificationItems().size(); }
    public boolean hasNotifications()  { return getNotificationCount() > 0; }
}
