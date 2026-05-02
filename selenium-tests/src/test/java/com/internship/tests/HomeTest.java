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

public class HomeTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeClass
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // TC-031: Home page header shows profile picture when set
    @Test
    public void TC031_homeShowsProfilePicture() {
        driver.get("http://localhost:4200/signin");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("nadiinahmed25@gmail.com");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        wait.until(ExpectedConditions.urlContains("/home"));
        WebElement avatar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                        "//img[contains(@class,'avatar') or contains(@class,'profile')] | //*[contains(@class,'profile-btn') or contains(@class,'avatar')]")));
        Assert.assertTrue(avatar.isDisplayed());
        System.out.println("TC-031 PASSED");
    }

    @AfterClass
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}