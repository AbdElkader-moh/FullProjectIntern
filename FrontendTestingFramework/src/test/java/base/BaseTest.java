package base;

import data.TestDataProvider;
import utils.ConfigReader;
import utils.DriverFactory;
import utils.WaitHelper;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * BaseTest: Superclass for all test classes.
 * Manages driver lifecycle and provides shared utilities.
 */
public class BaseTest {

    protected WebDriver driver;
    protected WaitHelper wait;
    protected String baseUrl;

    @BeforeClass
    public void setUp() {
        driver  = DriverFactory.getDriver();
        wait    = new WaitHelper(driver);
        baseUrl = ConfigReader.getAppUrl();
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    /** Helper: navigate to a path relative to baseUrl */
    protected void navigateTo(String path) {
        driver.get(baseUrl + path);
    }

    /** Helper: perform a fresh sign-in before tests that require authentication */
    protected void loginAs(String email, String password) {
        navigateTo("/signin");
        wait.waitForVisible(org.openqa.selenium.By.id("email"));
        driver.findElement(org.openqa.selenium.By.id("email")).clear();

        driver.findElement(org.openqa.selenium.By.id("email")).sendKeys(email);
        driver.findElement(org.openqa.selenium.By.id("password")).clear();
        driver.findElement(org.openqa.selenium.By.id("password")).sendKeys(password);
        driver.findElement(org.openqa.selenium.By.cssSelector("button.btn-primary")).click();

        try {
            wait.waitForUrlToContain("/home");
        } catch (Exception e) {
            String currentUrl = driver.getCurrentUrl();
            throw new RuntimeException(
                "[LOGIN FAILED] Could not log in as '" + email + "'. " +
                "Current URL: " + currentUrl + ". " +
                "Check that app.email and app.password in config.properties are correct " +
                "and that the application is running at " + baseUrl + ". " +
                "Original error: " + e.getMessage()
            );
        }
    }

    protected void loginWithDefaultUser() {
        loginAs(TestDataProvider.getEmail(), TestDataProvider.getPassword());
    }
}
