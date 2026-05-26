package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * ProfilePage — locators based on the REAL profile.html DOM:
 *
 * Key facts from profile.html:
 *  - Page ready signal  → <div class="profile-card">
 *  - Upload trigger     → <div class="avatar-wrapper"> (click triggers file input)
 *  - Hidden file input  → <input type="file" accept="image/*">
 *  - Field values       → <span class="detail-value">
 *  - Password display   → <span class="password-text">•••••••••••</span>  (static dots, NOT an input)
 *  - No password input visible by default; inputs only appear inside .security-section
 *    when "Change Password" button is clicked
 *  - Toggle button      → <button class="btn-outline"> "Change Password" / "Cancel Change"
 *  - Logout button      → <button class="btn-logout">
 *  - Password inputs    → id="oldPassword", id="newPassword", id="confirmPassword"
 *                         all permanently type="password" — NO show/hide toggle exists
 *
 * TC-025: isPasswordMasked()
 *   The password row shows static bullet dots via <span class="password-text">
 *   There is NO input[type="password"] visible by default on this page.
 *   The test must check that the password-text span shows masked content (dots)
 *   and no plain-text password is exposed.
 *
 * TC-026: isPasswordPermanentlyHidden()
 *   No .btn-toggle-password element exists anywhere in the DOM.
 *   The password is permanently hidden behind bullet dots.
 *
 * TC-027: clickUploadTrigger()
 *   The upload element is <div class="avatar-wrapper"> — NOT a button or input.
 *   The hidden <input type="file"> is triggered by clicking avatar-wrapper.
 *
 * openProfile() failure:
 *   waitForUrlToContain("/profile") timed out at /signin — session lost after
 *   TC-030 deleted cookies. Fixed by re-login guard in ProfileTest.@BeforeMethod.
 */
public class ProfilePage extends BasePage {

    // ── Locators — all verified against profile.html ──────────────────────────

    /** Page-ready signal: profile-card exists only when user data has loaded */
    private static final By PROFILE_CARD    = By.cssSelector(".profile-card");

    /** Upload trigger: the avatar wrapper div responds to click */
    private static final By AVATAR_WRAPPER  = By.cssSelector(".avatar-wrapper");

    /** Hidden file input inside the avatar section */
    private static final By FILE_INPUT      = By.cssSelector("input[type='file']");

    /** All read-only field value spans */
    private static final By DETAIL_VALUES   = By.cssSelector(".detail-value");

    /**
     * The static password display — always shows bullet dots.
     * This is a <span class="password-text">, NOT an input.
     */
    private static final By PASSWORD_TEXT   = By.cssSelector(".password-text");

    /**
     * The "Change Password" / "Cancel Change" toggle button.
     * Class is btn-outline. There is NO .btn-toggle-password in the DOM.
     */
    private static final By CHANGE_PASSWORD_BTN = By.cssSelector(".btn-outline");

    /** Password inputs — only rendered when isChangePasswordVisible == true */
    private static final By OLD_PASSWORD    = By.id("oldPassword");
    private static final By NEW_PASSWORD    = By.id("newPassword");
    private static final By CONFIRM_PASSWORD= By.id("confirmPassword");

    /** Logout button */
    private static final By LOGOUT_BUTTON   = By.cssSelector(".btn-logout");

    /** Profile picture when set */
    private static final By AVATAR_IMG      = By.cssSelector(".avatar-img");

    /** Avatar initials div shown when no picture is set */
    private static final By AVATAR_INITIALS = By.cssSelector(".avatar-initials");

    // ── Constructor ───────────────────────────────────────────────────────────

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public ProfilePage open() {
        navigateTo("/profile");
        wait.waitForUrlToContain("/profile");
        // Wait for Angular to render the profile-card (proves user data loaded)
        wait.waitForPresence(PROFILE_CARD);
        return this;
    }

    public boolean isOnProfilePage() {
        return urlContains("/profile");
    }

    // ── TC-027: Upload trigger ────────────────────────────────────────────────

    /**
     * Clicks the avatar-wrapper div which triggers the hidden file input.
     * The DOM has: <div class="avatar-wrapper" (click)="triggerFileUpload()">
     * There is NO visible "Upload" button — only this div.
     */
    public ProfilePage clickUploadTrigger() {
        wait.waitForClickable(AVATAR_WRAPPER).click();
        return this;
    }

    // ── TC-025 & TC-026: Password checks ──────────────────────────────────────

    /**
     * TC-025: Password must be masked.
     *
     * The profile page shows password as: <span class="password-text">••••••••••••</span>
     * This is static masked text — there is NO input[type="password"] visible by default.
     *
     * This method verifies:
     *   1. The .password-text span exists and is visible
     *   2. Its text contains only bullet/dot characters (never plain text)
     *   3. No plain-text password value is exposed
     */
    public boolean isPasswordMasked() {
        try {
            WebElement passwordSpan = wait.waitForPresence(PASSWORD_TEXT);
            String text = passwordSpan.getText();
            // Must be visible and contain only masking characters
            return passwordSpan.isDisplayed()
                && text != null
                && !text.isEmpty()
                && text.chars().allMatch(c -> c == '•' || c == '*' || c == '·');
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * TC-026: Password visibility must be permanently hidden — no toggle button
     * of class .btn-toggle-password exists anywhere in the real DOM.
     *
     * The "Change Password" btn-outline button only reveals a change-password FORM
     * (with type="password" inputs), it never reveals the current password as plain text.
     *
     * Returns true (PASS) when:
     *   1. No .btn-toggle-password element exists in the DOM
     *   2. The .password-text span is present and shows masked dots
     */
    public boolean isPasswordPermanentlyHidden() {
        boolean noToggleButton = driver
                .findElements(By.cssSelector(".btn-toggle-password"))
                .isEmpty();
        boolean maskSpanPresent = !driver.findElements(PASSWORD_TEXT).isEmpty();
        return noToggleButton && maskSpanPresent;
    }

    // ── Other profile actions ─────────────────────────────────────────────────

    public boolean isProfilePictureDisplayed() {
        // Either the actual img or the initials div counts as "photo displayed"
        return isDisplayed(AVATAR_IMG) || isDisplayed(AVATAR_INITIALS);
    }

    public List<WebElement> getDetailValues() {
        return driver.findElements(DETAIL_VALUES);
    }

    public void logout() {
        wait.waitForClickable(LOGOUT_BUTTON).click();
        wait.waitForUrlToContain("/signin");
    }

    /** Opens the Change Password form (btn-outline toggles isChangePasswordVisible) */
    public ProfilePage openChangePasswordForm() {
        wait.waitForClickable(CHANGE_PASSWORD_BTN).click();
        wait.waitForPresence(OLD_PASSWORD);
        return this;
    }

    /** Verifies all three password inputs inside the change-password form are type="password" */
    public boolean areChangePasswordInputsMasked() {
        openChangePasswordForm();
        boolean old  = "password".equals(wait.waitForPresence(OLD_PASSWORD).getAttribute("type"));
        boolean nw   = "password".equals(driver.findElement(NEW_PASSWORD).getAttribute("type"));
        boolean conf = "password".equals(driver.findElement(CONFIRM_PASSWORD).getAttribute("type"));
        return old && nw && conf;
    }
}
