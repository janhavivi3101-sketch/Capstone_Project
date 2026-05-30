package com.notesapp.tests.api;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.api.NotesAPI;
import com.notesapp.config.ConfigReader;
import com.notesapp.utils.RetryAnalyzer;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TC-API-02:  GET /notes returns 200 and responds in under 2 seconds (FR-08)
// TC-API-02b: GET /notes without a token returns 401
public class GetNoteAPITest {

    String token;

    @BeforeClass
    public void setUp() {
        BaseAPI.setUp();
        token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        Assert.assertNotNull(token, "Token is needed for GET notes test");
    }

    // retryAnalyzer added here because network response time can occasionally spike
    @Test(description = "TC-API-02: GET /notes should return 200 within 2 seconds",
          retryAnalyzer = RetryAnalyzer.class)
    public void getNotesReturns200() {
        long startTime = System.currentTimeMillis();

        Response response = NotesAPI.getAllNotes(token);

        long timeTaken = System.currentTimeMillis() - startTime;

        // FIXED: was printing the full response body (100+ notes, thousands of characters)
        // that flooded stdout for ~3 minutes during the parallel run, starving the UI
        // thread's ChromeDriver HTTP connection until it timed out
        // now we just log the count and time - enough to debug without the wall of text
        int noteCount = response.jsonPath().getList("data").size();
        System.out.println("GET /notes took: " + timeTaken + "ms | notes in account: " + noteCount);

        Assert.assertEquals(response.getStatusCode(), 200, "GET notes should return 200");

        // FR-08: API must respond within 2 seconds
        Assert.assertTrue(timeTaken < 2000,
                "API took too long: " + timeTaken + "ms (limit: 2000ms)");
    }

    @Test(description = "TC-API-02b: GET /notes without token should return 401")
    public void getNotesWithoutTokenFails() {
        Response response = RestAssured
                .given()
                .spec(BaseAPI.requestSpec)
                .when()
                .get("/notes");

        System.out.println("No token GET status: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 401,
                "GET notes without token should return 401");
    }
}
