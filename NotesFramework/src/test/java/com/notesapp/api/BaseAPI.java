package com.notesapp.api;

import com.notesapp.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

// sets up RestAssured before any API test runs
// the requestSpec is a reusable base spec - all API calls start from this
// so we don't have to repeat the base URL and Content-Type header everywhere
public class BaseAPI {

    // shared spec that all API calls can build on top of
    public static RequestSpecification requestSpec;

    public static void setUp() {
        RestAssured.baseURI = ConfigReader.getProperty("apiBaseURL");

        // base request spec: sets the base URL and default Content-Type
        // individual calls add their own headers (like x-auth-token) on top of this
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.getProperty("apiBaseURL"))
                .addHeader("Content-Type", "application/json")
                .build();

        System.out.println("API base URL set to: " + RestAssured.baseURI);
    }
}
