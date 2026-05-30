package com.notesapp.tests.api;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.api.NotesAPI;
import com.notesapp.config.ConfigReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TC-API-03: PUT /notes/{id} updates an existing note
public class UpdateNoteAPITest {

    String token;
    String noteId;

    @BeforeClass
    public void setUp() {
        BaseAPI.setUp();

        token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertNotNull(token, "Token needed for update test");

        // create a note first so we have something to update
        Response createResponse = NotesAPI.createNote(
                token, "API Note " + System.currentTimeMillis(), "Original description", "Home"
        );
        Assert.assertEquals(createResponse.getStatusCode(), 200, "Note creation must succeed");

        noteId = createResponse.jsonPath().getString("data.id");
        System.out.println("Created note with id: " + noteId);
    }

    @Test(description = "TC-API-03: updating a note should return 200 with the new values")
    public void updateNoteTest() {
        Response response = NotesAPI.updateNote(
                token, noteId, "Updated Title", "Updated description"
        );

        System.out.println("Update response: " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 200, "Update should return 200");

        String updatedTitle = response.jsonPath().getString("data.title");
        Assert.assertEquals(updatedTitle, "Updated Title",
                "Title in response should reflect the update");

        String updatedDesc = response.jsonPath().getString("data.description");
        Assert.assertEquals(updatedDesc, "Updated description",
                "Description in response should reflect the update");
    }
}
