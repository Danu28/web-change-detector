package com.uitester.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized configuration for UI change detection pipeline.
 * This class manages all settings and paths for the UI testing process.
 */
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    
    // Default configuration values (legacy fallback)
    private static final String TEST_FILES_DIR = "test_files";
    private static final String DEFAULT_OUTPUT_ROOT = "output";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    // Project configuration loaded from config.json
    private ProjectConfig projectConfig;
    
    // URLs
    private String baselineUrl;
    private String currentUrl;
    
    // Crawler settings
    private Integer maxElements;
    private int waitTime;
    private double scrollTime;
    private boolean headless;
    private boolean enableScrolling;
    private String containerXpath;
    private boolean parallelCrawling;
    
    // Viewport settings
    private Integer viewportWidth;
    private Integer viewportHeight;
    
    // Detection settings
    private Integer maxChanges;
    private boolean detectStructuralChanges;
    
    // Section info
    private String sectionName;
    
    // Output paths
    private String outputDir;
    private String baselineSnapshot;
    private String currentSnapshot;
    private String changesFile;
    private String reportFile;
    private String reportSimpleFile;

    /**
     * Create a new Configuration with default values
     */
    public Configuration() {
    // Load project configuration first (includes new modular sections)
    this.projectConfig = ConfigLoader.loadConfig();

    // Baseline/current default URLs (legacy local file fallback) if not overridden later
    try {
        this.baselineUrl = "file:///" + new File(TEST_FILES_DIR, "baseline.html").getAbsolutePath();
        this.currentUrl = "file:///" + new File(TEST_FILES_DIR, "current.html").getAbsolutePath();
    } catch (Exception e) {
        this.baselineUrl = "file:///test_files/baseline.html";
        this.currentUrl = "file:///test_files/current.html";
    }

    // Runtime / session-scoped knobs (these may be overridden via CLI)
    this.maxElements = projectConfig.getPerformanceSettings() != null ?
        projectConfig.getPerformanceSettings().getMaxElements() : null; // if null crawler decides
    this.waitTime = 45; // can be CLI override
    this.scrollTime = 2.0;
    this.headless = false;
    this.enableScrolling = true;
    this.containerXpath = null;
    this.parallelCrawling = projectConfig.getPerformanceSettings() != null ?
        Boolean.TRUE.equals(projectConfig.getPerformanceSettings().getEnableParallelProcessing()) : true;
    this.viewportWidth = null;
    this.viewportHeight = null;
    this.maxChanges = 500;
    // Default structural detection now governed by flags (legacy field retained for backward compat)
    this.detectStructuralChanges = projectConfig.getFlags() != null &&
        Boolean.TRUE.equals(projectConfig.getFlags().getEnableStructuralAnalysis());
    this.sectionName = null;

    // Output paths built lazily once we know URLs / section
    this.outputDir = null;
    this.baselineSnapshot = null;
    this.currentSnapshot = null;
    this.changesFile = null;
    this.reportFile = null;

    logger.info("Configuration initialized (flags: structuralAnalysis={}, semanticMatching={}, advancedClassification={})",
        isStructuralAnalysisEnabled(), isSemanticMatchingEnabled(), isAdvancedClassificationEnabled());
    }

    /**
     * Create output directory for this comparison.
     * 
     * @return Path to the output directory
     */
    private String createOutputDirectory() {
        String baselineHost = extractEnvName(baselineUrl);
        String currentHost = extractEnvName(currentUrl);
        String section = (sectionName != null && !sectionName.isEmpty()) ? sanitizeSegment(sectionName) : "full-page";
        String timestamp = LocalDateTime.now().format(TS_FORMAT);

        String template = null;
        if (projectConfig.getOutputSettings() != null && projectConfig.getOutputSettings().getDirectoryTemplate() != null) {
            template = projectConfig.getOutputSettings().getDirectoryTemplate();
        }
        if (template == null || template.trim().isEmpty()) {
            template = "{section}_{baselineHost}-vs-{currentHost}"; // backward compatible base
        }

        String dirName = template
                .replace("{section}", section)
                .replace("{baselineHost}", sanitizeSegment(baselineHost))
                .replace("{currentHost}", sanitizeSegment(currentHost))
                .replace("{timestamp}", timestamp);

        File outputDir = new File(DEFAULT_OUTPUT_ROOT, dirName);
        outputDir.mkdirs();
        return outputDir.getAbsolutePath();
    }
    
    /**
     * Extract environment name from URL or domain
     * 
     * @param url URL to extract environment name from
     * @return Extracted environment name (pie, stage, prod, etc.)
     */
    private String extractEnvName(String url) {
        // Handle local files
        if (url.toLowerCase().startsWith("file:")) {
            return "local";
        }
        
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null) {
                return domain;
            }
        } catch (URISyntaxException e) {
            // Ignore parsing errors
        }
        
        // Last resort
        return "unknown";
    }
    
    // Getters and setters
    public String getBaselineUrl() {
        return baselineUrl;
    }

    public void setBaselineUrl(String baselineUrl) {
        this.baselineUrl = baselineUrl;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public Integer getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(Integer maxElements) {
        this.maxElements = maxElements;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public double getScrollTime() {
        return scrollTime;
    }

    public void setScrollTime(double scrollTime) {
        this.scrollTime = scrollTime;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isEnableScrolling() {
        return enableScrolling;
    }

    public void setEnableScrolling(boolean enableScrolling) {
        this.enableScrolling = enableScrolling;
    }

    public String getContainerXpath() {
        return containerXpath;
    }

    public void setContainerXpath(String containerXpath) {
        this.containerXpath = containerXpath;
    }

    public boolean isParallelCrawling() {
        return parallelCrawling;
    }

    public void setParallelCrawling(boolean parallelCrawling) {
        this.parallelCrawling = parallelCrawling;
    }

    public Integer getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(Integer viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public Integer getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(Integer viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public Integer getMaxChanges() {
        return maxChanges;
    }

    public void setMaxChanges(Integer maxChanges) {
        this.maxChanges = maxChanges;
    }

    public boolean isDetectStructuralChanges() {
        return detectStructuralChanges;
    }

    public void setDetectStructuralChanges(boolean detectStructuralChanges) {
        this.detectStructuralChanges = detectStructuralChanges;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getBaselineSnapshot() {
        return baselineSnapshot;
    }

    public void setBaselineSnapshot(String baselineSnapshot) {
        this.baselineSnapshot = baselineSnapshot;
    }

    public String getCurrentSnapshot() {
        return currentSnapshot;
    }

    public void setCurrentSnapshot(String currentSnapshot) {
        this.currentSnapshot = currentSnapshot;
    }

    public String getChangesFile() {
        return changesFile;
    }

    public void setChangesFile(String changesFile) {
        this.changesFile = changesFile;
    }

    public String getReportFile() {
        return reportFile;
    }

    public void setReportFile(String reportFile) {
        this.reportFile = reportFile;
    }
    public String getReportSimpleFile() { return reportSimpleFile; }
    public void setReportSimpleFile(String reportSimpleFile) { this.reportSimpleFile = reportSimpleFile; }
    
    /**
     * Get the loaded project configuration.
     * 
     * @return ProjectConfig object with all configuration settings
     */
    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }
    
    /**
     * Updates output paths after changing the output directory.
     * This is the ONLY place where the output directory is created.
     */
    public void updateOutputPaths() {
        this.outputDir = createOutputDirectory();
        // Determine file names: prefer outputSettings overrides
        String baselineName = fileNameOrDefault(() -> projectConfig.getOutputSettings() != null ? projectConfig.getOutputSettings().getBaselineFile() : null, "baseline.json");
        String currentName = fileNameOrDefault(() -> projectConfig.getOutputSettings() != null ? projectConfig.getOutputSettings().getCurrentFile() : null, "current.json");
        String changesName = fileNameOrDefault(() -> projectConfig.getOutputSettings() != null ? projectConfig.getOutputSettings().getChangesFile() : null, "changes.json");
        String reportName = fileNameOrDefault(() -> projectConfig.getOutputSettings() != null ? projectConfig.getOutputSettings().getReportFile() : null, "report.html");
    String reportSimpleName = fileNameOrDefault(() -> projectConfig.getOutputSettings() != null ? projectConfig.getOutputSettings().getReportSimpleFile() : null, "report-simple.html");

        this.baselineSnapshot = new File(outputDir, baselineName).getAbsolutePath();
        this.currentSnapshot = new File(outputDir, currentName).getAbsolutePath();
        this.changesFile = new File(outputDir, changesName).getAbsolutePath();
        this.reportFile = new File(outputDir, reportName).getAbsolutePath();
    this.reportSimpleFile = new File(outputDir, reportSimpleName).getAbsolutePath();
    }

    private interface FileNameSupplier { String get(); }
    private String fileNameOrDefault(FileNameSupplier sup, String defVal) {
        try {
            String v = sup.get();
            if (v != null && !v.trim().isEmpty()) return v.trim();
        } catch (Exception ignored) {}
        return defVal;
    }

    private String sanitizeSegment(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ================= Convenience Effective Getters =================

    public int getEffectiveMaxElements() {
        if (maxElements != null) return maxElements;
        if (projectConfig.getPerformanceSettings() != null && projectConfig.getPerformanceSettings().getMaxElements() != null)
            return projectConfig.getPerformanceSettings().getMaxElements();
        return 10000; // fallback
    }

    public boolean isStructuralAnalysisEnabled() {
        if (projectConfig.getFlags() != null && projectConfig.getFlags().getEnableStructuralAnalysis() != null)
            return projectConfig.getFlags().getEnableStructuralAnalysis();
        return detectStructuralChanges; // legacy field
    }

    public boolean isSemanticMatchingEnabled() {
        if (projectConfig.getFlags() != null && projectConfig.getFlags().getEnableSemanticMatching() != null)
            return projectConfig.getFlags().getEnableSemanticMatching();
        return true;
    }

    public boolean isAdvancedClassificationEnabled() {
        if (projectConfig.getFlags() != null && projectConfig.getFlags().getEnableAdvancedClassification() != null)
            return projectConfig.getFlags().getEnableAdvancedClassification();
        return false;
    }

    // ================= Override Application (dot-notation) =================

    /**
     * Apply string overrides using dot notation (e.g. comparison.textSimilarityThreshold=0.9)
     * Recognized roots: comparison, matching, classification, fingerprint, performance.
     */
    public void applyOverrides(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) return;
        Map<String, String> unresolved = new HashMap<>();
        for (Map.Entry<String, String> e : overrides.entrySet()) {
            String key = e.getKey(); String value = e.getValue(); boolean applied = false;
            try {
                if (key.startsWith("comparison.")) {
                    ensureComparison();
                    applied = applyComparisonOverride(key.substring(11), value);
                } else if (key.startsWith("matching.")) {
                    ensureMatching();
                    applied = applyMatchingOverride(key.substring(9), value);
                } else if (key.startsWith("classification.")) {
                    ensureClassification();
                    applied = applyClassificationOverride(key.substring(14), value);
                } else if (key.startsWith("fingerprint.")) {
                    ensureFingerprint();
                    applied = applyFingerprintOverride(key.substring(12), value);
                } else if (key.startsWith("performance.")) {
                    ensurePerformance();
                    applied = applyPerformanceOverride(key.substring(12), value);
                }
            } catch (Exception ex) {
                logger.warn("Error applying override {}={}: {}", key, value, ex.getMessage());
            }
            if (!applied) unresolved.put(key, value);
        }
        if (!unresolved.isEmpty()) {
            logger.warn("Unresolved overrides (ignored): {}", unresolved);
        }
    }

    private void ensureComparison() { if (projectConfig.getComparisonSettings() == null) projectConfig.setComparisonSettings(new ProjectConfig.ComparisonSettings()); }
    private void ensureMatching() { if (projectConfig.getMatchingSettings() == null) projectConfig.setMatchingSettings(new ProjectConfig.MatchingSettings()); }
    private void ensureClassification() { if (projectConfig.getClassificationSettings() == null) projectConfig.setClassificationSettings(new ProjectConfig.ClassificationSettings()); }
    private void ensureFingerprint() { if (projectConfig.getFingerprintSettings() == null) projectConfig.setFingerprintSettings(new ProjectConfig.FingerprintSettings()); }
    private void ensurePerformance() { if (projectConfig.getPerformanceSettings() == null) projectConfig.setPerformanceSettings(new ProjectConfig.PerformanceSettings()); }

    private boolean applyComparisonOverride(String field, String value) {
        ProjectConfig.ComparisonSettings cs = projectConfig.getComparisonSettings();
        switch (field) {
            case "textSimilarityThreshold": cs.setTextSimilarityThreshold(Double.parseDouble(value)); return true;
            case "numericChangeThreshold": cs.setNumericChangeThreshold(Double.parseDouble(value)); return true;
            case "colorChangeThreshold": cs.setColorChangeThreshold(Double.parseDouble(value)); return true;
        }
        return false;
    }
    private boolean applyMatchingOverride(String field, String value) {
        ProjectConfig.MatchingSettings ms = projectConfig.getMatchingSettings();
        switch (field) {
            case "tagWeight": ms.setTagWeight(Double.parseDouble(value)); return true;
            case "textWeight": ms.setTextWeight(Double.parseDouble(value)); return true;
            case "structuralWeight": ms.setStructuralWeight(Double.parseDouble(value)); return true;
            case "contentWeight": ms.setContentWeight(Double.parseDouble(value)); return true;
            case "fuzzyMinConfidence": ms.setFuzzyMinConfidence(Double.parseDouble(value)); return true;
            case "semanticPriceConfidence": ms.setSemanticPriceConfidence(Double.parseDouble(value)); return true;
            case "enableSemanticPrice": ms.setEnableSemanticPrice(Boolean.parseBoolean(value)); return true;
        }
        return false;
    }
    private boolean applyClassificationOverride(String field, String value) {
        ProjectConfig.ClassificationSettings cs = projectConfig.getClassificationSettings();
        if (field.startsWith("magnitudeThresholds.")) {
            String name = field.substring("magnitudeThresholds.".length());
            Map<String, Double> map = cs.getMagnitudeThresholds();
            if (map == null) { map = new HashMap<>(); cs.setMagnitudeThresholds(map); }
            map.put(name, Double.parseDouble(value));
            return true;
        }
        return false;
    }
    private boolean applyFingerprintOverride(String field, String value) {
        ProjectConfig.FingerprintSettings fs = projectConfig.getFingerprintSettings();
        switch (field) {
            case "useTagName": fs.setUseTagName(Boolean.parseBoolean(value)); return true;
            case "useTextContent": fs.setUseTextContent(Boolean.parseBoolean(value)); return true;
            case "textContentMaxLength": fs.setTextContentMaxLength(Integer.parseInt(value)); return true;
            case "normalizeWhitespace": fs.setNormalizeWhitespace(Boolean.parseBoolean(value)); return true;
            case "caseSensitive": fs.setCaseSensitive(Boolean.parseBoolean(value)); return true;
            case "includePosition": fs.setIncludePosition(Boolean.parseBoolean(value)); return true;
        }
        return false;
    }
    private boolean applyPerformanceOverride(String field, String value) {
        ProjectConfig.PerformanceSettings ps = projectConfig.getPerformanceSettings();
        switch (field) {
            case "maxElements": ps.setMaxElements(Integer.parseInt(value)); return true;
            case "maxProcessingTimeMs": ps.setMaxProcessingTimeMs(Integer.parseInt(value)); return true;
            case "enableParallelProcessing": ps.setEnableParallelProcessing(Boolean.parseBoolean(value)); return true;
            case "memoryWarningThresholdMb": ps.setMemoryWarningThresholdMb(Integer.parseInt(value)); return true;
        }
        return false;
    }
}
