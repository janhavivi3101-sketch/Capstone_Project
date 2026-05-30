package com.notesapp.agentic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * McpSeleniumClient
 * =================
 * Thin HTTP client that talks to the local MCP server (server.js, port 3000).
 *
 * The MCP server exposes a REST endpoint:
 *   POST http://localhost:3000/suggest-locator
 *   Body: { "failedLocator": "...", "pageHtml": "..." }
 *   Response: { "suggestion": "css:: .btn-add" }
 *
 * SelfHealingLocator calls suggestLocator() when every predefined fallback
 * has been exhausted.  If the MCP server is not running, this method returns
 * null silently (the caller handles the null case).
 */
public class McpSeleniumClient {

    private static final Logger log = LogManager.getLogger(McpSeleniumClient.class);

    // Port where the MCP server listens for locator-suggestion requests.
    // Must match the Express route in server.js.
    private static final String MCP_URL =
            System.getProperty("mcp.url", "http://localhost:3000/suggest-locator");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpSeleniumClient() {}

    /**
     * Asks the MCP server to suggest a Selenium locator.
     *
     * @param failedLocator  the By.toString() of the locator that failed
     * @param pageHtml       trimmed page HTML (< 8 KB recommended)
     * @return suggestion string like "css:: .btn-add" or null if unavailable
     */
    public static String suggestLocator(String failedLocator, String pageHtml) {
        try {
            Map<String, String> payload = Map.of(
                    "failedLocator", failedLocator,
                    "pageHtml",      pageHtml
            );
            String body = MAPPER.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MCP_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<?, ?> json = MAPPER.readValue(response.body(), Map.class);
                Object suggestion = json.get("suggestion");
                return suggestion != null ? suggestion.toString() : null;
            }

            log.warn("[McpClient] MCP server returned HTTP {}: {}",
                    response.statusCode(), response.body());
            return null;

        } catch (Exception e) {
            // MCP server not running or network issue — degrade gracefully
            log.warn("[McpClient] Could not reach MCP server at {}: {}", MCP_URL, e.getMessage());
            return null;
        }
    }
}
