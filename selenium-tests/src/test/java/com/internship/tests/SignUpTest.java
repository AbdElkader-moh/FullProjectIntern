package com.internship.tests;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SignUpTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeClass
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // TC-001: Sign up without photo - should be blocked
    @Test
    public void TC001_signUpNoPhoto() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        String email = "tc001_" + System.currentTimeMillis() + "@gmail.com";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Ahmed");
        driver.findElement(By.id("lastName")).sendKeys("Ali");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys("abc123");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        // Should stay on signup or show error - NOT redirect to signin
        Assert.assertFalse(driver.getCurrentUrl().contains("/signin"),
                "TC-001 FAILED: Should not redirect without photo");
        System.out.println("TC-001 PASSED: Signup blocked without photo");
    }

    // TC-002: Sign up with all valid fields and photo
    @Test
    public void TC002_signUpWithPhoto() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        String email = "tc002_" + System.currentTimeMillis() + "@gmail.com";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Nadin");
        driver.findElement(By.id("lastName")).sendKeys("Abdelaal");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys("123456");
        WebElement uploadInput = driver.findElement(By.cssSelector("input[type='file']"));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-002 PASSED: Signup with photo successful");
    }

    // TC-003: First name exactly 2 characters
    @Test
    public void TC003_firstNameTwoChars() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        String email = "tc003_" + System.currentTimeMillis() + "@gmail.com";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Ab");
        driver.findElement(By.id("lastName")).sendKeys("Abdelaal");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys("123456");
        WebElement uploadInput = driver.findElement(By.cssSelector("input[type='file']"));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-003 PASSED");
    }

    // TC-004: Last name exactly 2 characters
    @Test
    public void TC004_lastNameTwoChars() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        String email = "tc004_" + System.currentTimeMillis() + "@gmail.com";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Nadin");
        driver.findElement(By.id("lastName")).sendKeys("Mo");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys("123456");
        WebElement uploadInput = driver.findElement(By.cssSelector("input[type='file']"));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-004 PASSED");
    }

    // TC-005: Password exactly 6 characters
    @Test
    public void TC005_passwordSixChars() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        String email = "tc005_" + System.currentTimeMillis() + "@gmail.com";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Nadin");
        driver.findElement(By.id("lastName")).sendKeys("Abdelaal");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys("abc123");
        WebElement uploadInput = driver.findElement(By.cssSelector("input[type='file']"));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-005 PASSED");
    }

    // TC-006: All fields empty
    @Test
    public void TC006_allFieldsEmpty() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-primary"))).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'First name is required')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-006 PASSED");
    }

    // TC-007: First name 1 character
    @Test
    public void TC007_firstNameOneChar() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("A");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'min 2')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-007 PASSED");
    }

    // TC-008: Last name 1 character
    @Test
    public void TC008_lastNameOneChar() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lastName"))).sendKeys("A");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'min 2')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-008 PASSED");
    }

    // TC-009: Password 5 characters
    @Test
    public void TC009_passwordFiveChars() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password"))).sendKeys("abc12");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'at least 6')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-009 PASSED");
    }

    // TC-010: Invalid email missing @
    @Test
    public void TC010_invalidEmail() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("ahmedtest.com");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'valid email')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-010 PASSED");
    }

    // TC-011: Invalid email missing domain after @
    @Test
    public void TC011_invalidEmailMissingDomain() {
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("ahmed@");
        driver.findElement(By.id("firstName")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'valid email')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-011 PASSED");
    }

    // TC-012: Upload image larger than 5MB
    // TC-012: Upload image larger than 5MB
    // @Test
    // public void TC012_uploadImageLargerThan5MB() {
    // driver.get("http://localhost:4200/signup");
    // wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Nada");
    // driver.findElement(By.id("lastName")).sendKeys("Fouad");
    // driver.findElement(By.id("email")).sendKeys("tc012test@gmail.com");
    // driver.findElement(By.id("password")).sendKeys("123456");
    // WebElement uploadInput =
    // driver.findElement(By.cssSelector("input[type='file']"));
    // uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\134107018174096243.jpg");
    // driver.findElement(By.cssSelector("button.btn-primary")).click();
    // WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(20));
    // WebElement error =
    // longWait.until(ExpectedConditions.visibilityOfElementLocated(
    // By.xpath(
    // "//*[contains(text(),'413') or contains(text(),'Too Large') or
    // contains(text(),'large') or contains(text(),'size')]")));
    // Assert.assertTrue(error.isDisplayed());
    // System.out.println("TC-012 PASSED");
    // }

    // TC-012: Upload image then remove it
    @Test
    public void TC012_uploadImageThenRemove() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        WebElement uploadInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='file']")));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        WebElement removeBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[contains(text(),'Remove')]")));
        removeBtn.click();
        WebElement uploadArea = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'Click to upload')]")));
        Assert.assertTrue(uploadArea.isDisplayed());
        System.out.println("TC-012 PASSED");
    }

    // TC-013: Duplicate email
    @Test
    public void TC013_duplicateEmail() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName"))).sendKeys("Nada");
        driver.findElement(By.id("lastName")).sendKeys("Fouad");
        driver.findElement(By.id("email")).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        WebElement uploadInput = driver.findElement(By.cssSelector("input[type='file']"));
        uploadInput.sendKeys("C:\\Users\\Nadaa\\OneDrive\\Pictures\\Screenshots\\Screenshot 2024-10-26 142611.png");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement error = longWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'already') or contains(text(),'exists') or contains(text(),'email')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-013 PASSED");
    }

    // TC-014: Navigate to sign in
    @Test
    public void TC014_navigateToSignIn() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signup");
        WebElement signInLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[contains(text(),'Sign in') or contains(text(),'Sign In')]")));
        signInLink.click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-014 PASSED");
    }
    // ==================== SIGN IN TESTS ====================

    // TC-017: Sign in with correct credentials
    // @Test
    // public void TC017_signInCorrectCredentials() {
    // driver.get("http://localhost:4200/signin");
    // wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
    // driver.findElement(By.id("password")).sendKeys("123456");
    // driver.findElement(By.cssSelector("button.btn-primary")).click();
    // wait.until(ExpectedConditions.urlContains("/home"));
    // Assert.assertTrue(driver.getCurrentUrl().contains("/home"));
    // System.out.println("TC-017 PASSED");
    // }

    // // TC-018: Sign in with wrong password
    // @Test
    // public void TC018_signInWrongPassword() {
    // driver.get("http://localhost:4200/signin");
    // wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
    // driver.findElement(By.id("password")).sendKeys("wrongpass");
    // driver.findElement(By.cssSelector("button.btn-primary")).click();
    // WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
    // By.xpath(
    // "//*[contains(text(),'Invalid') or contains(text(),'incorrect') or
    // contains(text(),'wrong')]")));
    // Assert.assertTrue(error.isDisplayed());
    // System.out.println("TC-018 PASSED");
    // }

    // // TC-019: Sign in with non-existent email
    // @Test
    // public void TC019_signInNonExistentEmail() {
    // driver.get("http://localhost:4200/signin");
    // wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("ghost@test.com");
    // driver.findElement(By.id("password")).sendKeys("anything");
    // driver.findElement(By.cssSelector("button.btn-primary")).click();
    // WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
    // By.xpath(
    // "//*[contains(text(),'Invalid') or contains(text(),'not found') or
    // contains(text(),'incorrect')]")));
    // Assert.assertTrue(error.isDisplayed());
    // System.out.println("TC-019 PASSED");
    // }

    // // TC-020: Sign in with all fields empty
    // @Test
    // public void TC020_signInAllFieldsEmpty() {
    // driver.get("http://localhost:4200/signin");
    // wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-primary"))).click();
    // WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
    // By.cssSelector(".field-error")));
    // Assert.assertTrue(error.isDisplayed());
    // System.out.println("TC-020 PASSED");
    // }

    @AfterClass
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}