package com.uitester.test;

import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Debug test to understand what's happening with the HTML file comparisons
 */
public class TestDebugIntegration {
    
    private WebDriver driver;
    private DOMCSSCrawler crawler;
    private ChangeDetector detector;
    private Configuration configuration;
    private String testFilesPath;
    
    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        configuration = new Configuration();
        crawler = new DOMCSSCrawler(true, driver, 1920, 1080, configuration);
        detector = new ChangeDetector(configuration);
        testFilesPath = new File("test_files").getAbsolutePath();
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    public void debugHTMLComparison() {
        System.out.println("\n=== Debug HTML Comparison ===");
        
        // Load elements from different files
        List<ElementData> baselineElements = loadPageElements("baseline_comprehensive.html");
        List<ElementData> identicalElements = loadPageElements("identical_comprehensive.html");
        List<ElementData> noiseElements = loadPageElements("noise_changes.html");
        List<ElementData> criticalElements = loadPageElements("critical_changes.html");
        List<ElementData> structuralElements = loadPageElements("structural_changes.html");
        
        System.out.println("\nElement counts:");
        System.out.println("Baseline: " + baselineElements.size());
        System.out.println("Identical: " + identicalElements.size());
        System.out.println("Noise: " + noiseElements.size());
        System.out.println("Critical: " + criticalElements.size());
        System.out.println("Structural: " + structuralElements.size());
        
        // Show sample elements from baseline
        System.out.println("\nSample baseline elements:");
        for (int i = 0; i < Math.min(5, baselineElements.size()); i++) {
            ElementData elem = baselineElements.get(i);
            System.out.println("  " + elem.getSelector() + " - " + elem.getTagName());
            System.out.println("    CSS props: " + elem.getStyles().size());
            if (!elem.getStyles().isEmpty()) {
                elem.getStyles().entrySet().stream().limit(3)
                    .forEach(entry -> System.out.println("      " + entry.getKey() + ": " + entry.getValue()));
            }
        }
        
        // Compare baseline vs critical
        System.out.println("\nComparing baseline vs critical:");
        List<ElementChange> changes = detector.detectChangesComprehensive(baselineElements, criticalElements, null).getChanges();
        System.out.println("Changes detected: " + changes.size());
        
        for (ElementChange change : changes) {
            System.out.println("  " + change.getProperty() + " in " + change.getElement() + 
                             ": '" + change.getOldValue() + "' -> '" + change.getNewValue() + "'");
        }
        
        // Compare specific elements if they exist
        if (!baselineElements.isEmpty() && !criticalElements.isEmpty()) {
            System.out.println("\nDetailed comparison of first elements:");
            ElementData baseline = baselineElements.get(0);
            ElementData critical = criticalElements.get(0);
            
            System.out.println("Baseline: " + baseline.getSelector() + " - " + baseline.getTagName());
            System.out.println("Critical: " + critical.getSelector() + " - " + critical.getTagName());
            
            System.out.println("Baseline CSS properties: " + baseline.getStyles().size());
            baseline.getStyles().entrySet().stream().limit(10)
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
            
            System.out.println("Critical CSS properties: " + critical.getStyles().size());
            critical.getStyles().entrySet().stream().limit(10)
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        }
    }
    
    private List<ElementData> loadPageElements(String filename) {
        try {
            String filePath = "file:///" + Paths.get(testFilesPath, filename).toString().replace("\\", "/");
            System.out.println("Loading: " + filePath);
            
            driver.get(filePath);
            Thread.sleep(1000);
            
            List<ElementData> elements = crawler.extractElements("//body", null);
            return elements;
        } catch (Exception e) {
            System.err.println("Failed to load " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}
