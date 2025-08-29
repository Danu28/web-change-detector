package com.uitester.test;

import com.uitester.core.ElementData;
import com.uitester.core.StructuralAnalyzer;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.report.EnhancedReportGenerator;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test suite for Phase 3 comprehensive change detection and enhanced reporting.
 * These tests verify advanced structural analysis, pattern recognition, and 
 * enhanced report generation capabilities.
 */
public class TestPhase3ComprehensiveAnalysis {
    
    private ChangeDetector detector;
    private StructuralAnalyzer analyzer;
    private EnhancedReportGenerator reportGenerator;
    
    @BeforeMethod
    public void setUp() {
        detector = new ChangeDetector();
        analyzer = new StructuralAnalyzer(null);
        reportGenerator = new EnhancedReportGenerator();
    }
    
    @Test
    public void testStructuralAnalysis() {
        // Create a complex DOM structure for testing
        List<ElementData> elements = createComplexDOMStructure();
        
        // Analyze structure
        StructuralAnalyzer.StructuralAnalysis analysis = analyzer.analyzeStructure(elements);
        
        // Verify analysis results
        assertNotNull(analysis, "Structural analysis should not be null");
        assertNotNull(analysis.getRootNode(), "Root node should be created");
        assertNotNull(analysis.getMetrics(), "Metrics should be calculated");
        
        // Check metrics
        StructuralAnalyzer.StructuralMetrics metrics = analysis.getMetrics();
        assertTrue(metrics.getTotalNodes() > 0, "Should count total nodes");
        assertTrue(metrics.getMaxDepth() > 0, "Should calculate max depth");
        assertFalse(metrics.getTagCounts().isEmpty(), "Should count tag types");
        
        System.out.println("Structural Analysis Results:");
        System.out.println("  Total nodes: " + metrics.getTotalNodes());
        System.out.println("  Max depth: " + metrics.getMaxDepth());
        System.out.println("  Average depth: " + String.format("%.2f", metrics.getAverageDepth()));
        System.out.println("  Leaf nodes: " + metrics.getLeafNodes());
        System.out.println("  Branching factor: " + String.format("%.2f", metrics.getBranchingFactor()));
        System.out.println("  Tag counts: " + metrics.getTagCounts());
    }
    
    @Test
    public void testPatternRecognition() {
        List<ElementData> elements = createPatternRichDOMStructure();
        
        StructuralAnalyzer.StructuralAnalysis analysis = analyzer.analyzeStructure(elements);
        
        // Should identify patterns
        assertFalse(analysis.getPatterns().isEmpty(), "Should identify structural patterns");
        
        System.out.println("Identified Patterns:");
        for (StructuralAnalyzer.StructuralPattern pattern : analysis.getPatterns()) {
            System.out.println("  " + pattern.getPatternType() + ": " + pattern.getDescription() + 
                              " (confidence: " + String.format("%.1f%%", pattern.getConfidence() * 100) + ")");
        }
        
        // Verify specific pattern types
        boolean hasNavPattern = analysis.getPatterns().stream()
            .anyMatch(p -> "navigation".equals(p.getPatternType()));
        boolean hasListPattern = analysis.getPatterns().stream()
            .anyMatch(p -> "list".equals(p.getPatternType()));
        
        assertTrue(hasNavPattern || hasListPattern, "Should identify at least navigation or list patterns");
    }
    
    @Test
    public void testComprehensiveChangeDetection() {
        List<ElementData> oldElements = createComplexDOMStructure();
        List<ElementData> newElements = createModifiedDOMStructure();
        
        // Run comprehensive analysis
        ChangeDetector.ComprehensiveAnalysisResult result = 
            detector.detectChangesComprehensive(oldElements, newElements, null);
        
        // Verify results
        assertNotNull(result, "Analysis result should not be null");
        assertNotNull(result.getChanges(), "Changes list should not be null");
        assertNotNull(result.getOldStructuralAnalysis(), "Old structural analysis should be present");
        assertNotNull(result.getNewStructuralAnalysis(), "New structural analysis should be present");
        assertTrue(result.getProcessingTimeMs() > 0, "Processing time should be recorded");
        assertTrue(result.getElementsAnalyzed() > 0, "Elements analyzed count should be positive");
        
        System.out.println("Comprehensive Analysis Results:");
        System.out.println("  Changes detected: " + result.getChanges().size());
        System.out.println("  Processing time: " + result.getProcessingTimeMs() + "ms");
        System.out.println("  Elements analyzed: " + result.getElementsAnalyzed());
        System.out.println("  Changes per second: " + String.format("%.2f", result.getChangesPerSecond()));
        
        // Check if structural context enhanced the changes
        boolean hasContextualChanges = result.getChanges().stream()
            .anyMatch(change -> !"noise".equals(change.getClassification()));
        
        assertTrue(hasContextualChanges, "Should have contextually classified changes");
    }
    
    @Test
    public void testEnhancedReportGeneration() throws Exception {
        List<ElementData> oldElements = createComplexDOMStructure();
        List<ElementData> newElements = createModifiedDOMStructure();
        
        // Run comprehensive analysis
        ChangeDetector.ComprehensiveAnalysisResult analysisResult = 
            detector.detectChangesComprehensive(oldElements, newElements, null);
        
        // Create performance metrics
        EnhancedReportGenerator.PerformanceMetrics performanceMetrics = 
            new EnhancedReportGenerator.PerformanceMetrics();
        performanceMetrics.setProcessingTimeMs(analysisResult.getProcessingTimeMs());
        performanceMetrics.setElementsAnalyzed(analysisResult.getElementsAnalyzed());
        performanceMetrics.setChangesPerSecond(analysisResult.getChangesPerSecond());
        performanceMetrics.setFingerprintsGenerated(oldElements.size() + newElements.size());
        performanceMetrics.setMemoryUsageMB(Runtime.getRuntime().totalMemory() / (1024 * 1024));
        
        // Generate enhanced report
        String outputPath = "target/test-enhanced-report.html";
        reportGenerator.generateEnhancedReport(
            outputPath,
            analysisResult.getChanges(),
            oldElements,
            newElements,
            analysisResult.getOldStructuralAnalysis(),
            analysisResult.getNewStructuralAnalysis(),
            performanceMetrics
        );
        
        // Verify report was generated
        File reportFile = new File(outputPath);
        assertTrue(reportFile.exists(), "Enhanced report file should be created");
        assertTrue(reportFile.length() > 0, "Enhanced report should have content");
        
        System.out.println("Enhanced report generated: " + reportFile.getAbsolutePath());
        System.out.println("Report size: " + reportFile.length() + " bytes");
    }
    
    @Test
    public void testStructuralChangeEnhancement() {
        List<ElementData> oldElements = createNavigationStructure();
        List<ElementData> newElements = createModifiedNavigationStructure();
        
        // Detect changes with structural context
        ChangeDetector.ComprehensiveAnalysisResult result = 
            detector.detectChangesComprehensive(oldElements, newElements, null);
        
        // Check if navigation changes are properly classified as critical
        boolean hasNavigationChanges = result.getChanges().stream()
            .anyMatch(change -> "critical".equals(change.getClassification()) &&
                               (change.getElement().contains("nav") || change.getElement().contains("menu")));
        
        if (hasNavigationChanges) {
            System.out.println("Navigation changes properly classified as critical");
        }
        
        // Verify structural analysis captured the navigation pattern
        boolean hasNavPattern = result.getOldStructuralAnalysis().getPatterns().stream()
            .anyMatch(pattern -> "navigation".equals(pattern.getPatternType()));
        
        if (hasNavPattern) {
            System.out.println("Navigation pattern detected in structural analysis");
        }
        
        // At least one of these should be true for a comprehensive test
        assertTrue(hasNavigationChanges || hasNavPattern, 
                  "Should detect navigation changes or patterns");
    }
    
    // Helper methods to create test data structures
    
    private List<ElementData> createComplexDOMStructure() {
        List<ElementData> elements = new ArrayList<>();
        
        // Header with navigation
        ElementData header = new ElementData();
        header.setSelector("header.main-header");
        header.setTagName("header");
        header.setAttributes(Map.of("class", "main-header"));
        elements.add(header);
        
        ElementData nav = new ElementData();
        nav.setSelector("header.main-header > nav");
        nav.setTagName("nav");
        nav.setAttributes(Map.of("role", "navigation"));
        elements.add(nav);
        
        // Main content
        ElementData main = new ElementData();
        main.setSelector("main.content");
        main.setTagName("main");
        main.setAttributes(Map.of("class", "content"));
        elements.add(main);
        
        // Article with paragraphs
        ElementData article = new ElementData();
        article.setSelector("main.content > article");
        article.setTagName("article");
        elements.add(article);
        
        for (int i = 1; i <= 3; i++) {
            ElementData paragraph = new ElementData();
            paragraph.setSelector("main.content > article > p:nth-child(" + i + ")");
            paragraph.setTagName("p");
            paragraph.setText("Paragraph " + i + " content");
            elements.add(paragraph);
        }
        
        // Sidebar
        ElementData sidebar = new ElementData();
        sidebar.setSelector("aside.sidebar");
        sidebar.setTagName("aside");
        sidebar.setAttributes(Map.of("class", "sidebar"));
        elements.add(sidebar);
        
        return elements;
    }
    
    private List<ElementData> createPatternRichDOMStructure() {
        List<ElementData> elements = new ArrayList<>();
        
        // Navigation pattern
        ElementData nav = new ElementData();
        nav.setSelector("nav.main-navigation");
        nav.setTagName("nav");
        nav.setAttributes(Map.of("class", "main-navigation", "role", "navigation"));
        elements.add(nav);
        
        // List pattern
        ElementData list = new ElementData();
        list.setSelector("ul.menu-list");
        list.setTagName("ul");
        list.setAttributes(Map.of("class", "menu-list"));
        elements.add(list);
        
        for (int i = 1; i <= 5; i++) {
            ElementData listItem = new ElementData();
            listItem.setSelector("ul.menu-list > li:nth-child(" + i + ")");
            listItem.setTagName("li");
            listItem.setText("Menu item " + i);
            elements.add(listItem);
        }
        
        // Form pattern
        ElementData form = new ElementData();
        form.setSelector("form.contact-form");
        form.setTagName("form");
        form.setAttributes(Map.of("class", "contact-form"));
        elements.add(form);
        
        ElementData input = new ElementData();
        input.setSelector("form.contact-form > input[type='text']");
        input.setTagName("input");
        input.setAttributes(Map.of("type", "text", "name", "name"));
        elements.add(input);
        
        ElementData button = new ElementData();
        button.setSelector("form.contact-form > button");
        button.setTagName("button");
        button.setText("Submit");
        elements.add(button);
        
        return elements;
    }
    
    private List<ElementData> createModifiedDOMStructure() {
        List<ElementData> elements = createComplexDOMStructure();
        
        // Modify some elements to create changes
        for (ElementData element : elements) {
            if ("p".equals(element.getTagName())) {
                // Change paragraph text
                element.setText(element.getText() + " - MODIFIED");
            }
            if (element.getSelector().contains("header")) {
                // Add style changes to header
                Map<String, String> styles = new HashMap<>();
                styles.put("background-color", "red");
                styles.put("color", "white");
                element.setStyles(styles);
            }
        }
        
        return elements;
    }
    
    private List<ElementData> createNavigationStructure() {
        List<ElementData> elements = new ArrayList<>();
        
        ElementData nav = new ElementData();
        nav.setSelector("nav.main-nav");
        nav.setTagName("nav");
        nav.setAttributes(Map.of("class", "main-nav", "role", "navigation"));
        Map<String, String> navStyles = new HashMap<>();
        navStyles.put("background-color", "blue");
        navStyles.put("padding", "10px");
        nav.setStyles(navStyles);
        elements.add(nav);
        
        return elements;
    }
    
    private List<ElementData> createModifiedNavigationStructure() {
        List<ElementData> elements = createNavigationStructure();
        
        // Modify navigation styling
        for (ElementData element : elements) {
            if ("nav".equals(element.getTagName())) {
                Map<String, String> styles = new HashMap<>(element.getStyles());
                styles.put("background-color", "green"); // Change color
                styles.put("padding", "15px"); // Change padding
                element.setStyles(styles);
            }
        }
        
        return elements;
    }
}
