package com.notesapp.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

// single instance of ExtentReports shared across all tests
// ExtentTestListener creates/logs individual tests; this class just owns the file handle
public class ExtentManager {

    private static ExtentReports extent;

    // output path is always target/extent-report/index.html
    // Jenkins will look for it there when archiving HTML artifacts
    private static final String REPORT_PATH =
            System.getProperty("user.dir") + "/target/extent-report/index.html";

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("Notes App - Automation Report");
            spark.config().setReportName("HybridNotesFramework - Test Results");
            spark.config().setEncoding("UTF-8");
            // inline CSS so the report is fully self-contained - no CDN needed
            spark.config().setOfflineMode(true);

            extent = new ExtentReports();
            extent.attachReporter(spark);

            // system info shown on the report dashboard
            extent.setSystemInfo("Application", "Notes App - ExpandTesting");
            extent.setSystemInfo("Framework",   "Selenium + TestNG + RestAssured");
            extent.setSystemInfo("Environment", System.getProperty("env", "QA"));
            extent.setSystemInfo("OS",          System.getProperty("os.name"));
            extent.setSystemInfo("Java",        System.getProperty("java.version"));
            extent.setSystemInfo("Browser",     "Chrome");
            extent.setSystemInfo("Execution",   System.getProperty("headless", "false")
                                                    .equals("true") ? "Headless" : "Headed");
        }
        return extent;
    }

    // called by the listener after all tests finish to write the file to disk
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
