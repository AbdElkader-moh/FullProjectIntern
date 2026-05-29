package pages;

import utils.ConfigReader;
import utils.WaitHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 * BasePage: Parent for all Page Objects.
 * Provides shared navigation, interaction helpers, and the WaitHelper instance.
 */
public abstract class BasePage {

    protected WebDriver driver;
    protected WaitHelper wait;
    protected String baseUrl;

    protected BasePage(WebDriver driver) {
        this.driver  = driver;
        this.wait    = new WaitHelper(driver);
        this.baseUrl = ConfigReader.getAppUrl();
        PageFactory.initElements(driver, this);
    }

    // ─── Navigation ─────────────────────────────────────────────────────────

    public void navigateTo(String path) {
        driver.get(baseUrl + path);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean urlContains(String fragment) {
        return driver.getCurrentUrl().contains(fragment);
    }

    // ─── Interaction helpers ─────────────────────────────────────────────────

    protected void clearAndType(WebElement element, String text) {
        wait.waitForClickable(element);
        element.clear();
        element.sendKeys(text);
    }

    protected void click(WebElement element) {
        wait.waitForClickable(element).click();
    }

    protected void click(By locator) {
        wait.waitForClickable(locator).click();
    }

    protected String getText(By locator) {
        return wait.waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return wait.waitForVisible(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }
}
