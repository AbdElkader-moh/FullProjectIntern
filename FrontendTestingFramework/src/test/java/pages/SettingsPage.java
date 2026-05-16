package com.internship.pages;

import com.internship.utils.ConfigReader;
import com.internship.utils.RetryHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * SettingsPage — Page Object for /settings (Alert Thresholds)
 *
 * ── WHY EVERY PREVIOUS LOCATOR WAS WRONG (settings.html analysis) ────────
 *
 * SENSOR TYPE BUTTONS — critical finding:
 *   Our code:  //button[contains(.,'Traffic')]
 *   Real DOM:  <label class="radio-option"><input type="radio" .../><span>Traffic</span></label>
 *
 *   The sensor type selectors are <label> elements wrapping radio inputs — NOT buttons.
 *   There is no <button> for Traffic/Air Quality/Street Light anywhere in the HTML.
 *   This is the single root cause of ALL TC038–TC046 failures — the XPath literally
 *   found zero elements because it searched for buttons that do not exist.
 *
 * ABOVE/BELOW — are real <button class="toggle-btn"> ✓
 *   BUT: identical toggle buttons also exist inside the edit-row inline form.
 *   Must scope to .form-card to avoid hitting edit-row buttons.
 *
 * DELETE BUTTON — class "delete-btn", title="Delete", text is "✕" (U+2715)
 *   Our code searched for "×" (U+00D7) — wrong Unicode character entirely.
 *   Fix: target by CSS class .delete-btn scoped to .threshold-item.
 *
 * THRESHOLD VALUE INPUT — <input type="number" class="form-control">
 *   Edit rows also have <input type="number"> — must scope to .form-card.
 *
 * METRIC SELECT — <select class="form-control">
 *   Must scope to .form-card to avoid any other selects.
 *
 * ERROR MESSAGE — <div class="alert alert-error">
 *   Fix: By.cssSelector(".alert-error") — exact and unambiguous.
 *
 * ACTIVE THRESHOLD ROWS — <div class="threshold-item"> inside .threshold-list ✓
 *   Two divs per saved threshold when editing (normal row + edit row).
 *   Must count only non-editing rows: .threshold-item:not(.threshold-item--editing)
 *
 * BACK BUTTON — <button class="btn-back"> ✓ same pattern as all other pages.
 */
public class SettingsPage extends BasePage {

    // ── Locators — derived directly from settings.html ────────────────────────

    /**
     * Page-load anchor: "Sensor Type" form label is the earliest stable element.
     * Real DOM: <label class="form-label">Sensor Type</label>
     */
    private static final By SENSOR_TYPE_LABEL =
            By.xpath("//label[contains(text(),'Sensor Type')]");

    /**
     * FIXED: Sensor type selectors are <label class="radio-option">, NOT buttons.
     * Real DOM:
     *   <label class="radio-option" [class.active]="sensorType === 'traffic'">
     *     <input type="radio" name="sensorType" value="traffic"/>
     *     <span class="radio-icon">🚗</span>
     *     <span>Traffic</span>
     *   </label>
     *
     * Strategy: click the <label> — this triggers the radio input's change event
     * via standard HTML label-for behaviour.
     * CSS targets the label by its text span content.
     */
    private static final By TRAFFIC_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Traffic']]");
    private static final By AIR_QUALITY_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Air Quality']]");
    private static final By STREET_LIGHT_LABEL =
            By.xpath("//label[contains(@class,'radio-option')][.//span[text()='Street Light']]");

    /**
     * Alternatively: click the hidden radio input directly via JS.
     * Used as fallback if clicking the label doesn't trigger Angular's (change) binding.
     * Real DOM: <input type="radio" name="sensorType" value="traffic"/>
     */
    private static final By TRAFFIC_RADIO      = By.cssSelector("input[value='traffic']");
    private static final By AIR_QUALITY_RADIO  = By.cssSelector("input[value='air']");
    private static final By STREET_LIGHT_RADIO = By.cssSelector("input[value='light']");

    /**
     * Metric dropdown — scoped to .form-card to avoid any future conflicts.
     * Real DOM: <select class="form-control" [(ngModel)]="metric">
     */
    private static final By METRIC_DROPDOWN =
            By.cssSelector(".form-card select.form-control");

    /**
     * Threshold value number input — scoped to .form-card.
     * Real DOM: <input type="number" class="form-control" [(ngModel)]="thresholdValue"/>
     * Edit rows also have number inputs — scoping prevents hitting them.
     */
    private static final By THRESHOLD_VALUE_INPUT =
            By.cssSelector(".form-card input[type='number']");

    /**
     * Above/Below toggle buttons — scoped to .form-card's .toggle-group.
     * Real DOM: <button class="toggle-btn" (click)="alertType = 'above'">↑ Above</button>
     * Edit rows contain identical buttons — scoping is essential.
     */
    private static final By ABOVE_BTN =
            By.cssSelector(".form-card .toggle-group .toggle-btn:first-child");
    private static final By BELOW_BTN =
            By.cssSelector(".form-card .toggle-group .toggle-btn:last-child");

    /**
     * Save Threshold button.
     * Real DOM: <button class="submit-btn" (click)="onSubmit()" [disabled]="isSubmitting">
     *   {{ isSubmitting ? 'Saving...' : 'Save Threshold' }}
     * </button>
     * Scoped to .form-card — the edit rows have their own Save buttons with class submit-btn--sm.
     */
    private static final By SAVE_THRESHOLD_BTN =
            By.cssSelector(".form-card .submit-btn:not(.submit-btn--sm)");

    /**
     * Active threshold rows — read-only (non-editing) rows only.
     * Real DOM: <div class="threshold-item"> (normal) and
     *           <div class="threshold-item threshold-item--editing"> (edit mode)
     * We count only normal rows so edit mode doesn't double-count.
     */
    private static final By ACTIVE_THRESHOLD_ROWS =
            By.cssSelector(".threshold-list .threshold-item:not(.threshold-item--editing)");

    /**
     * All threshold rows (including editing) — used for polling after delete.
     */
    private static final By ALL_THRESHOLD_ROWS =
            By.cssSelector(".threshold-list .threshold-item");

    /**
     * FIXED: Delete button — class "delete-btn", inside a normal (non-editing) row.
     * Real DOM: <button class="delete-btn" title="Delete">✕</button>
     * Previous code searched for "×" (wrong Unicode). CSS class is exact.
     */
    private static final By DELETE_BTN =
            By.cssSelector(".threshold-item:not(.threshold-item--editing) .delete-btn");

    /**
     * FIXED: Error message — exact CSS class from HTML.
     * Real DOM: <div class="alert alert-error">{{ errorMessage }}</div>
     */
    private static final By ERROR_MSG =
            By.cssSelector(".alert.alert-error");

    /**
     * Success message — used to confirm save completed.
     * Real DOM: <div class="alert alert-success">{{ successMessage }}</div>
     */
    private static final By SUCCESS_MSG =
            By.cssSelector(".alert.alert-success");

    /** Back button — confirmed same pattern as all pages. */
    private static final By BACK_BTN =
            By.cssSelector("button.btn-back");

    // ── Constructor ───────────────────────────────────────────────────────────

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    // ── JavaScript helpers ────────────────────────────────────────────────────

    /**
     * JS click — used for all button/label interactions.
     * Bypasses any overlay interception that Selenium's coordinate-based
     * click check would fail on.
     */
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
     * Selects a sensor type by clicking its <label class="radio-option">.
     *
     * Strategy A (primary): jsClick the <label> — triggers the radio + Angular (change).
     * Strategy B (fallback): directly set the radio input checked via JS and dispatch
     *   a 'change' event — guarantees Angular's ngModel picks up the value.
     *
     * After clicking, waits for the metric dropdown to refresh (confirms Angular
     * onSensorTypeChange() ran and repopulated availableMetrics).
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
                radioValue   = "air";
                break;
            case "street light": case "streetlight":
                labelLocator = STREET_LIGHT_LABEL;
                radioLocator = STREET_LIGHT_RADIO;
                radioValue   = "light";
                break;
            default:
                throw new IllegalArgumentException("Unknown sensor type: " + type);
        }

        final By finalLabel = labelLocator;
        final By finalRadio = radioLocator;
        final String finalValue = radioValue;

        RetryHelper.retryVoid(() -> {
            // Strategy A: click the label
            WebElement label = wait.waitForVisible(finalLabel);
            jsClick(label);

            // Verify Angular received the change by checking the radio is checked
            WebElement radio = driver.findElement(finalRadio);
            boolean isChecked = Boolean.parseBoolean(
                    radio.getAttribute("checked") != null ? "true" : "false");

            if (!isChecked) {
                // Strategy B: directly fire the radio change via JS
                System.out.println("[SETTINGS] Label click didn't check radio — using JS dispatch");
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].checked = true;" +
                    "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    radio);
            }

            // Wait for metric dropdown to reload (confirms onSensorTypeChange() ran)
            wait.waitForVisible(METRIC_DROPDOWN);
            System.out.println("[SETTINGS] Sensor type '" + finalValue + "' selected");
        }, "select sensor type: " + type);

        return this;
    }

    // ── Metric selection ──────────────────────────────────────────────────────

    /**
     * Metric index reference (from HTML + screenshots):
     *   Traffic:      0=Traffic Density,   1=Average Speed
     *   Air Quality:  0=Carbon Monoxide,   1=Ozone
     *   Street Light: 0=Brightness Level,  1=Power Consumption
     */
    public SettingsPage selectMetricByIndex(int index) {
        WebElement dropdown = wait.waitForVisible(METRIC_DROPDOWN);
        new Select(dropdown).selectByIndex(index);
        System.out.println("[SETTINGS] Metric index " + index + " selected");
        return this;
    }

    public SettingsPage selectMetric(String metricText) {
        WebElement dropdown = wait.waitForVisible(METRIC_DROPDOWN);
        new Select(dropdown).selectByVisibleText(metricText);
        return this;
    }

    // ── Threshold value ───────────────────────────────────────────────────────

    public SettingsPage enterThresholdValue(String value) {
        WebElement input = wait.waitForVisible(THRESHOLD_VALUE_INPUT);
        // JS clear handles cases where normal clear() leaves stale value in number inputs
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
        input.sendKeys(value);
        System.out.println("[SETTINGS] Threshold value set to: " + value);
        return this;
    }

    // ── Alert direction toggles ───────────────────────────────────────────────

    public SettingsPage clickAbove() {
        RetryHelper.retryVoid(() -> jsClick(ABOVE_BTN), "click Above toggle");
        return this;
    }

    public SettingsPage clickBelow() {
        RetryHelper.retryVoid(() -> jsClick(BELOW_BTN), "click Below toggle");
        return this;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public SettingsPage clickSaveThreshold() {
        RetryHelper.retryVoid(() -> jsClick(SAVE_THRESHOLD_BTN), "click Save Threshold");
        // Wait for either success or error message to confirm Angular processed the submit
        try {
            wait.waitForCondition(d ->
                !d.findElements(SUCCESS_MSG).isEmpty() ||
                !d.findElements(ERROR_MSG).isEmpty());
        } catch (TimeoutException ignored) {
            System.out.println("[SETTINGS] No success/error message after save — continuing");
        }
        return this;
    }

    /** Full create-threshold flow. */
    public SettingsPage createThreshold(String sensorType, int metricIndex,
                                        int thresholdValue, boolean above) {
        selectSensorType(sensorType);
        selectMetricByIndex(metricIndex);
        enterThresholdValue(String.valueOf(thresholdValue));
        if (above) clickAbove(); else clickBelow();
        clickSaveThreshold();
        return this;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes the first active threshold.
     *
     * The delete button: <button class="delete-btn" title="Delete">✕</button>
     * Note: while deleting, Angular sets deletingId = t.id and shows "…" instead
     * of "✕" and disables the button — the wait-for-count-decrease handles this.
     */
    public SettingsPage deleteFirstActiveThreshold() {
        List<WebElement> deleteButtons = driver.findElements(DELETE_BTN);
        if (deleteButtons.isEmpty()) {
            System.out.println("[SETTINGS] No delete buttons found");
            return this;
        }

        int countBefore = getActiveThresholdCount();
        System.out.println("[SETTINGS] Deleting — count before: " + countBefore);

        RetryHelper.retryVoid(() -> jsClick(deleteButtons.get(0)), "click delete button");

        // Wait for async API DELETE to reflect in DOM
        try {
            wait.waitForCondition(d -> {
                int current = d.findElements(ACTIVE_THRESHOLD_ROWS).size();
                System.out.println("[SETTINGS] Delete poll: " + current + " rows (was " + countBefore + ")");
                return current < countBefore;
            });
        } catch (TimeoutException e) {
            System.out.println("[SETTINGS] Count did not decrease after delete");
        }
        return this;
    }

    // ── Back ──────────────────────────────────────────────────────────────────

    public SettingsPage clickBack() {
        RetryHelper.retryVoid(() -> jsClick(BACK_BTN), "click Back");
        return this;
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public boolean isOnSettingsPage() { return urlContains("/settings"); }

    public List<WebElement> getActiveThresholds() {
        List<WebElement> rows = driver.findElements(ACTIVE_THRESHOLD_ROWS);
        System.out.println("[SETTINGS] Active threshold rows: " + rows.size());
        return rows;
    }

    public int getActiveThresholdCount() { return getActiveThresholds().size(); }

    /**
     * Checks for the .alert-error div rendered by Angular when errorMessage is set.
     * Real DOM: <div class="alert alert-error">{{ errorMessage }}</div>
     */
    public boolean isThresholdErrorDisplayed() {
        try {
            List<WebElement> errors = driver.findElements(ERROR_MSG);
            for (WebElement e : errors) {
                if (e.isDisplayed() && !e.getText().trim().isEmpty()) {
                    System.out.println("[SETTINGS] Error message: " + e.getText().trim());
                    return true;
                }
            }
            return false;
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            List<WebElement> msgs = driver.findElements(SUCCESS_MSG);
            return msgs.stream().anyMatch(e -> e.isDisplayed() && !e.getText().trim().isEmpty());
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks if Traffic label has the Angular [class.active] applied.
     * Real DOM: <label class="radio-option active"> when sensorType === 'traffic'
     */
    public boolean isTrafficBtnSelected() {
        try {
            WebElement label = driver.findElement(TRAFFIC_LABEL);
            String cls = label.getAttribute("class");
            System.out.println("[SETTINGS] Traffic label class: " + cls);
            return cls != null && cls.contains("active");
        } catch (Exception e) {
            return false;
        }
    }
}
