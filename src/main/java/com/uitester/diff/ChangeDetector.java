package com.uitester.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.core.StructuralAnalyzer;
import com.uitester.core.ProjectConfig;
import com.uitester.core.Defaults;

/**
 * Detects changes between two sets of elements.
 * Enhanced with Phase 2 fingerprint-based matching for more resilient change detection.
 */
public class ChangeDetector {
    private static final Logger logger = LoggerFactory.getLogger(ChangeDetector.class);
    
    private List<ElementChange> changes;
    private Configuration configuration;
    private ElementMatcher elementMatcher;
    private StructuralAnalyzer structuralAnalyzer; // Phase 3: Advanced structural analysis
    
    public ChangeDetector() {
        this.changes = new ArrayList<>();
        this.configuration = null; // Will use default behavior
        // Initialize ElementMatcher with default settings for Phase 2 features
        this.elementMatcher = new ElementMatcher(null);
        // Initialize StructuralAnalyzer for Phase 3 features
        this.structuralAnalyzer = new StructuralAnalyzer(null);
    }
    
    public ChangeDetector(Configuration configuration) {
        this.changes = new ArrayList<>();
        this.configuration = configuration;
        this.elementMatcher = configuration != null && configuration.getProjectConfig() != null ? 
            new ElementMatcher(configuration.getProjectConfig()) : new ElementMatcher(null);
        this.structuralAnalyzer = configuration != null && configuration.getProjectConfig() != null ?
            new StructuralAnalyzer(configuration.getProjectConfig()) : new StructuralAnalyzer(null);
    }
    
    /**
     * Calculate similarity between two text strings
     * 
     * @param oldText First text string
     * @param newText Second text string
     * @return Similarity ratio between 0.0 and 1.0
     */
    public double calculateTextSimilarity(String oldText, String newText) {
        if ((oldText == null || oldText.isEmpty()) && (newText == null || newText.isEmpty())) {
            return 1.0;
        }
        if (oldText == null || oldText.isEmpty() || newText == null || newText.isEmpty()) {
            return 0.0;
        }
        
        // Use Levenshtein distance to calculate similarity
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        int distance = levenshtein.apply(oldText, newText);
        
        // Convert distance to similarity ratio
        int maxLength = Math.max(oldText.length(), newText.length());
        if (maxLength == 0) return 1.0;
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculate percentage change for numeric values
     * 
     * @param oldVal Old value as string (may include units like "14px", "1.2em")
     * @param newVal New value as string
     * @return Magnitude of change as a ratio
     */
    public double calculateNumericChange(String oldVal, String newVal) {
        try {
            // Extract numeric values from CSS values like "14px", "1.2em"
            Pattern pattern = Pattern.compile("([0-9.]+)");
            
            Matcher oldMatcher = pattern.matcher(oldVal);
            Matcher newMatcher = pattern.matcher(newVal);
            
            if (oldMatcher.find() && newMatcher.find()) {
                double oldNum = Double.parseDouble(oldMatcher.group(1));
                double newNum = Double.parseDouble(newMatcher.group(1));
                
                if (oldNum == 0) {
                    return Math.abs(newNum);
                }
                
                return Math.abs((newNum - oldNum) / oldNum);
            }
            
            // If we can't extract numbers, consider them completely different if not equal
            return oldVal.equals(newVal) ? 0.0 : 1.0;
        } catch (Exception e) {
            logger.warn("Error calculating numeric change: {}", e.getMessage());
            return oldVal.equals(newVal) ? 0.0 : 1.0;
        }
    }
    
    /**
     * Detect changes in a single element
     * 
     * @param oldElement Old element data
     * @param newElement New element data
     * @return List of changes detected
     */
    public List<ElementChange> detectElementChanges(ElementData oldElement, ElementData newElement) {
        List<ElementChange> elementChanges = new ArrayList<>();
        String selector = oldElement.getSelector();
        
        // Text content changes
        if (!nullSafeEquals(oldElement.getText(), newElement.getText())) {
            double similarity = calculateTextSimilarity(
                oldElement.getText() == null ? "" : oldElement.getText(), 
                newElement.getText() == null ? "" : newElement.getText()
            );
            
            // Get threshold from configuration
            double threshold = Defaults.TEXT_SIMILARITY_DEFAULT; // centralized default
            if (configuration != null && configuration.getProjectConfig() != null &&
                configuration.getProjectConfig().getComparisonSettings() != null &&
                configuration.getProjectConfig().getComparisonSettings().getTextSimilarityThreshold() != null) {
                threshold = configuration.getProjectConfig().getComparisonSettings().getTextSimilarityThreshold();
            }
            
            // Only report change if similarity is below threshold
            if (similarity < threshold) {
                ElementChange ec = new ElementChange(
                    selector,
                    "text",
                    oldElement.getText(),
                    newElement.getText(),
                    "TEXT_MODIFICATION",
                    1 - similarity
                );
                ec.setClassification(classifyChange(ec));
                elementChanges.add(ec);
            }
        }
        
        // Attribute changes
        Map<String, String> oldAttrs = oldElement.getAttributes();
        Map<String, String> newAttrs = newElement.getAttributes();
        
        Set<String> allAttrs = new HashSet<>(oldAttrs.keySet());
        allAttrs.addAll(newAttrs.keySet());
        
        // Get ignore patterns from configuration
        List<String> ignoreAttributePatterns = new ArrayList<>();
        if (configuration != null && configuration.getProjectConfig() != null &&
            configuration.getProjectConfig().getCaptureSettings() != null &&
            configuration.getProjectConfig().getCaptureSettings().getIgnoreAttributePatterns() != null) {
            ignoreAttributePatterns = configuration.getProjectConfig().getCaptureSettings().getIgnoreAttributePatterns();
        }
        
        for (String attr : allAttrs) {
            String oldVal = oldAttrs.get(attr);
            String newVal = newAttrs.get(attr);
            
            if (!nullSafeEquals(oldVal, newVal)) {
                // Check if this attribute change should be ignored
                boolean shouldIgnore = false;
                
                // Check against ignore patterns - pattern matching with regex support
                for (String pattern : ignoreAttributePatterns) {
                    try {
                        // Support regex patterns
                        if ((oldVal != null && oldVal.matches(pattern)) ||
                            (newVal != null && newVal.matches(pattern))) {
                            shouldIgnore = true;
                            break;
                        }
                        // Also check simple contains for backwards compatibility
                        if ((oldVal != null && oldVal.contains(pattern.replace(".*", ""))) ||
                            (newVal != null && newVal.contains(pattern.replace(".*", "")))) {
                            shouldIgnore = true;
                            break;
                        }
                    } catch (Exception e) {
                        // If regex fails, fall back to simple contains check
                        String cleanPattern = pattern.replace(".*", "");
                        if ((oldVal != null && oldVal.contains(cleanPattern)) ||
                            (newVal != null && newVal.contains(cleanPattern))) {
                            shouldIgnore = true;
                            break;
                        }
                    }
                }
                
                if (shouldIgnore) {
                    continue; // Skip this attribute change
                }
                
                ElementChange ec = new ElementChange(
                    selector,
                    "attr_" + attr,
                    oldVal,
                    newVal,
                    "attribute",
                    oldVal != null && newVal != null ? Defaults.ATTRIBUTE_CHANGE_BASE : 1.0
                );
                ec.setClassification(classifyChange(ec));
                elementChanges.add(ec);
            }
        }
        
        // Style changes
        Map<String, String> oldStyles = oldElement.getStyles();
        Map<String, String> newStyles = newElement.getStyles();
        
        Set<String> allStyles = new HashSet<>(oldStyles.keySet());
        allStyles.addAll(newStyles.keySet());
        
        // Get ignore list from configuration
        List<String> ignoreStyleChanges = new ArrayList<>();
        if (configuration != null && configuration.getProjectConfig() != null &&
            configuration.getProjectConfig().getComparisonSettings() != null &&
            configuration.getProjectConfig().getComparisonSettings().getIgnoreStyleChanges() != null) {
            ignoreStyleChanges = configuration.getProjectConfig().getComparisonSettings().getIgnoreStyleChanges();
        }
        
        // Style categorization for better reporting
    for (String style : allStyles) {
            String oldVal = oldStyles.get(style);
            String newVal = newStyles.get(style);
            
            if (!nullSafeEquals(oldVal, newVal)) {
                // Skip if this style is in the ignore list
                if (ignoreStyleChanges.contains(style)) {
                    continue;
                }
                
                String category = getStyleCategory(style);
                double magnitude;
                
                // Calculate change magnitude based on category
                if (category.equals("color")) {
                    magnitude = Defaults.COLOR_STYLE_IMPORTANCE; // centralized color importance
                } else if (category.equals("dimension")) {
                    magnitude = calculateNumericChange(oldVal == null ? "0" : oldVal, newVal == null ? "0" : newVal);
                } else {
                    magnitude = oldVal != null && newVal != null ? Defaults.ATTRIBUTE_CHANGE_BASE : 1.0;
                }
                
                // Skip insignificant changes based on comparison thresholds
                if (configuration != null && configuration.getProjectConfig() != null &&
                    configuration.getProjectConfig().getComparisonSettings() != null) {
                    ProjectConfig.ComparisonSettings comp = configuration.getProjectConfig().getComparisonSettings();
                    if ("dimension".equals(category) && comp.getNumericChangeThreshold() != null && magnitude < comp.getNumericChangeThreshold()) {
                        continue;
                    }
                    if ("color".equals(category) && comp.getColorChangeThreshold() != null && magnitude < comp.getColorChangeThreshold()) {
                        continue;
                    }
                }
                ElementChange ec = new ElementChange(selector, style, oldVal, newVal, "style_" + category, magnitude);
                ec.setClassification(classifyChange(ec));
                elementChanges.add(ec);
            }
        }
        
        // Position changes - detect layout shifts
        // Only track position if position-x/position-y are in stylesToCapture
        List<String> stylesToCapture = new ArrayList<>();
        if (configuration != null && configuration.getProjectConfig() != null &&
            configuration.getProjectConfig().getCaptureSettings() != null &&
            configuration.getProjectConfig().getCaptureSettings().getStylesToCapture() != null) {
            stylesToCapture = configuration.getProjectConfig().getCaptureSettings().getStylesToCapture();
        }
        
        Map<String, Object> oldPosition = oldElement.getPosition();
        Map<String, Object> newPosition = newElement.getPosition();
        
        if (oldPosition != null && newPosition != null) {
            // Check for position changes - focus only on coordinates (x, y) for layout shifts
            String[] positionProps = {"x", "y"};
            for (String prop : positionProps) {
                // Only track position if "position" is in stylesToCapture
                if (!stylesToCapture.contains("position")) {
                    continue;
                }
                
                Object oldVal = oldPosition.get(prop);
                Object newVal = newPosition.get(prop);
                
                if (!nullSafeEquals(oldVal, newVal)) {
                    String positionProperty = "position_" + prop;
                    // Skip if this position property or general "position" is in the ignore list 
                    if (ignoreStyleChanges.contains(positionProperty) || ignoreStyleChanges.contains("position")) {
                        continue;
                    }
                    
                    double magnitude = Defaults.POSITION_CHANGE_BASE; // Default magnitude for position changes
                    
                    // Calculate magnitude for numeric position changes
                    if (oldVal instanceof Number && newVal instanceof Number) {
                        double oldNum = ((Number) oldVal).doubleValue();
                        double newNum = ((Number) newVal).doubleValue();
                        
                        if (oldNum != 0) {
                            magnitude = Math.abs((newNum - oldNum) / oldNum);
                        } else {
                            magnitude = Math.abs(newNum);
                        }
                    }
                    
                    ElementChange ec = new ElementChange(
                        selector,
                        "position_" + prop,
                        oldVal != null ? oldVal.toString() : "null",
                        newVal != null ? newVal.toString() : "null",
                        "layout",
                        magnitude
                    );
                    ec.setClassification(classifyChange(ec));
                    elementChanges.add(ec);
                }
            }
        }
        
        return elementChanges;
    }
    
    /**
     * Categorize a CSS property using configuration or fallback logic
     * 
     * @param property CSS property name
     * @return Category name
     */
    private String getStyleCategory(String property) {
        if (property == null) return "other";
        
        // Use configuration if available
        if (configuration != null && configuration.getProjectConfig() != null &&
            configuration.getProjectConfig().getComparisonSettings() != null &&
            configuration.getProjectConfig().getComparisonSettings().getStyleCategories() != null) {
            
            Map<String, List<String>> styleCategories = 
                configuration.getProjectConfig().getComparisonSettings().getStyleCategories();
            
            for (Map.Entry<String, List<String>> entry : styleCategories.entrySet()) {
                if (entry.getValue().contains(property)) {
                    return entry.getKey();
                }
            }
        }
        
        // Fallback to hardcoded logic if not found in configuration
        // Dimensions
        if (property.contains("width") || property.contains("height") ||
            property.contains("margin") || property.contains("padding") ||
            property.contains("border") || property.equals("top") ||
            property.equals("left") || property.equals("bottom") ||
            property.equals("right")) {
            return "dimension";
        }
        
        // Typography
        if (property.contains("font") || property.contains("text") ||
            property.contains("line-height") || property.contains("letter-spacing")) {
            return "typography";
        }
        
        // Colors
        if (property.contains("color") || property.contains("background")) {
            return "color";
        }
        
        // Visibility
        if (property.contains("opacity") || property.contains("display") ||
            property.contains("visibility") || property.contains("z-index")) {
            return "visibility";
        }
        
        return "other";
    }
    
    /**
     * Classify a change as Critical, Cosmetic, or Noise
     * Based on the Python implementation
     * 
     * @param change The change to classify
     * @return Classification string
     */
    public String classifyChange(ElementChange change) {
        String element = change.getElement() != null ? change.getElement().toLowerCase() : "";
        String property = change.getProperty() != null ? change.getProperty() : "";
        String changeType = change.getChangeType() != null ? change.getChangeType() : "";
        double magnitude = change.getMagnitude();

        ProjectConfig pc = configuration != null ? configuration.getProjectConfig() : null;
        ProjectConfig.ClassificationSettings cls = pc != null ? pc.getClassificationSettings() : null;
        ProjectConfig.Flags flags = pc != null ? pc.getFlags() : null;

        double textCritical = getMagnitudeThreshold(cls, "textCritical", 0.5);
        double colorCosmetic = getMagnitudeThreshold(cls, "colorCosmetic", 0.3);
    // layoutCosmetic retained in thresholds for backward compatibility but not directly used after positionBase externalization
    double styleCritical = getMagnitudeThreshold(cls, "styleCritical", Defaults.CLASS_STYLE_CRITICAL);
    double styleCosmetic = getMagnitudeThreshold(cls, "styleCosmetic", Defaults.CLASS_STYLE_COSMETIC);
    double positionBase = getMagnitudeThreshold(cls, "positionBase", Defaults.CLASS_POSITION_BASE_MAG);

        if (cls != null && cls.getInteractiveKeywords() != null && !cls.getInteractiveKeywords().isEmpty()) {
            for (String kw : cls.getInteractiveKeywords()) {
                if (kw != null && !kw.isEmpty() && element.contains(kw)) return "critical";
            }
        } else { // fallback defaults
            String[] defaults = {"button", "input", "form", "a", "select"};
            for (String kw : defaults) if (element.contains(kw)) return "critical";
        }

        if ("structural".equals(changeType) || property.startsWith("element_")) return "critical";
        // Accessibility-related keywords (configurable)
        if (cls != null && cls.getAccessibilityKeywords() != null && !cls.getAccessibilityKeywords().isEmpty()) {
            for (String kw : cls.getAccessibilityKeywords()) {
                if (kw != null && !kw.isEmpty() && property.contains(kw)) return "critical";
            }
        } else if (property.contains("aria") || property.contains("alt") || property.contains("role")) {
            return "critical";
        }

        if ("text".equals(property) || "text".equals(changeType)) {
            if (magnitude >= textCritical) return "critical";
            return magnitude >= (textCritical / 2.0) ? "cosmetic" : "noise";
        }
        if ("layout".equals(changeType) || property.startsWith("position_")) {
            // Use positionBase for classification of position changes
            return magnitude >= positionBase ? "cosmetic" : "noise";
        }
        if (property.equals("color") || property.equals("background-color") || property.contains("font-family")) {
            return magnitude >= colorCosmetic ? "cosmetic" : "noise";
        }
        if (changeType.startsWith("style_")) {
            if (magnitude >= styleCritical) return "critical";
            if (magnitude >= styleCosmetic) return "cosmetic";
            return "noise";
        }

        boolean advanced = flags != null && Boolean.TRUE.equals(flags.getEnableAdvancedClassification());
        if (advanced && cls != null && cls.getRules() != null) {
            String ruleResult = applyRuleEngine(change, cls);
            if (ruleResult != null) return ruleResult;
        }
        return "noise";
    }
    
    /**
     * Create a unique identifier for an element
     * Uses selector + stable attributes (but NOT position or text content) to handle duplicate selectors
     * Position and text content are excluded because those changes are what we want to detect
     */
    private String createElementId(ElementData element) {
        StringBuilder id = new StringBuilder();
        id.append(element.getSelector());
        
        // Add stable attributes to distinguish elements with same selector
        // But do NOT include position since that's what we want to detect changes in
        if (element.getAttributes() != null) {
            String classAttr = element.getAttributes().get("class");
            String idAttr = element.getAttributes().get("id");
            if (idAttr != null && !idAttr.trim().isEmpty()) {
                id.append("::id=").append(idAttr.trim());
            } else if (classAttr != null && !classAttr.trim().isEmpty()) {
                // Take first class name for stability
                String firstClass = classAttr.trim().split("\\s+")[0];
                id.append("::class=").append(firstClass);
            }
        }
        
        // Only add position as last resort if we still have duplicate selectors
        // and no distinguishing attributes
        String baseId = id.toString();
        
        // For now, let's use a simple approach: selector + stable attributes should be enough
        // If we need position for uniqueness, we can add it back but we need to handle
        // the case where positions change but it's the same logical element
        
        return baseId;
    }

    /**
     * Phase 2: Enhanced change detection using fingerprint-based element matching.
     * This method provides more resilient change detection that can handle DOM structure changes
     * and dynamic selectors by using element fingerprints instead of fragile selector matching.
     * 
     * @param oldElements List of baseline elements
     * @param newElements List of current elements
     * @param maxChanges Maximum number of changes to detect (null for unlimited)
     * @return List of changes detected
     */
    public List<ElementChange> detectChangesEnhanced(List<ElementData> oldElements, List<ElementData> newElements, Integer maxChanges) {
        changes.clear();
        
        if (elementMatcher == null) {
            logger.warn("ElementMatcher not available, falling back to legacy detection");
            return detectChanges(oldElements, newElements, maxChanges);
        }
        
        logger.info("Starting Phase 2 enhanced change detection with fingerprint matching");
        
        // Use ElementMatcher to find corresponding elements
        ElementMatcher.MatchResult matchResult = elementMatcher.matchElements(oldElements, newElements);
        
        // Detect changes in matched element pairs
        for (Map.Entry<ElementData, ElementData> entry : matchResult.getMatchedPairs().entrySet()) {
            // Check if we've reached the maximum number of changes
            if (maxChanges != null && changes.size() >= maxChanges) {
                logger.info("Reached maximum changes limit: {}", maxChanges);
                break;
            }
            
            ElementData oldElement = entry.getKey();
            ElementData newElement = entry.getValue();
            Double matchConfidence = matchResult.getMatchConfidences().get(oldElement);
            
            // Detect changes between matched elements
            List<ElementChange> elementChanges = detectElementChanges(oldElement, newElement);
            
            // Add match confidence information to changes
            for (ElementChange change : elementChanges) {
                if (matchConfidence != null && matchConfidence < 1.0) {
                    // Flag changes where element matching had lower confidence
                    change.setMatchConfidence(matchConfidence);
                    if (matchConfidence < Defaults.LOW_MATCH_CONFIDENCE) {
                        change.setClassification("potential"); // Uncertain match
                    }
                }
                changes.add(change);
            }
        }
        
        // Handle structural changes (added/removed elements)
        boolean detectStructural = configuration != null ? 
            configuration.isDetectStructuralChanges() : true; // Default to true for enhanced detection
            
        if (detectStructural) {
            // Add changes for removed elements
            for (ElementData removedElement : matchResult.getRemovedElements()) {
                if (maxChanges != null && changes.size() >= maxChanges) break;
                
                ElementChange change = new ElementChange();
                change.setElement(removedElement.getSelector());
                change.setProperty("element_removed");
                change.setOldValue("present");
                change.setNewValue("absent");
                change.setChangeType("structural");
                change.setMagnitude(1.0);
                change.setClassification("critical");
                changes.add(change);
            }
            
            // Add changes for added elements  
            for (ElementData addedElement : matchResult.getAddedElements()) {
                if (maxChanges != null && changes.size() >= maxChanges) break;
                
                ElementChange change = new ElementChange();
                change.setElement(addedElement.getSelector());
                change.setProperty("element_added");
                change.setOldValue("absent");
                change.setNewValue("present");
                change.setChangeType("structural");
                change.setMagnitude(1.0);
                change.setClassification("critical");
                changes.add(change);
            }
        }
        
        logger.info("Phase 2 enhanced detection complete: {} total changes, {} matched pairs, {} removed, {} added",
                   changes.size(), matchResult.getMatchedPairs().size(), 
                   matchResult.getRemovedElements().size(), matchResult.getAddedElements().size());
        
        return new ArrayList<>(changes);
    }

    /**
     * Phase 3: Comprehensive change detection with advanced structural analysis.
     * This method provides the most advanced change detection capabilities including
     * structural analysis, pattern recognition, and context-aware change classification.
     * 
     * @param oldElements List of baseline elements
     * @param newElements List of current elements
     * @param maxChanges Maximum number of changes to detect (null for unlimited)
     * @return Comprehensive change analysis results
     */
    public ComprehensiveAnalysisResult detectChangesComprehensive(List<ElementData> oldElements, 
                                                                 List<ElementData> newElements, 
                                                                 Integer maxChanges) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Phase 3 comprehensive change detection with structural analysis");
        
        ComprehensiveAnalysisResult result = new ComprehensiveAnalysisResult();
        
        // Step 1: Perform structural analysis on both versions
        StructuralAnalyzer.StructuralAnalysis oldStructure = structuralAnalyzer.analyzeStructure(oldElements);
        StructuralAnalyzer.StructuralAnalysis newStructure = structuralAnalyzer.analyzeStructure(newElements);
        
        result.setOldStructuralAnalysis(oldStructure);
        result.setNewStructuralAnalysis(newStructure);
        
        // Step 2: Detect changes using enhanced Phase 2 method
        List<ElementChange> changes = detectChangesEnhanced(oldElements, newElements, maxChanges);
        
        // Step 3: Enhance changes with structural context
        List<ElementChange> contextualChanges = structuralAnalyzer.analyzeStructuralChanges(
            changes, oldStructure, newStructure);
        
        result.setChanges(contextualChanges);
        
        // Step 4: Calculate performance metrics
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        result.setProcessingTimeMs(processingTime);
        result.setElementsAnalyzed(oldElements.size() + newElements.size());
        result.setChangesPerSecond(contextualChanges.size() > 0 ? 
            (contextualChanges.size() * 1000.0 / processingTime) : 0);
        
        logger.info("Phase 3 comprehensive analysis complete: {} changes analyzed in {}ms", 
                   contextualChanges.size(), processingTime);
        
        return result;
    }
    
    /**
     * Comprehensive analysis result containing all Phase 3 features
     */
    public static class ComprehensiveAnalysisResult {
        private List<ElementChange> changes;
        private StructuralAnalyzer.StructuralAnalysis oldStructuralAnalysis;
        private StructuralAnalyzer.StructuralAnalysis newStructuralAnalysis;
        private long processingTimeMs;
        private int elementsAnalyzed;
        private double changesPerSecond;
        
        public ComprehensiveAnalysisResult() {
            this.changes = new ArrayList<>();
        }
        
        // Getters and setters
        public List<ElementChange> getChanges() { return changes; }
        public void setChanges(List<ElementChange> changes) { this.changes = changes; }
        
        public StructuralAnalyzer.StructuralAnalysis getOldStructuralAnalysis() { return oldStructuralAnalysis; }
        public void setOldStructuralAnalysis(StructuralAnalyzer.StructuralAnalysis oldStructuralAnalysis) { 
            this.oldStructuralAnalysis = oldStructuralAnalysis; 
        }
        
        public StructuralAnalyzer.StructuralAnalysis getNewStructuralAnalysis() { return newStructuralAnalysis; }
        public void setNewStructuralAnalysis(StructuralAnalyzer.StructuralAnalysis newStructuralAnalysis) { 
            this.newStructuralAnalysis = newStructuralAnalysis; 
        }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public int getElementsAnalyzed() { return elementsAnalyzed; }
        public void setElementsAnalyzed(int elementsAnalyzed) { this.elementsAnalyzed = elementsAnalyzed; }
        
        public double getChangesPerSecond() { return changesPerSecond; }
        public void setChangesPerSecond(double changesPerSecond) { this.changesPerSecond = changesPerSecond; }
    }

    /**
     * Compare two sets of elements and detect all changes (Legacy method using selector-based matching)
     * 
     * @param oldElements List of old elements
     * @param newElements List of new elements
     * @param maxChanges Maximum number of changes to detect (null for unlimited)
     * @return List of changes detected
     */
    public List<ElementChange> detectChanges(List<ElementData> oldElements, List<ElementData> newElements, Integer maxChanges) {
        changes.clear();
        
        // Create maps for faster lookup using unique element IDs
        Map<String, ElementData> oldElementMap = new java.util.HashMap<>();
        Map<String, ElementData> newElementMap = new java.util.HashMap<>();
        
        for (ElementData element : oldElements) {
            String elementId = createElementId(element);
            oldElementMap.put(elementId, element);
        }
        
        for (ElementData element : newElements) {
            String elementId = createElementId(element);
            newElementMap.put(elementId, element);
        }
        
        // Find elements in both sets and compare them
        Set<String> allElementIds = new HashSet<>(oldElementMap.keySet());
        allElementIds.addAll(newElementMap.keySet());
        
        logger.info("Comparing {} unique elements between old and new snapshots", allElementIds.size());
        
        int processedCount = 0;
        for (String elementId : allElementIds) {
            try {
                // Check if we've reached the maximum number of changes
                if (maxChanges != null && changes.size() >= maxChanges) {
                    logger.info("Reached maximum changes limit: {}", maxChanges);
                    break;
                }
                
                ElementData oldElement = oldElementMap.get(elementId);
                ElementData newElement = newElementMap.get(elementId);
                
                if (oldElement != null && newElement != null) {
                    // Element exists in both snapshots - detect changes
                    List<ElementChange> elementChanges = detectElementChanges(oldElement, newElement);
                    changes.addAll(elementChanges);
                } else {
                    // Check if structural change detection is enabled
                    boolean detectStructural = configuration != null ? 
                        configuration.isDetectStructuralChanges() : false;
                    
                    if (detectStructural) {
                        if (oldElement != null) {
                            // Element removed in new snapshot
                            changes.add(new ElementChange(
                                oldElement.getSelector(),
                                "element_existence",
                                "present",
                                "removed",
                                "structural",
                                1.0
                            ));
                        } else if (newElement != null) {
                            // Element added in new snapshot
                            changes.add(new ElementChange(
                                newElement.getSelector(),
                                "element_existence",
                                "absent",
                                "added",
                                "structural",
                                1.0
                            ));
                        }
                    }
                }
                
                processedCount++;
                
            } catch (Exception e) {
                logger.error("Error processing element '{}': {}", elementId, e.getMessage());
            }
        }
        
        logger.info("Processed {} elements, found {} changes", processedCount, changes.size());
        
        // Apply classification to all changes
        for (ElementChange change : changes) {
            String classification = classifyChange(change);
            change.setClassification(classification);
        }
        
        return changes;
    }
    
    /**
     * Null-safe equality check
     */
    private boolean nullSafeEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ===================== Config Helpers =====================
    private double getMagnitudeThreshold(ProjectConfig.ClassificationSettings cls, String key, double defVal) {
        if (cls == null || cls.getMagnitudeThresholds() == null) return defVal;
        Double v = cls.getMagnitudeThresholds().get(key);
        return v != null ? v : defVal;
    }

    private String applyRuleEngine(ElementChange change, ProjectConfig.ClassificationSettings cls) {
        for (Map<String, Object> rule : cls.getRules()) {
            if (rule == null) continue;
            String propertyContains = getString(rule, "propertyContains");
            String changeTypeEquals = getString(rule, "changeType");
            Double minMagnitude = getDouble(rule, "minMagnitude");
            String classification = getString(rule, "classification");
            boolean match = true;
            if (propertyContains != null && (change.getProperty() == null || !change.getProperty().contains(propertyContains))) match = false;
            if (changeTypeEquals != null && (change.getChangeType() == null || !change.getChangeType().equals(changeTypeEquals))) match = false;
            if (minMagnitude != null && change.getMagnitude() < minMagnitude) match = false;
            if (match && classification != null) return classification;
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof String ? (String) v : null; }
    private Double getDouble(Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof Number ? ((Number) v).doubleValue() : null; }
    
    /**
     * Get the current list of detected changes
     * 
     * @return List of changes
     */
    public List<ElementChange> getChanges() {
        return changes;
    }
}
