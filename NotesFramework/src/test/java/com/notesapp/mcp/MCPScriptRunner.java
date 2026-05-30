package com.notesapp.mcp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.notesapp.config.ConfigReader;

/**
 * MCPScriptRunner
 * ===============
 * Generates plain-text browser-automation scripts and executes them step by step.
 *
 * Script format — one instruction per line, fields separated by "|":
 *
 *   clearStorage
 *   navigate|<url>
 *   waitVisible|<locator>
 *   waitClickable|<locator>
 *   type|<locator>|<text>
 *   jsClick|<locator>
 *   click|<locator>
 *   waitForUrl|<urlFragment>
 *   pause|<milliseconds>
 *   assertUrl|<urlFragment>
 *   assertVisible|<locator>
 *
 * Locator format: id=value  |  xpath=value  |  css=value
 *
 * How it plugs into the project
 * ─────────────────────────────
 * HybridTest.TC-E2E-03 calls:
 *   1. MCPScriptRunner.generateLoginScript(email, password, path)
 *      → writes a .txt script file to target/mcp-scripts/
 *   2. new MCPScriptRunner(driver).runScript(path)
 *      → reads the file line by line and drives Chrome
 *
 * The MCP Node server (server.js) exposes generate_test_script which lets
 * Claude Desktop produce and run NEW test scripts without you writing any Java.
 * This class is the Java-side executor that runs whatever the server generates.
 */
public class MCPScriptRunner{

    private static final Logger log = LogManager.getLogger(MCPScriptRunner.class);

    private final WebDriver driver;

    public MCPScriptRunner(WebDriver driver) {
        this.driver = driver;
    }

    // ── Script generator ─────────────────────────────────────────────────────

    /**
     * Writes a ready-to-run login script to the given file path.
     * Called by HybridTest.loginViaMCPScript() (TC-E2E-03).
     */
    public static void generateLoginScript(String email, String password, String outputFile) {
        try {
            new java.io.File(outputFile).getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("clearStorage\n");
                fw.write("navigate|" + ConfigReader.getProperty("baseURL") + "\n");
                fw.write("waitVisible|id=email\n");
                fw.write("type|id=email|" + email + "\n");
                fw.write("type|id=password|" + password + "\n");
                fw.write("waitClickable|xpath=//button[@type='submit']\n");
                fw.write("jsClick|xpath=//button[@type='submit']\n");
                fw.write("waitForUrl|/notes/app\n");
                fw.write("assertUrl|/notes/app\n");
            }
            log.info("[MCPScriptRunner] Script written to: {}", outputFile);
        } catch (Exception e) {
            log.error("[MCPScriptRunner] Could not write script: {}", e.getMessage());
        }
    }

    /**
     * Generates a script that creates a note via the UI.
     * Useful as an example of scripts produced by server.js generate_test_script.
     */
    public static void generateCreateNoteScript(String title, String description,
                                                String category, String outputFile) {
        try {
            new java.io.File(outputFile).getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("waitVisible|xpath=//*[@id='root']\n");
                fw.write("jsClick|xpath=//*[@id='root']//button[contains(.,'Add Note') or contains(.,'+ Note')]\n");
                fw.write("waitVisible|xpath=//div[@role='dialog']\n");
                fw.write("type|xpath=//div[@role='dialog']//input[@id='title']|" + title + "\n");
                fw.write("type|xpath=//div[@role='dialog']//textarea[@id='description']|" + description + "\n");
                fw.write("pause|300\n");
                fw.write("jsClick|xpath=//div[@role='dialog']//button[@type='submit']\n");
                fw.write("assertVisible|xpath=//*[contains(text(),'" + title + "')]\n");
            }
            log.info("[MCPScriptRunner] Create-note script written to: {}", outputFile);
        } catch (Exception e) {
            log.error("[MCPScriptRunner] Could not write create-note script: {}", e.getMessage());
        }
    }

    // ── Script executor ──────────────────────────────────────────────────────

    /**
     * Reads a script file and executes each line as a browser command.
     * Throws RuntimeException on any unrecoverable step failure.
     */
    public void runScript(String scriptFile) {
        log.info("[MCPScriptRunner] Running script: {}", scriptFile);
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip blanks and comments
                lineNum++;
                log.info("[MCPScriptRunner] Step {}: {}", lineNum, line);
                executeStep(line, lineNum);
            }
            log.info("[MCPScriptRunner] Script completed. Steps executed: {}", lineNum);

        } catch (RuntimeException re) {
            throw re; // already wrapped — let it propagate
        } catch (Exception e) {
            throw new RuntimeException(
                    "[MCPScriptRunner] Script failed at line " + lineNum + ": " + e.getMessage(), e);
        }
    }

    // ── Step dispatcher ──────────────────────────────────────────────────────

    private void executeStep(String line, int lineNum) {
        String[] parts = line.split("\\|", -1);
        String action = parts[0].trim();

        switch (action) {

            case "clearStorage" -> {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
                    driver.manage().deleteAllCookies();
                    log.info("[MCPScriptRunner] Storage + cookies cleared");
                } catch (Exception e) {
                    log.warn("[MCPScriptRunner] clearStorage skipped: {}", e.getMessage());
                }
            }

            case "navigate" -> {
                requireParts(parts, 2, line);
                driver.get(parts[1].trim());
                log.info("[MCPScriptRunner] Navigated to: {}", parts[1]);
            }

            case "waitVisible" -> {
                requireParts(parts, 2, line);
                By loc = resolveLocator(parts[1]);
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.visibilityOfElementLocated(loc));
            }

            case "waitClickable" -> {
                requireParts(parts, 2, line);
                By loc = resolveLocator(parts[1]);
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(loc));
            }

            case "type" -> {
                requireParts(parts, 3, line);
                By loc = resolveLocator(parts[1]);
                WebElement el = new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.visibilityOfElementLocated(loc));
                el.clear();
                el.sendKeys(parts[2]);
            }

            case "click" -> {
                requireParts(parts, 2, line);
                By loc = resolveLocator(parts[1]);
                WebElement el = new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(loc));
                el.click();
            }

            case "jsClick" -> {
                requireParts(parts, 2, line);
                By loc = resolveLocator(parts[1]);
                WebElement el = new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(loc));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            }

            case "waitForUrl" -> {
                requireParts(parts, 2, line);
                String fragment = parts[1].trim();
                new WebDriverWait(driver, Duration.ofSeconds(20))
                        .until(d -> d.getCurrentUrl().contains(fragment)
                                && !d.getCurrentUrl().endsWith("/login"));
                log.info("[MCPScriptRunner] URL now contains: {}", fragment);
            }

            case "assertUrl" -> {
                requireParts(parts, 2, line);
                String fragment = parts[1].trim();
                String current = driver.getCurrentUrl();
                if (!current.contains(fragment)) {
                    throw new RuntimeException(
                            "[MCPScriptRunner] assertUrl FAILED — expected URL to contain '"
                            + fragment + "' but got: " + current);
                }
                log.info("[MCPScriptRunner] assertUrl PASSED: {}", current);
            }

            case "assertVisible" -> {
                requireParts(parts, 2, line);
                By loc = resolveLocator(parts[1]);
                boolean visible;
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(10))
                            .until(ExpectedConditions.visibilityOfElementLocated(loc));
                    visible = true;
                } catch (Exception e) {
                    visible = false;
                }
                if (!visible) {
                    throw new RuntimeException(
                            "[MCPScriptRunner] assertVisible FAILED for locator: " + parts[1]);
                }
                log.info("[MCPScriptRunner] assertVisible PASSED: {}", parts[1]);
            }

            case "pause" -> {
                requireParts(parts, 2, line);
                try {
                    Thread.sleep(Long.parseLong(parts[1].trim()));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            default -> log.warn("[MCPScriptRunner] Unknown action '{}' on line {} — skipping",
                    action, lineNum);
        }
    }

    // ── Locator resolver ─────────────────────────────────────────────────────

    /**
     * Converts a locator string to a Selenium By object.
     * Supported prefixes: id=  xpath=  css=
     */
    private By resolveLocator(String locatorStr) {
        String s = locatorStr.trim();
        if (s.startsWith("id="))    return By.id(s.substring(3));
        if (s.startsWith("xpath=")) return By.xpath(s.substring(6));
        if (s.startsWith("css="))   return By.cssSelector(s.substring(4));
        // no prefix — treat as id (backward-compatible with original runner)
        log.warn("[MCPScriptRunner] No prefix on locator '{}' — assuming id=", s);
        return By.id(s);
    }

    // ── Guard helper ─────────────────────────────────────────────────────────

    private static void requireParts(String[] parts, int min, String line) {
        if (parts.length < min) {
            throw new RuntimeException(
                    "[MCPScriptRunner] Malformed step (expected " + min + " parts): " + line);
        }
    }
}
