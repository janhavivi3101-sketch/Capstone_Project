package com.notesapp.base;

import com.notesapp.config.ConfigReader;
import com.notesapp.drivers.DriverFactory;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

// base class for all UI tests
// each test CLASS gets its own browser instance - avoids parallel conflicts
public class BaseTest {

    protected WebDriver driver;

    @BeforeClass
    public void setUp() {
        driver = DriverFactory.initDriver();
        driver.get(ConfigReader.getProperty("baseURL"));
        System.out.println("Navigated to: " + ConfigReader.getProperty("baseURL"));
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver(driver);
        driver = null;
    }

    // clears localStorage so the React app forgets the login session
    // called before each login row in LoginTest
    public void resetBrowserState() {
        try {
            if (!driver.getCurrentUrl().contains("expandtesting.com")) {
                driver.get(ConfigReader.getProperty("baseURL"));
            }
            ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
            ((JavascriptExecutor) driver).executeScript("window.sessionStorage.clear();");
            driver.manage().deleteAllCookies();
        } catch (Exception e) {
            System.out.println("resetBrowserState error: " + e.getMessage());
        }
        driver.get(ConfigReader.getProperty("baseURL"));
    }
}