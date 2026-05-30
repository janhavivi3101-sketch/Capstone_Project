package com.notesapp.utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

// saves a screenshot to the screenshots folder when a test fails
public class ScreenshotUtils {

    public static void takeScreenshot(WebDriver driver, String testName) {
        if (driver == null) {
            System.out.println("Driver is null, skipping screenshot");
            return;
        }
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = System.getProperty("user.dir") + "/screenshots/" + testName + ".png";
            FileUtils.copyFile(src, new File(path));
            System.out.println("Screenshot saved to: " + path);
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
        }
    }
}
