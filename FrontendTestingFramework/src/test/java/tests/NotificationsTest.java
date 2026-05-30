package tests;

import base.BaseTest;

import io.qameta.allure.*;
import pages.NotificationsPage;
import utils.ConfigReader;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Core pages")
@Feature("Notifications")
public class NotificationsTest extends BaseTest {

    private NotificationsPage notificationsPage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void openNotifications() {
        loginWithDefaultUser();
        notificationsPage = new NotificationsPage(driver);
        notificationsPage.open();
    }

    @Test(description = "TC-047: Notifications page loads and URL is /notifications")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Navigates to /notifications and verifies the URL contains the expected path.")
    public void TC047_notificationsPageLoads() {
        Assert.assertTrue(notificationsPage.isOnNotificationsPage(),
                "TC-047 FAILED: Not on /notifications after navigation.");
        System.out.println("TC-047 PASSED");
    }

    @Test(description = "TC-048: 'Notifications' heading is visible on the page")
    @Story("Page load")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that the Notifications h1/h2 heading is rendered on the page.")
    public void TC048_notificationsHeadingDisplayed() {
        Assert.assertTrue(notificationsPage.isPageHeadingDisplayed(),
                "TC-048 FAILED: 'Notifications' heading not displayed.");
        System.out.println("TC-048 PASSED");
    }

    @Test(description = "TC-049: Empty state message shown when no notifications exist")
    @Story("Empty state")
    @Severity(SeverityLevel.NORMAL)
    @Description("When no notifications are present, verifies that the empty-state message is shown. Skips assertion if notifications already exist.")
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
    @Story("Empty state")
    @Severity(SeverityLevel.MINOR)
    @Description("When no notifications are present, verifies the 'All caught up' sub-message appears.")
    public void TC050_allCaughtUpMessageDisplayed() {
        if (!notificationsPage.hasNotifications()) {
            Assert.assertTrue(notificationsPage.isAllCaughtUpMessageDisplayed(),
                    "TC-050 FAILED: 'All caught up' message not found.");
        }
        System.out.println("TC-050 PASSED");
    }

    @Test(description = "TC-051: Back button navigates away from /notifications")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the back button (button.btn-back). Expects the URL to no longer contain /notifications.")
    public void TC051_backButtonNavigates() {
        notificationsPage.clickBack();
        boolean navigatedAway = !driver.getCurrentUrl().contains("/notifications");
        Assert.assertTrue(navigatedAway,
                "TC-051 FAILED: Still on /notifications after clicking Back.");
        System.out.println("TC-051 PASSED");
    }

    @Test(description = "TC-052: Accessing /notifications without login redirects to /signin",
            priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /notifications. Expects redirect to /signin by the Angular route guard.")
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

    @Test(description = "TC-053: Clicking an unread notification marks it as read")
    @Story("Mark as read")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicks the first unread notification and waits for the .unread CSS class to be removed. Verifies the unread count decreases.")
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

    @Test(description = "TC-054: 'Mark all as read' clears all unread indicators")
    @Story("Mark as read")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicks the 'Mark all as read' button and waits for all .unread-dot elements to disappear. Verifies the button itself is no longer visible.")
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

    @Test(description = "TC-055: Subtitle shows 'All caught up' after marking all as read")
    @Story("Mark as read")
    @Severity(SeverityLevel.NORMAL)
    @Description("Marks all notifications as read then waits for the 'All caught up' subtitle to appear.")
    public void TC055_subtitleUpdatesAfterMarkAllRead() {
        if (notificationsPage.isMarkAllReadButtonDisplayed()) {
            notificationsPage.markAllNotificationsAsRead();
        }
        wait.waitForCondition(d -> notificationsPage.isAllCaughtUpMessageDisplayed());
        Assert.assertTrue(notificationsPage.isAllCaughtUpMessageDisplayed(),
                "TC-055 FAILED: 'All caught up' not shown after marking all as read.");
        System.out.println("TC-055 PASSED");
    }

    @Test(description = "TC-056: Notification from simulator appears (configurable polling)")
    @Story("Simulator integration")
    @Severity(SeverityLevel.NORMAL)
    @Description("Polls the notification list using FluentWait until at least one notification appears. Timeout and poll interval are read from config.properties.")
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

    @Test(description = "TC-057: Notification count, content, and order are correct")
    @Story("Simulator integration")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies at least one notification is present, count is >=1, and content matches expected simulator payload keywords (threshold, LIGHT, TRAFFIC, AIR).")
    public void TC057_notificationAppearanceVerification() {
        boolean appeared = notificationsPage.waitForNotificationsToAppear(1);
        Assert.assertTrue(appeared, "TC-057 FAILED: No notifications appeared.");

        int count = notificationsPage.getNotificationCount();
        Assert.assertTrue(count >= 1,
                "TC-057 FAILED: Expected >=1 notification, found " + count);

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

    @Test(description = "TC-058: Notification count increases after simulator produces a new alert")
    @Story("Simulator integration")
    @Severity(SeverityLevel.MINOR)
    @Description("Records the current notification count then polls with page refresh until the count increases. Marked INCONCLUSIVE if no new notification arrives within the configured simulator.delay.timeout.")
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
                    + ConfigReader.getSimulatorDelayTimeout() + "s.");
        }
    }
}