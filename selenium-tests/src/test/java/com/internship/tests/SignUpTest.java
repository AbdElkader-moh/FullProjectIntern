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

    // ==================== SIGN UP TESTS ====================

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

    // TC-013: Upload image then remove it
    @Test
    public void TC013_uploadImageThenRemove() {
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
        System.out.println("TC-013 PASSED");
    }

    // TC-015: Duplicate email
    @Test
    public void TC015_duplicateEmail() {
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
        System.out.println("TC-015 PASSED");
    }

    // TC-016: Navigate to sign in
    @Test
    public void TC016_navigateToSignIn() {
        driver.get("http://localhost:4200/signup");
        WebElement signInLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[contains(text(),'Sign in') or contains(text(),'Sign In')]")));
        signInLink.click();
        wait.until(ExpectedConditions.urlContains("/signin"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signin"));
        System.out.println("TC-016 PASSED");
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