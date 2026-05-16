# FrontendTestingFramework

A production-ready Selenium + TestNG automation framework in Java following the **Page Object Model (POM)**.

---

## Project Structure

```
FrontendTestingFramework/
├── pom.xml                          Maven build file
├── testng.xml                       TestNG suite definition
├── config.properties                Environment-specific config (no secrets!)
│
└── src/test/java/
    ├── base/
    │   └── BaseTest.java            Driver lifecycle + shared setup/teardown
    ├── pages/
    │   ├── BasePage.java            Parent page with shared helpers
    │   ├── SignUpPage.java          /signup
    │   ├── SignInPage.java          /signin
    │   ├── HomePage.java            /home
    │   ├── ProfilePage.java         /profile
    │   ├── SettingsPage.java        /settings (Alert Thresholds)
    │   └── NotificationsPage.java   /notifications
    ├── tests/
    │   ├── SignUpTest.java           TC-001 – TC-014
    │   ├── SignInTest.java           TC-015 – TC-023
    │   ├── ProfileTest.java         TC-024 – TC-030
    │   ├── HomeTest.java            TC-031 – TC-036
    │   ├── SettingsTest.java        TC-037 – TC-046  (Sprint 2)
    │   └── NotificationsTest.java   TC-047 – TC-052  (Sprint 2)
    ├── utils/
    │   ├── ConfigReader.java        Priority config loader (props → env → -D)
    │   ├── DriverFactory.java       Thread-safe WebDriver management
    │   ├── WaitHelper.java          Centralised ExplicitWait (no Thread.sleep)
    │   └── ScreenshotHelper.java    On-failure screenshot capture
    ├── data/
    │   └── TestDataProvider.java    All test data constants & generators
    └── listeners/
        ├── TestListener.java        Console logging + screenshot on failure
        └── ExtentReportListener.java  HTML Extent Report generation
```

---

## Prerequisites

| Requirement          | Version         |
|----------------------|-----------------|
| Java JDK             | 11+             |
| Maven                | 3.8+            |
| Google Chrome        | Latest stable   |
| Application running  | localhost:4200  |

---

## Quick Setup

### 1. Clone / Extract the project

```bash
unzip FrontendTestingFramework.zip
cd FrontendTestingFramework
```

### 2. Add a test profile image

Copy any small JPG/PNG to:
```
src/test/resources/test-image.png
```

Or update `config.properties` to point to an existing image:
```properties
signup.image.path=C:/Users/yourname/Pictures/profile.png
```

### 3. Configure credentials

**Option A – config.properties (local dev only, never commit real creds):**
```properties
app.email=your-test-email@example.com
app.password=YourPassword
signup.duplicate.email=an-existing-account@example.com
```

**Option B – Environment variables (recommended for CI/CD):**
```bash
export APP_EMAIL=your-test-email@example.com
export APP_PASSWORD=YourPassword
export SIGNUP_DUPLICATE_EMAIL=an-existing-account@example.com
```

**Option C – Maven -D flags:**
```bash
mvn clean test -Dapp.email=x@x.com -Dapp.password=secret
```

---

## Running Tests

### Full suite
```bash
mvn clean test
```

### Single test class
```bash
mvn clean test -Dtest=SignInTest
```

### Headless Chrome (for CI)
```bash
mvn clean test -Dheadless=true
```

### Specific TestNG group
```bash
mvn clean test -Dgroups=signup
```

---

## Test Coverage Matrix

| Module          | Test Class             | TCs               | Sprint   |
|-----------------|------------------------|-------------------|----------|
| Sign Up         | `SignUpTest`           | TC-001 – TC-014   | Sprint 1 |
| Sign In         | `SignInTest`           | TC-015 – TC-023   | Sprint 1 |
| Profile         | `ProfileTest`          | TC-024 – TC-030   | Sprint 1 |
| Home            | `HomeTest`             | TC-031 – TC-036   | Sprint 1 |
| Settings        | `SettingsTest`         | TC-037 – TC-046   | Sprint 2 |
| Notifications   | `NotificationsTest`    | TC-047 – TC-052   | Sprint 2 |

---

## Reports

After a run, HTML reports are generated in:
```
reports/Report_YYYYMMDD_HHmmss.html
```

Screenshots on failure:
```
reports/screenshots/<TestName>_YYYYMMDD_HHmmss.png
```

Open the HTML file in any browser for a full interactive dashboard.

---

## What Changed vs. Old Sprint 1 Tests

| Issue in Old Code                        | Fix in New Framework                          |
|------------------------------------------|-----------------------------------------------|
| Hardcoded emails/passwords in tests      | `ConfigReader` + env vars + `TestDataProvider`|
| `driver` created inside every test class | `DriverFactory` (thread-safe, singleton)      |
| `WebDriverWait` created ad-hoc per test  | `WaitHelper` injected from `BasePage`         |
| Locators mixed into test logic           | All locators isolated in `Page` classes       |
| No POM – Selenium calls in test methods  | Full POM: test → page → locator              |
| `Thread.sleep()` used                    | Replaced with `ExplicitWait` everywhere       |
| No reporting                             | Extent Reports + listener-driven screenshots  |
| `@BeforeClass` login repeated every file | `BaseTest.loginWithDefaultUser()` reused      |
| Absolute Windows paths hardcoded         | `config.properties` / relative paths          |

---

## CI/CD Integration (GitHub Actions example)

```yaml
- name: Run Selenium Tests
  env:
    APP_EMAIL: ${{ secrets.APP_EMAIL }}
    APP_PASSWORD: ${{ secrets.APP_PASSWORD }}
  run: mvn clean test -Dheadless=true
```
