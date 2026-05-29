package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.HomePage;
import pages.NotificationsPage;
import pages.ProfilePage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Core pages")
@Feature("Home page")
public class HomeTest extends BaseTest {

    private HomePage homePage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void openHome() {
        loginWithDefaultUser();
        homePage = new HomePage(driver);
        homePage.open();
    }

    @Test(description = "TC-031: Profile avatar is visible in the home page header")
    @Story("Header elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that the profile avatar link is displayed in the home page header after login.")
    public void TC031_homeShowsProfilePicture() {
        Assert.assertTrue(homePage.isProfileAvatarDisplayed(),
                "TC-031 FAILED: Profile avatar not displayed on home page.");
        System.out.println("TC-031 PASSED");
    }

    @Test(description = "TC-032: Home page is accessible and URL is /home")
    @Story("Page load")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verifies that after login the URL contains /home, confirming the home page loaded correctly.")
    public void TC032_homePageLoads() {
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-032 FAILED: Not on /home after navigation.");
        System.out.println("TC-032 PASSED");
    }

    @Test(description = "TC-033: Notifications link is visible on home page")
    @Story("Header elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that the notification bell link is present and enabled in the home page header.")
    public void TC033_notificationBellDisplayed() {
        Assert.assertTrue(homePage.isNotificationBellDisplayed(),
                "TC-033 FAILED: Notification bell not found.");
        System.out.println("TC-033 PASSED");
    }

    @Test(description = "TC-034: Clicking avatar navigates to /profile")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the profile avatar in the header. Expects navigation to /profile.")
    public void TC034_navigateToProfileViaAvatar() {
        ProfilePage profilePage = homePage.clickProfileAvatar();
        Assert.assertTrue(profilePage.isOnProfilePage(),
                "TC-034 FAILED: Expected navigation to /profile.");
        System.out.println("TC-034 PASSED");
    }

    @Test(description = "TC-035: Clicking notification bell navigates to /notifications")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the notification bell in the header. Expects navigation to /notifications.")
    public void TC035_navigateToNotifications() {
        NotificationsPage notificationsPage = homePage.clickNotificationBell();
        Assert.assertTrue(notificationsPage.isOnNotificationsPage(),
                "TC-035 FAILED: Expected navigation to /notifications.");
        System.out.println("TC-035 PASSED");
    }

    @Test(description = "TC-036: Accessing /home while unauthenticated redirects to /signin",
            priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates to /home. Expects the Angular route guard to redirect to /signin.")
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