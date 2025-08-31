package com.uitester.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.diff.ElementMatcher;
import com.uitester.report.SimpleReportGenerator;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level facade / builder API to run change detection programmatically.
 * Allows callers to:
 *   - Provide their own WebDriver instance (re-used for baseline & current)
 *   - Configure baseline/current ("test") URLs
 *   - Configure container XPath, wait time, max elements
 *   - Run either full capture+compare or compareOnly using existing snapshots
 *
 * Usage example:
 *  WebChangeDetector detector = WebChangeDetector.builder()
 *      .baselineUrl("https://baseline.example/page")
 *      .currentUrl("https://current.example/page")
 *      .containerXpath("//div[@id='main']")
 *      .waitTimeSeconds(30)
 *      .maxElements(5000)
 *      .webDriver(driver) // optional; if omitted a ChromeDriver is created
 *      .headless(true)
 *      .compareOnly(false)
 *      .build();
 *  WebChangeDetector.Result result = detector.run();
 */
public class WebChangeDetector {
    private static final Logger logger = LoggerFactory.getLogger(WebChangeDetector.class);

    // Builder configuration
    private final String baselineUrl;
    private final String currentUrl;
    private final String containerXpath;
    private final Integer waitTimeSeconds;
    private final Integer maxElements;
    private final boolean headless;
    private final boolean compareOnly;
    private final WebDriver externalDriver; // nullable (single driver for sequential reuse)
    private final WebDriver baselineExternalDriver; // optional distinct baseline driver
    private final WebDriver currentExternalDriver;  // optional distinct current driver
    private final boolean closeDriverOnFinish;
    private final String baselineSnapshotPath;
    private final String currentSnapshotPath;
    private final boolean generateReport;
    private final boolean useDefaultOutput;
    private final boolean writeChangesFile;
    private final boolean parallelCapture;

    // Internal
    private final ObjectMapper mapper = new ObjectMapper();

    private WebChangeDetector(Builder b) {
        this.baselineUrl = b.baselineUrl;
        this.currentUrl = b.currentUrl;
        this.containerXpath = b.containerXpath;
        this.waitTimeSeconds = b.waitTimeSeconds;
        this.maxElements = b.maxElements;
        this.headless = b.headless;
        this.compareOnly = b.compareOnly;
    this.externalDriver = b.webDriver;
    this.baselineExternalDriver = b.baselineDriver;
    this.currentExternalDriver = b.currentDriver;
        this.closeDriverOnFinish = b.closeDriverOnFinish && b.webDriver != null; // only close if user asked
        this.baselineSnapshotPath = b.baselineSnapshotPath;
        this.currentSnapshotPath = b.currentSnapshotPath;
        this.generateReport = b.generateReport;
    this.useDefaultOutput = b.useDefaultOutput;
    this.writeChangesFile = b.writeChangesFile;
    this.parallelCapture = b.parallelCapture;
    }

    public static Builder builder() { return new Builder(); }

    public Result run() throws Exception {
        validate();
        Configuration config = new Configuration();
        ProjectConfig projectConfig = config.getProjectConfig();

        // Derive URLs if not explicitly provided and dedicated drivers are given
        String effectiveBaselineUrl = baselineUrl;
        String effectiveCurrentUrl = currentUrl;
        if (effectiveBaselineUrl == null && baselineExternalDriver != null) {
            effectiveBaselineUrl = safeCurrentUrl(baselineExternalDriver);
        }
        if (effectiveCurrentUrl == null && currentExternalDriver != null) {
            effectiveCurrentUrl = safeCurrentUrl(currentExternalDriver);
        }
        if (effectiveBaselineUrl == null && externalDriver != null) effectiveBaselineUrl = safeCurrentUrl(externalDriver);
        if (effectiveCurrentUrl == null && externalDriver != null) effectiveCurrentUrl = safeCurrentUrl(externalDriver);
        config.setBaselineUrl(effectiveBaselineUrl);
        config.setCurrentUrl(effectiveCurrentUrl);
        if (containerXpath != null) config.setContainerXpath(containerXpath);
        if (waitTimeSeconds != null) config.setWaitTime(waitTimeSeconds);
        if (maxElements != null) config.setMaxElements(maxElements);
        config.setHeadless(headless);

        // Build output paths (allows saving snapshots / report). If user supplied custom snapshot paths we keep them.
        config.updateOutputPaths();
        String baselineSnap;
        String currentSnap;
        if (useDefaultOutput) {
            baselineSnap = config.getBaselineSnapshot();
            currentSnap = config.getCurrentSnapshot();
        } else {
            baselineSnap = baselineSnapshotPath != null ? baselineSnapshotPath : config.getBaselineSnapshot();
            currentSnap = currentSnapshotPath != null ? currentSnapshotPath : config.getCurrentSnapshot();
        }

        List<ElementData> baselineElements;
        List<ElementData> currentElements;

        if (compareOnly) {
            logger.info("Running in compareOnly mode – loading existing snapshots");
            baselineElements = readSnapshot(baselineSnap);
            currentElements = readSnapshot(currentSnap);
        } else if (parallelCapture && baselineExternalDriver != null && currentExternalDriver != null) {
            logger.info("Parallel capture enabled using provided baseline & current drivers");
            final String bUrl = effectiveBaselineUrl;
            final String cUrl = effectiveCurrentUrl;
            java.util.concurrent.CompletableFuture<List<ElementData>> baseF = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try { return captureElementsWithProvidedDriver(baselineExternalDriver, bUrl, baselineSnap, config, projectConfig, true); } catch (Exception e) { throw new RuntimeException(e); }
            });
            java.util.concurrent.CompletableFuture<List<ElementData>> currF = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try { return captureElementsWithProvidedDriver(currentExternalDriver, cUrl, currentSnap, config, projectConfig, false); } catch (Exception e) { throw new RuntimeException(e); }
            });
            try {
                baselineElements = baseF.get();
                currentElements = currF.get();
            } catch (Exception ex) {
                logger.warn("Parallel capture failed ({}); falling back to sequential", ex.getMessage());
                baselineElements = captureElementsWithProvidedDriver(baselineExternalDriver, bUrl, baselineSnap, config, projectConfig, true);
                currentElements = captureElementsWithProvidedDriver(currentExternalDriver, cUrl, currentSnap, config, projectConfig, false);
            }
        } else {
            logger.info("Capturing baseline & current elements (compareOnly=false, sequential mode)");
            baselineElements = captureElements(effectiveBaselineUrl, baselineSnap, config, projectConfig, true);
            currentElements = captureElements(effectiveCurrentUrl, currentSnap, config, projectConfig, false);
        }

        // Match & detect changes
        ElementMatcher matcher = new ElementMatcher(projectConfig);
        ElementMatcher.MatchResult pairings = matcher.matchElements(baselineElements, currentElements);
        ChangeDetector detector = new ChangeDetector(config);
        List<ElementChange> changes = new ArrayList<>();
        pairings.getMatchedPairs().forEach((b,c) -> changes.addAll(detector.detectElementChanges(b,c)));
        pairings.getAddedElements().forEach(e -> { ElementChange ch = new ElementChange(); ch.setElement(e.getSelector()); ch.setChangeType("ELEMENT_ADDED"); ch.setNewValue(e.getTagName()); changes.add(ch); });
        pairings.getRemovedElements().forEach(e -> { ElementChange ch = new ElementChange(); ch.setElement(e.getSelector()); ch.setChangeType("ELEMENT_REMOVED"); ch.setOldValue(e.getTagName()); changes.add(ch); });
        logger.info("Detected {} changes", changes.size());

        // Optional report
        if (generateReport) {
            try {
                new SimpleReportGenerator(projectConfig).generateReport(changes, baselineElements, currentElements, config.getReportSimpleFile());
                logger.info("Report generated at {}", config.getReportSimpleFile());
            } catch (IOException ioe) {
                logger.warn("Failed generating report: {}", ioe.getMessage());
            }
        }

        // Optionally persist changes file when using default output directory
        String changesFilePath = null;
        if (useDefaultOutput && writeChangesFile) {
            changesFilePath = config.getChangesFile();
            writeChanges(changes, changesFilePath);
        } else if (useDefaultOutput && !writeChangesFile) {
            // Ensure no stale changes.json left over from prior runs
            File cf = new File(config.getChangesFile());
            if (cf.exists()) {
                boolean deleted = cf.delete();
                if (!deleted) {
                    logger.debug("Could not delete pre-existing changes file {} when writeChangesFile=false", cf.getAbsolutePath());
                }
            }
        }

        return new Result(baselineElements, currentElements, changes, baselineSnap, currentSnap, config.getOutputDir(), changesFilePath, generateReport ? config.getReportSimpleFile() : null);
    }

    private List<ElementData> captureElements(String url, String snapshotPath, Configuration config, ProjectConfig projectConfig, boolean baseline) throws Exception {
        boolean createdDriver = false;
        WebDriver driverToUse = externalDriver;
        DOMCSSCrawler crawler;
        if (driverToUse != null) {
            crawler = new DOMCSSCrawler(config.isHeadless(), driverToUse, config.getViewportWidth(), config.getViewportHeight(), config);
        } else {
            crawler = new DOMCSSCrawler(config.isHeadless(), null, config.getViewportWidth(), config.getViewportHeight(), config);
            createdDriver = true; // crawler internally creates driver
        }
        List<ElementData> elements;
        try {
            if (url != null) crawler.navigate(url, config.getWaitTime());
            elements = crawler.extractElements(config.getContainerXpath(), config.getMaxElements());
            for (ElementData e : elements) e.generateFingerprint(projectConfig);
            writeSnapshot(elements, snapshotPath);
            logger.info("Saved {} {} elements to {}", elements.size(), baseline?"baseline":"current", snapshotPath);
        } finally {
            if (createdDriver) crawler.close();
            else if (closeDriverOnFinish && !baseline) { // only close once after second capture
                try { crawler.close(); } catch (Exception ignore) {}
            }
        }
        return elements;
    }

    private List<ElementData> captureElementsWithProvidedDriver(WebDriver providedDriver, String url, String snapshotPath, Configuration config, ProjectConfig projectConfig, boolean baseline) throws Exception {
        DOMCSSCrawler crawler = new DOMCSSCrawler(config.isHeadless(), providedDriver, config.getViewportWidth(), config.getViewportHeight(), config);
        if (url != null && (safeCurrentUrl(providedDriver) == null || !safeCurrentUrl(providedDriver).equals(url))) {
            crawler.navigate(url, config.getWaitTime());
        }
        List<ElementData> elements = crawler.extractElements(config.getContainerXpath(), config.getMaxElements());
        for (ElementData e : elements) e.generateFingerprint(projectConfig);
        writeSnapshot(elements, snapshotPath);
        logger.info("Saved {} {} elements to {} (provided driver)", elements.size(), baseline?"baseline":"current", snapshotPath);
        return elements;
    }

    private String safeCurrentUrl(WebDriver d) { try { return d.getCurrentUrl(); } catch (Exception e) { return null; } }

    private void writeSnapshot(List<ElementData> elements, String path) {
        if (path == null) return;
        try {
            File f = new File(path); File parent = f.getParentFile(); if (parent != null && !parent.exists()) parent.mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, elements); // ensures UTF-8
        } catch (IOException e) {
            logger.warn("Unable to write snapshot {}: {}", path, e.getMessage());
        }
    }

    private void writeChanges(List<ElementChange> changes, String path) {
        if (path == null) return;
        try {
            File f = new File(path); File parent = f.getParentFile(); if (parent != null && !parent.exists()) parent.mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, changes); // ensures UTF-8
        } catch (IOException e) {
            logger.warn("Unable to write changes file {}: {}", path, e.getMessage());
        }
    }

    private List<ElementData> readSnapshot(String path) throws IOException {
        if (path == null) throw new IOException("Snapshot path is null");
        File f = new File(path);
        if (!f.exists()) throw new IOException("Snapshot not found: " + path);
        return mapper.readValue(f, mapper.getTypeFactory().constructCollectionType(List.class, ElementData.class));
    }

    private void validate() {
        if (baselineUrl == null && baselineExternalDriver == null && externalDriver == null) {
            throw new NullPointerException("baselineUrl or a baseline driver must be provided");
        }
        if (currentUrl == null && currentExternalDriver == null && externalDriver == null) {
            throw new NullPointerException("currentUrl or a current driver must be provided");
        }
        if (compareOnly) {
            if (baselineSnapshotPath == null || currentSnapshotPath == null) {
                logger.warn("compareOnly=true but snapshot paths not provided – will load defaults from generated output directory");
            }
        }
    }

    // =====================================================================
    // Builder
    // =====================================================================
    public static class Builder {
        private String baselineUrl;
        private String currentUrl;
        private String containerXpath;
        private Integer waitTimeSeconds;
        private Integer maxElements;
        private boolean headless = true;
        private boolean compareOnly = false;
    private WebDriver webDriver; // single reuse driver
    private WebDriver baselineDriver; // dual-driver mode
    private WebDriver currentDriver;
        private boolean closeDriverOnFinish = false;
        private String baselineSnapshotPath;
        private String currentSnapshotPath;
        private boolean generateReport = false;
    private boolean useDefaultOutput = false;
    private boolean writeChangesFile = true;
    private boolean parallelCapture = false;

        public Builder baselineUrl(String url) { this.baselineUrl = url; return this; }
        public Builder currentUrl(String url) { this.currentUrl = url; return this; }
        public Builder containerXpath(String xpath) { this.containerXpath = xpath; return this; }
        public Builder waitTimeSeconds(int seconds) { this.waitTimeSeconds = seconds; return this; }
        public Builder maxElements(int max) { this.maxElements = max; return this; }
        public Builder headless(boolean headless) { this.headless = headless; return this; }
        public Builder compareOnly(boolean compareOnly) { this.compareOnly = compareOnly; return this; }
    public Builder webDriver(WebDriver driver) { this.webDriver = driver; return this; }
    public Builder baselineDriver(WebDriver driver) { this.baselineDriver = driver; return this; }
    public Builder currentDriver(WebDriver driver) { this.currentDriver = driver; return this; }
        public Builder closeDriverOnFinish(boolean close) { this.closeDriverOnFinish = close; return this; }
        public Builder baselineSnapshotPath(String path) { this.baselineSnapshotPath = path; return this; }
        public Builder currentSnapshotPath(String path) { this.currentSnapshotPath = path; return this; }
        public Builder generateReport(boolean val) { this.generateReport = val; return this; }
    public Builder useDefaultOutput(boolean val) { this.useDefaultOutput = val; return this; }
    public Builder writeChangesFile(boolean val) { this.writeChangesFile = val; return this; }
    public Builder parallelCapture(boolean val) { this.parallelCapture = val; return this; }
        public WebChangeDetector build() { return new WebChangeDetector(this); }
    }

    // =====================================================================
    // Result
    // =====================================================================
    public static class Result {
        private final List<ElementData> baselineElements;
        private final List<ElementData> currentElements;
        private final List<ElementChange> changes;
        private final String baselineSnapshotPath;
        private final String currentSnapshotPath;
        private final String outputDir;
        private final String changesFilePath;
        private final String reportSimplePath;

        public Result(List<ElementData> baselineElements, List<ElementData> currentElements, List<ElementChange> changes, String baselineSnapshotPath, String currentSnapshotPath, String outputDir, String changesFilePath, String reportSimplePath) {
            this.baselineElements = baselineElements;
            this.currentElements = currentElements;
            this.changes = changes;
            this.baselineSnapshotPath = baselineSnapshotPath;
            this.currentSnapshotPath = currentSnapshotPath;
            this.outputDir = outputDir;
            this.changesFilePath = changesFilePath;
            this.reportSimplePath = reportSimplePath;
        }
        public List<ElementData> getBaselineElements() { return baselineElements; }
        public List<ElementData> getCurrentElements() { return currentElements; }
        public List<ElementChange> getChanges() { return changes; }
        public String getBaselineSnapshotPath() { return baselineSnapshotPath; }
        public String getCurrentSnapshotPath() { return currentSnapshotPath; }
        public String getOutputDir() { return outputDir; }
        public String getChangesFilePath() { return changesFilePath; }
        public String getReportSimplePath() { return reportSimplePath; }
    }
}
