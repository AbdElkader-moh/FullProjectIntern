package com.internship.utils;

import java.util.function.Supplier;

/**
 * RetryHelper: Generic configurable retry wrapper.
 * Retry count and interval are always read from ConfigReader — never hardcoded.
 *
 * Usage:
 *   RetryHelper.retry(() -> driver.findElement(By.cssSelector(".bell")), "find bell");
 *   RetryHelper.retryVoid(() -> page.clickBack(), "click Back button");
 *   boolean ok = RetryHelper.retryUntilTrue(() -> page.isBellVisible(), "bell visible");
 */
public final class RetryHelper {

    private RetryHelper() {}

    // ── Core ─────────────────────────────────────────────────────────────────

    public static <T> T retry(Supplier<T> action, int maxAttempts,
                               int intervalMs, String description) {
        Exception last = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                System.out.println("[RETRY] Attempt " + i + "/" + maxAttempts + ": " + description);
                T result = action.get();
                if (i > 1) System.out.println("[RETRY] Succeeded on attempt " + i);
                return result;
            } catch (Exception e) {
                last = e;
                System.out.println("[RETRY] Attempt " + i + " failed: " + e.getMessage());
                if (i < maxAttempts) sleep(intervalMs);
            }
        }
        throw new RuntimeException("All " + maxAttempts + " attempts failed: " + description, last);
    }

    // ── Convenience overloads ─────────────────────────────────────────────────

    /** Uses retry.attempts and retry.interval.ms from config. */
    public static <T> T retry(Supplier<T> action, String description) {
        return retry(action,
                ConfigReader.getRetryAttempts(),
                ConfigReader.getRetryIntervalMs(),
                description);
    }

    public static void retryVoid(Runnable action, String description) {
        retry(() -> { action.run(); return null; },
                ConfigReader.getRetryAttempts(),
                ConfigReader.getRetryIntervalMs(),
                description);
    }

    /**
     * Retries a boolean check; returns false (not throws) when all attempts fail.
     * Useful for soft checks where you want to assert separately.
     */
    public static boolean retryUntilTrue(Supplier<Boolean> predicate, String description) {
        int maxAttempts = ConfigReader.getRetryAttempts();
        int intervalMs  = ConfigReader.getRetryIntervalMs();
        for (int i = 1; i <= maxAttempts; i++) {
            System.out.println("[RETRY] Boolean check " + i + "/" + maxAttempts + ": " + description);
            try {
                if (Boolean.TRUE.equals(predicate.get())) {
                    System.out.println("[RETRY] Condition met on attempt " + i);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("[RETRY] Exception on attempt " + i + ": " + e.getMessage());
            }
            if (i < maxAttempts) sleep(intervalMs);
        }
        System.out.println("[RETRY] Condition never met after " + maxAttempts + " attempts");
        return false;
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
