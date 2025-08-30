package com.uitester.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
import com.uitester.core.StructuralAnalyzer;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ElementChange;
import com.uitester.diff.ElementMatcher;
import com.uitester.report.SimpleReportGenerator;

/**
 * Enhanced UI Tester Main Application
 * 
 * This class integrates all enhanced components:
 * - Enhanced element capture with fingerprinting
 * - Advanced element matching (Phase 2)
 * - Structural analysis (Phase 3)
 * - Simple, clean human-readable reporting
 */
public class EnhancedUITesterMain {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedUITesterMain.class);
    
    private Configuration config;
    private ProjectConfig projectConfig;
    private ObjectMapper objectMapper;
    
    public EnhancedUITesterMain(Configuration config, ProjectConfig projectConfig) {
        this.config = config;
        this.projectConfig = projectConfig;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Main entry point for Enhanced UI Tester
     */
    public static void main(String[] args) {
        logger.info("=".repeat(70));
        logger.info("🚀 ENHANCED UI TESTER - Advanced Change Detection System");
        logger.info("=".repeat(70));
        
        try {
            // Parse command line arguments
            CommandLine cmd = parseCommandLine(args);
            
            // Create enhanced configurations
            Configuration config = createConfiguration(cmd);
            ProjectConfig projectConfig = createProjectConfig(cmd);
            
            // Create and run enhanced application
            EnhancedUITesterMain app = new EnhancedUITesterMain(config, projectConfig);
            app.runEnhancedAnalysis();
            
            logger.info("✅ Enhanced UI Testing completed successfully!");
            logger.info("=".repeat(70));
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("❌ Error running Enhanced UI Tester: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Run the complete enhanced analysis workflow
     */
    public void runEnhancedAnalysis() throws Exception {
        logger.info("🔍 Starting Enhanced UI Analysis Workflow");
        
        // Phase 1: Enhanced Element Capture
        logger.info("📊 Phase 1: Enhanced Element Capture with Fingerprinting");
        List<ElementData> baselineElements = captureEnhancedElements(config.getBaselineUrl(), "baseline");
        List<ElementData> currentElements = captureEnhancedElements(config.getCurrentUrl(), "current");
        
        // Save captured elements
        saveElementsToFile(baselineElements, config.getBaselineSnapshot());
        saveElementsToFile(currentElements, config.getCurrentSnapshot());
        
        // Phase 2: Advanced Element Matching
        logger.info("🎯 Phase 2: Advanced Element Matching with Fingerprints");
        ElementMatcher elementMatcher = new ElementMatcher(projectConfig);
        ElementMatcher.MatchResult matchResult = elementMatcher.matchElements(baselineElements, currentElements);
        
        logger.info("📈 Matching Results: {} matched, {} added, {} removed", 
                   matchResult.getMatchedPairs().size(),
                   matchResult.getAddedElements().size(), 
                   matchResult.getRemovedElements().size());
        
        // Convert match results to enhanced changes
        List<ElementChange> changes = convertMatchesToEnhancedChanges(matchResult);
        
        // Phase 3: Structural Analysis
        logger.info("🏗️ Phase 3: Structural Analysis and Context Enhancement");
        StructuralAnalyzer structuralAnalyzer = new StructuralAnalyzer(projectConfig);
        StructuralAnalyzer.StructuralAnalysis baselineStructure = structuralAnalyzer.analyzeStructure(baselineElements);
        StructuralAnalyzer.StructuralAnalysis currentStructure = structuralAnalyzer.analyzeStructure(currentElements);
        
        // Enhance changes with structural context
        List<ElementChange> structurallyEnhancedChanges = structuralAnalyzer.analyzeStructuralChanges(
            changes, baselineStructure, currentStructure);
        
        // Save enhanced changes
        saveChangesToFile(structurallyEnhancedChanges, config.getChangesFile());
        
        // Phase 4: Simple Report Generation
        logger.info("📝 Phase 4: Simple Report Generation");
        generateSimpleReport(structurallyEnhancedChanges, baselineElements, currentElements, 
                           baselineStructure, currentStructure);
        
        // Performance Summary
        printPerformanceSummary(baselineElements, currentElements, structurallyEnhancedChanges);
    }
    
    /**
     * Capture elements with enhanced fingerprinting
     */
    private List<ElementData> captureEnhancedElements(String url, String label) throws Exception {
        logger.info("🌐 Capturing enhanced elements from {} page: {}", label, url);
        
        List<ElementData> elements;
        
        // Create enhanced crawler with fingerprinting enabled
        DOMCSSCrawler crawler = new DOMCSSCrawler(
            config.isHeadless(),
            null, // Enhanced crawler auto-configures
            config.getViewportWidth(),
            config.getViewportHeight(),
            config);
        
        try {
            // Navigate and extract elements with fingerprinting
            crawler.navigate(url, config.getWaitTime());
            elements = crawler.extractElements(config.getContainerXpath(), config.getMaxElements());
            
            // Generate fingerprints for each element
            for (ElementData element : elements) {
                element.generateFingerprint(projectConfig); // This will create enhanced fingerprints
            }
            
            logger.info("✅ Captured {} enhanced elements with fingerprints", elements.size());
            
        } finally {
            crawler.close();
        }
        
        return elements;
    }
    
    /**
     * Convert match results to enhanced changes with better analysis
     */
    private List<ElementChange> convertMatchesToEnhancedChanges(ElementMatcher.MatchResult matchResult) {
        List<ElementChange> changes = new ArrayList<>();
        
        logger.info("🔄 Converting match results to enhanced changes");
        
        // Process matched pairs for modifications
        for (var pair : matchResult.getMatchedPairs().entrySet()) {
            ElementData baseline = pair.getKey();
            ElementData current = pair.getValue();
            Double confidence = matchResult.getMatchConfidences().getOrDefault(baseline, 1.0);
            
            // Analyze style changes
            if (!baseline.getStyles().equals(current.getStyles())) {
                ElementChange change = new ElementChange();
                change.setElement(baseline.getSelector());
                change.setProperty("styles");
                change.setChangeType("CSS_MODIFICATION");
                change.setOldValue(baseline.getStyles().toString());
                change.setNewValue(current.getStyles().toString());
                change.setMatchConfidence(confidence);
                
                // Calculate magnitude based on style differences
                double magnitude = calculateStyleChangeMagnitude(baseline.getStyles(), current.getStyles());
                change.setMagnitude(magnitude);
                
                changes.add(change);
            }
            
            // Analyze text changes with improved logic
            String baselineText = baseline.getText() != null ? baseline.getText() : "";
            String currentText = current.getText() != null ? current.getText() : "";
            
            logger.info("🔍 Checking text for {}: baseline='{}', current='{}'", 
                       baseline.getSelector(), baselineText, currentText);
            
            if (!baselineText.equals(currentText)) {
                // Log the text comparison for debugging
                logger.info("📝 Text change detected for {}: '{}' -> '{}'", 
                           baseline.getSelector(), baselineText, currentText);
                
                ElementChange change = new ElementChange();
                change.setElement(baseline.getSelector());
                change.setProperty("text");
                change.setChangeType("TEXT_MODIFICATION");
                change.setOldValue(baselineText);
                change.setNewValue(currentText);
                change.setMatchConfidence(confidence);
                
                // Calculate magnitude based on text difference
                double magnitude = calculateTextChangeMagnitude(baselineText, currentText);
                change.setMagnitude(magnitude);
                
                changes.add(change);
                logger.info("✅ Added TEXT_MODIFICATION: {} ('{}' -> '{}')", 
                          baseline.getSelector(), baselineText, currentText);
            } else {
                logger.info("✅ No text change for {}", baseline.getSelector());
            }
            
            // Analyze attribute changes
            if (!baseline.getAttributes().equals(current.getAttributes())) {
                ElementChange change = new ElementChange();
                change.setElement(baseline.getSelector());
                change.setProperty("attributes");
                change.setChangeType("ATTRIBUTE_MODIFICATION");
                change.setOldValue(baseline.getAttributes().toString());
                change.setNewValue(current.getAttributes().toString());
                change.setMatchConfidence(confidence);
                change.setMagnitude(0.5); // Attribute changes are moderate
                changes.add(change);
            }
        }
        
        // Process added elements
        for (ElementData added : matchResult.getAddedElements()) {
            ElementChange change = new ElementChange();
            change.setElement(added.getSelector());
            change.setProperty("element");
            change.setChangeType("ELEMENT_ADDED");
            change.setNewValue(added.getTagName() + " element added");
            change.setMatchConfidence(1.0);
            change.setMagnitude(0.9); // Structural changes are significant
            changes.add(change);
        }
        
        // Process removed elements
        for (ElementData removed : matchResult.getRemovedElements()) {
            ElementChange change = new ElementChange();
            change.setElement(removed.getSelector());
            change.setProperty("element");
            change.setChangeType("ELEMENT_REMOVED");
            change.setOldValue(removed.getTagName() + " element removed");
            change.setMatchConfidence(1.0);
            change.setMagnitude(0.9); // Structural changes are significant
            changes.add(change);
        }
        
        logger.info("✅ Generated {} enhanced changes", changes.size());
        return changes;
    }
    
    /**
     * Calculate the magnitude of a text change
     */
    private double calculateTextChangeMagnitude(String oldText, String newText) {
        if (oldText == null && newText == null) return 0.0;
        if (oldText == null || newText == null) return 1.0; // Complete change
        if (oldText.equals(newText)) return 0.0;
        
        // Use Levenshtein distance to calculate similarity
        int maxLength = Math.max(oldText.length(), newText.length());
        if (maxLength == 0) return 0.0;
        
        // Simple character-based difference calculation
        double similarity = 1.0 - (double) levenshteinDistance(oldText, newText) / maxLength;
        return Math.max(0.0, Math.min(1.0, 1.0 - similarity));
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return s2.length();
        if (s2 == null) return s1.length();
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calculate magnitude of style changes
     */
    private double calculateStyleChangeMagnitude(java.util.Map<String, String> oldStyles, 
                                                java.util.Map<String, String> newStyles) {
        int totalProperties = Math.max(oldStyles.size(), newStyles.size());
        int changedProperties = 0;
        
        // Count changed properties
        for (String key : oldStyles.keySet()) {
            if (!java.util.Objects.equals(oldStyles.get(key), newStyles.get(key))) {
                changedProperties++;
            }
        }
        
        // Count new properties
        for (String key : newStyles.keySet()) {
            if (!oldStyles.containsKey(key)) {
                changedProperties++;
            }
        }
        
        return totalProperties > 0 ? (double) changedProperties / totalProperties : 0.0;
    }
    
    /**
     * Generate simple human-readable reports only
     */
    private void generateSimpleReport(List<ElementChange> changes, 
                                    List<ElementData> baselineElements,
                                    List<ElementData> currentElements,
                                    StructuralAnalyzer.StructuralAnalysis baselineStructure,
                                    StructuralAnalyzer.StructuralAnalysis currentStructure) throws IOException {
        
        // Generate Simple Human-Readable Report
        logger.info("📄 Generating Simple Human-Readable Report");
        String simpleReportPath = config.getReportFile().replace(".html", "-simple.html");
        SimpleReportGenerator simpleGenerator = new SimpleReportGenerator();
        simpleGenerator.generateReport(changes, baselineElements, currentElements, simpleReportPath);
        
        logger.info("✅ Report generated:");
        logger.info("   📄 Simple Report: {}", simpleReportPath);
    }
    
    /**
     * Print performance summary
     */
    private void printPerformanceSummary(List<ElementData> baselineElements, 
                                       List<ElementData> currentElements,
                                       List<ElementChange> changes) {
        logger.info("📊 PERFORMANCE SUMMARY");
        logger.info("   Elements Analyzed: {} baseline + {} current = {}", 
                   baselineElements.size(), currentElements.size(), 
                   baselineElements.size() + currentElements.size());
        logger.info("   Changes Detected: {}", changes.size());
        
        // Count change types
        long cssChanges = changes.stream().filter(c -> "CSS_MODIFICATION".equals(c.getChangeType())).count();
        long textChanges = changes.stream().filter(c -> "TEXT_MODIFICATION".equals(c.getChangeType())).count();
        long structuralChanges = changes.stream().filter(c -> c.getChangeType().contains("ELEMENT_")).count();
        
        logger.info("   CSS Changes: {}", cssChanges);
        logger.info("   Text Changes: {}", textChanges);
        logger.info("   Structural Changes: {}", structuralChanges);
    }
    
    /**
     * Save elements to JSON file
     */
    private void saveElementsToFile(List<ElementData> elements, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, elements);
        }
        
        logger.info("💾 Saved {} elements to {}", elements.size(), filePath);
    }
    
    /**
     * Save changes to JSON file
     */
    private void saveChangesToFile(List<ElementChange> changes, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, changes);
        }
        
        logger.info("💾 Saved {} changes to {}", changes.size(), filePath);
    }
    
    /**
     * Parse command line arguments with enhanced options
     */
    private static CommandLine parseCommandLine(String[] args) throws ParseException {
        Options options = new Options();
        
        // Basic comparison options
        options.addOption(Option.builder("b").longOpt("baseline").hasArg()
                .desc("Baseline URL to compare against").build());
        options.addOption(Option.builder("c").longOpt("current").hasArg()
                .desc("Current URL to compare to baseline").build());
        
        // Enhanced analysis options
        options.addOption(Option.builder().longOpt("enable-fingerprinting")
                .desc("Enable enhanced fingerprinting (default: true)").build());
        options.addOption(Option.builder().longOpt("enable-structural-analysis")
                .desc("Enable structural analysis (default: true)").build());
        options.addOption(Option.builder().longOpt("confidence-threshold").hasArg()
                .desc("Minimum confidence threshold for matches (0.0-1.0, default: 0.7)").build());
        
        // Crawler settings
        options.addOption(Option.builder("m").longOpt("max-elements").hasArg().type(Number.class)
                .desc("Maximum number of elements to extract").build());
        options.addOption(Option.builder("w").longOpt("wait-time").hasArg().type(Number.class)
                .desc("Time in seconds to wait for page load").build());
        options.addOption(Option.builder("h").longOpt("headless")
                .desc("Run in headless mode").build());
        options.addOption(Option.builder().longOpt("xpath").hasArg()
                .desc("XPath selector for container element").build());
        
        // Section info
        options.addOption(Option.builder("s").longOpt("section-name").hasArg()
                .desc("Section name for output").build());
        
        // Help
        options.addOption(Option.builder("?").longOpt("help")
                .desc("Show help").build());
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar enhanced-ui-tester.jar", options);
            System.exit(0);
        }
        
        return cmd;
    }
    
    /**
     * Create enhanced configuration
     */
    private static Configuration createConfiguration(CommandLine cmd) {
        Configuration config = new Configuration();
        
        // URLs
        if (cmd.hasOption("baseline")) {
            config.setBaselineUrl(cmd.getOptionValue("baseline"));
        }
        if (cmd.hasOption("current")) {
            config.setCurrentUrl(cmd.getOptionValue("current"));
        }
        
        // Crawler settings
        if (cmd.hasOption("max-elements")) {
            config.setMaxElements(Integer.parseInt(cmd.getOptionValue("max-elements")));
        }
        if (cmd.hasOption("wait-time")) {
            config.setWaitTime(Integer.parseInt(cmd.getOptionValue("wait-time")));
        }
        if (cmd.hasOption("headless")) {
            config.setHeadless(true);
        }
        if (cmd.hasOption("xpath")) {
            config.setContainerXpath(cmd.getOptionValue("xpath"));
        }
        
        // Section info
        if (cmd.hasOption("section-name")) {
            config.setSectionName(cmd.getOptionValue("section-name"));
        }
        
        // Update output paths
        config.updateOutputPaths();
        
        return config;
    }
    
    /**
     * Create enhanced project configuration
     */
    private static ProjectConfig createProjectConfig(CommandLine cmd) {
        ProjectConfig projectConfig = new ProjectConfig();
        
        // Set enhanced defaults
        ProjectConfig.FingerprintSettings fingerprintSettings = new ProjectConfig.FingerprintSettings();
        fingerprintSettings.setUseTagName(true);
        fingerprintSettings.setUseTextContent(true);
        fingerprintSettings.setUseAttributes(java.util.Arrays.asList("id", "class", "data-*"));
        fingerprintSettings.setIncludePosition(true);
        projectConfig.setFingerprintSettings(fingerprintSettings);
        
        ProjectConfig.ComparisonSettings comparisonSettings = new ProjectConfig.ComparisonSettings();
        comparisonSettings.setTextSimilarityThreshold(cmd.hasOption("confidence-threshold") ? 
            Double.parseDouble(cmd.getOptionValue("confidence-threshold")) : 0.7);
        comparisonSettings.setColorChangeThreshold(0.1);
        projectConfig.setComparisonSettings(comparisonSettings);
        
        return projectConfig;
    }
}
