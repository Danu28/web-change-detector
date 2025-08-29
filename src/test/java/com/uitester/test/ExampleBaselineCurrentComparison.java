package com.uitester.test;

import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.core.StructuralAnalyzer;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.report.EnhancedReportGenerator;

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
 * Example demonstrating how to use the enhanced UI testing system
 * to compare baseline.html and current.html files.
 * 
 * This example shows all three phases in action:
 * - Phase 1: Configuration-driven behavior
 * - Phase 2: Enhanced change detection with fingerprinting
 * - Phase 3: Structural analysis and enhanced reporting
 */
public class ExampleBaselineCurrentComparison {
    
    private WebDriver driver;
    private DOMCSSCrawler crawler;
    private ChangeDetector detector;
    private Configuration configuration;
    private String testFilesPath;
    
    @BeforeMethod
    public void setUp() {
        // Setup headless Chrome browser
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        
        // Initialize the enhanced UI testing system
        configuration = new Configuration(); // Loads config.json automatically
        crawler = new DOMCSSCrawler(true, driver, 1920, 1080, configuration);
        detector = new ChangeDetector(configuration);
        
        testFilesPath = new File("test_files").getAbsolutePath();
        
        System.out.println("=== Enhanced UI Testing System Initialized ===");
        System.out.println("✅ Configuration loaded with " + 
                          configuration.getProjectConfig().getCaptureSettings().getStylesToCapture().size() + 
                          " CSS properties");
        System.out.println("✅ Chrome WebDriver ready");
        System.out.println("✅ All three phases available");
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    public void demonstrateBaselineCurrentComparison() throws Exception {
        System.out.println("\n🔍 COMPARING BASELINE.HTML vs CURRENT.HTML");
        System.out.println("=" .repeat(60));
        
        // STEP 1: Load elements from both pages using the enhanced crawler
        System.out.println("\n📄 STEP 1: Loading and analyzing pages...");
        
        List<ElementData> baselineElements = loadPageElements("baseline.html");
        List<ElementData> currentElements = loadPageElements("current.html");
        
        System.out.println("✅ Baseline page: " + baselineElements.size() + " elements extracted");
        System.out.println("✅ Current page: " + currentElements.size() + " elements extracted");
        
        // Show sample elements
        System.out.println("\n📋 Sample elements from baseline:");
        for (int i = 0; i < Math.min(3, baselineElements.size()); i++) {
            ElementData elem = baselineElements.get(i);
            System.out.println("  • " + elem.getSelector() + " (" + elem.getTagName() + ") - " + 
                             elem.getStyles().size() + " CSS properties");
        }
        
        // STEP 2: Run Phase 1 - Basic change detection
        System.out.println("\n🔧 STEP 2: Phase 1 - Configuration-driven basic detection...");
        
        long startTime = System.currentTimeMillis();
        List<ElementChange> basicChanges = detector.detectChanges(baselineElements, currentElements, null);
        long phase1Time = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Phase 1 complete: " + basicChanges.size() + " changes detected in " + phase1Time + "ms");
        
        // STEP 3: Run Phase 2 - Enhanced change detection with fingerprinting
        System.out.println("\n🔍 STEP 3: Phase 2 - Enhanced detection with fingerprinting...");
        
        startTime = System.currentTimeMillis();
        List<ElementChange> enhancedChanges = detector.detectChangesEnhanced(baselineElements, currentElements, null);
        long phase2Time = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Phase 2 complete: " + enhancedChanges.size() + " changes detected in " + phase2Time + "ms");
        
        // STEP 4: Run Phase 3 - Comprehensive analysis with structural insights
        System.out.println("\n📊 STEP 4: Phase 3 - Comprehensive analysis with structural insights...");
        
        startTime = System.currentTimeMillis();
        ChangeDetector.ComprehensiveAnalysisResult comprehensiveResult = 
            detector.detectChangesComprehensive(baselineElements, currentElements, null);
        long phase3Time = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Phase 3 complete: " + comprehensiveResult.getChanges().size() + 
                          " changes detected in " + phase3Time + "ms");
        
        // STEP 5: Analyze the results
        System.out.println("\n📈 STEP 5: Analyzing detected changes...");
        
        List<ElementChange> allChanges = comprehensiveResult.getChanges();
        
        // Count changes by type
        long criticalChanges = allChanges.stream().filter(c -> "critical".equals(c.getClassification())).count();
        long cosmeticChanges = allChanges.stream().filter(c -> "cosmetic".equals(c.getClassification())).count();
        long noiseChanges = allChanges.stream().filter(c -> "noise".equals(c.getClassification())).count();
        
        System.out.println("📊 Change Classification Summary:");
        System.out.println("  🔴 Critical changes: " + criticalChanges);
        System.out.println("  🟡 Cosmetic changes: " + cosmeticChanges);
        System.out.println("  🔵 Noise changes: " + noiseChanges);
        
        // Show detailed changes
        System.out.println("\n📝 Detailed Changes Found:");
        if (allChanges.isEmpty()) {
            System.out.println("  ℹ️  No significant changes detected between baseline and current");
        } else {
            for (ElementChange change : allChanges) {
                String icon = getChangeIcon(change.getClassification());
                System.out.println("  " + icon + " " + change.getClassification().toUpperCase() + 
                                 ": " + change.getProperty() + " in " + change.getElement());
                System.out.println("      Old: '" + change.getOldValue() + "'");
                System.out.println("      New: '" + change.getNewValue() + "'");
                System.out.println("      Magnitude: " + String.format("%.2f", change.getMagnitude()));
                System.out.println();
            }
        }
        
        // STEP 6: Show structural analysis
        System.out.println("\n🏗️ STEP 6: Structural Analysis Results...");
        
        StructuralAnalyzer.StructuralMetrics baselineMetrics = comprehensiveResult.getOldStructuralAnalysis().getMetrics();
        StructuralAnalyzer.StructuralMetrics currentMetrics = comprehensiveResult.getNewStructuralAnalysis().getMetrics();
        
        System.out.println("📊 Structural Comparison:");
        System.out.println("  Baseline - Nodes: " + baselineMetrics.getTotalNodes() + 
                          ", Max Depth: " + baselineMetrics.getMaxDepth());
        System.out.println("  Current  - Nodes: " + currentMetrics.getTotalNodes() + 
                          ", Max Depth: " + currentMetrics.getMaxDepth());
        
        if (baselineMetrics.getTotalNodes() != currentMetrics.getTotalNodes()) {
            System.out.println("  ⚠️  Structural difference detected: " + 
                             (currentMetrics.getTotalNodes() - baselineMetrics.getTotalNodes()) + " node difference");
        } else {
            System.out.println("  ✅ No structural differences in node count");
        }
        
        // STEP 7: Generate enhanced report
        System.out.println("\n📋 STEP 7: Generating enhanced HTML report...");
        
        EnhancedReportGenerator reportGenerator = new EnhancedReportGenerator();
        String outputPath = "target/baseline-vs-current-report.html";
        
        // Create performance metrics
        EnhancedReportGenerator.PerformanceMetrics metrics = new EnhancedReportGenerator.PerformanceMetrics();
        metrics.setProcessingTimeMs(comprehensiveResult.getProcessingTimeMs());
        metrics.setElementsAnalyzed(comprehensiveResult.getElementsAnalyzed());
        metrics.setChangesPerSecond(comprehensiveResult.getChangesPerSecond());
        metrics.setFingerprintsGenerated(baselineElements.size() + currentElements.size());
        metrics.setMemoryUsageMB(Runtime.getRuntime().totalMemory() / (1024 * 1024));
        
        reportGenerator.generateEnhancedReport(
            outputPath,
            allChanges,
            baselineElements,
            currentElements,
            comprehensiveResult.getOldStructuralAnalysis(),
            comprehensiveResult.getNewStructuralAnalysis(),
            metrics
        );
        
        File reportFile = new File(outputPath);
        System.out.println("✅ Enhanced report generated:");
        System.out.println("   📁 Location: " + reportFile.getAbsolutePath());
        System.out.println("   📊 Size: " + reportFile.length() + " bytes");
        System.out.println("   🌐 Open in browser to view interactive report");
        
        // STEP 8: Performance summary
        System.out.println("\n⚡ STEP 8: Performance Summary...");
        System.out.println("📊 Processing Performance:");
        System.out.println("  Phase 1 (Basic): " + phase1Time + "ms");
        System.out.println("  Phase 2 (Enhanced): " + phase2Time + "ms");
        System.out.println("  Phase 3 (Comprehensive): " + phase3Time + "ms");
        System.out.println("  Total Processing: " + comprehensiveResult.getProcessingTimeMs() + "ms");
        System.out.println("  Elements Analyzed: " + comprehensiveResult.getElementsAnalyzed());
        System.out.println("  Changes per Second: " + String.format("%.2f", comprehensiveResult.getChangesPerSecond()));
        
        System.out.println("\n🎯 COMPARISON COMPLETE!");
        System.out.println("=" .repeat(60));
        System.out.println("Your baseline.html and current.html have been thoroughly analyzed");
        System.out.println("using the enhanced UI testing system with all three phases.");
        System.out.println("Check the generated report for detailed interactive analysis!");
    }
    
    // Helper method to load page elements
    private List<ElementData> loadPageElements(String filename) {
        try {
            String filePath = "file:///" + Paths.get(testFilesPath, filename).toString().replace("\\", "/");
            System.out.println("  📄 Loading: " + filename);
            
            driver.get(filePath);
            Thread.sleep(1000); // Allow page to load
            
            List<ElementData> elements = crawler.extractElements("//body", null);
            System.out.println("  ✅ Extracted " + elements.size() + " elements with fingerprints");
            
            return elements;
        } catch (Exception e) {
            System.err.println("  ❌ Failed to load " + filename + ": " + e.getMessage());
            throw new RuntimeException("Failed to load page: " + filename, e);
        }
    }
    
    // Helper method to get appropriate icon for change type
    private String getChangeIcon(String classification) {
        switch (classification.toLowerCase()) {
            case "critical": return "🔴";
            case "cosmetic": return "🟡";
            case "noise": return "🔵";
            default: return "⚪";
        }
    }
    
    /**
     * Example of how to use the system programmatically for custom comparisons
     */
    @Test
    public void demonstrateCustomUsage() throws Exception {
        System.out.println("\n🛠️ CUSTOM USAGE EXAMPLE");
        System.out.println("=" .repeat(40));
        
        // Custom configuration example
        System.out.println("📋 You can customize the comparison by:");
        System.out.println("  1. Modifying config.json for different CSS properties");
        System.out.println("  2. Adjusting thresholds for change sensitivity");
        System.out.println("  3. Configuring which styles to ignore");
        System.out.println("  4. Setting up parallel processing options");
        
        // Load your files
        List<ElementData> baseline = loadPageElements("baseline.html");
        List<ElementData> current = loadPageElements("current.html");
        
        // Quick comparison using any phase
        System.out.println("\n🔍 Quick Phase 2 comparison:");
        List<ElementChange> quickChanges = detector.detectChangesEnhanced(baseline, current, null);
        
        System.out.println("📊 Quick Results: " + quickChanges.size() + " changes found");
        
        // You can also filter changes by type
        long importantChanges = quickChanges.stream()
            .filter(c -> "critical".equals(c.getClassification()) || "cosmetic".equals(c.getClassification()))
            .count();
        
        System.out.println("⚠️  Important changes (critical + cosmetic): " + importantChanges);
        
        System.out.println("\n✅ Custom usage example complete!");
    }
}
