package com.uitester.api;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/** Parallel capture test with two pre-navigated drivers (skips explicit URL arguments). */
public class WebChangeDetectorParallelDriverTest {
    private WebDriver baseDriver;
    private WebDriver currentDriver;

    private String toFileUrl(String relativePath) {
        File f = new File(relativePath);
        String abs = f.getAbsolutePath().replace("\\", "/");
        if (!abs.startsWith("/")) abs = "/" + abs;
        return "file:///" + abs;
    }

    @BeforeClass
    public void initDrivers() {
        baseDriver = new ChromeDriver();
        currentDriver = new ChromeDriver();
        baseDriver.get(toFileUrl("test_files/baseline.html"));
        currentDriver.get(toFileUrl("test_files/current.html"));
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        try { if (baseDriver != null) baseDriver.quit(); } catch (Exception ignored) {}
        try { if (currentDriver != null) currentDriver.quit(); } catch (Exception ignored) {}
    }

    @Test
    public void parallelProvidedDrivers() throws Exception {
        WebChangeDetector detector = WebChangeDetector.builder()
                .baselineDriver(baseDriver)
                .currentDriver(currentDriver)
                .parallelCapture(true)
                .useDefaultOutput(true)
                .generateReport(false)
                .headless(true)
                .build();
        WebChangeDetector.Result result = detector.run();
        Assert.assertTrue(result.getBaselineElements().size() > 0, "No baseline elements captured");
        Assert.assertTrue(result.getCurrentElements().size() > 0, "No current elements captured");
    }
}