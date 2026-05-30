package com.notesapp.api;

import io.restassured.response.Response;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

// all notes-related API calls
// each method uses the base request spec and adds the auth token on top
public class NotesAPI {

    public static Response getAllNotes(String token) {
        return given()
                .spec(BaseAPI.requestSpec)
                .header("x-auth-token", token)
                .when()
                .get("/notes");
    }

    public static Response createNote(String token, String title, String description, String category) {
        HashMap<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("category", category);

        return given()
                .spec(BaseAPI.requestSpec)
                .header("x-auth-token", token)
                .body(body)
                .when()
                .post("/notes");
    }

    public static Response updateNote(String token, String noteId, String title, String description) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("category", "Home");
        body.put("completed", false);

        return given()
                .spec(BaseAPI.requestSpec)
                .header("x-auth-token", token)
                .body(body)
                .when()
                .put("/notes/" + noteId);
    }

    public static Response deleteNote(String token, String noteId) {
        return given()
                .spec(BaseAPI.requestSpec)
                .header("x-auth-token", token)
                .when()
                .delete("/notes/" + noteId);
    }
}
