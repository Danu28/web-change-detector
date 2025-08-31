package com.uitester.api;

import com.uitester.diff.ElementChange;
import com.uitester.core.ElementData;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * Basic smoke test for {@link WebChangeDetector} using local test HTML files in test_files directory.
 * Verifies the facade runs end-to-end without throwing and produces element snapshots & change list.
 */
public class WebChangeDetectorTest {

    private String toFileUrl(String relativePath) {
        File f = new File(relativePath);
        String abs = f.getAbsolutePath().replace("\\", "/");
        if (!abs.startsWith("/")) abs = "/" + abs; // ensure leading slash for file URI on Windows
        return "file:///" + abs;
    }

    @Test
    public void runLocalFileComparison() throws Exception {
        String baselineUrl = toFileUrl("test_files/baseline.html");
        String currentUrl = toFileUrl("test_files/current.html");

        WebChangeDetector detector = WebChangeDetector.builder()
                .baselineUrl(baselineUrl)
                .currentUrl(currentUrl)
                .waitTimeSeconds(10)
                .maxElements(2000)
                .headless(true)
                .useDefaultOutput(true) // ensure files appear under standard output dir
                .generateReport(true)
                .build();

        WebChangeDetector.Result result = detector.run();

        // Basic assertions
        List<ElementData> baselineElements = result.getBaselineElements();
        List<ElementData> currentElements = result.getCurrentElements();
        List<ElementChange> changes = result.getChanges();

        Assert.assertNotNull(baselineElements, "Baseline elements should not be null");
        Assert.assertNotNull(currentElements, "Current elements should not be null");
        Assert.assertTrue(baselineElements.size() > 0, "Expected some baseline elements");
        Assert.assertTrue(currentElements.size() > 0, "Expected some current elements");
        Assert.assertNotNull(changes, "Changes list should not be null");

        // Snapshot & change files created under output dir
        Assert.assertNotNull(result.getOutputDir(), "Output directory should be set");
        File outDir = new File(result.getOutputDir());
        Assert.assertTrue(outDir.exists(), "Output directory does not exist");
        Assert.assertTrue(new File(result.getBaselineSnapshotPath()).exists(), "Baseline snapshot file missing");
        Assert.assertTrue(new File(result.getCurrentSnapshotPath()).exists(), "Current snapshot file missing");
        Assert.assertTrue(new File(result.getChangesFilePath()).exists(), "Changes file missing");
        if (result.getReportSimplePath() != null) {
            Assert.assertTrue(new File(result.getReportSimplePath()).exists(), "Report file missing");
        }
    }

    @Test
    public void runWithCustomSnapshotPaths() throws Exception {
        String baselineUrl = toFileUrl("test_files/baseline.html");
        String currentUrl = toFileUrl("test_files/current.html");

        File customDir = new File("target/custom-snapshots");
        customDir.mkdirs();
        File baselineFile = new File(customDir, "my-baseline.json");
        File currentFile = new File(customDir, "my-current.json");

        WebChangeDetector detector = WebChangeDetector.builder()
                .baselineUrl(baselineUrl)
                .currentUrl(currentUrl)
                .waitTimeSeconds(10)
                .maxElements(2000)
                .headless(true)
                .baselineSnapshotPath(baselineFile.getAbsolutePath())
                .currentSnapshotPath(currentFile.getAbsolutePath())
                .generateReport(true)
                .useDefaultOutput(false) // we are overriding snapshot paths explicitly
                .build();

        WebChangeDetector.Result result = detector.run();

        // Assert custom paths used
        Assert.assertEquals(new File(result.getBaselineSnapshotPath()).getCanonicalPath(), baselineFile.getCanonicalPath(), "Baseline snapshot path mismatch");
        Assert.assertEquals(new File(result.getCurrentSnapshotPath()).getCanonicalPath(), currentFile.getCanonicalPath(), "Current snapshot path mismatch");
        Assert.assertTrue(baselineFile.exists(), "Custom baseline snapshot not created");
        Assert.assertTrue(currentFile.exists(), "Custom current snapshot not created");

        // Output directory is still created for report naming etc., but should be different from our custom dir
        Assert.assertNotNull(result.getOutputDir());
        File outDir = new File(result.getOutputDir());
        Assert.assertTrue(outDir.exists(), "Expected generated output directory to exist");
        // Ensure our custom snapshots are not inside the output dir (path prefix check)
        String outPath = outDir.getCanonicalPath();
        Assert.assertFalse(baselineFile.getCanonicalPath().startsWith(outPath), "Baseline snapshot unexpectedly inside output dir");
        Assert.assertFalse(currentFile.getCanonicalPath().startsWith(outPath), "Current snapshot unexpectedly inside output dir");

    // Report should have been generated in output directory (not custom snapshot dir)
    Assert.assertNotNull(result.getReportSimplePath(), "Report simple path should be set when generateReport=true");
    Assert.assertTrue(new File(result.getReportSimplePath()).exists(), "Expected report file to exist");
    }
}
