package listeners;

import utils.DriverFactory;
import utils.ScreenshotHelper;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestListener: Hooks into TestNG lifecycle.
 * Captures a screenshot automatically on every test failure.
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("▶ STARTING: " + getTestName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("✔ PASSED:   " + getTestName(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.err.println("✘ FAILED:   " + getTestName(result));
        System.err.println("  Reason:   " + result.getThrowable().getMessage());
        captureFailureScreenshot(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("⊘ SKIPPED:  " + getTestName(result));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        System.out.println("~ PARTIAL:  " + getTestName(result));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getTestName(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getName();
    }

    private void captureFailureScreenshot(ITestResult result) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver != null) {
                String path = ScreenshotHelper.captureScreenshot(driver, result.getName());
                if (!path.isEmpty()) {
                    System.err.println("  Screenshot: " + path);
                }
            }
        } catch (Exception e) {
            System.err.println("  [Screenshot] Could not capture: " + e.getMessage());
        }
    }
}
