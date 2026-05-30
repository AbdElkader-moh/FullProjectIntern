package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;

public class SignUpPage extends BasePage {

    @FindBy(id = "firstName")  private WebElement firstNameInput;
    @FindBy(id = "lastName")   private WebElement lastNameInput;
    @FindBy(id = "email")      private WebElement emailInput;
    @FindBy(id = "password")   private WebElement passwordInput;
    @FindBy(css = "input[type='file']") private WebElement fileInput;
    @FindBy(css = "button.btn-primary") private WebElement createAccountBtn;

    private static final By SIGN_IN_LINK      = By.xpath("//*[contains(text(),'Sign in') or contains(text(),'Sign In')]");
    private static final By REMOVE_PHOTO_BTN  = By.xpath("//*[contains(text(),'Remove')]");
    private static final By UPLOAD_AREA       = By.xpath("//*[contains(text(),'Click to upload')]");
    private static final By FIRST_NAME_ERROR  = By.xpath("//*[contains(text(),'First name is required') or contains(text(),'min 2')]");
    private static final By LAST_NAME_ERROR   = By.xpath("//*[contains(text(),'Last name is required') or contains(text(),'min 2')]");
    private static final By EMAIL_ERROR       = By.xpath("//*[contains(text(),'valid email') or contains(text(),'Email is required')]");
    private static final By PASSWORD_ERROR    = By.xpath("//*[contains(text(),'at least 6') or contains(text(),'Password is required')]");
    private static final By DUPLICATE_EMAIL_ERROR = By.xpath("//*[contains(text(),'already') or contains(text(),'exists') or contains(text(),'email')]");

    public SignUpPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open sign-up page")
    public SignUpPage open() {
        navigateTo("/signup");
        wait.waitForVisible(By.id("firstName"));
        return this;
    }

    @Step("Enter first name: {firstName}")
    public SignUpPage enterFirstName(String firstName) {
        clearAndType(firstNameInput, firstName);
        return this;
    }

    @Step("Enter last name: {lastName}")
    public SignUpPage enterLastName(String lastName) {
        clearAndType(lastNameInput, lastName);
        return this;
    }

    @Step("Enter email: {email}")
    public SignUpPage enterEmail(String email) {
        clearAndType(emailInput, email);
        return this;
    }

    @Step("Enter password")
    public SignUpPage enterPassword(String password) {
        clearAndType(passwordInput, password);
        return this;
    }

    @Step("Upload profile photo: {absoluteImagePath}")
    public SignUpPage uploadPhoto(String absoluteImagePath) {
        File imageFile = new File(absoluteImagePath);
        wait.waitForPresence(By.cssSelector("input[type='file']"));
        fileInput.sendKeys(imageFile.getAbsolutePath());
        return this;
    }

    @Step("Click Create Account button")
    public SignUpPage clickCreateAccount() {
        click(createAccountBtn);
        return this;
    }

    @Step("Submit registration form")
    public void submitRegistration(String firstName, String lastName,
                                   String email, String password,
                                   String imagePath) {
        enterFirstName(firstName);
        enterLastName(lastName);
        enterEmail(email);
        enterPassword(password);
        if (imagePath != null && !imagePath.isEmpty()) {
            uploadPhoto(imagePath);
        }
        clickCreateAccount();
    }

    @Step("Click 'Sign in' link")
    public SignInPage clickSignInLink() {
        click(SIGN_IN_LINK);
        wait.waitForUrlToContain("/signin");
        return new SignInPage(driver);
    }

    @Step("Click Remove photo button")
    public SignUpPage clickRemovePhoto() {
        click(REMOVE_PHOTO_BTN);
        return this;
    }

    public boolean isOnSignUpPage()                  { return urlContains("/signup"); }
    public boolean isFirstNameErrorDisplayed()       { return isDisplayed(FIRST_NAME_ERROR); }
    public boolean isLastNameErrorDisplayed()        { return isDisplayed(LAST_NAME_ERROR); }
    public boolean isEmailErrorDisplayed()           { return isDisplayed(EMAIL_ERROR); }
    public boolean isPasswordErrorDisplayed()        { return isDisplayed(PASSWORD_ERROR); }
    public boolean isUploadAreaDisplayed()           { return isDisplayed(UPLOAD_AREA); }
    public boolean isRemoveButtonDisplayed()         { return isDisplayed(REMOVE_PHOTO_BTN); }
    public boolean staysOnSignUp()                   { return urlContains("/signup"); }
    public boolean redirectedToSignIn()              { return wait.waitForUrlToContain("/signin"); }

    public boolean isDuplicateEmailErrorDisplayed() {
        try {
            WebDriverWait longWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            WebElement error = longWait.until(
                    org.openqa.selenium.support.ui.ExpectedConditions
                            .visibilityOfElementLocated(DUPLICATE_EMAIL_ERROR));
            return error.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}