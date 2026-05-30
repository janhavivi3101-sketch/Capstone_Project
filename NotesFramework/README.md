# HybridNotesFramework

A Selenium Java test automation framework for the Notes demo app at  
`https://practice.expandtesting.com/notes/app`

---

## How to run

```bash
# single test class
mvn test -Dtest=LoginTest
mvn test -Dtest=CreateNoteTest
mvn test -Dtest=LoginAPITest
mvn test -Dtest=CreateNoteAPITest
mvn test -Dtest=GetNoteAPITest
mvn test -Dtest=UpdateNoteAPITest
mvn test -Dtest=DeleteNoteAPITest
mvn test -Dtest=HybridTest

# full suite (UI + API + E2E in parallel)
mvn test

# headless mode for CI
mvn test -Dheadless=true
```
# Step 1 — Install Node dependencies

```bash
cd fixfw
npm install
```

## Step 2 — Start the MCP server

```bash
node server.js
```

You should see:
```
[REST] Self-healing endpoint ready on http://127.0.0.1:3000/suggest-locator
Capstone MCP Server running (stdio + REST :3000)
```

Keep this terminal open while tests run.

## Step 3 — Run the tests

In a second terminal:

```bash
# All tests
mvn clean test

# E2E only
mvn clean test -Dtest=HybridTest

# Single test
mvn clean test -Dtest=HybridTest#loginViaMCPScript
```

## Step 4 — View Allure report

```bash
mvn allure:serve


# In NotesLoginPage.java, the email field locator was changed from:
By emailField = By.id("email");  # correct, always worked

# to:
   By emailField     = By.id("email-field-v2");
    By emailFallback1 = By.cssSelector("input[type='email']");
    By emailFallback2 = By.name("email");



mvn clean test -Dtest=LoginTest



package com.notesapp.pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.notesapp.agentic.SelfHealingLocator;

public class NotesLoginPage {

    WebDriver driver;

    // PRIMARY locator is intentionally broken to simulate a UI change.
    // Self-healing will fail on "email-field-v2" and recover via fallbacks.
    By emailField     = By.id("email-field-v2");
    By emailFallback1 = By.cssSelector("input[type='email']");
    By emailFallback2 = By.name("email");

    By passwordField = By.id("password");
    By loginButton   = By.xpath("//button[@type='submit']");

    public NotesLoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public void login(String email, String password) {
        WebElement emailEl = SelfHealingLocator.find(
                driver,
                emailField,
                emailFallback1,
                emailFallback2
        );
        emailEl.clear();
        emailEl.sendKeys(email);

        driver.findElement(passwordField).clear();
        driver.findElement(passwordField).sendKeys(password);

        WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(loginButton));
        try {
            btn.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("document.querySelector('button[type=submit]').click();");
        }
    }

    public boolean isLoginSuccessful() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.getCurrentUrl().contains("login"));
            System.out.println("After login URL: " + driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            System.out.println("Still on login page: " + driver.getCurrentUrl());
            return false;
        }
    }

    public boolean isErrorMessageShown() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(d -> d.getCurrentUrl().contains("login"));
            System.out.println("Login rejected - still on login page");
            return true;
        } catch (Exception e) {
            System.out.println("Unexpected redirect: " + driver.getCurrentUrl());
            return false;
        }
    }
}


package com.notesapp.pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.notesapp.utils.WaitUtils;

public class NotesLoginPage {

    WebDriver driver;

    By emailField    = By.id("email");
    By passwordField = By.id("password");
    By loginButton   = By.xpath("//button[@type='submit']");

    public NotesLoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public void login(String email, String password) {
        WaitUtils.waitForVisible(driver, emailField).clear();
        driver.findElement(emailField).sendKeys(email);
        driver.findElement(passwordField).clear();
        driver.findElement(passwordField).sendKeys(password);

        // re-find the button RIGHT before clicking to avoid stale reference
        // after localStorage.clear() + navigate, React remounts and old references go stale
        WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(loginButton));
        try {
            btn.click();
        } catch (Exception e) {
            // if even the fresh reference is stale (very fast remount), use JS with a new lookup
            ((JavascriptExecutor) driver)
                    .executeScript("document.querySelector('button[type=submit]').click();");
        }
    }

    public boolean isLoginSuccessful() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.getCurrentUrl().contains("login"));
            System.out.println("After login URL: " + driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            System.out.println("Still on login page: " + driver.getCurrentUrl());
            return false;
        }
    }

    public boolean isErrorMessageShown() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(d -> d.getCurrentUrl().contains("login"));
            System.out.println("Login rejected - still on login page");
            return true;
        } catch (Exception e) {
            System.out.println("Unexpected redirect: " + driver.getCurrentUrl());
            return false;
        }
    }
}