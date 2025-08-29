package com.uitester.main;

import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.report.EnhancedReportGenerator;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Simple command-line utility to compare baseline.html and current.html
 * Usage: Run this main method to compare the files in test_files folder
 */
public class CompareBaselineCurrent {
    
    public static void main(String[] args) {
        System.out.println("🔍 UI Testing System - Baseline vs Current Comparison");
        System.out.println("=" .repeat(60));
        
        WebDriver driver = null;
        try {
            // Setup
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            
            driver = new ChromeDriver(options);
            Configuration config = new Configuration();
            DOMCSSCrawler crawler = new DOMCSSCrawler(true, driver, 1920, 1080, config);
            ChangeDetector detector = new ChangeDetector(config);
            
            System.out.println("✅ System initialized");
            
            // Load pages
            System.out.println("\n📄 Loading pages...");
            List<ElementData> baseline = loadPage(driver, crawler, "baseline.html");
            List<ElementData> current = loadPage(driver, crawler, "current.html");
            
            // Run comprehensive analysis
            System.out.println("\n🔍 Running comprehensive analysis...");
            ChangeDetector.ComprehensiveAnalysisResult result = 
                detector.detectChangesComprehensive(baseline, current, null);
            
            // Display results
            displayResults(result.getChanges());
            
            // Generate report
            System.out.println("\n📋 Generating report...");
            generateReport(result, baseline, current);
            
            System.out.println("\n🎯 Analysis complete! Check the generated report.");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    private static List<ElementData> loadPage(WebDriver driver, DOMCSSCrawler crawler, String filename) throws Exception {
        String filePath = "file:///" + Paths.get("test_files", filename).toAbsolutePath().toString().replace("\\", "/");
        System.out.println("  📄 Loading: " + filename);
        
        driver.get(filePath);
        Thread.sleep(1000);
        
        List<ElementData> elements = crawler.extractElements("//body", null);
        System.out.println("  ✅ " + elements.size() + " elements extracted");
        
        return elements;
    }
    
    private static void displayResults(List<ElementChange> changes) {
        System.out.println("\n📊 ANALYSIS RESULTS");
        System.out.println("-".repeat(40));
        
        if (changes.isEmpty()) {
            System.out.println("ℹ️  No significant changes detected");
            return;
        }
        
        // Count by classification
        long critical = changes.stream().filter(c -> "critical".equals(c.getClassification())).count();
        long cosmetic = changes.stream().filter(c -> "cosmetic".equals(c.getClassification())).count();
        long noise = changes.stream().filter(c -> "noise".equals(c.getClassification())).count();
        
        System.out.println("📈 Summary:");
        System.out.println("  🔴 Critical: " + critical);
        System.out.println("  🟡 Cosmetic: " + cosmetic);
        System.out.println("  🔵 Noise: " + noise);
        System.out.println("  📊 Total: " + changes.size());
        
        // Show important changes
        System.out.println("\n🔍 Important Changes:");
        changes.stream()
            .filter(c -> !"noise".equals(c.getClassification()))
            .limit(10) // Show first 10 important changes
            .forEach(change -> {
                String icon = "critical".equals(change.getClassification()) ? "🔴" : "🟡";
                System.out.println("  " + icon + " " + change.getProperty() + " in " + change.getElement());
                System.out.println("      " + change.getOldValue() + " → " + change.getNewValue());
            });
    }
    
    private static void generateReport(ChangeDetector.ComprehensiveAnalysisResult result, 
                                     List<ElementData> baseline, List<ElementData> current) throws Exception {
        EnhancedReportGenerator generator = new EnhancedReportGenerator();
        String outputPath = "baseline-vs-current-report.html";
        
        EnhancedReportGenerator.PerformanceMetrics metrics = new EnhancedReportGenerator.PerformanceMetrics();
        metrics.setProcessingTimeMs(result.getProcessingTimeMs());
        metrics.setElementsAnalyzed(result.getElementsAnalyzed());
        metrics.setChangesPerSecond(result.getChangesPerSecond());
        metrics.setFingerprintsGenerated(baseline.size() + current.size());
        metrics.setMemoryUsageMB(Runtime.getRuntime().totalMemory() / (1024 * 1024));
        
        generator.generateEnhancedReport(
            outputPath,
            result.getChanges(),
            baseline,
            current,
            result.getOldStructuralAnalysis(),
            result.getNewStructuralAnalysis(),
            metrics
        );
        
        File reportFile = new File(outputPath);
        System.out.println("✅ Report generated: " + reportFile.getAbsolutePath());
        System.out.println("📊 Report size: " + reportFile.length() + " bytes");
    }
}
