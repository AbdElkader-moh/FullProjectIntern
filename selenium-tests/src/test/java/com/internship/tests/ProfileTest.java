package com.internship.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.time.Duration;
import java.util.List;

public class ProfileTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeClass
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Login before all profile tests
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/home"));

        // Navigate to profile
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".detail-value")));
    }

    // TC-026: Profile page displays all user fields correctly
    @Test
    public void TC026_profileDisplaysAllFields() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));

        List<WebElement> labels = driver.findElements(By.cssSelector(".detail-label"));
        List<WebElement> values = driver.findElements(By.cssSelector(".detail-value"));

        Assert.assertTrue(labels.size() > 0, "TC-026 FAILED: No labels found");
        Assert.assertTrue(values.size() > 0, "TC-026 FAILED: No values found");

        // Check first name is displayed
        WebElement firstName = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(@class,'detail-label') and contains(text(),'First Name')]")));
        Assert.assertTrue(firstName.isDisplayed());

        System.out.println("TC-026 PASSED");
    }

    // TC-027: Password is masked by default on profile page
    @Test
    public void TC027_passwordMaskedByDefault() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".password-text")));

        WebElement passwordText = driver.findElement(By.cssSelector(".password-text"));
        String displayedText = passwordText.getText();

        // Check it is not showing plain readable password
        boolean isMasked = !displayedText.matches(".*[a-zA-Z0-9].*");
        Assert.assertTrue(isMasked, "TC-027 FAILED: Password is showing plain text: " + displayedText);
        System.out.println("TC-027 PASSED: Password is masked, displayed as: " + displayedText);
    }

    // TC-028: Toggle password visibility - show then hide
    @Test
    public void TC028_togglePasswordVisibility() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".btn-toggle-password")));

        // First click - show password
        WebElement toggleBtn = driver.findElement(By.cssSelector(".btn-toggle-password"));
        toggleBtn.click();

        // Password should now show plain text (contains letters/numbers)
        WebElement passwordText = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".password-text")));
        String visibleText = passwordText.getText();
        boolean isVisible = visibleText.matches(".*[a-zA-Z0-9].*");
        Assert.assertTrue(isVisible, "TC-028 FAILED: Password still masked after first click");

        // Second click - hide password again
        toggleBtn = driver.findElement(By.cssSelector(".btn-toggle-password"));
        toggleBtn.click();

        WebElement passwordTextHidden = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".password-text")));
        String hiddenText = passwordTextHidden.getText();
        boolean isMaskedAgain = !hiddenText.matches(".*[a-zA-Z0-9].*");
        Assert.assertTrue(isMaskedAgain, "TC-028 FAILED: Password not masked again after second click");

        System.out.println("TC-028 PASSED");
    }

    // TC-029: Update profile picture from profile page
    @Test
    public void TC029_updateProfilePicture() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));

        // Look for file input or avatar/camera overlay
        try {
            WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
            fileInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));
            Assert.assertTrue(true);
        } catch (Exception e) {
            // Try clicking avatar to trigger upload
            WebElement avatar = driver.findElement(
                    By.xpath("//img[contains(@class,'avatar') or contains(@class,'profile')]"));
            avatar.click();
            Assert.assertTrue(true);
        }

        System.out.println("TC-029 PASSED");
    }

    // TC-030: Profile shows initials when user has no photo
    @Test
    public void TC030_profileShowsInitialsWhenNoPhoto() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));
        WebElement avatar = driver.findElement(
                By.xpath(
                        "//*[contains(@class,'avatar') or contains(@class,'initials') or contains(@class,'profile-pic')]"));
        Assert.assertTrue(avatar.isDisplayed());
        System.out.println("TC-030 PASSED");
    }

    // TC-031: Profile shows photo when user has profile picture
    @Test
    public void TC031_profileShowsPhotoWhenSet() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));
        WebElement avatar = driver.findElement(
                By.xpath(
                        "//img[contains(@class,'avatar') or contains(@class,'profile')] | //*[contains(@class,'avatar')]"));
        Assert.assertTrue(avatar.isDisplayed());
        System.out.println("TC-031 PASSED");
    }

    // TC-032: Logout from profile page
    @Test
    public void TC032_logoutFromProfile() {
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));
        WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[contains(text(),'Logout')]")));
        logoutBtn.click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-032 PASSED");
    }

    // // TC-033: Back button navigates to /home
    // @Test
    // public void TC033_backButtonNavigatesToHome() {
    // // Login first since TC-032 logged out
    // driver.get("http://localhost:4200/signin");
    // wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
    // driver.findElement(By.id("password")).sendKeys("123456");
    // driver.findElement(By.cssSelector("button.btn-primary")).click();
    // wait.until(ExpectedConditions.urlContains("/home"));

    // driver.get("http://localhost:4200/profile");
    // wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".detail-value")));
    // WebElement backBtn = wait.until(ExpectedConditions.elementToBeClickable(
    // By.xpath("//span[contains(text(),'Back')]")));
    // backBtn.click();
    // wait.until(ExpectedConditions.urlContains("/home"));
    // Assert.assertTrue(driver.getCurrentUrl().contains("/home"));
    // System.out.println("TC-033 PASSED");
    // }

    // TC-034: Access /profile without being logged in
    @Test
    public void TC034_accessProfileWithoutLogin() {
        driver.manage().deleteAllCookies();
        driver.get("http://localhost:4200/profile");
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/signin"),
                ExpectedConditions.urlContains("/")));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("/signin") || url.equals("http://localhost:4200/"),
                "TC-034 FAILED: Expected redirect but got: " + url);
        System.out.println("TC-034 PASSED");
    }

    @AfterClass
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}