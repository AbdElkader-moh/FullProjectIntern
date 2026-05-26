package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * SignInPage: Page Object for /signin
 * Covers: Login form, validation messages, navigation to sign-up.
 */
public class SignInPage extends BasePage {

    // ─── Locators ────────────────────────────────────────────────────────────

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "button.btn-primary")
    private WebElement signInBtn;

    // "Create one" / "Sign up" link
    private static final By CREATE_ONE_LINK =
            By.xpath("//*[contains(text(),'Create one') or contains(text(),'create one') or contains(text(),'Sign up')]");

    // Error messages
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

    // ─── Constructor ─────────────────────────────────────────────────────────

    public SignInPage(WebDriver driver) {
        super(driver);
    }

    // ─── Page Actions ────────────────────────────────────────────────────────

    public SignInPage open() {
        driver.get("about:blank");
        navigateTo("/signin");
        wait.waitForVisible(By.id("email"));
        return this;
    }

    public SignInPage enterEmail(String email) {
        clearAndType(emailInput, email);
        return this;
    }

    public SignInPage enterPassword(String password) {
        clearAndType(passwordInput, password);
        return this;
    }

    public SignInPage clickSignIn() {
        click(signInBtn);
        return this;
    }

    /** Full sign-in flow */
    public HomePage signInAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
        wait.waitForUrlToContain("/home");
        return new HomePage(driver);
    }

    public SignUpPage clickCreateOneLink() {
        click(CREATE_ONE_LINK);
        wait.waitForUrlToContain("/signup");
        return new SignUpPage(driver);
    }

    // ─── Validations ─────────────────────────────────────────────────────────

    public boolean isOnSignInPage() {
        return urlContains("/signin");
    }

    public boolean isFieldErrorDisplayed() {
        return isDisplayed(FIELD_ERROR);
    }

    public boolean isInvalidCredentialsErrorDisplayed() {
        return isDisplayed(INVALID_CREDENTIALS_ERROR);
    }

    public boolean isEmailFormatErrorDisplayed() {
        return isDisplayed(VALID_EMAIL_ERROR);
    }

    public boolean isPasswordRequiredErrorDisplayed() {
        return isDisplayed(PASSWORD_REQUIRED_ERROR);
    }

    public boolean isSignInButtonDisabledOrLoading() {
        WebElement button = signInBtn;
        return !button.isEnabled()
                || button.getText().contains("Signing")
                || button.getAttribute("class").contains("loading");
    }

    public boolean isCreateOneLinkDisabled() {
        return isDisplayed(DISABLED_LINK);
    }

    public boolean redirectedToHome() {
        return wait.waitForUrlToContain("/home");
    }
}
