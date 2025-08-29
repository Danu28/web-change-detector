package com.uitester.report;

import com.uitester.core.ElementData;
import com.uitester.core.StructuralAnalyzer;
import com.uitester.diff.ElementChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 3: Enhanced reporting system with advanced analytics and visualizations.
 * Generates comprehensive HTML reports with structural insights and interactive features.
 */
public class EnhancedReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedReportGenerator.class);
    
    private final TemplateEngine templateEngine;
    
    public EnhancedReportGenerator() {
        this.templateEngine = createTemplateEngine();
    }
    
    /**
     * Comprehensive report data structure
     */
    public static class EnhancedReportData {
        private String title;
        private LocalDateTime generatedAt;
        private ComparisonSummary summary;
        private List<CategorySection> categorySections;
        private StructuralInsights structuralInsights;
        private PerformanceMetrics performanceMetrics;
        private List<ElementChange> allChanges;
        private Map<String, Object> metadata;
        
        public EnhancedReportData() {
            this.generatedAt = LocalDateTime.now();
            this.categorySections = new ArrayList<>();
            this.allChanges = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public String getFormattedGeneratedAt() { 
            return generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        public ComparisonSummary getSummary() { return summary; }
        public void setSummary(ComparisonSummary summary) { this.summary = summary; }
        
        public List<CategorySection> getCategorySections() { return categorySections; }
        public StructuralInsights getStructuralInsights() { return structuralInsights; }
        public void setStructuralInsights(StructuralInsights structuralInsights) { this.structuralInsights = structuralInsights; }
        
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
        
        public List<ElementChange> getAllChanges() { return allChanges; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Summary statistics for the comparison
     */
    public static class ComparisonSummary {
        private int totalChanges;
        private int criticalChanges;
        private int cosmeticChanges;
        private int noiseChanges;
        private int structuralChanges;
        private double averageConfidence;
        private String overallRisk;
        private List<String> keyFindings;
        
        public ComparisonSummary() {
            this.keyFindings = new ArrayList<>();
        }
        
        // Getters and setters
        public int getTotalChanges() { return totalChanges; }
        public void setTotalChanges(int totalChanges) { this.totalChanges = totalChanges; }
        
        public int getCriticalChanges() { return criticalChanges; }
        public void setCriticalChanges(int criticalChanges) { this.criticalChanges = criticalChanges; }
        
        public int getCosmeticChanges() { return cosmeticChanges; }
        public void setCosmeticChanges(int cosmeticChanges) { this.cosmeticChanges = cosmeticChanges; }
        
        public int getNoiseChanges() { return noiseChanges; }
        public void setNoiseChanges(int noiseChanges) { this.noiseChanges = noiseChanges; }
        
        public int getStructuralChanges() { return structuralChanges; }
        public void setStructuralChanges(int structuralChanges) { this.structuralChanges = structuralChanges; }
        
        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }
        
        public String getOverallRisk() { return overallRisk; }
        public void setOverallRisk(String overallRisk) { this.overallRisk = overallRisk; }
        
        public List<String> getKeyFindings() { return keyFindings; }
        
        public String getCriticalPercentage() {
            return totalChanges > 0 ? String.format("%.1f", (criticalChanges * 100.0 / totalChanges)) : "0";
        }
        
        public String getCosmeticPercentage() {
            return totalChanges > 0 ? String.format("%.1f", (cosmeticChanges * 100.0 / totalChanges)) : "0";
        }
        
        public String getNoisePercentage() {
            return totalChanges > 0 ? String.format("%.1f", (noiseChanges * 100.0 / totalChanges)) : "0";
        }
    }
    
    /**
     * Categorized changes section
     */
    public static class CategorySection {
        private String categoryName;
        private String categoryDescription;
        private int changeCount;
        private String riskLevel;
        private List<ElementChange> changes;
        private Map<String, Integer> propertyBreakdown;
        
        public CategorySection(String categoryName, String categoryDescription) {
            this.categoryName = categoryName;
            this.categoryDescription = categoryDescription;
            this.changes = new ArrayList<>();
            this.propertyBreakdown = new HashMap<>();
        }
        
        // Getters and setters
        public String getCategoryName() { return categoryName; }
        public String getCategoryDescription() { return categoryDescription; }
        public int getChangeCount() { return changeCount; }
        public void setChangeCount(int changeCount) { this.changeCount = changeCount; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public List<ElementChange> getChanges() { return changes; }
        public Map<String, Integer> getPropertyBreakdown() { return propertyBreakdown; }
    }
    
    /**
     * Structural analysis insights
     */
    public static class StructuralInsights {
        private StructuralAnalyzer.StructuralMetrics oldMetrics;
        private StructuralAnalyzer.StructuralMetrics newMetrics;
        private List<StructuralAnalyzer.StructuralPattern> identifiedPatterns;
        private List<String> structuralObservations;
        private Map<String, Double> changeCoverage; // Pattern type -> percentage of changes
        
        public StructuralInsights() {
            this.identifiedPatterns = new ArrayList<>();
            this.structuralObservations = new ArrayList<>();
            this.changeCoverage = new HashMap<>();
        }
        
        // Getters and setters
        public StructuralAnalyzer.StructuralMetrics getOldMetrics() { return oldMetrics; }
        public void setOldMetrics(StructuralAnalyzer.StructuralMetrics oldMetrics) { this.oldMetrics = oldMetrics; }
        
        public StructuralAnalyzer.StructuralMetrics getNewMetrics() { return newMetrics; }
        public void setNewMetrics(StructuralAnalyzer.StructuralMetrics newMetrics) { this.newMetrics = newMetrics; }
        
        public List<StructuralAnalyzer.StructuralPattern> getIdentifiedPatterns() { return identifiedPatterns; }
        public List<String> getStructuralObservations() { return structuralObservations; }
        public Map<String, Double> getChangeCoverage() { return changeCoverage; }
        
        public boolean hasStructuralChanges() {
            return oldMetrics != null && newMetrics != null && 
                   (oldMetrics.getTotalNodes() != newMetrics.getTotalNodes() ||
                    oldMetrics.getMaxDepth() != newMetrics.getMaxDepth());
        }
    }
    
    /**
     * Performance and processing metrics
     */
    public static class PerformanceMetrics {
        private long processingTimeMs;
        private int elementsAnalyzed;
        private int fingerprintsGenerated;
        private long memoryUsageMB;
        private double changesPerSecond;
        private Map<String, Long> phaseTiming;
        
        public PerformanceMetrics() {
            this.phaseTiming = new HashMap<>();
        }
        
        // Getters and setters
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public int getElementsAnalyzed() { return elementsAnalyzed; }
        public void setElementsAnalyzed(int elementsAnalyzed) { this.elementsAnalyzed = elementsAnalyzed; }
        
        public int getFingerprintsGenerated() { return fingerprintsGenerated; }
        public void setFingerprintsGenerated(int fingerprintsGenerated) { this.fingerprintsGenerated = fingerprintsGenerated; }
        
        public long getMemoryUsageMB() { return memoryUsageMB; }
        public void setMemoryUsageMB(long memoryUsageMB) { this.memoryUsageMB = memoryUsageMB; }
        
        public double getChangesPerSecond() { return changesPerSecond; }
        public void setChangesPerSecond(double changesPerSecond) { this.changesPerSecond = changesPerSecond; }
        
        public Map<String, Long> getPhaseTiming() { return phaseTiming; }
        
        public String getFormattedProcessingTime() {
            if (processingTimeMs < 1000) {
                return processingTimeMs + " ms";
            } else {
                return String.format("%.2f s", processingTimeMs / 1000.0);
            }
        }
    }
    
    /**
     * Generate an enhanced HTML report
     */
    public void generateEnhancedReport(String outputPath, 
                                     List<ElementChange> changes,
                                     List<ElementData> oldElements,
                                     List<ElementData> newElements,
                                     StructuralAnalyzer.StructuralAnalysis oldStructure,
                                     StructuralAnalyzer.StructuralAnalysis newStructure,
                                     PerformanceMetrics performanceMetrics) throws IOException {
        
        logger.info("Generating enhanced report with {} changes", changes.size());
        
        // Build comprehensive report data
        EnhancedReportData reportData = buildEnhancedReportData(changes, oldElements, newElements, 
                                                                oldStructure, newStructure, performanceMetrics);
        
        // Generate HTML using Thymeleaf template
        Context context = new Context();
        context.setVariable("report", reportData);
        
        String htmlContent = templateEngine.process("enhanced_report_template", context);
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(htmlContent);
        }
        
        logger.info("Enhanced report generated: {}", outputPath);
    }
    
    /**
     * Build comprehensive report data from analysis results
     */
    private EnhancedReportData buildEnhancedReportData(List<ElementChange> changes,
                                                       List<ElementData> oldElements,
                                                       List<ElementData> newElements,
                                                       StructuralAnalyzer.StructuralAnalysis oldStructure,
                                                       StructuralAnalyzer.StructuralAnalysis newStructure,
                                                       PerformanceMetrics performanceMetrics) {
        
        EnhancedReportData reportData = new EnhancedReportData();
        reportData.setTitle("Enhanced UI Change Analysis Report");
        reportData.getAllChanges().addAll(changes);
        reportData.setPerformanceMetrics(performanceMetrics);
        
        // Build summary
        ComparisonSummary summary = buildComparisonSummary(changes);
        reportData.setSummary(summary);
        
        // Build category sections
        List<CategorySection> sections = buildCategorySections(changes);
        reportData.getCategorySections().addAll(sections);
        
        // Build structural insights
        StructuralInsights insights = buildStructuralInsights(oldStructure, newStructure, changes);
        reportData.setStructuralInsights(insights);
        
        // Add metadata
        reportData.getMetadata().put("oldElementCount", oldElements.size());
        reportData.getMetadata().put("newElementCount", newElements.size());
        reportData.getMetadata().put("analysisVersion", "Phase 3 Enhanced");
        
        return reportData;
    }
    
    /**
     * Build comparison summary statistics
     */
    private ComparisonSummary buildComparisonSummary(List<ElementChange> changes) {
        ComparisonSummary summary = new ComparisonSummary();
        
        summary.setTotalChanges(changes.size());
        
        // Count by classification
        int critical = 0, cosmetic = 0, noise = 0, structural = 0;
        double confidenceSum = 0;
        int confidenceCount = 0;
        
        for (ElementChange change : changes) {
            switch (change.getClassification()) {
                case "critical":
                    critical++;
                    break;
                case "cosmetic":
                    cosmetic++;
                    break;
                case "noise":
                    noise++;
                    break;
            }
            
            if ("structural".equals(change.getChangeType())) {
                structural++;
            }
            
            if (change.getMatchConfidence() != null) {
                confidenceSum += change.getMatchConfidence();
                confidenceCount++;
            }
        }
        
        summary.setCriticalChanges(critical);
        summary.setCosmeticChanges(cosmetic);
        summary.setNoiseChanges(noise);
        summary.setStructuralChanges(structural);
        summary.setAverageConfidence(confidenceCount > 0 ? confidenceSum / confidenceCount : 1.0);
        
        // Determine overall risk
        if (critical > 10 || structural > 5) {
            summary.setOverallRisk("HIGH");
        } else if (critical > 3 || cosmetic > 20) {
            summary.setOverallRisk("MEDIUM");
        } else {
            summary.setOverallRisk("LOW");
        }
        
        // Generate key findings
        if (critical > 0) {
            summary.getKeyFindings().add(critical + " critical changes detected requiring immediate attention");
        }
        if (structural > 0) {
            summary.getKeyFindings().add(structural + " structural changes may affect layout and functionality");
        }
        if (summary.getAverageConfidence() < 0.8) {
            summary.getKeyFindings().add("Low average match confidence (" + 
                String.format("%.1f%%", summary.getAverageConfidence() * 100) + ") indicates significant DOM changes");
        }
        if (summary.getKeyFindings().isEmpty()) {
            summary.getKeyFindings().add("No critical issues detected - changes appear to be cosmetic");
        }
        
        return summary;
    }
    
    /**
     * Build categorized sections for the report
     */
    private List<CategorySection> buildCategorySections(List<ElementChange> changes) {
        Map<String, CategorySection> sectionMap = new HashMap<>();
        
        // Create sections for each classification
        sectionMap.put("critical", new CategorySection("Critical Changes", 
            "Changes that significantly impact functionality or user experience"));
        sectionMap.put("cosmetic", new CategorySection("Cosmetic Changes", 
            "Visual changes that don't affect functionality"));
        sectionMap.put("noise", new CategorySection("Minor Changes", 
            "Small changes that are likely insignificant"));
        
        // Add structural changes section
        CategorySection structuralSection = new CategorySection("Structural Changes", 
            "Changes to the DOM structure (additions, removals)");
        sectionMap.put("structural", structuralSection);
        
        // Populate sections
        for (ElementChange change : changes) {
            String key = "structural".equals(change.getChangeType()) ? "structural" : change.getClassification();
            CategorySection section = sectionMap.get(key);
            if (section != null) {
                section.getChanges().add(change);
                section.getPropertyBreakdown().merge(change.getProperty(), 1, Integer::sum);
            }
        }
        
        // Set counts and risk levels
        for (CategorySection section : sectionMap.values()) {
            section.setChangeCount(section.getChanges().size());
            
            // Determine risk level based on count and type
            if ("critical".equals(section.getCategoryName().toLowerCase()) || 
                "structural".equals(section.getCategoryName().toLowerCase())) {
                if (section.getChangeCount() > 5) {
                    section.setRiskLevel("HIGH");
                } else if (section.getChangeCount() > 0) {
                    section.setRiskLevel("MEDIUM");
                } else {
                    section.setRiskLevel("LOW");
                }
            } else {
                section.setRiskLevel(section.getChangeCount() > 10 ? "MEDIUM" : "LOW");
            }
        }
        
        // Return non-empty sections
        return sectionMap.values().stream()
            .filter(section -> section.getChangeCount() > 0)
            .sorted((a, b) -> Integer.compare(b.getChangeCount(), a.getChangeCount()))
            .collect(Collectors.toList());
    }
    
    /**
     * Build structural insights from analysis results
     */
    private StructuralInsights buildStructuralInsights(StructuralAnalyzer.StructuralAnalysis oldStructure,
                                                       StructuralAnalyzer.StructuralAnalysis newStructure,
                                                       List<ElementChange> changes) {
        StructuralInsights insights = new StructuralInsights();
        
        if (oldStructure != null) {
            insights.setOldMetrics(oldStructure.getMetrics());
            insights.getIdentifiedPatterns().addAll(oldStructure.getPatterns());
        }
        
        if (newStructure != null) {
            insights.setNewMetrics(newStructure.getMetrics());
        }
        
        // Generate observations
        if (insights.hasStructuralChanges()) {
            int oldNodes = oldStructure != null ? oldStructure.getMetrics().getTotalNodes() : 0;
            int newNodes = newStructure != null ? newStructure.getMetrics().getTotalNodes() : 0;
            
            if (newNodes > oldNodes) {
                insights.getStructuralObservations().add(
                    (newNodes - oldNodes) + " new elements added to the DOM");
            } else if (newNodes < oldNodes) {
                insights.getStructuralObservations().add(
                    (oldNodes - newNodes) + " elements removed from the DOM");
            }
            
            if (oldStructure != null && newStructure != null) {
                int oldDepth = oldStructure.getMetrics().getMaxDepth();
                int newDepth = newStructure.getMetrics().getMaxDepth();
                if (newDepth != oldDepth) {
                    insights.getStructuralObservations().add(
                        "DOM depth changed from " + oldDepth + " to " + newDepth + " levels");
                }
            }
        }
        
        // Analyze change coverage by pattern
        Map<String, Integer> patternChanges = new HashMap<>();
        int totalChanges = changes.size();
        
        for (ElementChange change : changes) {
            // This is simplified - in a real implementation, you'd match changes to patterns
            if (change.getElement().contains("nav") || change.getElement().contains("menu")) {
                patternChanges.merge("navigation", 1, Integer::sum);
            } else if (change.getElement().contains("form") || change.getElement().contains("input")) {
                patternChanges.merge("form", 1, Integer::sum);
            } else if (change.getElement().contains("list") || change.getElement().contains("ul") || 
                      change.getElement().contains("ol")) {
                patternChanges.merge("list", 1, Integer::sum);
            } else {
                patternChanges.merge("content", 1, Integer::sum);
            }
        }
        
        for (Map.Entry<String, Integer> entry : patternChanges.entrySet()) {
            double percentage = totalChanges > 0 ? (entry.getValue() * 100.0 / totalChanges) : 0;
            insights.getChangeCoverage().put(entry.getKey(), percentage);
        }
        
        return insights;
    }
    
    /**
     * Create and configure Thymeleaf template engine
     */
    private TemplateEngine createTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
