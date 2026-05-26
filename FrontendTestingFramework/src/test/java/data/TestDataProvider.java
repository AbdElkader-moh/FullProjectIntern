package data;

/**
 * TestDataProvider: Centralizes all test data generation and retrieval.
 * No credentials or PII are hardcoded here; use ConfigReader for sensitive values.
 */
public class TestDataProvider {

    private TestDataProvider() {}

    // ─── Email generators ───────────────────────────────────────────────────

    /** Generates a unique email to avoid duplicate-registration conflicts */
    public static String generateUniqueEmail() {
        return "tc_auto_" + System.currentTimeMillis() + "@testdomain.com";
    }

    public static String generateUniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@testdomain.com";
    }

    // ─── Name data ──────────────────────────────────────────────────────────

    public static String validFirstName() { return "TestFirst"; }
    public static String validLastName()  { return "TestLast"; }

    public static String minLengthFirstName() { return "Ab"; }   // exactly 2 chars
    public static String minLengthLastName()  { return "Mo"; }   // exactly 2 chars

    public static String tooShortFirstName() { return "A"; }     // 1 char – invalid
    public static String tooShortLastName()  { return "B"; }     // 1 char – invalid

    // ─── Password data ──────────────────────────────────────────────────────

    public static String validPassword()       { return "Secure@123"; }
    public static String minLengthPassword()   { return "abc123"; }   // exactly 6 chars
    public static String tooShortPassword()    { return "abc12"; }    // 5 chars – invalid
    public static String wrongPassword()       { return "WrongPass!"; }

    // ─── Email data ─────────────────────────────────────────────────────────

    public static String invalidEmailNoAt()        { return "notanemail"; }
    public static String invalidEmailNoAtSymbol()  { return "ahmedtest.com"; }
    public static String invalidEmailNoAfterAt()   { return "ahmed@"; }
    public static String nonExistentEmail()        { return "ghost_" + System.currentTimeMillis() + "@nowhere.com"; }

    // ─── Threshold data (Settings page) ─────────────────────────────────────

    public static int validThresholdValue()   { return 50; }
    public static int minThresholdValue()     { return 0; }
    public static int maxThresholdValue()     { return 500; }
    public static int aboveMaxThreshold()     { return 501; }
    public static int belowMinThreshold()     { return -1; }
}
