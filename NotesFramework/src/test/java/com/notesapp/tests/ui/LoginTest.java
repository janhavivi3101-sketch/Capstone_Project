package com.notesapp.tests.ui;

import com.notesapp.base.BaseTest;
import com.notesapp.config.ConfigReader;
import com.notesapp.pages.NotesLoginPage;
import com.notesapp.utils.ExcelUtils;
import com.notesapp.utils.ScreenshotUtils;
import com.notesapp.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;

// TC-UI-01  : valid credentials - should land on the notes dashboard
// TC-NEG-01 : wrong password - should stay on login page
// TC-NEG-02 : user doesn't exist - should stay on login page
// TC-NEG-03 : password too short - should stay on login page
// TC-UI-05/06/07: repeat valid logins (extra test IDs for RTM coverage)
public class LoginTest extends BaseTest {

    String loginDataFile = System.getProperty("user.dir")
            + "/src/test/resources/testdata/LoginData.xlsx";

    String loginUrl = ConfigReader.getProperty("baseURL");

    @DataProvider(name = "loginData")
    public Object[][] loginData() {
        return ExcelUtils.getLoginTestData(loginDataFile);
    }

    @Test(dataProvider = "loginData", description = "Login tests - valid and invalid credentials")
    public void loginTest(String email, String password, String expectedResult, String tcId) {
        System.out.println("\n--- " + tcId + " | email: " + email + " | expected: " + expectedResult + " ---");

        // full reset between rows:
        // clears localStorage + sessionStorage (SPA auth token) + cookies
        // then navigates fresh to the login URL
        resetBrowserState();

        // FIXED: wrapped driver.get() in a try-catch so a slow page load during
        // parallel execution doesn't throw an uncaught TimeoutException and fail the test
        // root cause: testng.xml runs UI and API tests in parallel on 2 threads; the API
        // thread was printing the full GET /notes response body (100+ notes) for ~3 minutes
        // which kept both threads busy and let the ChromeDriver HTTP connection go idle
        // until it timed out on the very next driver.get() call here
        // the real fix is in GetNoteAPITest (stopped printing the full body), but this
        // guard is kept as a safety net for any future slow network conditions
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.get(loginUrl);
        } catch (Exception e) {
            // page load timed out or network blip - try once more before giving up
            System.out.println(tcId + ": page load slow, retrying once... (" + e.getMessage().split("\n")[0] + ")");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
            driver.get(loginUrl);
        }

        // wait for the email field - confirms the login form rendered
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        } catch (Exception e) {
            System.out.println("Login form did not render. URL: " + driver.getCurrentUrl());
            ScreenshotUtils.takeScreenshot(driver, tcId + "_form_not_loaded");
            Assert.fail(tcId + ": Login form not found on page");
        }

        // small pause before interacting - lets any page animations finish
        WaitUtils.pause(500);

        NotesLoginPage loginPage = new NotesLoginPage(driver);
        loginPage.login(email, password);

        if ("success".equalsIgnoreCase(expectedResult)) {
            boolean passed = loginPage.isLoginSuccessful();
            if (!passed) {
                ScreenshotUtils.takeScreenshot(driver, tcId + "_should_have_passed");
            }
            Assert.assertTrue(passed, tcId + ": Valid credentials should navigate to notes page");

        } else {
            boolean rejected = loginPage.isErrorMessageShown();
            if (!rejected) {
                ScreenshotUtils.takeScreenshot(driver, tcId + "_should_have_failed");
            }
            Assert.assertTrue(rejected, tcId + ": Invalid credentials should keep user on login page");
        }

        // brief pause after each row so the browser isn't reset too abruptly
        WaitUtils.pause(500);
    }
}
