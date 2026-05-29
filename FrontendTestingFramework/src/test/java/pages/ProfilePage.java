package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ProfilePage extends BasePage {

    private static final By PROFILE_CARD        = By.cssSelector(".profile-card");
    private static final By AVATAR_WRAPPER      = By.cssSelector(".avatar-wrapper");
    private static final By FILE_INPUT          = By.cssSelector("input[type='file']");
    private static final By DETAIL_VALUES       = By.cssSelector(".detail-value");
    private static final By PASSWORD_TEXT       = By.cssSelector(".password-text");
    private static final By CHANGE_PASSWORD_BTN = By.cssSelector(".btn-outline");
    private static final By OLD_PASSWORD        = By.id("oldPassword");
    private static final By NEW_PASSWORD        = By.id("newPassword");
    private static final By CONFIRM_PASSWORD    = By.id("confirmPassword");
    private static final By LOGOUT_BUTTON       = By.cssSelector(".btn-logout");
    private static final By AVATAR_IMG          = By.cssSelector(".avatar-img");
    private static final By AVATAR_INITIALS     = By.cssSelector(".avatar-initials");

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    @Step("Open profile page")
    public ProfilePage open() {
        navigateTo("/profile");
        wait.waitForUrlToContain("/profile");
        wait.waitForPresence(PROFILE_CARD);
        return this;
    }

    @Step("Click avatar wrapper to trigger file upload")
    public ProfilePage clickUploadTrigger() {
        wait.waitForClickable(AVATAR_WRAPPER).click();
        return this;
    }

    @Step("Click logout button")
    public void logout() {
        wait.waitForClickable(LOGOUT_BUTTON).click();
        wait.waitForUrlToContain("/signin");
    }

    @Step("Open change password form")
    public ProfilePage openChangePasswordForm() {
        wait.waitForClickable(CHANGE_PASSWORD_BTN).click();
        wait.waitForPresence(OLD_PASSWORD);
        return this;
    }

    public boolean isOnProfilePage()           { return urlContains("/profile"); }
    public List<WebElement> getDetailValues()  { return driver.findElements(DETAIL_VALUES); }

    public boolean isProfilePictureDisplayed() {
        return isDisplayed(AVATAR_IMG) || isDisplayed(AVATAR_INITIALS);
    }

    public boolean isPasswordMasked() {
        try {
            WebElement passwordSpan = wait.waitForPresence(PASSWORD_TEXT);
            String text = passwordSpan.getText();
            return passwordSpan.isDisplayed()
                    && text != null
                    && !text.isEmpty()
                    && text.chars().allMatch(c -> c == '•' || c == '*' || c == '·');
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPasswordPermanentlyHidden() {
        boolean noToggleButton = driver.findElements(By.cssSelector(".btn-toggle-password")).isEmpty();
        boolean maskSpanPresent = !driver.findElements(PASSWORD_TEXT).isEmpty();
        return noToggleButton && maskSpanPresent;
    }

    public boolean areChangePasswordInputsMasked() {
        openChangePasswordForm();
        boolean old  = "password".equals(wait.waitForPresence(OLD_PASSWORD).getAttribute("type"));
        boolean nw   = "password".equals(driver.findElement(NEW_PASSWORD).getAttribute("type"));
        boolean conf = "password".equals(driver.findElement(CONFIRM_PASSWORD).getAttribute("type"));
        return old && nw && conf;
    }
}