package com.notesapp.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.notesapp.drivers.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Base64;

// wires every TestNG test result into the Extent report
// registered in testng.xml so no annotation changes are needed in any test class
public class ExtentTestListener implements ITestListener {

    // ThreadLocal so parallel tests each get their own ExtentTest node
    private static final ThreadLocal<ExtentTest> testNode = new ThreadLocal<>();
    private static ExtentReports extent;

    @Override
    public void onStart(ITestContext context) {
        extent = ExtentManager.getInstance();
    }

    @Override
    public void onTestStart(ITestResult result) {
        // build the test name - include parameters if this is a data-driven row
        String name = buildTestName(result);

        // category tag helps filter the report by UI / API / E2E
        String category = resolveCategory(result);

        ExtentTest test = extent.createTest(name, result.getMethod().getDescription());
        test.assignCategory(category);
        testNode.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testNode.get().log(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = testNode.get();
        test.log(Status.FAIL, result.getThrowable());

        // attach a base64 screenshot inline - works even in CI where there is no shared drive
        String shot = captureBase64Screenshot(result);
        if (shot != null) {
            try {
                test.fail(MediaEntityBuilder.createScreenCaptureFromBase64String(shot).build());
            } catch (Exception e) {
                test.log(Status.WARNING, "Could not attach screenshot: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        testNode.get().log(Status.SKIP, "Test skipped - "
                + (result.getThrowable() != null ? result.getThrowable().getMessage() : "no reason"));
    }

    @Override
    public void onFinish(ITestContext context) {
        // flush happens per-context so the file is written even if later suites fail
        ExtentManager.flush();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String buildTestName(ITestResult result) {
        String base = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            // for data-driven tests show the first param (title / email) in the node name
            // keeps report readable - "createNoteTest [Work Planning Q3]" etc.
            base = base + " [" + params[0].toString() + "]";
        }
        return base;
    }

    private String resolveCategory(ITestResult result) {
        String pkg = result.getMethod().getRealClass().getPackageName();
        if (pkg.contains(".ui"))  return "UI";
        if (pkg.contains(".api")) return "API";
        if (pkg.contains(".e2e")) return "E2E / Hybrid";
        return "Other";
    }

    private String captureBase64Screenshot(ITestResult result) {
        try {
            // try to get the driver from DriverFactory's ThreadLocal store
            WebDriver driver = DriverFactory.getCurrentDriver();
            if (driver == null) return null;
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.out.println("Extent screenshot capture failed: " + e.getMessage());
            return null;
        }
    }
}
