package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ExcelDataReader — reads test data from test_data.xlsx using Apache POI.
 *
 * Sheet structure (all sheets share the same conventions):
 *   Row 1  → header row (skipped)
 *   Row 2+ → data rows
 *
 * The workbook is loaded ONCE per JVM run and cached.
 * All public methods are static so callers need no instance.
 *
 * ── Sheet reference ──────────────────────────────────────────────────────────
 *  "Credentials"   → key / value / description
 *  "SignUp"        → tcId / firstName / lastName / emailPrefix / password /
 *                    imagePath / expectedResult / description
 *  "SignIn"        → tcId / email / password / expectedResult / description
 *  "Thresholds"    → tcId / sensor / metricIndex / value / direction /
 *                    expectedResult / description
 *  "BoundaryValues"→ key / value / description
 */
public class ExcelDataReader {

    private static final String EXCEL_PATH =
            ConfigReader.get("test.data.excel.path", "src/test/resources/test_data.xlsx");

    /** Cached workbook — loaded once, reused for every read call. */
    private static Workbook workbook;

    static {
        try (FileInputStream fis = new FileInputStream(EXCEL_PATH)) {
            workbook = new XSSFWorkbook(fis);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot load test data Excel file at: " + EXCEL_PATH
                    + ". Make sure test_data.xlsx is placed in src/test/resources/ "
                    + "or override the path with -Dtest.data.excel.path=...", e);
        }
    }

    private ExcelDataReader() {}

    // ── Core reader ───────────────────────────────────────────────────────────

    /**
     * Reads a sheet and returns all data rows as an array of String arrays.
     * Row 0 of the result is the HEADER row; Row 1 onward is data.
     * Use getSheetData() when you want TestNG @DataProvider style Object[][].
     *
     * @param sheetName  Excel sheet name (e.g. "SignUp")
     * @return           String[rows][cols], row 0 = headers
     */
    public static String[][] getSheetData(String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException(
                    "Sheet '" + sheetName + "' not found in " + EXCEL_PATH);
        }

        int rowCount = sheet.getLastRowNum() + 1;                   // 1-indexed
        int colCount = sheet.getRow(0).getLastCellNum();

        String[][] data = new String[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < colCount; c++) {
                data[r][c] = getCellValueAsString(row.getCell(c));
            }
        }
        return data;
    }

    /**
     * Returns data rows ONLY (skips the header row).
     * Suitable for direct use in TestNG @DataProvider methods.
     */
    public static Object[][] getDataRows(String sheetName) {
        String[][] all  = getSheetData(sheetName);
        Object[][] data = new Object[all.length - 1][all[0].length];
        for (int i = 1; i < all.length; i++) {
            data[i - 1] = all[i];
        }
        return data;
    }

    // ── Credentials sheet helpers ─────────────────────────────────────────────

    /**
     * Loads the "Credentials" sheet into a key→value map.
     * Column layout: key (col 0), value (col 1).
     */
    public static Map<String, String> getCredentials() {
        String[][] rows = getSheetData("Credentials");
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i < rows.length; i++) {         // skip header
            if (rows[i][0] != null && !rows[i][0].isEmpty()) {
                map.put(rows[i][0].trim(), rows[i][1] != null ? rows[i][1].trim() : "");
            }
        }
        return map;
    }

    /** Convenience: get a single credential value by key. */
    public static String getCredential(String key) {
        Map<String, String> creds = getCredentials();
        if (!creds.containsKey(key)) {
            throw new RuntimeException(
                    "Credential key '" + key + "' not found in Credentials sheet.");
        }
        return creds.get(key);
    }

    // ── BoundaryValues sheet helpers ──────────────────────────────────────────

    /**
     * Loads the "BoundaryValues" sheet into a key→value map.
     * Mirrors the old static methods in TestDataProvider.
     */
    public static Map<String, String> getBoundaryValues() {
        String[][] rows = getSheetData("BoundaryValues");
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i < rows.length; i++) {
            if (rows[i][0] != null && !rows[i][0].isEmpty()) {
                map.put(rows[i][0].trim(), rows[i][1] != null ? rows[i][1].trim() : "");
            }
        }
        return map;
    }

    /** Convenience: get a single boundary value by key. */
    public static String getBoundaryValue(String key) {
        Map<String, String> bv = getBoundaryValues();
        if (!bv.containsKey(key)) {
            throw new RuntimeException(
                    "BoundaryValues key '" + key + "' not found.");
        }
        return bv.get(key);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: {
                double d = cell.getNumericCellValue();
                // Return integer string if the value has no fractional part
                return (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCachedFormulaResultType() == CellType.STRING
                    ? cell.getStringCellValue().trim()
                    : String.valueOf(cell.getNumericCellValue());
            default:      return "";
        }
    }
}
