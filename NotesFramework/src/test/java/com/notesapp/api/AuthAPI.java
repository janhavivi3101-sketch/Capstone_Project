package com.notesapp.api;

import io.restassured.response.Response;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

// handles login API calls
public class AuthAPI {

    // logs in and returns the token - returns null if login fails
    public static String getToken(String email, String password) {
        HashMap<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        Response response = given()
                .spec(BaseAPI.requestSpec)
                .body(body)
                .when()
                .post("/users/login");

        System.out.println("Login response status: " + response.getStatusCode());
        return response.jsonPath().getString("data.token");
    }

    // returns the full response so tests can check status code too
    public static Response login(String email, String password) {
        HashMap<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        return given()
                .spec(BaseAPI.requestSpec)
                .body(body)
                .when()
                .post("/users/login");
    }
}
