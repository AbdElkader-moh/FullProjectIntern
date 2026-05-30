package tests;

import base.BaseTest;
<<<<<<< HEAD
import pages.HomePage;
import pages.NotificationsPage;
import pages.ProfilePage;
=======
import io.qameta.allure.*;
import pages.HomePage;
import pages.NotificationsPage;
import pages.ProfilePage;
import pages.TrafficDashboardPage;
>>>>>>> b485ec14d5e88360bd0794f0fa63bdb60e3edea4
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * HomeTest — updated for Sprint 3.
 *
 * Added TC-036b: clicking the Traffic Monitoring card navigates to /traffic.
 * The home page now has 3 dashboard cards (Traffic, Street Light, Air Pollution)
 * driven by the dashboardCards array in home.ts. Existing TC-031 to TC-036 unchanged.
 */
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
    @Description("Verifies that after login the URL contains /home.")
    public void TC032_homePageLoads() {
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-032 FAILED: Not on /home after navigation.");
        System.out.println("TC-032 PASSED");
    }

    @Test(description = "TC-033: Notifications link is visible on home page")
    @Story("Header elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that the notification bell link is present and enabled in the header.")
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

    /**
     * TC-036b — Sprint 3 addition.
     * The home page now shows 3 dashboard cards. This test verifies the Traffic
     * Monitoring card is present and clickable, and navigates to /traffic.
     * Locator: .dashboard-card containing the text "Traffic Monitoring".
     */
    @Test(description = "TC-036b: Clicking the Traffic Monitoring dashboard card navigates to /traffic")
    @Story("Dashboard cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Finds the Traffic Monitoring card on the home page by its title text, clicks it, and verifies navigation to /traffic.")
    public void TC036b_trafficCardNavigatesToDashboard() {
        // Find the Traffic Monitoring card by title text
        org.openqa.selenium.By trafficCard = org.openqa.selenium.By.xpath(
                "//div[contains(@class,'dashboard-card') and .//h2[contains(text(),'Traffic')]]"
        );
        wait.waitForClickable(trafficCard).click();
        wait.waitForUrlToContain("/traffic");

        TrafficDashboardPage trafficPage = new TrafficDashboardPage(driver);
        Assert.assertTrue(trafficPage.isOnTrafficDashboard(),
                "TC-036b FAILED: Expected navigation to /traffic after clicking Traffic card.");
        System.out.println("TC-036b PASSED");
    }
}