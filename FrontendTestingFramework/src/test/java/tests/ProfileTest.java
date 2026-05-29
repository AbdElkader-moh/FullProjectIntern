package tests;

import base.BaseTest;
import io.qameta.allure.*;
import pages.ProfilePage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("User profile")
@Feature("Profile page")
public class ProfileTest extends BaseTest {

    private ProfilePage profilePage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void openProfile() {
        loginWithDefaultUser();
        profilePage = new ProfilePage(driver);
        profilePage.open();
    }

    @Test(description = "TC-024: Profile page shows all expected field labels and values")
    @Story("Profile data display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that .detail-value spans are present on the profile page, meaning user data has loaded correctly.")
    public void TC024_profileDisplaysAllFields() {
        Assert.assertFalse(profilePage.getDetailValues().isEmpty(),
                "TC-024 FAILED: No .detail-value spans found on the profile page.");
        System.out.println("TC-024 PASSED");
    }

    @Test(description = "TC-025: Password is displayed as masked dots, never plain text")
    @Story("Password security")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Checks that the .password-text span is visible and contains only bullet dot characters — the current password is never exposed as plain text.")
    public void TC025_passwordMaskedByDefault() {
        Assert.assertTrue(profilePage.isPasswordMasked(),
                "TC-025 FAILED: Password is not masked. "
                        + "Expected .password-text span with bullet dots only.");
        System.out.println("TC-025 PASSED");
    }

    @Test(description = "TC-026: Password visibility is permanently hidden — no toggle button exists")
    @Story("Password security")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Asserts that no .btn-toggle-password element exists in the DOM and that the .password-text masking span is present. Password must never be made visible.")
    public void TC026_togglePasswordVisibility() {
        Assert.assertTrue(profilePage.isPasswordPermanentlyHidden(),
                "TC-026 FAILED: Either .btn-toggle-password exists in the DOM, "
                        + "or the .password-text masking span is missing.");
        System.out.println("TC-026 PASSED");
    }

    @Test(description = "TC-027: Clicking avatar-wrapper triggers profile picture upload")
    @Story("Profile photo upload")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks the .avatar-wrapper div which should trigger triggerFileUpload() on the Angular component. Verifies the hidden file input is reachable afterward.")
    public void TC027_updateProfilePicture() {
        profilePage.clickUploadTrigger();
        Assert.assertFalse(
                driver.findElements(org.openqa.selenium.By.cssSelector("input[type='file']")).isEmpty(),
                "TC-027 FAILED: File input not found after clicking avatar-wrapper.");
        System.out.println("TC-027 PASSED");
    }

    @Test(description = "TC-028: Profile avatar/photo element is displayed")
    @Story("Profile data display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks that either the avatar image or the initials div is visible — at least one must be shown to represent the user.")
    public void TC028_profileShowsPhotoWhenSet() {
        Assert.assertTrue(profilePage.isProfilePictureDisplayed(),
                "TC-028 FAILED: Neither avatar-img nor avatar-initials is displayed.");
        System.out.println("TC-028 PASSED");
    }

    @Test(description = "TC-029: Clicking Logout navigates back to /signin")
    @Story("Logout")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Clicks the logout button on the profile page. Expects the browser to land on /signin.")
    public void TC029_logoutFromProfile() {
        profilePage.logout();
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-029 FAILED: Expected /signin after logout.");
        System.out.println("TC-029 PASSED");
    }

    @Test(description = "TC-030: Accessing /profile while unauthenticated redirects to /signin",
            priority = 10)
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deletes all cookies then navigates directly to /profile. Expects the Angular route guard to redirect to /signin.")
    public void TC030_accessProfileWithoutLogin() {
        driver.manage().deleteAllCookies();
        navigateTo("/profile");
        wait.waitForCondition(d -> d.getCurrentUrl().contains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-030 FAILED: Expected redirect to /signin but got: "
                        + driver.getCurrentUrl());
        System.out.println("TC-030 PASSED");
    }
}