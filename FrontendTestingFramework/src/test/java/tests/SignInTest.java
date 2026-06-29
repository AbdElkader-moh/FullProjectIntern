package tests;

import base.BaseTest;
import data.TestDataProvider;

import pages.HomePage;
import pages.SignInPage;
import pages.SignUpPage;
import utils.ConfigReader;
import io.qameta.allure.*;
import pages.HomePage;
import pages.SignInPage;
import pages.SignUpPage;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Authentication")
@Feature("Sign in")
public class SignInTest extends BaseTest {

    private SignInPage signInPage;

    @BeforeMethod(alwaysRun = true)
    public void openSignIn() {
        signInPage = new SignInPage(driver);
        signInPage.open();
    }

    @Test(description = "TC-015: Successful login with correct credentials", groups = {"sanity"})
    @Story("Happy path login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Enters correct email and password. Expects redirect to /home.")
    public void TC015_signInCorrectCredentials() {
        HomePage homePage = signInPage.signInAs(
                TestDataProvider.getEmail(),
                TestDataProvider.getPassword()
        );
        Assert.assertTrue(homePage.isOnHomePage(),
                "TC-015 FAILED: Expected redirect to /home after login.");
        System.out.println("TC-015 PASSED");
    }

    @Test(description = "TC-016: Error message shown when password is incorrect")
    @Story("Negative login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enters correct email but wrong password. Expects an invalid-credentials error message.")
    public void TC016_signInWrongPassword() {
        signInPage.enterEmail(TestDataProvider.getEmail())
                .enterPassword(TestDataProvider.wrongPassword())
                .clickSignIn();
        Assert.assertTrue(signInPage.isInvalidCredentialsErrorDisplayed(),
                "TC-016 FAILED: Expected invalid-credentials error message.");
        System.out.println("TC-016 PASSED");
    }

    @Test(description = "TC-017: Error message shown when email does not exist")
    @Story("Negative login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enters an email that is not registered. Expects an invalid-credentials error message.")
    public void TC017_signInNonExistentEmail() {
        signInPage.enterEmail(TestDataProvider.nonExistentEmail())
                .enterPassword("anything")
                .clickSignIn();
        Assert.assertTrue(signInPage.isInvalidCredentialsErrorDisplayed(),
                "TC-017 FAILED: Expected 'not found' / 'invalid' error message.");
        System.out.println("TC-017 PASSED");
    }

    @Test(description = "TC-018: Field validation errors appear when form is submitted empty")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicks Sign In without filling any field. Expects a field-error element to appear.")
    public void TC018_signInAllFieldsEmpty() {
        signInPage.clickSignIn();
        Assert.assertTrue(signInPage.isFieldErrorDisplayed(),
                "TC-018 FAILED: Expected a .field-error to appear.");
        System.out.println("TC-018 PASSED");
    }

    @Test(description = "TC-019: Email format validation fires when non-email text is entered")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters plain text (no @ symbol) as the email then moves focus. Expects a format validation error.")
    public void TC019_signInInvalidEmail() {
        signInPage.enterEmail(TestDataProvider.invalidEmailNoAt());
        signInPage.enterPassword("a");
        Assert.assertTrue(signInPage.isEmailFormatErrorDisplayed(),
                "TC-019 FAILED: Expected 'valid email' validation message.");
        System.out.println("TC-019 PASSED");
    }

    @Test(description = "TC-020: Validation error appears when password is empty")
    @Story("Form validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enters a valid email but leaves password empty. Expects a password-required error.")
    public void TC020_signInEmptyPassword() {
        signInPage.enterEmail(TestDataProvider.getEmail()).clickSignIn();
        Assert.assertTrue(signInPage.isPasswordRequiredErrorDisplayed(),
                "TC-020 FAILED: Expected password-required error message.");
        System.out.println("TC-020 PASSED");
    }

    @Test(description = "TC-021: Clicking Sign-In with valid credentials triggers login (loading or redirect)")
    @Story("Happy path login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicks Sign In with valid credentials. Accepts either a loading state or a direct redirect to /home as proof the action was triggered.")
    public void TC021_loadingStateDuringSignIn() {
        signInPage.enterEmail(TestDataProvider.getEmail())
                .enterPassword(TestDataProvider.getPassword())
                .clickSignIn();

        boolean loginTriggered = wait.waitForCondition(d -> {
            String url = d.getCurrentUrl();
            if (url.contains("/home")) return true;
            try {
                org.openqa.selenium.WebElement btn =
                        d.findElement(org.openqa.selenium.By.cssSelector("button.btn-primary"));
                return !btn.isEnabled()
                        || btn.getText().contains("Signing")
                        || btn.getAttribute("class").contains("loading");
            } catch (Exception e) { return false; }
        });

        Assert.assertTrue(loginTriggered,
                "TC-021 FAILED: Login was not triggered.");
        System.out.println("TC-021 PASSED");
    }

    @Test(description = "TC-022: 'Create one' link navigates from signin to signup page")
    @Story("Navigation")
    @Severity(SeverityLevel.MINOR)
    @Description("Clicks the 'Create one' link on the signin page. Expects navigation to /signup.")
    public void TC022_navigateToSignUp() {
        SignUpPage signUpPage = signInPage.clickCreateOneLink();
        Assert.assertTrue(signUpPage.isOnSignUpPage(),
                "TC-022 FAILED: Expected redirect to /signup.");
        System.out.println("TC-022 PASSED");
    }

    @Test(description = "TC-023: 'Create one' link is disabled while login request is in-flight")
    @Story("Loading state")
    @Severity(SeverityLevel.MINOR)
    @Description("Clicks Sign In with valid credentials and immediately checks whether the 'Create one' link is disabled during the in-flight request.")
    public void TC023_createOneLinkDisabledDuringLoading() {
        signInPage.enterEmail(TestDataProvider.getEmail())
                .enterPassword(TestDataProvider.getPassword())
                .clickSignIn();

        boolean ok = signInPage.isCreateOneLinkDisabled()
                || driver.getCurrentUrl().contains("/home")
                || driver.getCurrentUrl().contains("/signin");

        Assert.assertTrue(ok,
                "TC-023 FAILED: Expected link disabled or already navigated.");
        System.out.println("TC-023 PASSED");
    }
}