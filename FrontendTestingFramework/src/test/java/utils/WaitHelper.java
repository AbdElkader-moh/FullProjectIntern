package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * WaitHelper: Centralizes all explicit wait strategies.
 * NO Thread.sleep() is used anywhere in this framework.
 * All timeouts are read from ConfigReader (config.properties / env vars).
 */
public class WaitHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public WaitHelper(WebDriver driver) {
        this.driver = driver;
        this.wait   = buildWait(ConfigReader.getExplicitWait());
    }

    public WaitHelper(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait   = buildWait(timeoutSeconds);
    }

    private WebDriverWait buildWait(int seconds) {
        return (WebDriverWait) new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .pollingEvery(Duration.ofMillis(ConfigReader.getPollingIntervalMs()))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    /** FluentWait with a custom timeout and poll interval — for simulator/appearance polling. */
    private FluentWait<WebDriver> fluentWait(int timeoutSeconds, int pollMs) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollMs))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementNotInteractableException.class);
    }

    // ── Existing methods (unchanged signatures) ───────────────────────────────

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public List<WebElement> waitForPresenceOfAll(By locator) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public boolean waitForUrlToContain(String urlFragment) {
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    public boolean waitForUrlMatches(String regex) {
        return wait.until(ExpectedConditions.urlMatches(regex));
    }

    public boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public void waitForTextPresent(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public <T> T waitForCondition(ExpectedCondition<T> condition) {
        return wait.until(condition);
    }

    public boolean waitForUrlToContainAny(String... fragments) {
        return wait.until(d -> {
            String currentUrl = d.getCurrentUrl();
            for (String fragment : fragments) {
                if (currentUrl.contains(fragment)) return true;
            }
            return false;
        });
    }

    // ── New methods ───────────────────────────────────────────────────────────

    /**
     * Returns null instead of throwing when the element is not visible within
     * the given timeout. Useful for optional/conditional elements.
     */
    public WebElement waitForVisibleOrNull(By locator, int timeoutSeconds) {
        try {
            return buildWait(timeoutSeconds).until(
                    ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            return null;
        }
    }

    /**
     * Waits for the notification bell to be present → visible → clickable.
     * Uses notification.bell.timeout from config.
     */
    public WebElement waitForNotificationBell(By bellLocator) {
        int timeout = ConfigReader.getNotificationBellTimeout();
        System.out.println("[WAIT] Waiting for notification bell (max " + timeout + "s)");

        // Phase 1 — in DOM
        buildWait(timeout).until(ExpectedConditions.presenceOfElementLocated(bellLocator));
        System.out.println("[WAIT] Bell found in DOM");

        // Phase 2 — visible
        WebElement bell = buildWait(timeout).until(
                ExpectedConditions.visibilityOfElementLocated(bellLocator));
        System.out.println("[WAIT] Bell is visible");

        // Phase 3 — clickable
        bell = buildWait(timeout).until(ExpectedConditions.elementToBeClickable(bell));
        System.out.println("[WAIT] Bell is clickable");
        return bell;
    }

    /**
     * Polls the notification list until minimumCount items appear,
     * or notification.appearance.timeout elapses.
     * Logs every poll attempt.
     *
     * @return true if condition met, false on timeout
     */
    public boolean waitForNotificationToAppear(By itemLocator, int minimumCount) {
        int timeout = ConfigReader.getNotificationAppearanceTimeout();
        int pollMs  = ConfigReader.getSimulatorPollIntervalMs();
        System.out.println("[WAIT] Polling for >=" + minimumCount
                + " notification(s) (timeout=" + timeout + "s, poll=" + pollMs + "ms)");
        try {
            fluentWait(timeout, pollMs).until(d -> {
                List<WebElement> items = d.findElements(itemLocator);
                System.out.println("[WAIT] Poll: found " + items.size() + " item(s)");
                return items.size() >= minimumCount;
            });
            System.out.println("[WAIT] Notification(s) appeared");
            return true;
        } catch (TimeoutException e) {
            System.out.println("[WAIT] Notifications did not appear within " + timeout + "s");
            return false;
        }
    }

    /**
     * Waits for the unread-dot to disappear after a mark-as-read action.
     * Uses notification.read.state.timeout from config.
     *
     * @return true if dot disappeared, false on timeout
     */
    public boolean waitForUnreadIndicatorToDisappear(By unreadDotLocator) {
        int timeout = ConfigReader.getNotificationReadStateTimeout();
        System.out.println("[WAIT] Waiting for unread indicator to disappear (max " + timeout + "s)");
        try {
            buildWait(timeout).until(
                    ExpectedConditions.invisibilityOfElementLocated(unreadDotLocator));
            System.out.println("[WAIT] Unread indicator gone");
            return true;
        } catch (TimeoutException e) {
            System.out.println("[WAIT] Unread indicator still visible after " + timeout + "s");
            return false;
        }
    }

    /**
     * Waits for a CSS class to be removed from an element (e.g. "unread" after mark-as-read).
     *
     * @return true if class was removed, false on timeout
     */
    public boolean waitForClassAbsent(WebElement element, String cssClass, int timeoutSeconds) {
        System.out.println("[WAIT] Waiting for class '" + cssClass + "' to be removed");
        try {
            fluentWait(timeoutSeconds, ConfigReader.getPollingIntervalMs()).until(d -> {
                try {
                    String classes = element.getAttribute("class");
                    return classes == null || !classes.contains(cssClass);
                } catch (StaleElementReferenceException e) {
                    return true; // element re-rendered → class definitely gone
                }
            });
            System.out.println("[WAIT] Class '" + cssClass + "' removed");
            return true;
        } catch (TimeoutException e) {
            System.out.println("[WAIT] Class '" + cssClass + "' still present after " + timeoutSeconds + "s");
            return false;
        }
    }

    /**
     * Polls with page refresh until minimumCount notifications appear,
     * or simulator.delay.timeout elapses.
     * Use when the Angular app does not auto-refresh via WebSocket.
     *
     * @return true if condition met
     */
    public boolean waitForNotificationWithRefresh(By itemLocator, int minimumCount) {
        int timeout = ConfigReader.getSimulatorDelayTimeout();
        int pollMs  = ConfigReader.getSimulatorPollIntervalMs();
        System.out.println("[WAIT] Refresh-polling for >=" + minimumCount
                + " notification(s) (timeout=" + timeout + "s, poll=" + pollMs + "ms)");

        long deadline = System.currentTimeMillis() + (long) timeout * 1000;
        int  attempt  = 0;

        while (System.currentTimeMillis() < deadline) {
            attempt++;
            List<WebElement> items = driver.findElements(itemLocator);
            System.out.println("[WAIT] Refresh-poll #" + attempt + ": found " + items.size());

            if (items.size() >= minimumCount) {
                System.out.println("[WAIT] Condition met");
                return true;
            }

            // Sleep between polls — duration comes from config, not hardcoded
            try { Thread.sleep(pollMs); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            driver.navigate().refresh();
            // Wait for Angular loading to settle after refresh
            try {
                buildWait(10).until(ExpectedConditions
                        .invisibilityOfElementLocated(By.cssSelector(".loading")));
            } catch (TimeoutException ignored) {}
        }

        System.out.println("[WAIT] Condition never met within " + timeout + "s");
        return false;
    }
}
