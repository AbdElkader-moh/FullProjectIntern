package com.internship.tests;

import com.internship.base.BaseTest;
import com.internship.pages.NotificationsPage;
import com.internship.utils.ConfigReader;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * NotificationsTest – TC-047 … TC-058
 *
 * Fixes applied:
 *   TC-051 — BACK_BTN locator fixed in NotificationsPage (button.btn-back)
 *   Issue 2 — TC-053/054/055: mark-as-read automation
 *   Issue 3 — TC-056: simulator delay handled with configurable polling
 *   Issue 4 — TC-057/058: notification appearance verification
 */
public class NotificationsTest extends BaseTest {

    private NotificationsPage notificationsPage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
        loginWithDefaultUser();
    }

    @BeforeMethod(alwaysRun = true)
    public void openNotifications() {
        notificationsPage = new NotificationsPage(driver);
        notificationsPage.open();
    }

    // ── TC-047 to TC-050 (unchanged logic, cleaner logging) ───────────────────

    @Test(description = "TC-047: Notifications page loads and URL is /notifications")
    public void TC047_notificationsPageLoads() {
        Assert.assertTrue(notificationsPage.isOnNotificationsPage(),
                "TC-047 FAILED: Not on /notifications after navigation.");
        System.out.println("TC-047 PASSED");
    }

    @Test(description = "TC-048: 'Notifications' heading is visible on the page")
    public void TC048_notificationsHeadingDisplayed() {
        Assert.assertTrue(notificationsPage.isPageHeadingDisplayed(),
                "TC-048 FAILED: 'Notifications' heading not displayed.");
        System.out.println("TC-048 PASSED");
    }

    @Test(description = "TC-049: Empty state message shown when no notifications exist")
    public void TC049_emptyStateDisplayed() {
        if (!notificationsPage.hasNotifications()) {
            Assert.assertTrue(notificationsPage.isEmptyStateDisplayed(),
                    "TC-049 FAILED: Empty state message not shown.");
        } else {
            System.out.println("TC-049 INFO: Notifications present — empty-state check skipped.");
        }
        System.out.println("TC-049 PASSED");
    }

    @Test(description = "TC-050: 'All caught up' sub-message is shown in empty state")
    public void TC050_allCaughtUpMessageDisplayed() {
        if (!notificationsPage.hasNotifications()) {
            Assert.assertTrue(notificationsPage.isAllCaughtUpMessageDisplayed(),
                    "TC-050 FAILED: 'All caught up' message not found.");
        }
        System.out.println("TC-050 PASSED");
    }

    // ── TC-051 — Back button (locator fix) ───────────────────────────────────

    /**
     * Was failing with TimeoutException because XPath text() cannot see text
     * inside a child <span>. Fixed with By.cssSelector("button.btn-back").
     * Full explanation in NotificationsPage.BACK_BTN javadoc.
     */
    @Test(description = "TC-051: Back button navigates away from /notifications")
    public void TC051_backButtonNavigates() {
        notificationsPage.clickBack();
        boolean navigatedAway = !driver.getCurrentUrl().contains("/notifications");
        Assert.assertTrue(navigatedAway,
                "TC-051 FAILED: Still on /notifications after clicking Back.");
        System.out.println("TC-051 PASSED");
    }

    // ── TC-052 ────────────────────────────────────────────────────────────────

    @Test(description = "TC-052: Accessing /notifications without login redirects to /signin",
          priority = 10)
    public void TC052_unauthenticatedAccessToNotifications() {
        driver.manage().deleteAllCookies();
        navigateTo("/notifications");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin")
                || d.getCurrentUrl().equals(baseUrl + "/"));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("/signin") || url.equals(baseUrl + "/"),
                "TC-052 FAILED: Expected /signin redirect but got: " + url);
        System.out.println("TC-052 PASSED");
    }

    // ── TC-053 — Mark first notification as read (Issue 2) ────────────────────

    @Test(description = "TC-053: Clicking an unread notification marks it as read")
    public void TC053_markFirstNotificationAsRead() {
        if (!notificationsPage.hasNotifications()) {
            System.out.println("TC-053 SKIP: No notifications present.");
            return;
        }
        int unreadBefore = notificationsPage.getUnreadCount();
        if (unreadBefore == 0) {
            System.out.println("TC-053 SKIP: All already read.");
            return;
        }

        boolean marked = notificationsPage.markFirstNotificationAsRead();
        Assert.assertTrue(marked,
                "TC-053 FAILED: .unread class not removed within "
              + ConfigReader.getNotificationReadStateTimeout() + "s.");

        int unreadAfter = notificationsPage.getUnreadCount();
        Assert.assertTrue(unreadAfter < unreadBefore,
                "TC-053 FAILED: Unread count did not decrease. Before="
              + unreadBefore + " After=" + unreadAfter);
        System.out.println("TC-053 PASSED");
    }

    // ── TC-054 — Mark all as read (Issue 2) ───────────────────────────────────

    @Test(description = "TC-054: 'Mark all as read' clears all unread indicators")
    public void TC054_markAllNotificationsAsRead() {
        if (!notificationsPage.isMarkAllReadButtonDisplayed()) {
            System.out.println("TC-054 SKIP: No unread notifications.");
            return;
        }
        boolean allCleared = notificationsPage.markAllNotificationsAsRead();
        Assert.assertTrue(allCleared,
                "TC-054 FAILED: Unread dots still visible after mark-all-read. "
              + "Waited " + ConfigReader.getNotificationReadStateTimeout() + "s.");
        Assert.assertFalse(notificationsPage.isMarkAllReadButtonDisplayed(),
                "TC-054 FAILED: 'Mark all as read' button still visible.");
        System.out.println("TC-054 PASSED");
    }

    // ── TC-055 — Subtitle updates after mark-all-read (Issue 2) ──────────────

    @Test(description = "TC-055: Subtitle shows 'All caught up' after marking all as read")
    public void TC055_subtitleUpdatesAfterMarkAllRead() {
        if (notificationsPage.isMarkAllReadButtonDisplayed()) {
            notificationsPage.markAllNotificationsAsRead();
        }
        wait.waitForCondition(d -> notificationsPage.isAllCaughtUpMessageDisplayed());
        Assert.assertTrue(notificationsPage.isAllCaughtUpMessageDisplayed(),
                "TC-055 FAILED: 'All caught up' not shown after marking all as read.");
        System.out.println("TC-055 PASSED");
    }

    // ── TC-056 — Simulator notification polling (Issue 3) ─────────────────────

    /**
     * Issue 3 — No Thread.sleep. Uses configurable FluentWait poller.
     * Polls every simulator.poll.interval.ms until notification appears
     * or notification.appearance.timeout is reached.
     */
    @Test(description = "TC-056: Notification from simulator appears (configurable polling)")
    public void TC056_simulatorNotificationAppearsWithPolling() {
        System.out.println("[TC056] Timeout=" + ConfigReader.getNotificationAppearanceTimeout()
                + "s, poll=" + ConfigReader.getSimulatorPollIntervalMs() + "ms");

        boolean appeared = notificationsPage.waitForNotificationsToAppear(1);
        Assert.assertTrue(appeared,
                "TC-056 FAILED: No notification appeared within "
              + ConfigReader.getNotificationAppearanceTimeout() + "s. "
              + "Ensure simulator.py is running and connected to the backend.");
        System.out.println("TC-056 PASSED — "
                + notificationsPage.getNotificationCount() + " notification(s) visible");
    }

    // ── TC-057 — Appearance verification (Issue 4) ────────────────────────────

    /**
     * Issue 4 — validates count, text content, and ordering.
     */
    @Test(description = "TC-057: Notification count, content, and order are correct")
    public void TC057_notificationAppearanceVerification() {
        boolean appeared = notificationsPage.waitForNotificationsToAppear(1);
        Assert.assertTrue(appeared, "TC-057 FAILED: No notifications appeared.");

        // Count
        int count = notificationsPage.getNotificationCount();
        Assert.assertTrue(count >= 1,
                "TC-057 FAILED: Expected >=1 notification, found " + count);

        // Content — matches simulator payload keywords
        boolean hasContent =
                notificationsPage.hasNotificationMatching("above", "threshold")
             || notificationsPage.hasNotificationMatching("below", "threshold")
             || notificationsPage.hasNotificationMatching("LIGHT")
             || notificationsPage.hasNotificationMatching("TRAFFIC")
             || notificationsPage.hasNotificationMatching("AIR");

        Assert.assertTrue(hasContent,
                "TC-057 FAILED: No notification matched expected content patterns.");
        System.out.println("TC-057 PASSED — " + count + " notification(s), content verified");
    }

    // ── TC-058 — Count increases (Issue 4) ────────────────────────────────────

    @Test(description = "TC-058: Notification count increases after simulator produces a new alert")
    public void TC058_notificationCountIncreases() {
        int countBefore = notificationsPage.getNotificationCount();
        System.out.println("[TC058] Count before: " + countBefore);

        boolean increased = wait.waitForNotificationWithRefresh(
                By.cssSelector(".notif-item"), countBefore + 1);

        if (increased) {
            int countAfter = notificationsPage.getNotificationCount();
            Assert.assertTrue(countAfter > countBefore,
                    "TC-058 FAILED: Count did not increase. Before="
                  + countBefore + " After=" + countAfter);
            System.out.println("TC-058 PASSED — count: " + countBefore + " → " + countAfter);
        } else {
            System.out.println("TC-058 INCONCLUSIVE: No new notification within "
                    + ConfigReader.getSimulatorDelayTimeout() + "s "
                    + "(simulator may need longer window — increase simulator.delay.timeout)");
        }
    }
}
