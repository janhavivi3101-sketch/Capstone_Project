package com.notesapp.tests.api;

import com.notesapp.api.AuthAPI;
import com.notesapp.api.BaseAPI;
import com.notesapp.config.ConfigReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

// TC-API-LOGIN-01: valid credentials return 200 and a token
// TC-NEG-API-01:   wrong password returns 401
// TC-NEG-API-02:   non-existent user returns 401
// TC-NEG-API-03:   empty password is rejected (not 200)
public class LoginAPITest {

    @BeforeClass
    public void setUp() {
        BaseAPI.setUp();
    }

    @Test(description = "TC-API-LOGIN-01: valid credentials should return 200 and a token")
    public void validLoginReturnsToken() {
        String token = AuthAPI.getToken(
                ConfigReader.getProperty("email"),
                ConfigReader.getProperty("password")
        );
        System.out.println("Token received: " + token);
        Assert.assertNotNull(token, "Token should not be null for valid credentials");
        Assert.assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Test(description = "TC-NEG-API-01: wrong password should return 401")
    public void wrongPasswordReturns401() {
        Response response = AuthAPI.login(
                ConfigReader.getProperty("email"),
                "WrongPassword99"
        );
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.asString());
        Assert.assertEquals(response.getStatusCode(), 401,
                "Wrong password should return 401");
    }

    @Test(description = "TC-NEG-API-02: non-existent user should return 401")
    public void nonExistentUserReturns401() {
        Response response = AuthAPI.login("nobody@notexist.com", "Test@1234");
        System.out.println("Status: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 401,
                "Non-existent user should return 401");
    }

    @Test(description = "TC-NEG-API-03: empty password should not return 200")
    public void emptyPasswordFails() {
        Response response = AuthAPI.login(ConfigReader.getProperty("email"), "");
        int status = response.getStatusCode();
        System.out.println("Empty password status: " + status);
        Assert.assertNotEquals(status, 200, "Empty password should not return success");
    }
}
