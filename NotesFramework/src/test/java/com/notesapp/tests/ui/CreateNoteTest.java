package com.notesapp.tests.ui;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.notesapp.base.BaseTest;
import com.notesapp.config.ConfigReader;
import com.notesapp.pages.NotesLoginPage;
import com.notesapp.pages.NotesPage;
import com.notesapp.utils.ExcelUtils;
import com.notesapp.utils.ScreenshotUtils;

// TC-UI-02: create Work note
// TC-UI-03: create Personal note
// TC-UI-04: create Home note
// TC-UI-08: create second Work note
// TC-UI-09: create second Personal note
public class CreateNoteTest extends BaseTest {

    String notesDataFile = System.getProperty("user.dir")
            + "/src/test/resources/testdata/NotesData.xlsx";

    // login once before all note tests run
    @BeforeClass
    public void loginFirst() {
        NotesLoginPage loginPage = new NotesLoginPage(driver);
        loginPage.login(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertTrue(loginPage.isLoginSuccessful(),
                "Must be logged in before running create note tests");
        System.out.println("Logged in - starting note creation tests");
    }

    @DataProvider(name = "notesData")
    public Object[][] notesData() {
        return ExcelUtils.getNotesTestData(notesDataFile);
    }

    @Test(dataProvider = "notesData", description = "Create notes via UI")
    public void createNoteTest(String title, String description, String category) {
        System.out.println("Creating note: " + title + " | category: " + category);

        NotesPage notesPage = new NotesPage(driver);
        notesPage.createNote(title, description, category);

        // after submit the app should redirect back to the notes list
        // wait a moment for the redirect to complete
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        boolean noteVisible = notesPage.isNoteVisible(title);

        if (!noteVisible) {
            ScreenshotUtils.takeScreenshot(driver,
                    "createNote_" + title.replace(" ", "_") + "_failed");
        }

        Assert.assertTrue(noteVisible,
                "Note '" + title + "' should appear in the list after creation");
    }
}