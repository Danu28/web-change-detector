package com.uitester.api;

import com.uitester.diff.ElementChange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/** Additional tests for the WebChangeDetector facade layer. */
public class WebChangeDetectorAdditionalTest {

    private String toFileUrl(String relativePath) {
        File f = new File(relativePath);
        String abs = f.getAbsolutePath().replace("\\", "/");
        if (!abs.startsWith("/")) abs = "/" + abs;
        return "file:///" + abs;
    }

    @Test
    public void compareOnlyUsesExistingSnapshots() throws Exception {
        String baselineUrl = toFileUrl("test_files/baseline.html");
        String currentUrl = toFileUrl("test_files/current.html");

        File dir = new File("target/compare-only-snapshots");
        dir.mkdirs();
        File baseSnap = new File(dir, "baseline.json");
        File currSnap = new File(dir, "current.json");

        // Step 1: capture snapshots
        WebChangeDetector initial = WebChangeDetector.builder()
                .baselineUrl(baselineUrl)
                .currentUrl(currentUrl)
                .baselineSnapshotPath(baseSnap.getAbsolutePath())
                .currentSnapshotPath(currSnap.getAbsolutePath())
                .headless(true)
                .waitTimeSeconds(5)
                .maxElements(1500)
                .useDefaultOutput(false)
                .generateReport(false)
                .build();
    initial.run();
        Assert.assertTrue(baseSnap.exists());
        Assert.assertTrue(currSnap.exists());
        long baseModified = baseSnap.lastModified();
        long currModified = currSnap.lastModified();

        // Step 2: run in compareOnly mode (should NOT recapture or overwrite snapshots)
        Thread.sleep(20); // ensure timestamp difference would show if rewritten
        WebChangeDetector compareOnly = WebChangeDetector.builder()
                .baselineUrl(baselineUrl) // still required but ignored for capture
                .currentUrl(currentUrl)
                .baselineSnapshotPath(baseSnap.getAbsolutePath())
                .currentSnapshotPath(currSnap.getAbsolutePath())
                .compareOnly(true)
                .headless(true)
                .useDefaultOutput(false)
                .build();
        WebChangeDetector.Result second = compareOnly.run();

        Assert.assertEquals(baseSnap.lastModified(), baseModified, "Baseline snapshot unexpectedly modified in compareOnly mode");
        Assert.assertEquals(currSnap.lastModified(), currModified, "Current snapshot unexpectedly modified in compareOnly mode");
        Assert.assertFalse(new File(second.getOutputDir()).equals(dir), "Output dir should be auto-generated and separate");
        List<ElementChange> changes = second.getChanges();
        Assert.assertNotNull(changes);
    }

    @Test
    public void writeChangesFileDisabled() throws Exception {
        String baselineUrl = toFileUrl("test_files/baseline.html");
        String currentUrl = toFileUrl("test_files/current.html");

        // Cleanup any prior output directory to avoid stale changes.json from earlier tests
        File outParent = new File("output");
        if (outParent.exists()) {
            deleteRecursively(outParent);
        }

        WebChangeDetector detector = WebChangeDetector.builder()
                .baselineUrl(baselineUrl)
                .currentUrl(currentUrl)
                .useDefaultOutput(true)
                .writeChangesFile(false)
                .generateReport(false)
                .headless(true)
                .waitTimeSeconds(5)
                .build();
        WebChangeDetector.Result result = detector.run();
        Assert.assertNull(result.getChangesFilePath(), "Changes file path should be null when writeChangesFile=false");
        // ensure the default changes file does not exist
        File potential = new File(result.getOutputDir(), "changes.json");
        if (potential.exists()) {
            // content should be empty file if somehow created; treat as failure
            Assert.fail("Changes.json unexpectedly created while writeChangesFile=false");
        }
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void missingBaselineUrlThrows() throws Exception {
        String currentUrl = toFileUrl("test_files/current.html");
        WebChangeDetector.builder()
                .currentUrl(currentUrl)
                .headless(true)
                .build()
                .run(); // should throw due to missing baselineUrl
    }
}
