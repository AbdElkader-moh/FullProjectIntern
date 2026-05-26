package pages;

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

/**
 * SettingsPage — Page Object for /settings (Alert Thresholds)
 *
 * ── KNOWN FRONTEND BUG ───────────────────────────────────────────────────────
 *
 *   The UI always displays "(0 - 100)" as the constraint hint regardless of
 *   the selected metric. This is a frontend defect — the hint is hardcoded.
 *
 *   Backend validation enforces the REAL ranges (from API spec screenshot):
 *     Traffic Density   : 0 – 500
 *     Average Speed     : 0 – 120
 *     Carbon Monoxide   : 0 – 50  (ppm)
 *     Ozone             : 0 – 300 (ppb)
 *     Brightness Level  : 0 – 100
 *     Power Consumption : 0 – 5000
 *
 *   Tests target backend validation, not the broken UI hint.
 *
 * ── DOM ANALYSIS (settings.html) ─────────────────────────────────────────────
 *
 *   SENSOR TYPE — <label class="radio-option"> wrapping <input type="radio">
 *     NOT <button> elements. Previous XPath //button[contains(.,'Traffic')]
 *     found zero elements — root cause of all original TC038–TC046 failures.
 *
 *   ABOVE/BELOW — <button class="toggle-btn"> scoped to .form-card
 *     (edit rows have identical buttons — scoping prevents collision).
 *
 *   DELETE — <button class="delete-btn" title="Delete">✕</button> (U+2715)
 *     Previous code used × (U+00D7) — wrong Unicode. Fixed via CSS class.
 *
 *   ERROR MESSAGE  — <div class="alert alert-error">
 *   SUCCESS MESSAGE— <div class="alert alert-success">
 *
 *   ACTIVE ROWS — .threshold-item:not(.threshold-item--editing)
 *     Excludes inline-edit rows to avoid double-counting.
 */
public class SettingsPage extends BasePage {

    // ── Backend constraint map  key = "sensor:metricIndex" → [min, max] ───────
    // Source: API spec screenshot (backend enforced — UI hint is bugged at 0-100)
    public static final Map<String, int[]> CONSTRAINTS = new HashMap<>();
    static {
        CONSTRAINTS.put("traffic:0",      new int[]{0, 500});   // Traffic Density
        CONSTRAINTS.put("traffic:1",      new int[]{0, 120});   // Average Speed
        CONSTRAINTS.put("air quality:0",  new int[]{0, 50});    // Carbon Monoxide (ppm)
        CONSTRAINTS.put("air quality:1",  new int[]{0, 300});   // Ozone (ppb)
        CONSTRAINTS.put("street light:0", new int[]{0, 100});   // Brightness Level
        CONSTRAINTS.put("street light:1", new int[]{0, 5000});  // Power Consumption
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

    // ── Locators ──────────────────────────────────────────────────────────────

    private static final By SENSOR_TYPE_LABEL =
            By.xpath("//label[contains(text(),'Sensor Type')]");

    // Sensor labels — <label class="radio-option">
    private static final By TRAFFIC_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Traffic']]");
    private static final By AIR_QUALITY_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Air Quality']]");
    private static final By STREET_LIGHT_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Street Light']]");

    // Radio inputs — JS fallback when label click does not fire Angular (change)
    private static final By TRAFFIC_RADIO      = By.cssSelector("input[value='traffic']");
    private static final By AIR_QUALITY_RADIO  = By.cssSelector("input[value='air']");
    private static final By STREET_LIGHT_RADIO = By.cssSelector("input[value='light']");

    // Form controls — scoped to .form-card to avoid edit-row collisions
    private static final By METRIC_DROPDOWN =
            By.cssSelector(".form-card select.form-control");
    private static final By THRESHOLD_VALUE_INPUT =
            By.cssSelector(".form-card input[type='number']");
    private static final By ABOVE_BTN =
            By.cssSelector(".form-card .toggle-group .toggle-btn:first-child");
    private static final By BELOW_BTN =
            By.cssSelector(".form-card .toggle-group .toggle-btn:last-child");
    private static final By SAVE_THRESHOLD_BTN =
            By.cssSelector(".form-card .submit-btn:not(.submit-btn--sm)");

    // Threshold list
    private static final By ACTIVE_THRESHOLD_ROWS =
            By.cssSelector(".threshold-list .threshold-item:not(.threshold-item--editing)");
    private static final By DELETE_BTN =
            By.cssSelector(".threshold-item:not(.threshold-item--editing) .delete-btn");

    // Alerts
    private static final By ERROR_MSG   = By.cssSelector(".alert.alert-error");
    private static final By SUCCESS_MSG = By.cssSelector(".alert.alert-success");

    private static final By BACK_BTN = By.cssSelector("button.btn-back");

    // ── Constructor ───────────────────────────────────────────────────────────

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    // ── JS helpers ────────────────────────────────────────────────────────────

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void jsClick(By locator) {
        jsClick(wait.waitForVisible(locator));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public SettingsPage open() {
        navigateTo("/settings");
        wait.waitForVisible(SENSOR_TYPE_LABEL);
        wait.waitForVisible(SAVE_THRESHOLD_BTN);
        System.out.println("[SETTINGS] Page ready");
        return this;
    }

    // ── Sensor type selection ─────────────────────────────────────────────────

    /**
     * Clicks the radio-option label for the given sensor type.
     *
     * Strategy A — jsClick the label (fires radio + Angular change event).
     * Strategy B — JS radio.checked = true + dispatchEvent fallback when
     *              label click does not propagate to Angular ngModel.
     *
     * Waits for metric dropdown to reload before returning.
     */
    public SettingsPage selectSensorType(String type) {
        By labelLocator;
        By radioLocator;
        String radioValue;

        switch (type.toLowerCase().trim()) {
            case "traffic":
                labelLocator = TRAFFIC_LABEL;
                radioLocator = TRAFFIC_RADIO;
                radioValue   = "traffic";
                break;
            case "air quality": case "airquality":
                labelLocator = AIR_QUALITY_LABEL;
                radioLocator = AIR_QUALITY_RADIO;
                radioValue   = "air quality";
                break;
            case "street light": case "streetlight":
                labelLocator = STREET_LIGHT_LABEL;
                radioLocator = STREET_LIGHT_RADIO;
                radioValue   = "street light";
                break;
            default:
                throw new IllegalArgumentException(
                    "[SETTINGS] Unknown sensor type: '" + type +
                    "'. Valid: traffic | air quality | street light");
        }

        final By finalLabel = labelLocator;
        final By finalRadio = radioLocator;
        final String finalValue = radioValue;

        RetryHelper.retryVoid(() -> {
            WebElement label = wait.waitForVisible(finalLabel);
            jsClick(label);

            WebElement radio = driver.findElement(finalRadio);
            boolean isChecked = radio.getAttribute("checked") != null;

            if (!isChecked) {
                System.out.println("[SETTINGS] Label click did not register — using JS dispatch");
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].checked = true;" +
                    "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    radio);
            }

            wait.waitForVisible(METRIC_DROPDOWN);
            System.out.println("[SETTINGS] Sensor type '" + finalValue + "' selected");
        }, "select sensor type: " + type);

        return this;
    }

    // ── Metric selection ──────────────────────────────────────────────────────

    /**
     * Selects a metric by index.
     *   Traffic:      0=Traffic Density, 1=Average Speed
     *   Air Quality:  0=Carbon Monoxide, 1=Ozone
     *   Street Light: 0=Brightness Level, 1=Power Consumption
     */
    public SettingsPage selectMetricByIndex(int index) {
        if (index < 0 || index > 1) {
            throw new IllegalArgumentException(
                "[SETTINGS] Metric index must be 0 or 1, got: " + index);
        }
        WebElement dropdown = wait.waitForVisible(METRIC_DROPDOWN);
        new Select(dropdown).selectByIndex(index);
        System.out.println("[SETTINGS] Metric index " + index + " selected");
        return this;
    }

    public SettingsPage selectMetric(String metricText) {
        WebElement dropdown = wait.waitForVisible(METRIC_DROPDOWN);
        new Select(dropdown).selectByVisibleText(metricText);
        System.out.println("[SETTINGS] Metric '" + metricText + "' selected");
        return this;
    }

    // ── Threshold value ───────────────────────────────────────────────────────

    /**
     * Enters a threshold value with range logging.
     * Logs [WITHIN RANGE] or [OUT OF RANGE] based on backend spec constraints.
     * Note: UI always shows "(0 - 100)" — this is a known frontend display bug.
     */
    public SettingsPage enterThresholdValue(String value, String sensorType, int metricIndex) {
        if (sensorType != null) {
            String key  = sensorType.toLowerCase().trim() + ":" + metricIndex;
            int[] range = CONSTRAINTS.get(key);
            String name = METRIC_NAMES.getOrDefault(key, key);
            if (range != null) {
                try {
                    double v = Double.parseDouble(value);
                    if (v < range[0] || v > range[1]) {
                        System.out.println("[SETTINGS][OUT OF RANGE] " + value +
                            " exceeds backend range [" + range[0] + "–" + range[1] +
                            "] for " + name + " → expect rejection");
                    } else {
                        System.out.println("[SETTINGS][WITHIN RANGE] " + value +
                            " in [" + range[0] + "–" + range[1] + "] for " + name);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        WebElement input = wait.waitForVisible(THRESHOLD_VALUE_INPUT);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
        input.sendKeys(value);
        System.out.println("[SETTINGS] Threshold value entered: " + value);
        return this;
    }

    /** Overload without range logging — for simple/toggle-only tests. */
    public SettingsPage enterThresholdValue(String value) {
        return enterThresholdValue(value, null, -1);
    }

    // ── Alert direction ───────────────────────────────────────────────────────

    public SettingsPage clickAbove() {
        RetryHelper.retryVoid(() -> jsClick(ABOVE_BTN), "click Above toggle");
        return this;
    }

    public SettingsPage clickBelow() {
        RetryHelper.retryVoid(() -> jsClick(BELOW_BTN), "click Below toggle");
        return this;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public SettingsPage clickSaveThreshold() {
        RetryHelper.retryVoid(() -> jsClick(SAVE_THRESHOLD_BTN), "click Save Threshold");
        try {
            wait.waitForCondition(d ->
                !d.findElements(SUCCESS_MSG).isEmpty() ||
                !d.findElements(ERROR_MSG).isEmpty());
        } catch (TimeoutException ignored) {
            System.out.println("[SETTINGS] No success/error message after save — continuing");
        }
        return this;
    }

    /** Full flow: select sensor → select metric → enter value → direction → save. */
    public SettingsPage createThreshold(String sensorType, int metricIndex,
                                        int thresholdValue, boolean above) {
        selectSensorType(sensorType);
        selectMetricByIndex(metricIndex);
        enterThresholdValue(String.valueOf(thresholdValue), sensorType, metricIndex);
        if (above) clickAbove(); else clickBelow();
        clickSaveThreshold();
        return this;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public SettingsPage deleteFirstActiveThreshold() {
        List<WebElement> buttons = driver.findElements(DELETE_BTN);
        if (buttons.isEmpty()) {
            System.out.println("[SETTINGS] No delete buttons found");
            return this;
        }
        int countBefore = getActiveThresholdCount();
        RetryHelper.retryVoid(() -> jsClick(buttons.get(0)), "click delete button");
        try {
            wait.waitForCondition(d -> {
                int current = d.findElements(ACTIVE_THRESHOLD_ROWS).size();
                System.out.println("[SETTINGS] Delete poll: " + current +
                    " rows (was " + countBefore + ")");
                return current < countBefore;
            });
        } catch (TimeoutException e) {
            System.out.println("[SETTINGS] Row count did not decrease after delete");
        }
        return this;
    }

    // ── Back ──────────────────────────────────────────────────────────────────

    public SettingsPage clickBack() {
        RetryHelper.retryVoid(() -> jsClick(BACK_BTN), "click Back");
        return this;
    }

    // ── Constraint helpers ────────────────────────────────────────────────────

    public int[] getConstraint(String sensorType, int metricIndex) {
        String key = sensorType.toLowerCase().trim() + ":" + metricIndex;
        int[] range = CONSTRAINTS.get(key);
        if (range == null) throw new IllegalArgumentException(
            "[SETTINGS] No constraint for: '" + key + "'");
        return range;
    }

    public int getMin(String sensorType, int metricIndex) {
        return getConstraint(sensorType, metricIndex)[0];
    }

    public int getMax(String sensorType, int metricIndex) {
        return getConstraint(sensorType, metricIndex)[1];
    }

    // ── State / assertions ────────────────────────────────────────────────────

    public boolean isOnSettingsPage() { return urlContains("/settings"); }

    public List<WebElement> getActiveThresholds() {
        List<WebElement> rows = driver.findElements(ACTIVE_THRESHOLD_ROWS);
        System.out.println("[SETTINGS] Active threshold rows: " + rows.size());
        return rows;
    }

    public int getActiveThresholdCount() { return getActiveThresholds().size(); }

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

    /** @deprecated Use isTrafficSensorSelected() */
    @Deprecated
    public boolean isTrafficBtnSelected() { return isTrafficSensorSelected(); }
}
