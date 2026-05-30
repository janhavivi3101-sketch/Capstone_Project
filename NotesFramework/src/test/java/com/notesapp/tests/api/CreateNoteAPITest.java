package com.notesapp.tests.api;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.api.NotesAPI;
import com.notesapp.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TC-API-01:  POST /notes creates a note and returns the data
// TC-API-01b: POST /notes without a token returns 401
public class CreateNoteAPITest {

    String token;

    @BeforeClass
    public void setUp() {
        BaseAPI.setUp();
        token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertNotNull(token, "Need a valid token to run create note tests");
    }

    @Test(description = "TC-API-01: creating a note should return 200 with note data")
    public void createNoteReturns200() {
        Response response = NotesAPI.createNote(
                token,
                "My API Note",
                "This note was created using the API",
                "Work"
        );

        System.out.println("Create note response: " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 200, "Create note should return 200");

        String noteId = response.jsonPath().getString("data.id");
        Assert.assertNotNull(noteId, "Created note should have an id");

        String returnedTitle = response.jsonPath().getString("data.title");
        Assert.assertEquals(returnedTitle, "My API Note", "Title in response should match");
    }

    @Test(description = "TC-API-01b: creating a note without a token should return 401")
    public void createNoteWithoutTokenFails() {
        Response response = RestAssured
                .given()
                .spec(BaseAPI.requestSpec)
                .body("{\"title\":\"test\",\"description\":\"test\",\"category\":\"Home\"}")
                .when()
                .post("/notes");

        System.out.println("No token create status: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 401,
                "Creating note without token should return 401");
    }
}
