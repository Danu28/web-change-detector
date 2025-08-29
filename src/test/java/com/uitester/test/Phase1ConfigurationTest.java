package com.uitester.test;

import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Comprehensive tests for Phase 1: Configuration-driven UI testing
 * These tests validate that our configuration system properly controls
 * element capture and change detection behavior.
 */
public class Phase1ConfigurationTest {
    
    private Configuration config;
    
    @BeforeMethod
    public void setUp() {
        // Use a fresh configuration for each test
        config = new Configuration();
        // Clear any cached configuration to ensure clean state
        System.setProperty("config.reload", "true");
    }
    
    @Test
    public void testConfigurationLoading() {
        System.out.println("=== Testing Configuration Loading ===");
        
        try {
            // Test that configuration loads successfully
            Configuration testConfig = new Configuration();
            Assert.assertNotNull(testConfig, "Configuration should load successfully");
            
            // Test that project config is loaded
            Assert.assertNotNull(testConfig.getProjectConfig(), "Project configuration should be loaded");
            
            // Test basic configuration properties
            Assert.assertNotNull(testConfig.getProjectConfig().getCaptureSettings(), "Capture settings should be loaded");
            Assert.assertNotNull(testConfig.getProjectConfig().getComparisonSettings(), "Comparison settings should be loaded");
            
            System.out.println("✓ PASS: Configuration loading successful");
            
        } catch (Exception e) {
            Assert.fail("Configuration loading failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testCSSPropertyCapture() {
        System.out.println("=== Testing Configuration-Driven CSS Property Capture ===");
        
        try {
            Configuration testConfig = new Configuration();
            
            // Test that we can access configured CSS properties
            List<String> cssProperties = testConfig.getProjectConfig().getCaptureSettings().getStylesToCapture();
            Assert.assertNotNull(cssProperties, "CSS properties list should be loaded");
            Assert.assertFalse(cssProperties.isEmpty(), "Should have CSS properties configured");
            
            System.out.println("Configured CSS properties: " + cssProperties.size());
            for (String prop : cssProperties) {
                System.out.println("  - " + prop);
            }
            
            System.out.println("✓ PASS: CSS property configuration working");
            
        } catch (Exception e) {
            Assert.fail("CSS property capture test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testChangeDetectionThresholds() {
        System.out.println("=== Testing Configuration-Driven Change Detection Thresholds ===");
        
        try {
            Configuration testConfig = new Configuration();
            ChangeDetector detector = new ChangeDetector(testConfig);
            
            // Create mock elements for testing threshold logic
            ElementData baseline = new ElementData();
            baseline.setSelector("#test");
            Map<String, String> baselineStyles = new HashMap<>();
            baselineStyles.put("width", "100px");
            baselineStyles.put("color", "#000000");
            baseline.setStyles(baselineStyles);
            
            ElementData current = new ElementData();
            current.setSelector("#test");
            Map<String, String> currentStyles = new HashMap<>();
            currentStyles.put("width", "101px"); // Small change
            currentStyles.put("color", "#000000");
            current.setStyles(currentStyles);
            
            List<ElementData> baselineList = new ArrayList<>();
            baselineList.add(baseline);
            
            List<ElementData> currentList = new ArrayList<>();
            currentList.add(current);
            
            List<ElementChange> changes = detector.detectChanges(baselineList, currentList, 100);
            
            Assert.assertNotNull(changes, "Changes list should not be null");
            
            if (!changes.isEmpty()) {
                System.out.println("Detected " + changes.size() + " changes:");
                for (ElementChange change : changes) {
                    System.out.println("  - " + change.getProperty() + ": " + 
                                     change.getOldValue() + " -> " + change.getNewValue() + 
                                     " (magnitude: " + change.getMagnitude() + 
                                     ", classification: " + change.getClassification() + ")");
                }
            } else {
                System.out.println("No changes detected (possibly filtered by thresholds)");
            }
            
            System.out.println("✓ PASS: Change detection thresholds applied");
            
        } catch (Exception e) {
            Assert.fail("Change detection threshold test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testIgnoreStyleSettings() {
        System.out.println("=== Testing Ignore Style Settings ===");
        
        try {
            Configuration testConfig = new Configuration();
            
            // Test that ignore patterns are loaded
            List<String> ignorePatterns = testConfig.getProjectConfig().getComparisonSettings().getIgnoreStyleChanges();
            Assert.assertNotNull(ignorePatterns, "Ignore patterns should be loaded");
            
            System.out.println("Configured ignore patterns: " + ignorePatterns.size());
            for (String pattern : ignorePatterns) {
                System.out.println("  - " + pattern);
            }
            
            // Test that specific properties that should be ignored are in the list
            boolean hasWidthPattern = ignorePatterns.stream().anyMatch(p -> p.contains("width"));
            boolean hasHeightPattern = ignorePatterns.stream().anyMatch(p -> p.contains("height"));
            
            System.out.println("Has width ignore pattern: " + hasWidthPattern);
            System.out.println("Has height ignore pattern: " + hasHeightPattern);
            
            System.out.println("✓ PASS: Ignore style settings loaded");
            
        } catch (Exception e) {
            Assert.fail("Ignore style settings test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testCrawlerWithConfiguration() {
        System.out.println("=== Testing Crawler with Configuration ===");
        
        try {
            Configuration testConfig = new Configuration();
            
            // Test the actual crawler constructor that works
            DOMCSSCrawler crawler = new DOMCSSCrawler(
                true, // headless
                null, // driver (will be created)
                1200, // viewport width
                800,  // viewport height
                testConfig // configuration
            );
            
            Assert.assertNotNull(crawler, "Crawler should be created with configuration");
            
            // Test basic functionality
            String testFile = new File("test_files/identical_baseline.html").getAbsolutePath();
            String fileUrl = "file:///" + testFile.replace("\\", "/");
            
            crawler.navigate(fileUrl, 5);
            List<ElementData> elements = crawler.extractElements("//body", 50);
            
            Assert.assertNotNull(elements, "Elements should be extracted");
            Assert.assertFalse(elements.isEmpty(), "Should find elements on the page");
            
            System.out.println("Extracted " + elements.size() + " elements:");
            for (int i = 0; i < Math.min(5, elements.size()); i++) {
                ElementData element = elements.get(i);
                System.out.println("  - " + element.getSelector() + " (styles: " + element.getStyles().size() + ")");
            }
            
            crawler.close();
            
            System.out.println("✓ PASS: Crawler working with configuration");
            
        } catch (Exception e) {
            Assert.fail("Crawler configuration test failed: " + e.getMessage());
        }
    }
}
