package tests;

import base.BaseTest;
import pages.HomePage;
import pages.NotificationsPage;
import pages.ProfilePage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HomeTest extends BaseTest {

    private HomePage homePage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    /**
     * FIX — openHome TimeoutException:
     *
     * Root cause: @BeforeClass called loginWithDefaultUser() exactly once.
     * TC-036 (priority=10) deletes all cookies mid-suite. Any @BeforeMethod
     * that runs afterwards navigates to /home with no session, gets redirected
     * to /signin, and the waitForPageToLoad condition times out at 10 s.
     *
     * Fix: call loginWithDefaultUser() in @BeforeMethod so every test starts
     * with a guaranteed valid session. loginWithDefaultUser() should be
     * idempotent (skip login if already on /home).
     */
    @BeforeMethod(alwaysRun = true)
    public void openHome() {
        loginWithDefaultUser();
        homePage = new HomePage(driver);
        homePage.open();
    }

    @Test(description = "TC-031: Profile avatar is visible in the home page header")
    public void TC031_homeShowsProfilePicture() {
        Assert.assertTrue(homePage.isProfileAvatarDisplayed(),
                "TC-031 FAILED: Profile avatar not displayed on home page.");
        System.out.println("TC-031 PASSED");
    }

    @Test(description = "TC-032: Home page is accessible and URL is /home")
    public void TC032_homePageLoads() {
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-032 FAILED: Not on /home after navigation.");
        System.out.println("TC-032 PASSED");
    }

    @Test(description = "TC-033: Notifications link is visible on home page")
    public void TC033_notificationBellDisplayed() {
        Assert.assertTrue(homePage.isNotificationBellDisplayed(),
                "TC-033 FAILED: Notification bell not found.");
        System.out.println("TC-033 PASSED");
    }

    @Test(description = "TC-034: Clicking avatar navigates to /profile")
    public void TC034_navigateToProfileViaAvatar() {
        ProfilePage profilePage = homePage.clickProfileAvatar();
        Assert.assertTrue(profilePage.isOnProfilePage(),
                "TC-034 FAILED: Expected navigation to /profile.");
        System.out.println("TC-034 PASSED");
    }

    @Test(description = "TC-035: Clicking notification bell navigates to /notifications")
    public void TC035_navigateToNotifications() {
        NotificationsPage notificationsPage = homePage.clickNotificationBell();
        Assert.assertTrue(notificationsPage.isOnNotificationsPage(),
                "TC-035 FAILED: Expected navigation to /notifications.");
        System.out.println("TC-035 PASSED");
    }

    /**
     * TC-036: Unauthenticated access — runs at priority=10 (last).
     * Deletes cookies then verifies redirect to /signin.
     * Does NOT need loginWithDefaultUser() because it intentionally tests
     * the unauthenticated state. The @BeforeMethod re-login runs first,
     * then this test deletes the cookies again for its own assertion.
     */
    @Test(description = "TC-036: Accessing /home while unauthenticated redirects to /signin",
          priority = 10)
    public void TC036_unauthenticatedAccessToHome() {
        driver.manage().deleteAllCookies();
        navigateTo("/home");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("/signin"),
                "TC-036 FAILED: Expected redirect to /signin but got: " + url);
        System.out.println("TC-036 PASSED");
    }
}
