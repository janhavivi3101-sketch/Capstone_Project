package com.notesapp.agentic;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Self-Healing Locator
 * ====================
 * Tries the primary locator first.
 * If it fails, cycles through every fallback locator.
 * If ALL fallbacks also fail, sends the page HTML to the MCP server and asks
 * Claude to suggest a working locator — then tries that too.
 *
 * Usage example (in any Page class):
 *
 *   WebElement btn = SelfHealingLocator.find(
 *       driver,
 *       By.id("add-note"),                        // primary  (most reliable)
 *       By.cssSelector("button.add-note-btn"),    // fallback 1
 *       By.xpath("//button[contains(.,'Add Note')]") // fallback 2
 *   );
 */
public class SelfHealingLocator {

    private static final Logger log = LogManager.getLogger(SelfHealingLocator.class);
    private static final int WAIT_SECONDS = 8;

    // utility class — no instances
    private SelfHealingLocator() {}

    /**
     * Main entry point.  Pass driver + primary locator + any number of fallbacks.
     * Throws RuntimeException only if every strategy (including MCP) fails.
     */
    public static WebElement find(WebDriver driver, By primary, By... fallbacks) {

        // ── 1. Try the primary locator ───────────────────────────────────────
        WebElement element = tryLocator(driver, primary);
        if (element != null) {
            log.info("[SelfHealing] Primary locator succeeded: {}", primary);
            return element;
        }
        log.warn("[SelfHealing] Primary locator FAILED: {} — trying {} fallback(s)...",
                primary, fallbacks.length);

        // ── 2. Try each fallback in order ────────────────────────────────────
        List<By> fallbackList = Arrays.asList(fallbacks);
        for (By fallback : fallbackList) {
            element = tryLocator(driver, fallback);
            if (element != null) {
                log.warn("[SelfHealing] Fallback locator succeeded: {}", fallback);
                attachAllureEvent(primary.toString(), fallback.toString(), "fallback");
                return element;
            }
            log.warn("[SelfHealing] Fallback FAILED: {}", fallback);
        }

        // ── 3. Ask MCP / Claude for a locator suggestion ─────────────────────
        log.warn("[SelfHealing] All fallbacks failed. Asking MCP for locator suggestion...");

        // Strip scripts/styles to keep the payload small (< 8 KB)
        String rawHtml = driver.getPageSource();
        String cleanHtml = rawHtml
                .replaceAll("(?s)<script[^>]*>.*?</script>", "")
                .replaceAll("(?s)<style[^>]*>.*?</style>", "");
        String trimmedHtml = cleanHtml.length() > 8000
                ? cleanHtml.substring(0, 8000) + "\n...[TRIMMED]"
                : cleanHtml;

        String mcpSuggestion = McpSeleniumClient.suggestLocator(primary.toString(), trimmedHtml);

        if (mcpSuggestion != null && !mcpSuggestion.isBlank()) {
            log.info("[SelfHealing] MCP suggested locator: {}", mcpSuggestion);
            By mcpLocator = parseMcpLocator(mcpSuggestion);
            element = tryLocator(driver, mcpLocator);
            if (element != null) {
                log.info("[SelfHealing] MCP locator succeeded!");
                attachAllureEvent(primary.toString(), mcpSuggestion + " [via MCP]", "mcp");
                return element;
            }
            log.error("[SelfHealing] MCP locator also failed: {}", mcpSuggestion);
        } else {
            log.error("[SelfHealing] MCP returned no suggestion.");
        }

        // ── 4. All strategies exhausted ──────────────────────────────────────
        throw new RuntimeException(
                "[SelfHealing] ALL locators failed."
                + "\n  Primary  : " + primary
                + "\n  Fallbacks: " + fallbackList
                + "\n  MCP hint : " + mcpSuggestion
        );
    }

    /**
     * Convenience overload — no fallbacks, no MCP.
     * Just wraps a single locator in a clean explicit wait.
     */
    public static WebElement find(WebDriver driver, By primary) {
        return find(driver, primary, new By[0]);
    }

    /**
     * Backward-compatible alias used by older page classes that call findElement().
     * Internally delegates to find() with one fallback.
     */
    public static WebElement findElement(WebDriver driver, By primaryLocator, By backupLocator) {
        return find(driver, primaryLocator, backupLocator);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private static WebElement tryLocator(WebDriver driver, By locator) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses the MCP suggestion string.
     * Expected formats:
     *   css::   .btn-add
     *   xpath:: //button[@data-id='add']
     *   id::    add-note-btn
     * Falls back to treating the whole string as a CSS selector.
     */
    private static By parseMcpLocator(String suggestion) {
        String clean = suggestion.trim();
        if (clean.startsWith("css::"))   return By.cssSelector(clean.substring(5).trim());
        if (clean.startsWith("xpath::")) return By.xpath(clean.substring(7).trim());
        if (clean.startsWith("id::"))    return By.id(clean.substring(4).trim());
        log.warn("[SelfHealing] Unknown MCP locator format — treating as CSS: {}", clean);
        return By.cssSelector(clean);
    }

    /**
     * Adds a self-healing event as an Allure attachment so it shows up in reports.
     * Safe to call even if Allure is not on the classpath — the catch swallows it.
     */
    private static void attachAllureEvent(String original, String healed, String strategy) {
        try {
            Allure.addAttachment(
                    "Self-Healing Event [" + strategy + "]",
                    "text/plain",
                    "Original locator FAILED : " + original + "\n"
                  + "Healed  locator USED    : " + healed   + "\n"
                  + "Strategy                : " + strategy
            );
        } catch (Exception ignored) {}
    }
}
