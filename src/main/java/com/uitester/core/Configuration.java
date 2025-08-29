package com.uitester.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized configuration for UI change detection pipeline.
 * This class manages all settings and paths for the UI testing process.
 */
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    
    // Default configuration values
    private static final String TEST_FILES_DIR = "test_files";
    private static final String DEFAULT_OUTPUT_DIR = "output";
    
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

    /**
     * Create a new Configuration with default values
     */
    public Configuration() {
        // Load project configuration first
        this.projectConfig = ConfigLoader.loadConfig();
        
        // Set default values
        try {
            this.baselineUrl = "file:///" + new File(TEST_FILES_DIR, "baseline.html").getAbsolutePath();
            this.currentUrl = "file:///" + new File(TEST_FILES_DIR, "current.html").getAbsolutePath();
        } catch (Exception e) {
            this.baselineUrl = "file:///test_files/baseline.html";
            this.currentUrl = "file:///test_files/current.html";
        }
        
        // Use configuration values if available, otherwise use hardcoded defaults
        this.maxElements = projectConfig.getPerformanceSettings() != null ? 
                          projectConfig.getPerformanceSettings().getMaxElements() : null;
        this.waitTime = 45;
        this.scrollTime = 2.0;
        this.headless = false;
        this.enableScrolling = true;
        this.containerXpath = null;
        this.parallelCrawling = projectConfig.getPerformanceSettings() != null ? 
                               Boolean.TRUE.equals(projectConfig.getPerformanceSettings().getEnableParallelProcessing()) : true;
        
        this.viewportWidth = null;
        this.viewportHeight = null;
        
        this.maxChanges = 500;
        this.detectStructuralChanges = false; // Default to not detecting structural changes
        
        this.sectionName = null;
        
        // Output paths will be set when updateOutputPaths() is called
        this.outputDir = null;
        this.baselineSnapshot = null;
        this.currentSnapshot = null;
        this.changesFile = null;
        this.reportFile = null;
        
        logger.info("Configuration initialized with project config");
    }

    /**
     * Create output directory for this comparison.
     * 
     * @return Path to the output directory
     */
    private String createOutputDirectory() {
        String baselineEnv = extractEnvName(baselineUrl);
        String currentEnv = extractEnvName(currentUrl);
        
        // Use section name if provided, otherwise default to "full-page"
        String section = (sectionName != null && !sectionName.isEmpty()) 
                        ? sectionName.replaceAll("[^a-zA-Z0-9_-]", "_") 
                        : "full-page";
        
        // Create directory name: section_env1-vs-env2
        String dirName = String.format("%s_%s-vs-%s", section, baselineEnv, currentEnv);
        
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
        // Create output directory with current settings - ONLY ONCE HERE
        this.outputDir = createOutputDirectory();
        
        this.baselineSnapshot = new File(outputDir, "baseline.json").getAbsolutePath();
        this.currentSnapshot = new File(outputDir, "current.json").getAbsolutePath();
        this.changesFile = new File(outputDir, "changes.json").getAbsolutePath();
        this.reportFile = new File(outputDir, "report.html").getAbsolutePath();
    }
}
