package tests;

import base.BaseTest;
import data.TestDataProvider;
import pages.SignInPage;
import pages.SignUpPage;
import utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * SignUpTest – Sprint 1 coverage (TC-001 … TC-014)
 *
 * Each test opens a fresh /signup page (BeforeMethod) so tests are independent.
 * No Thread.sleep(), no hardcoded credentials.
 */
public class SignUpTest extends BaseTest {

    private SignUpPage signUpPage;

    @BeforeMethod
    public void openSignUp() {
        signUpPage = new SignUpPage(driver);
        signUpPage.open();
    }

    // ─── TC-001: Sign up without a photo should be blocked ───────────────────

    @Test(description = "TC-001: Signup blocked when no profile photo is provided")
    public void TC001_signUpNoPhoto() {
        signUpPage.enterFirstName(TestDataProvider.validFirstName())
                  .enterLastName(TestDataProvider.validLastName())
                  .enterEmail(TestDataProvider.generateUniqueEmail("tc001"))
                  .enterPassword(TestDataProvider.validPassword())
                  .clickCreateAccount();

        Assert.assertTrue(signUpPage.staysOnSignUp(),
                "TC-001 FAILED: Should not redirect to /signin without a profile photo.");
        System.out.println("TC-001 PASSED");
    }

    // ─── TC-002: Sign up with all valid fields and photo ─────────────────────

    @Test(description = "TC-002: Successful signup with all valid fields and a photo")
    public void TC002_signUpWithPhoto() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                "Nadin",
                "Abdelaal",
                TestDataProvider.generateUniqueEmail("tc002"),
                TestDataProvider.minLengthPassword(),
                imagePath
        );

        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-002 FAILED: Expected redirect to /signin after successful signup.");
        System.out.println("TC-002 PASSED");
    }

    // ─── TC-003: First name exactly 2 characters ──────────────────────────────

    @Test(description = "TC-003: Signup accepted with first name of exactly 2 characters")
    public void TC003_firstNameTwoChars() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                TestDataProvider.minLengthFirstName(),
                TestDataProvider.validLastName(),
                TestDataProvider.generateUniqueEmail("tc003"),
                TestDataProvider.minLengthPassword(),
                imagePath
        );

        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-003 FAILED: 2-char first name should be accepted.");
        System.out.println("TC-003 PASSED");
    }

    // ─── TC-004: Last name exactly 2 characters ───────────────────────────────

    @Test(description = "TC-004: Signup accepted with last name of exactly 2 characters")
    public void TC004_lastNameTwoChars() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                TestDataProvider.validFirstName(),
                TestDataProvider.minLengthLastName(),
                TestDataProvider.generateUniqueEmail("tc004"),
                TestDataProvider.minLengthPassword(),
                imagePath
        );

        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-004 FAILED: 2-char last name should be accepted.");
        System.out.println("TC-004 PASSED");
    }

    // ─── TC-005: Password exactly 6 characters ────────────────────────────────

    @Test(description = "TC-005: Signup accepted with password of exactly 6 characters")
    public void TC005_passwordSixChars() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                TestDataProvider.validFirstName(),
                TestDataProvider.validLastName(),
                TestDataProvider.generateUniqueEmail("tc005"),
                TestDataProvider.minLengthPassword(),  // exactly 6 chars
                imagePath
        );

        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-005 FAILED: 6-char password should be accepted.");
        System.out.println("TC-005 PASSED");
    }

    // ─── TC-006: All fields empty ─────────────────────────────────────────────

    @Test(description = "TC-006: Validation error shown when all fields are empty")
    public void TC006_allFieldsEmpty() {
        signUpPage.clickCreateAccount();

        Assert.assertTrue(signUpPage.isFirstNameErrorDisplayed(),
                "TC-006 FAILED: Expected 'First name is required' error.");
        System.out.println("TC-006 PASSED");
    }

    // ─── TC-007: First name exactly 1 character ───────────────────────────────

    @Test(description = "TC-007: Validation error when first name is only 1 character")
    public void TC007_firstNameOneChar() {
        signUpPage.enterFirstName(TestDataProvider.tooShortFirstName())
                  .clickCreateAccount();

        Assert.assertTrue(signUpPage.isFirstNameErrorDisplayed(),
                "TC-007 FAILED: Expected min-length error for first name.");
        System.out.println("TC-007 PASSED");
    }

    // ─── TC-008: Last name exactly 1 character ────────────────────────────────

    @Test(description = "TC-008: Validation error when last name is only 1 character")
    public void TC008_lastNameOneChar() {
        signUpPage.enterLastName(TestDataProvider.tooShortLastName())
                  .clickCreateAccount();

        Assert.assertTrue(signUpPage.isLastNameErrorDisplayed(),
                "TC-008 FAILED: Expected min-length error for last name.");
        System.out.println("TC-008 PASSED");
    }

    // ─── TC-009: Password exactly 5 characters ────────────────────────────────

    @Test(description = "TC-009: Validation error when password is only 5 characters")
    public void TC009_passwordFiveChars() {
        signUpPage.enterPassword(TestDataProvider.tooShortPassword())
                  .clickCreateAccount();

        Assert.assertTrue(signUpPage.isPasswordErrorDisplayed(),
                "TC-009 FAILED: Expected 'at least 6 characters' error.");
        System.out.println("TC-009 PASSED");
    }

    // ─── TC-010: Invalid email missing @ ──────────────────────────────────────

    @Test(description = "TC-010: Validation error for email missing @ symbol")
    public void TC010_invalidEmail() {
        signUpPage.enterEmail(TestDataProvider.invalidEmailNoAtSymbol())
                  .clickCreateAccount();

        Assert.assertTrue(signUpPage.isEmailErrorDisplayed(),
                "TC-010 FAILED: Expected 'valid email' error.");
        System.out.println("TC-010 PASSED");
    }

    // ─── TC-011: Invalid email missing domain after @ ─────────────────────────

    @Test(description = "TC-011: Validation error for email with no domain after @")
    public void TC011_invalidEmailMissingDomain() {
        signUpPage.enterEmail(TestDataProvider.invalidEmailNoAfterAt());
        // Trigger blur to fire validation
        signUpPage.enterFirstName("T");

        Assert.assertTrue(signUpPage.isEmailErrorDisplayed(),
                "TC-011 FAILED: Expected 'valid email' error.");
        System.out.println("TC-011 PASSED");
    }

    // ─── TC-012: Upload photo then remove it ─────────────────────────────────

    @Test(description = "TC-012: Uploaded photo can be removed, restoring upload area")
    public void TC012_uploadImageThenRemove() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.uploadPhoto(imagePath);

        Assert.assertTrue(signUpPage.isRemoveButtonDisplayed(),
                "TC-012 FAILED: Remove button should appear after upload.");

        signUpPage.clickRemovePhoto();

        Assert.assertTrue(signUpPage.isUploadAreaDisplayed(),
                "TC-012 FAILED: Upload area should be restored after removal.");
        System.out.println("TC-012 PASSED");
    }

    // ─── TC-013: Duplicate email ─────────────────────────────────────────────

    @Test(description = "TC-013: Error shown when trying to register with an already-used email")
    public void TC013_duplicateEmail() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                "Nada",
                "Fouad",
                ConfigReader.getDuplicateEmail(),   // existing email from config
                TestDataProvider.minLengthPassword(),
                imagePath
        );

        Assert.assertTrue(signUpPage.isDuplicateEmailErrorDisplayed(),
                "TC-013 FAILED: Expected duplicate-email error message.");
        System.out.println("TC-013 PASSED");
    }

    // ─── TC-014: Navigate to sign-in from sign-up ────────────────────────────

    @Test(description = "TC-014: 'Sign in' link navigates from signup to signin page")
    public void TC014_navigateToSignIn() {
        SignInPage signInPage = signUpPage.clickSignInLink();

        Assert.assertTrue(signInPage.isOnSignInPage(),
                "TC-014 FAILED: Expected redirect to /signin.");
        System.out.println("TC-014 PASSED");
    }
}
