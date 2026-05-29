package listeners;

import utils.DriverFactory;
import utils.ScreenshotHelper;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

/**
 * AllureListener — TestNG listener that bridges TestNG lifecycle events
 * to the Allure reporting framework.
 *
 * What it does:
 *   - On failure: attaches a screenshot as an Allure attachment so the
 *     HTML report shows exactly what the browser looked like when the test broke.
 *   - On skip: marks the result SKIPPED with the skip reason.
 *   - All Allure annotations (@Epic, @Feature, @Story, @Severity, @Description,
 *     @Step) in the test/page classes work automatically once this listener is
 *     registered — this class just handles screenshots and lifecycle hooks.
 *
 * Registration (testng.xml):
 *   <listeners>
 *     <listener class-name="com.internship.listeners.AllureListener"/>
 *     <listener class-name="com.internship.listeners.TestListener"/>
 *     <listener class-name="com.internship.listeners.ExtentReportListener"/>
 *   </listeners>
 *
 * Or in BaseTest via @Listeners annotation:
 *   @Listeners({AllureListener.class, TestListener.class, ExtentReportListener.class})
 */
public class AllureListener implements ITestListener {

    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    @Override
    public void onTestStart(ITestResult result) {
        // Allure's TestNG integration handles test start automatically via the
        // allure-testng dependency; no manual lifecycle call needed here.
        System.out.println("[Allure] ▶ " + getTestName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("[Allure] ✔ " + getTestName(result));
        // Optional: attach a "success screenshot" for visual proof
        // Uncomment if your team wants screenshots on pass too:
        // attachScreenshot("Success Screenshot");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.err.println("[Allure] ✘ " + getTestName(result));
        attachScreenshot("Failure Screenshot");

        // Attach the exception stack trace as a text attachment for easy reading
        Throwable cause = result.getThrowable();
        if (cause != null) {
            String trace = getStackTraceAsString(cause);
            Allure.addAttachment(
                    "Stack Trace",
                    "text/plain",
                    new ByteArrayInputStream(trace.getBytes()),
                    ".txt"
            );
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("[Allure] ⊘ " + getTestName(result));
        // Mark the Allure result as skipped with a reason
        Throwable cause = result.getThrowable();
        String reason = (cause != null) ? cause.getMessage() : "No reason provided";
        lifecycle.updateTestCase(tc ->
                tc.setStatus(Status.SKIPPED)
                  .setStatusDetails(new StatusDetails().setMessage(reason))
        );
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("[Allure] Suite finished: " + context.getName()
                + " | Passed=" + context.getPassedTests().size()
                + " | Failed=" + context.getFailedTests().size()
                + " | Skipped=" + context.getSkippedTests().size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void attachScreenshot(String attachmentName) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver != null) {
                byte[] screenshot = ScreenshotHelper.captureScreenshotAsBytes(driver);
                Allure.addAttachment(
                        attachmentName,
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        ".png"
                );
            }
        } catch (Exception e) {
            System.err.println("[Allure] Could not attach screenshot: " + e.getMessage());
        }
    }

    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "#" + result.getName();
    }

    private String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("    at ").append(el).append("\n");
        }
        return sb.toString();
    }
}
