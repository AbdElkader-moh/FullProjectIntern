package utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ScreenshotHelper: Captures screenshots on test failure and on demand.
 */
public class ScreenshotHelper {

    private ScreenshotHelper() {}

    public static String captureScreenshot(WebDriver driver, String testName) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = testName + "_" + timestamp + ".png";
        String screenshotDir = ConfigReader.getScreenshotsDir();

        try {
            File screenshotDir_ = new File(screenshotDir);
            if (!screenshotDir_.exists()) {
                screenshotDir_.mkdirs();
            }

            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String destPath = screenshotDir + File.separator + fileName;
            FileUtils.copyFile(srcFile, new File(destPath));

            System.out.println("[SCREENSHOT] Saved: " + destPath);
            return destPath;

        } catch (IOException e) {
            System.err.println("[SCREENSHOT] Failed to save screenshot: " + e.getMessage());
            return "";
        }
    }

    public static byte[] captureScreenshotAsBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
