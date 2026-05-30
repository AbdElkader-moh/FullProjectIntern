package data;
<<<<<<< HEAD
=======
import utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
>>>>>>> b485ec14d5e88360bd0794f0fa63bdb60e3edea4

/**
 * TestDataProvider — Excel-driven replacement for the old static-constant class.
 *
 * All data now comes from test_data.xlsx (src/test/resources/).
 * The static helper methods preserve the exact same API as before so that
 * all existing test classes compile without changes.
 *
 * New: @DataProvider methods supply parametric data to tests that use
 * TestNG's data-driven approach (see SignUpTest, SignInTest, SettingsTest).
 *
 * ── How it works ─────────────────────────────────────────────────────────────
 *  ExcelDataReader.getBoundaryValue(key) → reads the "BoundaryValues" sheet.
 *  ExcelDataReader.getCredential(key)    → reads the "Credentials" sheet.
 *  ExcelDataReader.getDataRows(sheet)    → returns Object[][] for @DataProvider.
 */
public class TestDataProvider {

    private TestDataProvider() {}

    // ─── Email generators (unchanged API) ────────────────────────────────────

    public static String generateUniqueEmail() {
        return "tc_auto_" + System.currentTimeMillis() + "@testdomain.com";
    }

    public static String generateUniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@testdomain.com";
    }

    // ─── Name data (now Excel-backed) ────────────────────────────────────────

    public static String validFirstName()      { return bv("validFirstName"); }
    public static String validLastName()       { return bv("validLastName"); }
    public static String minLengthFirstName()  { return bv("minFirstName"); }
    public static String minLengthLastName()   { return bv("minLastName"); }
    public static String tooShortFirstName()   { return bv("tooShortFirstName"); }
    public static String tooShortLastName()    { return bv("tooShortLastName"); }

    // ─── Password data (now Excel-backed) ────────────────────────────────────

    public static String validPassword()       { return bv("validPassword"); }
    public static String minLengthPassword()   { return bv("minPassword"); }
    public static String tooShortPassword()    { return bv("tooShortPassword"); }
    public static String wrongPassword()       { return bv("wrongPassword"); }

    // ─── Email data (now Excel-backed) ───────────────────────────────────────

    public static String invalidEmailNoAt()        { return bv("invalidEmailNoAt"); }
    public static String invalidEmailNoAtSymbol()  { return bv("invalidEmailNoAtSymbol"); }
    public static String invalidEmailNoAfterAt()   { return bv("invalidEmailNoAfterAt"); }
    public static String nonExistentEmail()        {
        return "ghost_" + System.currentTimeMillis() + "@nowhere.com";
    }

    // ─── Threshold data (now Excel-backed) ───────────────────────────────────

    public static int validThresholdValue() { return Integer.parseInt(bv("validThreshold")); }
    public static int minThresholdValue()   { return Integer.parseInt(bv("minThreshold")); }

    // ─── Credentials (replaces ConfigReader.getEmail/getPassword) ────────────

    /**
     * Returns the default login email from the Credentials sheet.
     * ConfigReader still works for infrastructure config (URL, browser, timeouts).
     * Only human-readable test inputs live here.
     */
    public static String getEmail()    { return ExcelDataReader.getCredential("app.email"); }
    public static String getPassword() { return ExcelDataReader.getCredential("app.password"); }
    public static String getDuplicateEmail() {
        return ExcelDataReader.getCredential("signup.duplicate.email");
    }

    // ─── @DataProvider methods for parametric tests ───────────────────────────

    /**
     * Provides all SignUp rows from the "SignUp" sheet.
     *
     * Column order: tcId, firstName, lastName, emailPrefix, password,
     *               imagePath, expectedResult, description
     *
     * Usage in test:
     *   @Test(dataProvider = "signUpData", dataProviderClass = TestDataProvider.class)
     *   public void myTest(String tcId, String firstName, ...) { ... }
     */
    @DataProvider(name = "signUpData")
    public static Object[][] signUpData() {
        return ExcelDataReader.getDataRows("SignUp");
    }

    /**
     * Provides all SignIn rows from the "SignIn" sheet.
     *
     * Column order: tcId, email, password, expectedResult, description
     */
    @DataProvider(name = "signInData")
    public static Object[][] signInData() {
        return ExcelDataReader.getDataRows("SignIn");
    }

    /**
     * Provides all Threshold rows from the "Thresholds" sheet.
     *
     * Column order: tcId, sensor, metricIndex, value, direction,
     *               expectedResult, description
     */
    @DataProvider(name = "thresholdData")
    public static Object[][] thresholdData() {
        return ExcelDataReader.getDataRows("Thresholds");
    }

    // ─── Private convenience helper ───────────────────────────────────────────

    private static String bv(String key) {
        return ExcelDataReader.getBoundaryValue(key);
    }
}
