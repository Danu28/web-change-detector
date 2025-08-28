package com.uitester.test;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.main.UITesterApplication;
import com.uitester.report.ReportGenerator;

/**
 * TestNG-based test class for UI change detection
 * Supports parameterized testing with optional xpath and section parameters
 */
public class UIChangeDetectionTest {
    
    private Configuration config;
    private ObjectMapper objectMapper;
    
    @BeforeClass
    @Parameters({"baseline.url", "current.url", "xpath", "section.name", "detect.structural", "headless"})
    public void setUp(
            @Optional("") String baselineUrl,
            @Optional("") String currentUrl,
            @Optional("") String xpath,
            @Optional("") String sectionName,
            @Optional("false") String detectStructural,
            @Optional("true") String headless) {
        
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
    
    @Test(description = "Test UI change detection between baseline and current URLs")
    public void testUIChangeDetection() throws Exception {
        System.out.println("Starting UI change detection test...");
        
        // Create UITesterApplication instance
        UITesterApplication app = new UITesterApplication(config);
        
        // Run the UI testing process
        app.run();
        
        // Verify that output files were created
        verifyOutputFiles();
        
        // Load and validate changes
        List<ElementChange> changes = loadChanges();
        
        // Print summary
        printTestSummary(changes);
        
        System.out.println("UI change detection test completed successfully!");
    }
    
    @Test(description = "Test change detection with structural changes enabled", dependsOnMethods = "testUIChangeDetection")
    @Parameters({"enable.structural.test"})
    public void testUIChangeDetectionWithStructural(@Optional("false") String enableTest) throws Exception {
        if (!Boolean.parseBoolean(enableTest)) {
            System.out.println("Structural change test skipped (enable.structural.test=false)");
            return;
        }
        
        System.out.println("Starting UI change detection test with structural changes...");
        
        // Create new configuration with structural changes enabled
        Configuration structuralConfig = getConfiguration();

        // Create UITesterApplication instance
        UITesterApplication app = new UITesterApplication(structuralConfig);
        
        // Run the UI testing process
        app.run();
        
        // Load changes from both runs
        List<ElementChange> regularChanges = loadChanges();
        List<ElementChange> structuralChanges = loadChangesFromConfig(structuralConfig);
        
        // Verify that structural test found more or equal changes
        Assert.assertTrue(structuralChanges.size() >= regularChanges.size(), 
            "Structural test should find more or equal changes than regular test");
        
        System.out.println("Regular changes: " + regularChanges.size());
        System.out.println("Structural changes: " + structuralChanges.size());
        System.out.println("Structural change detection test completed successfully!");
    }

    private Configuration getConfiguration() {
        Configuration structuralConfig = new Configuration();
        structuralConfig.setBaselineUrl(config.getBaselineUrl());
        structuralConfig.setCurrentUrl(config.getCurrentUrl());
        structuralConfig.setContainerXpath(config.getContainerXpath());
        structuralConfig.setSectionName(config.getSectionName() + "_structural");
        structuralConfig.setDetectStructuralChanges(true);
        structuralConfig.setHeadless(config.isHeadless());
        structuralConfig.updateOutputPaths();
        return structuralConfig;
    }

    @Test(description = "Validate change classifications", dependsOnMethods = "testUIChangeDetection")
    public void testChangeClassifications() throws Exception {
        System.out.println("Validating change classifications...");
        
        List<ElementChange> changes = loadChanges();
        
        for (ElementChange change : changes) {
            // Verify classification is valid
            String classification = change.getClassification();
            Assert.assertTrue(
                "critical".equals(classification) || 
                "cosmetic".equals(classification) || 
                "noise".equals(classification),
                "Invalid classification: " + classification
            );
            
            // Verify magnitude is within valid range
            double magnitude = change.getMagnitude();
            Assert.assertTrue(magnitude >= 0.0 && magnitude <= 1.0, 
                "Magnitude should be between 0.0 and 1.0, got: " + magnitude);
        }
        
        System.out.println("Change classifications validation completed successfully!");
    }
    
    @Test(description = "Test report generation", dependsOnMethods = "testUIChangeDetection")
    public void testReportGeneration() throws Exception {
        System.out.println("Testing report generation...");
        
        // Verify HTML report exists
        File reportFile = new File(config.getReportFile());
        Assert.assertTrue(reportFile.exists(), "HTML report should be generated");
        Assert.assertTrue(reportFile.length() > 0, "HTML report should not be empty");
        
        // Verify report contains expected content
        String reportContent = new String(java.nio.file.Files.readAllBytes(reportFile.toPath()));
        Assert.assertTrue(reportContent.contains("<html"), "Report should be valid HTML");
        Assert.assertTrue(reportContent.contains("UI Change Detection Report"), "Report should have title");
        
        System.out.println("Report generation test completed successfully!");
        System.out.println("Report available at: " + config.getReportFile());
    }
    
    private void verifyOutputFiles() {
        // Verify baseline snapshot
        File baselineFile = new File(config.getBaselineSnapshot());
        Assert.assertTrue(baselineFile.exists(), "Baseline snapshot should be created");
        
        // Verify current snapshot
        File currentFile = new File(config.getCurrentSnapshot());
        Assert.assertTrue(currentFile.exists(), "Current snapshot should be created");
        
        // Verify changes file
        File changesFile = new File(config.getChangesFile());
        Assert.assertTrue(changesFile.exists(), "Changes file should be created");
        
        // Verify report file
        File reportFile = new File(config.getReportFile());
        Assert.assertTrue(reportFile.exists(), "Report file should be created");
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
    
    private void printTestSummary(List<ElementChange> changes) {
        System.out.println("\n=== TEST SUMMARY ===");
        System.out.println("Total changes detected: " + changes.size());
        
        // Count by change type
        long attributeChanges = changes.stream().filter(c -> c.getChangeType().equals("attribute")).count();
        long textChanges = changes.stream().filter(c -> c.getChangeType().equals("text")).count();
        long styleChanges = changes.stream().filter(c -> c.getChangeType().startsWith("style_")).count();
        long layoutChanges = changes.stream().filter(c -> c.getChangeType().equals("layout")).count();
        long structuralChanges = changes.stream().filter(c -> c.getChangeType().equals("structural")).count();
        
        System.out.println("- Attribute changes: " + attributeChanges);
        System.out.println("- Text changes: " + textChanges);
        System.out.println("- Style changes: " + styleChanges);
        System.out.println("- Layout changes: " + layoutChanges);
        System.out.println("- Structural changes: " + structuralChanges);
        
        // Count by classification
        long criticalChanges = changes.stream().filter(c -> "critical".equals(c.getClassification())).count();
        long cosmeticChanges = changes.stream().filter(c -> "cosmetic".equals(c.getClassification())).count();
        long noiseChanges = changes.stream().filter(c -> "noise".equals(c.getClassification())).count();
        
        System.out.println("- Critical changes: " + criticalChanges);
        System.out.println("- Cosmetic changes: " + cosmeticChanges);
        System.out.println("- Noise changes: " + noiseChanges);
        
        System.out.println("===================\n");
    }
}
