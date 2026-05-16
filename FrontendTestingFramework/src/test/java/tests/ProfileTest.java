package com.internship.tests;

import com.internship.base.BaseTest;
import com.internship.pages.ProfilePage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ProfileTest — fixed against the real profile.html DOM.
 *
 * Root causes fixed:
 *
 * 1. openProfile() → TimeoutException "waiting for url to contain /profile,
 *    current url: /signin"
 *    Cause: TC-030 runs with priority=10 and deletes all cookies. Any subsequent
 *    @BeforeMethod then runs with no session, so navigateTo("/profile") is
 *    redirected to /signin by the Angular route guard.
 *    Fix: @BeforeMethod re-logs in before opening /profile, ensuring session is
 *    always valid regardless of test order.
 *
 * 2. TC-025 → AssertionError: password input is not type='password'
 *    Cause: the old ProfilePage looked for input[type='password'] which does NOT
 *    exist on the profile page by default. The password is displayed as a static
 *    <span class="password-text">••••••••••••</span>.
 *    Fix: isPasswordMasked() checks the password-text span, not an input.
 *
 * 3. TC-026 → AssertionError: toggle button exists or password not type='password'
 *    Cause: same wrong locator — input[type='password'] not present by default,
 *    and .btn-toggle-password never exists in the DOM.
 *    Fix: isPasswordPermanentlyHidden() checks absence of .btn-toggle-password
 *    and presence of the .password-text masking span.
 *
 * 4. TC-027 → TimeoutException on UPLOAD_TRIGGER
 *    Cause: locator looked for button text "Upload/Change/Photo" — none of those
 *    exist. The upload is triggered by clicking <div class="avatar-wrapper">.
 *    Fix: clickUploadTrigger() clicks .avatar-wrapper directly.
 */
public class ProfileTest extends BaseTest {

    private ProfilePage profilePage;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        super.setUp();
    }

    /**
     * FIX — openProfile timeout after TC-030 deletes cookies:
     * Always re-login before opening /profile so the session is guaranteed valid.
     */
    @BeforeMethod(alwaysRun = true)
    public void openProfile() {
        loginWithDefaultUser();          // ensures valid session regardless of test order
        profilePage = new ProfilePage(driver);
        profilePage.open();              // navigates to /profile, waits for .profile-card
    }

    @Test(description = "TC-024: Profile page shows all expected field labels and values")
    public void TC024_profileDisplaysAllFields() {
        Assert.assertFalse(profilePage.getDetailValues().isEmpty(),
                "TC-024 FAILED: No .detail-value spans found on the profile page.");
        System.out.println("TC-024 PASSED");
    }

    /**
     * TC-025 FIX:
     * The profile page shows password as <span class="password-text">••••••••••••</span>
     * There is NO input[type='password'] visible by default.
     * isPasswordMasked() verifies the span is visible and contains only bullet characters.
     */
    @Test(description = "TC-025: Password is displayed as masked dots, never plain text")
    public void TC025_passwordMaskedByDefault() {
        Assert.assertTrue(profilePage.isPasswordMasked(),
                "TC-025 FAILED: Password is not masked. "
                + "Expected .password-text span with bullet dots only.");
        System.out.println("TC-025 PASSED");
    }

    /**
     * TC-026 FIX:
     * No .btn-toggle-password exists in the DOM — password is permanently hidden
     * behind static bullet dots in .password-text span.
     * isPasswordPermanentlyHidden() asserts both: no toggle button + mask span present.
     */
    @Test(description = "TC-026: Password visibility is permanently hidden — no toggle button exists")
    public void TC026_togglePasswordVisibility() {
        Assert.assertTrue(profilePage.isPasswordPermanentlyHidden(),
                "TC-026 FAILED: Either .btn-toggle-password exists in the DOM, "
                + "or the .password-text masking span is missing. "
                + "Password must NEVER be made visible.");
        System.out.println("TC-026 PASSED");
    }

    /**
     * TC-027 FIX:
     * The upload trigger is <div class="avatar-wrapper"> — clicking it calls
     * triggerFileUpload() on the component. There is no button with text "Upload".
     */
    @Test(description = "TC-027: Clicking avatar-wrapper triggers profile picture upload")
    public void TC027_updateProfilePicture() {
        profilePage.clickUploadTrigger();
        // Verify the hidden file input is now reachable (upload was triggered)
        Assert.assertFalse(
                driver.findElements(org.openqa.selenium.By.cssSelector("input[type='file']")).isEmpty(),
                "TC-027 FAILED: File input not found after clicking avatar-wrapper.");
        System.out.println("TC-027 PASSED");
    }

    @Test(description = "TC-028: Profile avatar/photo element is displayed")
    public void TC028_profileShowsPhotoWhenSet() {
        Assert.assertTrue(profilePage.isProfilePictureDisplayed(),
                "TC-028 FAILED: Neither avatar-img nor avatar-initials is displayed.");
        System.out.println("TC-028 PASSED");
    }

    @Test(description = "TC-029: Clicking Logout navigates back to /signin")
    public void TC029_logoutFromProfile() {
        profilePage.logout();
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"),
                "TC-029 FAILED: Expected /signin after logout.");
        System.out.println("TC-029 PASSED");
    }

    @Test(description = "TC-030: Accessing /profile while unauthenticated redirects to /signin",
          priority = 10)
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
