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