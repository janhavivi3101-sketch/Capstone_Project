package com.notesapp.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

// helper methods for waiting - reduces NoSuchElementException and timing issues
public class WaitUtils {

    private static final int DEFAULT_TIMEOUT = 20;

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    // scrolls element into view then clicks via JavaScript
    // bypasses ad iframes and overlapping elements that block normal clicks
    public static void jsClick(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        js.executeScript("arguments[0].click();", element);
    }

    // scrolls the page to top - useful before interacting with navbar items
    public static void scrollToTop(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    // small pause used after page transitions to let the React app settle
    public static void pause(int millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    // dismiss any unexpected browser alerts (like session-expired warnings)
    public static void dismissAlertIfPresent(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.alertIsPresent())
                    .dismiss();
            System.out.println("Dismissed unexpected alert");
        } catch (Exception ignored) {
            // no alert present - that's fine
        }
    }

    // presses Escape key to close any native browser dialog/popup
    // Chrome's "Change your password" warning is a native dialog - it is NOT a JS alert
    // so driver.switchTo().alert() won't see it. The Password Manager prefs in DriverFactory
    // should prevent it from appearing at all, but this is a safety net in case it slips through.
    // Pressing Escape is the safest cross-platform way to dismiss it without clicking anything.
    public static void pressEscapeToCloseBrowserDialog(WebDriver driver) {
        try {
            driver.findElement(By.tagName("body"))
                  .sendKeys(org.openqa.selenium.Keys.ESCAPE);
            pause(300);
        } catch (Exception ignored) {
            // body not interactable - not a problem
        }
    }
}
