package com.uitester.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized configuration for UI change detection pipeline.
 * This class manages all settings and paths for the UI testing process.
 */
public class Configuration {
    // Default configuration values
    private static final String TEST_FILES_DIR = "test_files";
    private static final String DEFAULT_OUTPUT_DIR = "output";
    
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

    /**
     * Create a new Configuration with default values
     */
    public Configuration() {
        // Set default values
        try {
            this.baselineUrl = "file:///" + new File(TEST_FILES_DIR, "baseline.html").getAbsolutePath();
            this.currentUrl = "file:///" + new File(TEST_FILES_DIR, "current.html").getAbsolutePath();
        } catch (Exception e) {
            this.baselineUrl = "file:///test_files/baseline.html";
            this.currentUrl = "file:///test_files/current.html";
        }
        
        this.maxElements = null;
        this.waitTime = 45;
        this.scrollTime = 2.0;
        this.headless = false;
        this.enableScrolling = true;
        this.containerXpath = null;
        this.parallelCrawling = true;
        
        this.viewportWidth = null;
        this.viewportHeight = null;
        
        this.maxChanges = 500;
        this.detectStructuralChanges = false; // Default to not detecting structural changes
        
        this.sectionName = null;
        
        // Set up output paths
        this.outputDir = createOutputDirectory();
        this.baselineSnapshot = new File(outputDir, "baseline.json").getAbsolutePath();
        this.currentSnapshot = new File(outputDir, "current.json").getAbsolutePath();
        this.changesFile = new File(outputDir, "changes.json").getAbsolutePath();
        this.reportFile = new File(outputDir, "report.html").getAbsolutePath();
    }

    /**
     * Create a unique output directory for this comparison.
     * 
     * @return Path to the output directory
     */
    private String createOutputDirectory() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baselineEnv = extractEnvName(baselineUrl);
        String currentEnv = extractEnvName(currentUrl);
        
        // Determine section name
        String section;
        if (sectionName != null && !sectionName.isEmpty()) {
            section = sectionName.replaceAll("[^a-zA-Z0-9_-]", "_");
        } else if (containerXpath != null && !containerXpath.isEmpty()) {
            section = extractSectionName(containerXpath);
        } else {
            section = "full-page";
        }
        
        // Create directory name
        String dirName = String.format("%s_%s_vs_%s_%s", timestamp, baselineEnv, currentEnv, section);
        
        // Create full path
        File outputDir = new File(DEFAULT_OUTPUT_DIR, dirName);
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
        String urlLower = url.toLowerCase();
        
        // Check for common environment indicators
        Pattern[] patterns = {
            Pattern.compile("[-_.]pie[-_.]"),
            Pattern.compile("[-_.]stage[-_.]"),
            Pattern.compile("[-_.]test[-_.]"),
            Pattern.compile("[-_.]dev[-_.]"),
            Pattern.compile("[-_.]qa[-_.]")
        };
        
        String[] envNames = {"pie", "stage", "test", "dev", "qa"};
        
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(urlLower);
            if (matcher.find()) {
                return envNames[i];
            }
        }
        
        // Try to get domain name for production URLs
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null) {
                // Extract first part of domain (e.g. "example" from "example.com")
                return domain.split("\\.")[0];
            }
        } catch (URISyntaxException e) {
            // Ignore parsing errors
        }
        
        // Default to a simple name for file:// URLs
        if (urlLower.startsWith("file:")) {
            return "local";
        }
        
        // Last resort
        return "unknown";
    }
    
    /**
     * Extract a section name from an XPath expression
     * 
     * @param xpath XPath to extract section name from
     * @return Extracted section name
     */
    private String extractSectionName(String xpath) {
        // Extract ID from #id
        Pattern idPattern = Pattern.compile("id\\(['\"]([^'\"]+)['\"]\\)|\\[@id=['\"]([^'\"]+)['\"]\\]");
        Matcher idMatcher = idPattern.matcher(xpath);
        if (idMatcher.find()) {
            String id = idMatcher.group(1) != null ? idMatcher.group(1) : idMatcher.group(2);
            return id;
        }
        
        // Extract from simple tag name
        Pattern tagPattern = Pattern.compile("//([a-zA-Z0-9]+)\\b");
        Matcher tagMatcher = tagPattern.matcher(xpath);
        if (tagMatcher.find()) {
            return tagMatcher.group(1).toLowerCase();
        }
        
        // Default for complex XPath
        return "section";
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
    
    /**
     * Updates output paths after changing the output directory
     */
    public void updateOutputPaths() {
        this.baselineSnapshot = new File(outputDir, "baseline.json").getAbsolutePath();
        this.currentSnapshot = new File(outputDir, "current.json").getAbsolutePath();
        this.changesFile = new File(outputDir, "changes.json").getAbsolutePath();
        this.reportFile = new File(outputDir, "report.html").getAbsolutePath();
    }
}
