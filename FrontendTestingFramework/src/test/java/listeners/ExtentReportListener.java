package com.internship.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.internship.utils.ConfigReader;
import com.internship.utils.DriverFactory;
import com.internship.utils.ScreenshotHelper;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ExtentReportListener: Generates an HTML Extent Report after each suite run.
 * Report is saved to the reports/ directory with a timestamped filename.
 */
public class ExtentReportListener implements ITestListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportsDir = ConfigReader.getReportsDir();
        new File(reportsDir).mkdirs();
        String reportPath = reportsDir + File.separator + "Report_" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("SmartCity Frontend Test Report");
        spark.config().setReportName(ConfigReader.get("extent.report.name"));
        spark.config().setEncoding("UTF-8");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", ConfigReader.getAppUrl());
        extent.setSystemInfo("Browser", ConfigReader.getBrowser());
        extent.setSystemInfo("OS", System.getProperty("os.name"));

        System.out.println("[ExtentReport] Report will be saved to: " + reportPath);
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (extent == null) return;
        String description = result.getMethod().getDescription();
        ExtentTest test = extent.createTest(
                result.getName(),
                description.isEmpty() ? result.getName() : description
        );
        testThread.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = testThread.get();
        if (test != null) test.log(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = testThread.get();
        if (test == null) return;

        test.log(Status.FAIL, result.getThrowable());

        // Attach screenshot
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver != null) {
                byte[] screenshotBytes = ScreenshotHelper.captureScreenshotAsBytes(driver);
                test.addScreenCaptureFromBase64String(
                        java.util.Base64.getEncoder().encodeToString(screenshotBytes),
                        "Failure Screenshot"
                );
            }
        } catch (Exception e) {
            test.log(Status.WARNING, "Could not attach screenshot: " + e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = testThread.get();
        if (test != null) test.log(Status.SKIP, "Test skipped");
    }
}
