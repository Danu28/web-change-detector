package com.uitester.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.diff.ElementChange;
import com.uitester.main.UITesterApplication;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * TestNG-based test class for UI change detection
 * Supports parameterized testing with optional xpath and section parameters
 */
public class LandingPageTests {
    
    private Configuration config;
    private ObjectMapper objectMapper;

    
    @BeforeClass
    @Parameters({"baseline.url", "current.url", "xpath", "section.name",
            "detect.structural", "headless", "viewport.width", "viewport.height", "wait.time"})
    public void setUp(
            @Optional("") String baselineUrl,
            @Optional("") String currentUrl,
            @Optional("") String xpath,
            @Optional("") String sectionName,
            @Optional("false") String detectStructural,
            @Optional("true") String headless,
            @Optional("1920") int viewportWidth,
            @Optional("1080") int viewportHeight,
            @Optional("60") int waitTime) {
        
        // Initialize configuration
        config = new Configuration();
        objectMapper = new ObjectMapper();
        
        // Set URLs if provided
        if (!baselineUrl.isEmpty()) {
            config.setBaselineUrl(baselineUrl);
        }
        if (!currentUrl.isEmpty()) {
            config.setCurrentUrl(currentUrl);
        }
        
        // Set XPath container if provided
        if (!xpath.isEmpty()) {
            config.setContainerXpath(xpath);
        }
        
        // Set section name if provided
        if (!sectionName.isEmpty()) {
            config.setSectionName(sectionName);
        }
        
        // Set structural change detection
        config.setDetectStructuralChanges(Boolean.parseBoolean(detectStructural));
        
        // Set headless mode
        config.setHeadless(Boolean.parseBoolean(headless));

        // Set viewport dimensions
        config.setViewportWidth(viewportWidth);
        config.setViewportHeight(viewportHeight);

        // Set wait time
        config.setWaitTime(waitTime);
        
        // Update output paths
        config.updateOutputPaths();
        
        System.out.println("Test Configuration:");
        System.out.println("- Baseline URL: " + config.getBaselineUrl());
        System.out.println("- Current URL: " + config.getCurrentUrl());
        System.out.println("- XPath: " + config.getContainerXpath());
        System.out.println("- Section: " + config.getSectionName());
        System.out.println("- Detect Structural: " + config.isDetectStructuralChanges());
        System.out.println("- Headless: " + config.isHeadless());
        System.out.println("- Output Dir: " + config.getOutputDir());
    }
    
    @Test(priority = 1)
    public void verifySection() throws Exception {
        System.out.println("Starting UI change detection test...");
        
        // Create UITesterApplication instance
        UITesterApplication app = new UITesterApplication(config);
        
        // Run the UI testing process
        app.run();
    
        // Load and validate changes
        List<ElementChange> changes = loadChanges();
    
        Assert.assertTrue(changes.isEmpty(), "No changes should be detected between identical pages");
    }
    
    private List<ElementChange> loadChanges() throws Exception {
        return loadChangesFromConfig(config);
    }
    
    private List<ElementChange> loadChangesFromConfig(Configuration cfg) throws Exception {
        File changesFile = new File(cfg.getChangesFile());
        return objectMapper.readValue(
            new FileReader(changesFile),
                new TypeReference<>() {
                }
        );
    }
}
