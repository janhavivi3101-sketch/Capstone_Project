package com.notesapp.tests.e2e;

import java.time.Duration;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.api.NotesAPI;
import com.notesapp.base.BaseTest;
import com.notesapp.config.ConfigReader;
import com.notesapp.mcp.MCPScriptRunner;
import com.notesapp.pages.NotesLoginPage;
import com.notesapp.pages.NotesPage;
import com.notesapp.utils.ScreenshotUtils;

import io.restassured.response.Response;

public class HybridTest extends BaseTest {

    String token;

    @BeforeClass
    public void setUpAPI() {
        BaseAPI.setUp();
        token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertNotNull(token, "Need a valid API token for E2E tests");
        System.out.println("API token obtained for E2E tests");
    }

    // run in explicit order so browser state is predictable
    @Test(priority = 1, description = "TC-E2E-01: note created via UI should appear in GET /notes API")
    public void uiNoteVisibleInAPI() {
        resetBrowserState();

        NotesLoginPage loginPage = new NotesLoginPage(driver);
        loginPage.login(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertTrue(loginPage.isLoginSuccessful(), "Login must succeed");

        String noteTitle = "E2E UI Note " + System.currentTimeMillis();
        NotesPage notesPage = new NotesPage(driver);
        notesPage.createNote(noteTitle, "Created via UI for E2E test", "Work");

        Assert.assertTrue(notesPage.isNoteVisible(noteTitle),
                "TC-E2E-01: Note should appear in UI after creation");

        Response apiResponse = NotesAPI.getAllNotes(token);
        Assert.assertEquals(apiResponse.getStatusCode(), 200);
        Assert.assertTrue(apiResponse.asString().contains(noteTitle),
                "TC-E2E-01: Note created via UI must appear in GET /notes API response");
    }

    @Test(priority = 2, description = "TC-E2E-02: note deleted via API should disappear from UI")
    public void apiDeletedNoteGoneFromUI() {
        resetBrowserState();

        NotesLoginPage loginPage = new NotesLoginPage(driver);
        loginPage.login(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertTrue(loginPage.isLoginSuccessful(), "Login must succeed");

        String noteTitle = "E2E Delete " + System.currentTimeMillis();
        Response createResp = NotesAPI.createNote(token, noteTitle, "Will be deleted", "Home");
        Assert.assertEquals(createResp.getStatusCode(), 200);
        String noteId = createResp.jsonPath().getString("data.id");
        System.out.println("Created note id: " + noteId);

        driver.navigate().refresh();
        NotesPage notesPage = new NotesPage(driver);
        Assert.assertTrue(notesPage.isNoteVisible(noteTitle),
                "Note must be visible in UI before deleting via API");

        Response deleteResp = NotesAPI.deleteNote(token, noteId);
        Assert.assertEquals(deleteResp.getStatusCode(), 200);

        driver.navigate().refresh();
        boolean gone = notesPage.isNoteGone(noteTitle);
        if (!gone) ScreenshotUtils.takeScreenshot(driver, "e2e02_note_still_visible");
        Assert.assertTrue(gone,
                "TC-E2E-02: Note deleted via API should not appear in UI after refresh");
    }

    @Test(priority = 3, description = "TC-E2E-03: login using MCP-generated script")
    public void loginViaMCPScript() {
        // start from a clean state - no active session
        resetBrowserState();

        String scriptPath = System.getProperty("user.dir")
                + "/target/mcp-scripts/login.txt";

        // generateLoginScript now uses ConfigReader.baseURL and emits assertUrl step
        MCPScriptRunner.generateLoginScript(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password"),
                scriptPath
        );

        new MCPScriptRunner(driver).runScript(scriptPath);

        // Extra safety net — React router may not have finished the redirect yet
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.urlContains("/notes/app"));
        } catch (Exception e) {
            ScreenshotUtils.takeScreenshot(driver, "mcp_login_failed");
        }

        String currentUrl = driver.getCurrentUrl();
        System.out.println("MCP login result URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/notes/app") && !currentUrl.contains("login"),
                "TC-E2E-03: MCP script should land on app page, but URL was: " + currentUrl);
        System.out.println("MCP login succeeded. URL: " + currentUrl);
    }

    @Test(priority = 4, description = "TC-E2E-04: create note using MCP-generated script and verify via API")
    public void createNoteViaMCPScript() {
        resetBrowserState();

        // ── Step 1: login via page object ────────────────────────────────
        NotesLoginPage loginPage = new NotesLoginPage(driver);
        loginPage.login(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertTrue(loginPage.isLoginSuccessful(), "Login must succeed before MCP note creation");

        // ── Step 2: generate and run create-note script via MCP runner ───
        String noteTitle = "MCP Note " + System.currentTimeMillis();
        String scriptPath = System.getProperty("user.dir")
                + "/target/mcp-scripts/create_note.txt";

        MCPScriptRunner.generateCreateNoteScript(
                noteTitle,
                "Created by MCP script runner",
                "Work",
                scriptPath
        );

        new MCPScriptRunner(driver).runScript(scriptPath);

        // ── Step 3: confirm note appears in GET /notes API ───────────────
        Response apiResponse = NotesAPI.getAllNotes(token);
        Assert.assertEquals(apiResponse.getStatusCode(), 200,
                "GET /notes API must return 200 after MCP note creation");
        Assert.assertTrue(apiResponse.asString().contains(noteTitle),
                "TC-E2E-04: Note created via MCP script must appear in GET /notes API response");

        System.out.println("TC-E2E-04 PASSED — MCP-created note found in API: " + noteTitle);
    }
}
