package com.notesapp.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

// simple retry mechanism for flaky tests
// if a test fails, TestNG will retry it up to MAX_RETRY times before marking it as failed
// useful for network-dependent tests that occasionally get a 5xx error
// intern note: to use this, add retryAnalyzer = RetryAnalyzer.class to your @Test annotation
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int MAX_RETRY = 2; // retry up to 2 extra times

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            System.out.println("Retrying test: " + result.getName() + " (attempt " + retryCount + " of " + MAX_RETRY + ")");
            return true; // tell TestNG to retry
        }
        return false; // no more retries
    }
}
