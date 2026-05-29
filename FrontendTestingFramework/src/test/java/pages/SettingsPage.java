package pages;

import io.qameta.allure.Step;
import utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsPage extends BasePage {

    public static final Map<String, int[]> CONSTRAINTS = new HashMap<>();
    static {
        CONSTRAINTS.put("traffic:0",      new int[]{0, 500});
        CONSTRAINTS.put("traffic:1",      new int[]{0, 120});
        CONSTRAINTS.put("air quality:0",  new int[]{0, 50});
        CONSTRAINTS.put("air quality:1",  new int[]{0, 300});
        CONSTRAINTS.put("street light:0", new int[]{0, 100});
        CONSTRAINTS.put("street light:1", new int[]{0, 5000});
    }

    public static final Map<String, String> METRIC_NAMES = new HashMap<>();
    static {
        METRIC_NAMES.put("traffic:0",      "Traffic Density (0–500)");
        METRIC_NAMES.put("traffic:1",      "Average Speed (0–120)");
        METRIC_NAMES.put("air quality:0",  "Carbon Monoxide (0–50 ppm)");
        METRIC_NAMES.put("air quality:1",  "Ozone (0–300 ppb)");
        METRIC_NAMES.put("street light:0", "Brightness Level (0–100)");
        METRIC_NAMES.put("street light:1", "Power Consumption (0–5000)");
    }

    private static final By SENSOR_TYPE_LABEL    = By.xpath("//label[contains(text(),'Sensor Type')]");
    private static final By TRAFFIC_LABEL        = By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Traffic']]");
    private static final By AIR_QUALITY_LABEL    = By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Air Quality']]");
    private static final By STREET_LIGHT_LABEL   = By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Street Light']]");
    private static final By TRAFFIC_RADIO        = By.cssSelector("input[value='traffic']");
    private static final By AIR_QUALITY_RADIO    = By.cssSelector("input[value='air']");
    private static final By STREET_LIGHT_RADIO   = By.cssSelector("input[value='light']");
    private static final By METRIC_DROPDOWN      = By.cssSelector(".form-card select.form-control");
    private static final By THRESHOLD_VALUE_INPUT= By.cssSelector(".form-card input[type='number']");
    private static final By ABOVE_BTN            = By.cssSelector(".form-card .toggle-group .toggle-btn:first-child");
    private static final By BELOW_BTN            = By.cssSelector(".form-card .toggle-group .toggle-btn:last-child");
    private static final By SAVE_THRESHOLD_BTN   = By.cssSelector(".form-card .submit-btn:not(.submit-btn--sm)");
    private static final By ACTIVE_THRESHOLD_ROWS= By.cssSelector(".threshold-list .threshold-item:not(.threshold-item--editing)");
    private static final By DELETE_BTN           = By.cssSelector(".threshold-item:not(.threshold-item--editing) .delete-btn");
    private static final By ERROR_MSG            = By.cssSelector(".alert.alert-error");
    private static final By SUCCESS_MSG          = By.cssSelector(".alert.alert-success");
    private static final By BACK_BTN             = By.cssSelector("button.btn-back");

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void jsClick(By locator) {
        jsClick(wait.waitForVisible(locator));
    }

    @Step("Open settings page")
    public SettingsPage open() {
        navigateTo("/settings");
        wait.waitForVisible(SENSOR_TYPE_LABEL);
        wait.waitForVisible(SAVE_THRESHOLD_BTN);
        System.out.println("[SETTINGS] Page ready");
        return this;
    }

    @Step("Select sensor type: {type}")
    public SettingsPage selectSensorType(String type) {
        By labelLocator;
        By radioLocator;
        String radioValue;

        switch (type.toLowerCase().trim()) {
            case "traffic":
                labelLocator = TRAFFIC_LABEL; radioLocator = TRAFFIC_RADIO; radioValue = "traffic"; break;
            case "air quality": case "airquality":
                labelLocator = AIR_QUALITY_LABEL; radioLocator = AIR_QUALITY_RADIO; radioValue = "air quality"; break;
            case "street light": case "streetlight":
                labelLocator = STREET_LIGHT_LABEL; radioLocator = STREET_LIGHT_RADIO; radioValue = "street light"; break;
            default:
                throw new IllegalArgumentException("[SETTINGS] Unknown sensor type: '" + type + "'");
        }

        final By finalLabel = labelLocator;
        final By finalRadio = radioLocator;
        final String finalValue = radioValue;

        RetryHelper.retryVoid(() -> {
            jsClick(finalLabel);
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            WebElement radio = driver.findElement(finalRadio);
            if (!radio.isSelected()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].checked = true; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                        radio);
            }
            System.out.println("[SETTINGS] Sensor selected: " + finalValue);
        }, "select sensor " + radioValue);

        try { wait.waitForVisible(METRIC_DROPDOWN); } catch (Exception ignored) {}
        return this;
    }

    @Step("Select metric by index: {index}")
    public SettingsPage selectMetricByIndex(int index) {
        if (index < 0 || index > 1) throw new IllegalArgumentException("[SETTINGS] Metric index must be 0 or 1, got: " + index);
        WebElement dropdown = wait.waitForVisible(METRIC_DROPDOWN);
        new Select(dropdown).selectByIndex(index);
        System.out.println("[SETTINGS] Metric index " + index + " selected");
        return this;
    }

    @Step("Enter threshold value: {value}")
    public SettingsPage enterThresholdValue(String value, String sensorType, int metricIndex) {
        if (sensorType != null) {
            String key  = sensorType.toLowerCase().trim() + ":" + metricIndex;
            int[] range = CONSTRAINTS.get(key);
            String name = METRIC_NAMES.getOrDefault(key, key);
            if (range != null) {
                try {
                    double v = Double.parseDouble(value);
                    if (v < range[0] || v > range[1]) {
                        System.out.println("[SETTINGS][OUT OF RANGE] " + value + " exceeds backend range [" + range[0] + "–" + range[1] + "] for " + name);
                    } else {
                        System.out.println("[SETTINGS][WITHIN RANGE] " + value + " in [" + range[0] + "–" + range[1] + "] for " + name);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        WebElement input = wait.waitForVisible(THRESHOLD_VALUE_INPUT);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
        input.sendKeys(value);
        return this;
    }

    public SettingsPage enterThresholdValue(String value) {
        return enterThresholdValue(value, null, -1);
    }

    @Step("Click Above direction toggle")
    public SettingsPage clickAbove() {
        RetryHelper.retryVoid(() -> jsClick(ABOVE_BTN), "click Above toggle");
        return this;
    }

    @Step("Click Below direction toggle")
    public SettingsPage clickBelow() {
        RetryHelper.retryVoid(() -> jsClick(BELOW_BTN), "click Below toggle");
        return this;
    }

    @Step("Click Save Threshold button")
    public SettingsPage clickSaveThreshold() {
        RetryHelper.retryVoid(() -> jsClick(SAVE_THRESHOLD_BTN), "click Save Threshold");
        try {
            wait.waitForCondition(d ->
                    !d.findElements(SUCCESS_MSG).isEmpty() || !d.findElements(ERROR_MSG).isEmpty());
        } catch (TimeoutException ignored) {
            System.out.println("[SETTINGS] No success/error message after save — continuing");
        }
        return this;
    }

    @Step("Create threshold: sensor={sensorType}, metricIndex={metricIndex}, value={thresholdValue}, above={above}")
    public SettingsPage createThreshold(String sensorType, int metricIndex, int thresholdValue, boolean above) {
        selectSensorType(sensorType);
        selectMetricByIndex(metricIndex);
        enterThresholdValue(String.valueOf(thresholdValue), sensorType, metricIndex);
        if (above) clickAbove(); else clickBelow();
        clickSaveThreshold();
        return this;
    }

    @Step("Delete first active threshold")
    public SettingsPage deleteFirstActiveThreshold() {
        List<WebElement> buttons = driver.findElements(DELETE_BTN);
        if (buttons.isEmpty()) { System.out.println("[SETTINGS] No delete buttons found"); return this; }
        int countBefore = getActiveThresholdCount();
        RetryHelper.retryVoid(() -> jsClick(buttons.get(0)), "click delete button");
        try {
            wait.waitForCondition(d -> {
                int current = d.findElements(ACTIVE_THRESHOLD_ROWS).size();
                return current < countBefore;
            });
        } catch (TimeoutException e) {
            System.out.println("[SETTINGS] Row count did not decrease after delete");
        }
        return this;
    }

    public boolean isOnSettingsPage()    { return urlContains("/settings"); }
    public int getActiveThresholdCount() { return driver.findElements(ACTIVE_THRESHOLD_ROWS).size(); }

    public List<WebElement> getActiveThresholds() {
        List<WebElement> rows = driver.findElements(ACTIVE_THRESHOLD_ROWS);
        System.out.println("[SETTINGS] Active threshold rows: " + rows.size());
        return rows;
    }

    public boolean isThresholdErrorDisplayed() {
        try {
            for (WebElement e : driver.findElements(ERROR_MSG)) {
                if (e.isDisplayed() && !e.getText().trim().isEmpty()) {
                    System.out.println("[SETTINGS] Error: " + e.getText().trim());
                    return true;
                }
            }
            return false;
        } catch (StaleElementReferenceException e) { return false; }
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            return driver.findElements(SUCCESS_MSG)
                    .stream().anyMatch(e -> e.isDisplayed() && !e.getText().trim().isEmpty());
        } catch (StaleElementReferenceException e) { return false; }
    }

    public boolean isTrafficSensorSelected() {
        try {
            String cls = driver.findElement(TRAFFIC_LABEL).getAttribute("class");
            return cls != null && cls.contains("active");
        } catch (Exception e) { return false; }
    }

    public int[] getConstraint(String sensorType, int metricIndex) {
        String key = sensorType.toLowerCase().trim() + ":" + metricIndex;
        int[] range = CONSTRAINTS.get(key);
        if (range == null) throw new IllegalArgumentException("[SETTINGS] No constraint for: '" + key + "'");
        return range;
    }

    public int getMin(String sensorType, int metricIndex) { return getConstraint(sensorType, metricIndex)[0]; }
    public int getMax(String sensorType, int metricIndex) { return getConstraint(sensorType, metricIndex)[1]; }

    @Deprecated
    public boolean isTrafficBtnSelected() { return isTrafficSensorSelected(); }
}