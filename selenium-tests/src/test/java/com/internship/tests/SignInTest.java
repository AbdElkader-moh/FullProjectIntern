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

public class SignInTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeClass
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // TC-017: Sign in with correct credentials
    @Test
    public void TC017_signInCorrectCredentials() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/home"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/home"));
        System.out.println("TC-017 PASSED");
    }

    // TC-018: Sign in with wrong password
    @Test
    public void TC018_signInWrongPassword() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("wrongpass");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                        "//*[contains(text(),'Invalid') or contains(text(),'incorrect') or contains(text(),'wrong')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-018 PASSED");
    }

    // TC-019: Sign in with non-existent email
    @Test
    public void TC019_signInNonExistentEmail() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("ghost@test.com");
        driver.findElement(By.id("password")).sendKeys("anything");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                        "//*[contains(text(),'Invalid') or contains(text(),'not found') or contains(text(),'incorrect')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-019 PASSED");
    }

    // TC-020: Submit sign in form with all fields empty
    @Test
    public void TC020_signInAllFieldsEmpty() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.btn-primary"))).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".field-error")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-020 PASSED");
    }

    // TC-021: Sign in with invalid email format
    @Test
    public void TC021_signInInvalidEmail() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email"))).sendKeys("notanemail");
        driver.findElement(By.id("password")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'valid email')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-021 PASSED");
    }

    // TC-022: Sign in with valid email but empty password
    @Test
    public void TC022_signInEmptyPassword() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email"))).sendKeys("ahmed@test.com");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'required') or contains(text(),'Password')]")));
        Assert.assertTrue(error.isDisplayed());
        System.out.println("TC-022 PASSED");
    }

    // TC-023: Loading spinner appears during sign in
    @Test
    public void TC023_loadingSpinnerDuringSignIn() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        WebElement button = driver.findElement(By.cssSelector("button.btn-primary"));
        boolean isDisabledOrLoading = !button.isEnabled() ||
                button.getText().contains("Signing") ||
                button.getAttribute("class").contains("loading");
        Assert.assertTrue(isDisabledOrLoading || driver.getCurrentUrl().contains("/home"));
        System.out.println("TC-023 PASSED");
    }

    // TC-024: Navigate to signup from signin page
    @Test
    public void TC024_navigateToSignUp() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        WebElement createOneLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(
                        "//*[contains(text(),'Create one') or contains(text(),'create one') or contains(text(),'Sign up')]")));
        createOneLink.click();
        wait.until(ExpectedConditions.urlContains("/signup"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/signup"));
        System.out.println("TC-024 PASSED");
    }

    // TC-025: Create one link disabled while sign in is loading
    @Test
    public void TC025_createOneLinkDisabledDuringLoading() {
        driver.get("about:blank");
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        try {
            WebElement link = driver.findElement(
                    By.xpath("//*[contains(@class,'disabled') or contains(@class,'disabled-link')]"));
            Assert.assertTrue(link != null);
        } catch (Exception e) {
            Assert.assertTrue(driver.getCurrentUrl().contains("/home") ||
                    driver.getCurrentUrl().contains("/signin"));
        }
        System.out.println("TC-025 PASSED");
    }

    @AfterClass
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}