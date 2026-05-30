package com.notesapp.tests.api;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.api.NotesAPI;
import com.notesapp.config.ConfigReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TC-API-04: DELETE /notes/{id} removes the note
// setUp creates a note so we always have something to delete
public class DeleteNoteAPITest {

    String token;
    String noteId;

    @BeforeClass
    public void setUp() {
        BaseAPI.setUp();

        token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertNotNull(token, "Token is needed");

        // create the note we are going to delete in the test
        Response createResp = NotesAPI.createNote(
                token, "Note To Delete " + System.currentTimeMillis(), "This will be deleted", "Home"
        );
        Assert.assertEquals(createResp.getStatusCode(), 200, "Must create note before deleting");

        noteId = createResp.jsonPath().getString("data.id");
        System.out.println("Will delete note id: " + noteId);
    }

    @Test(description = "TC-API-04: deleting a note should return 200 and note should be gone")
    public void deleteNoteReturns200() {
        Response deleteResp = NotesAPI.deleteNote(token, noteId);

        System.out.println("Delete response: " + deleteResp.asString());
        Assert.assertEquals(deleteResp.getStatusCode(), 200, "Delete should return 200");

        // double-check: GET /notes should no longer contain this note's id
        Response getResp = NotesAPI.getAllNotes(token);
        Assert.assertEquals(getResp.getStatusCode(), 200, "GET notes after delete should return 200");

        String allNotes = getResp.asString();
        Assert.assertFalse(allNotes.contains(noteId),
                "Deleted note id should not appear in GET /notes anymore");

        System.out.println("Confirmed: deleted note is no longer in the list");
    }
}
