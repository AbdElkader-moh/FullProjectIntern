package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SignInPage extends BasePage {

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "button.btn-primary")
    private WebElement signInBtn;

    private static final By CREATE_ONE_LINK =
            By.xpath("//*[contains(text(),'Create one') or contains(text(),'create one') or contains(text(),'Sign up')]");
    private static final By FIELD_ERROR =
            By.cssSelector(".field-error");
    private static final By INVALID_CREDENTIALS_ERROR =
            By.xpath("//*[contains(text(),'Invalid') or contains(text(),'incorrect') or contains(text(),'wrong')]");
    private static final By VALID_EMAIL_ERROR =
            By.xpath("//*[contains(text(),'valid email')]");
    private static final By PASSWORD_REQUIRED_ERROR =
            By.xpath("//*[contains(text(),'required') or contains(text(),'Password')]");
    private static final By DISABLED_LINK =
            By.xpath("//*[contains(@class,'disabled') or contains(@class,'disabled-link')]");

    public SignInPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open sign-in page")
    public SignInPage open() {
        driver.get("about:blank");
        navigateTo("/signin");
        wait.waitForVisible(By.id("email"));
        return this;
    }

    @Step("Enter email: {email}")
    public SignInPage enterEmail(String email) {
        clearAndType(emailInput, email);
        return this;
    }

    @Step("Enter password")
    public SignInPage enterPassword(String password) {
        clearAndType(passwordInput, password);
        return this;
    }

    @Step("Click Sign In button")
    public SignInPage clickSignIn() {
        click(signInBtn);
        return this;
    }

    @Step("Sign in as {email}")
    public HomePage signInAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    @Step("Click 'Create one' link")
    public SignUpPage clickCreateOneLink() {
        click(CREATE_ONE_LINK);
        wait.waitForUrlToContain("/signup");
        return new SignUpPage(driver);
    }

    public boolean isOnSignInPage()                      { return urlContains("/signin"); }
    public boolean isFieldErrorDisplayed()               { return isDisplayed(FIELD_ERROR); }
    public boolean isInvalidCredentialsErrorDisplayed()  { return isDisplayed(INVALID_CREDENTIALS_ERROR); }
    public boolean isEmailFormatErrorDisplayed()         { return isDisplayed(VALID_EMAIL_ERROR); }
    public boolean isPasswordRequiredErrorDisplayed()    { return isDisplayed(PASSWORD_REQUIRED_ERROR); }
    public boolean isCreateOneLinkDisabled()             { return isDisplayed(DISABLED_LINK); }

    public boolean isSignInButtonDisabledOrLoading() {
        return !signInBtn.isEnabled()
                || signInBtn.getText().contains("Signing")
                || signInBtn.getAttribute("class").contains("loading");
    }

    public boolean redirectedToHome() {
        return wait.waitForUrlToContain("/home");
    }
}