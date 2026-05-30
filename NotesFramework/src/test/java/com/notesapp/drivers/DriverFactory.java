package com.notesapp.drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory {

    // ThreadLocal lets each parallel test thread get its own driver reference
    // ExtentTestListener uses this to grab the driver for failure screenshots
    // without needing it passed as a parameter
    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    // returns the driver for the current thread - null if not initialised yet
    public static WebDriver getCurrentDriver() {
        return driverThread.get();
    }

    public static WebDriver initDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-features=PasswordLeakDetection");

        // suppress the "password found in breach" and "save password" Chrome dialogs
        // these native dialogs block Selenium from clicking anything on the page
        options.addArguments("--password-store=basic");
        options.setExperimentalOption("prefs", java.util.Map.of(
            "credentials_enable_service", false,
            "profile.password_manager_enabled", false,
            "profile.password_manager_leak_detection", false,
            "safebrowsing.enabled", false
        ));

        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driverThread.set(driver);
        System.out.println("Browser opened");
        return driver;
    }

    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("Browser closed");
            } catch (Exception e) {
                System.out.println("Browser already closed: " + e.getMessage());
            } finally {
                driverThread.remove();
            }
        }
    }
}