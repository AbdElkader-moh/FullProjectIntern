package tests;

import base.BaseTest;
import data.TestDataProvider;

import io.qameta.allure.*;

import pages.SignInPage;
import pages.SignUpPage;
import utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Authentication")
@Feature("Sign up")
public class SignUpTest extends BaseTest {

    private SignUpPage signUpPage;

    @BeforeMethod(alwaysRun = true)
    public void openSignUp() {
        signUpPage = new SignUpPage(driver);
        signUpPage.open();
    }

    @Test(description = "TC-001: Signup blocked when no profile photo is provided")
    @Story("Photo requirement")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Fills all fields with valid data but skips the photo upload. Expects the form to stay on /signup and not redirect.")
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

    @Test(description = "TC-002: Successful signup with all valid fields and a photo", groups = {"sanity"})
    @Story("Happy path registration")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Submits the signup form with all valid inputs including a photo. Expects redirect to /signin.")
    public void TC002_signUpWithPhoto() {
        String imagePath = ConfigReader.getSignupImagePath();
        signUpPage.submitRegistration(
                "Nadin", "Abdelaal",
                TestDataProvider.generateUniqueEmail("tc002"),
                TestDataProvider.minLengthPassword(),
                imagePath
        );
        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-002 FAILED: Expected redirect to /signin after successful signup.");
        System.out.println("TC-002 PASSED");
    }

    @Test(description = "TC-003: Signup accepted with first name of exactly 2 characters")
    @Story("Boundary values — name length")
    @Severity(SeverityLevel.NORMAL)
    @Description("First name is exactly 2 characters (minimum allowed). Expects successful registration.")
    public void TC003_firstNameTwoChars() {
        signUpPage.submitRegistration(
                TestDataProvider.minLengthFirstName(),
                TestDataProvider.validLastName(),
                TestDataProvider.generateUniqueEmail("tc003"),
                TestDataProvider.minLengthPassword(),
                ConfigReader.getSignupImagePath()
        );
        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-003 FAILED: 2-char first name should be accepted.");
        System.out.println("TC-003 PASSED");
    }

    @Test(description = "TC-004: Signup accepted with last name of exactly 2 characters")
    @Story("Boundary values — name length")
    @Severity(SeverityLevel.NORMAL)
    @Description("Last name is exactly 2 characters (minimum allowed). Expects successful registration.")
    public void TC004_lastNameTwoChars() {
        signUpPage.submitRegistration(
                TestDataProvider.validFirstName(),
                TestDataProvider.minLengthLastName(),
                TestDataProvider.generateUniqueEmail("tc004"),
                TestDataProvider.minLengthPassword(),
                ConfigReader.getSignupImagePath()
        );
        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-004 FAILED: 2-char last name should be accepted.");
        System.out.println("TC-004 PASSED");
    }

    @Test(description = "TC-005: Signup accepted with password of exactly 6 characters")
    @Story("Boundary values — password length")
    @Severity(SeverityLevel.NORMAL)
    @Description("Password is exactly 6 characters (minimum allowed). Expects successful registration.")
    public void TC005_passwordSixChars() {
        signUpPage.submitRegistration(
                TestDataProvider.validFirstName(),
                TestDataProvider.validLastName(),
                TestDataProvider.generateUniqueEmail("tc005"),
                TestDataProvider.minLengthPassword(),
                ConfigReader.getSignupImagePath()
        );
        Assert.assertTrue(signUpPage.redirectedToSignIn(),
                "TC-005 FAILED: 6-char password should be accepted.");
        System.out.println("TC-005 PASSED");
    }

    @Test(description = "TC-006: Validation error shown when all fields are empty")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submits the form with all fields empty. Expects a first-name-required error to appear.")
    public void TC006_allFieldsEmpty() {
        signUpPage.clickCreateAccount();
        Assert.assertTrue(signUpPage.isFirstNameErrorDisplayed(),
                "TC-006 FAILED: Expected 'First name is required' error.");
        System.out.println("TC-006 PASSED");
    }

    @Test(description = "TC-007: Validation error when first name is only 1 character")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters a single character as first name. Expects a min-length validation error.")
    public void TC007_firstNameOneChar() {
        signUpPage.enterFirstName(TestDataProvider.tooShortFirstName())
                .clickCreateAccount();
        Assert.assertTrue(signUpPage.isFirstNameErrorDisplayed(),
                "TC-007 FAILED: Expected min-length error for first name.");
        System.out.println("TC-007 PASSED");
    }

    @Test(description = "TC-008: Validation error when last name is only 1 character")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters a single character as last name. Expects a min-length validation error.")
    public void TC008_lastNameOneChar() {
        signUpPage.enterLastName(TestDataProvider.tooShortLastName())
                .clickCreateAccount();
        Assert.assertTrue(signUpPage.isLastNameErrorDisplayed(),
                "TC-008 FAILED: Expected min-length error for last name.");
        System.out.println("TC-008 PASSED");
    }

    @Test(description = "TC-009: Validation error when password is only 5 characters")
    @Story("Boundary values — password length")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters a 5-character password (one below the minimum). Expects a min-length validation error.")
    public void TC009_passwordFiveChars() {
        signUpPage.enterPassword(TestDataProvider.tooShortPassword())
                .clickCreateAccount();
        Assert.assertTrue(signUpPage.isPasswordErrorDisplayed(),
                "TC-009 FAILED: Expected 'at least 6 characters' error.");
        System.out.println("TC-009 PASSED");
    }

    @Test(description = "TC-010: Validation error for email missing @ symbol")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters an email address without the @ symbol. Expects an email format validation error.")
    public void TC010_invalidEmail() {
        signUpPage.enterEmail(TestDataProvider.invalidEmailNoAtSymbol())
                .clickCreateAccount();
        Assert.assertTrue(signUpPage.isEmailErrorDisplayed(),
                "TC-010 FAILED: Expected 'valid email' error.");
        System.out.println("TC-010 PASSED");
    }

    @Test(description = "TC-011: Validation error for email with no domain after @")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters an email that stops at the @ with no domain after it. Expects a format validation error.")
    public void TC011_invalidEmailMissingDomain() {
        signUpPage.enterEmail(TestDataProvider.invalidEmailNoAfterAt());
        signUpPage.enterFirstName("T");
        Assert.assertTrue(signUpPage.isEmailErrorDisplayed(),
                "TC-011 FAILED: Expected 'valid email' error.");
        System.out.println("TC-011 PASSED");
    }

    @Test(description = "TC-012: Uploaded photo can be removed, restoring upload area")
    @Story("Photo upload")
    @Severity(SeverityLevel.MINOR)
    @Description("Uploads a photo then clicks Remove. Expects the upload area placeholder to reappear.")
    public void TC012_uploadImageThenRemove() {
        signUpPage.uploadPhoto(ConfigReader.getSignupImagePath());
        Assert.assertTrue(signUpPage.isRemoveButtonDisplayed(),
                "TC-012 FAILED: Remove button should appear after upload.");
        signUpPage.clickRemovePhoto();
        Assert.assertTrue(signUpPage.isUploadAreaDisplayed(),
                "TC-012 FAILED: Upload area should be restored after removal.");
        System.out.println("TC-012 PASSED");
    }

    @Test(description = "TC-013: Error shown when trying to register with an already-used email")
    @Story("Duplicate email")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Submits registration with an email that already exists in the system. Expects a duplicate-email error.")
    public void TC013_duplicateEmail() {
        signUpPage.submitRegistration(
                "Nada", "Fouad",
                TestDataProvider.getDuplicateEmail(),
                TestDataProvider.minLengthPassword(),
                ConfigReader.getSignupImagePath()
        );
        Assert.assertTrue(signUpPage.isDuplicateEmailErrorDisplayed(),
                "TC-013 FAILED: Expected duplicate-email error message.");
        System.out.println("TC-013 PASSED");
    }

    @Test(description = "TC-014: 'Sign in' link navigates from signup to signin page")
    @Story("Navigation")
    @Severity(SeverityLevel.MINOR)
    @Description("Clicks the 'Sign in' link at the bottom of the signup form. Expects navigation to /signin.")
    public void TC014_navigateToSignIn() {
        SignInPage signInPage = signUpPage.clickSignInLink();
        Assert.assertTrue(signInPage.isOnSignInPage(),
                "TC-014 FAILED: Expected redirect to /signin.");
        System.out.println("TC-014 PASSED");
    }
}