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

/**
 * Detects changes between two sets of elements.
 * This is equivalent to the Python ChangeDetector class.
 */
public class ChangeDetector {
    private static final Logger logger = LoggerFactory.getLogger(ChangeDetector.class);
    
    private List<ElementChange> changes;
    private Configuration configuration;
    
    public ChangeDetector() {
        this.changes = new ArrayList<>();
        this.configuration = null; // Will use default behavior
    }
    
    public ChangeDetector(Configuration configuration) {
        this.changes = new ArrayList<>();
        this.configuration = configuration;
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
            
            elementChanges.add(new ElementChange(
                selector,
                "text",
                oldElement.getText(),
                newElement.getText(),
                "text",
                1 - similarity
            ));
        }
        
        // Attribute changes
        Map<String, String> oldAttrs = oldElement.getAttributes();
        Map<String, String> newAttrs = newElement.getAttributes();
        
        Set<String> allAttrs = new HashSet<>(oldAttrs.keySet());
        allAttrs.addAll(newAttrs.keySet());
        
        for (String attr : allAttrs) {
            String oldVal = oldAttrs.get(attr);
            String newVal = newAttrs.get(attr);
            
            if (!nullSafeEquals(oldVal, newVal)) {
                elementChanges.add(new ElementChange(
                    selector,
                    "attr_" + attr,
                    oldVal,
                    newVal,
                    "attribute",
                    oldVal != null && newVal != null ? 0.5 : 1.0
                ));
            }
        }
        
        // Style changes
        Map<String, String> oldStyles = oldElement.getStyles();
        Map<String, String> newStyles = newElement.getStyles();
        
        Set<String> allStyles = new HashSet<>(oldStyles.keySet());
        allStyles.addAll(newStyles.keySet());
        
        // Style categorization for better reporting
        for (String style : allStyles) {
            String oldVal = oldStyles.get(style);
            String newVal = newStyles.get(style);
            
            if (!nullSafeEquals(oldVal, newVal)) {
                String category = getStyleCategory(style);
                double magnitude;
                
                // Calculate change magnitude based on category
                if (category.equals("color")) {
                    magnitude = 0.7; // Color changes are subjectively important
                } else if (category.equals("dimension")) {
                    magnitude = calculateNumericChange(oldVal == null ? "0" : oldVal, newVal == null ? "0" : newVal);
                } else {
                    magnitude = oldVal != null && newVal != null ? 0.5 : 1.0;
                }
                
                elementChanges.add(new ElementChange(
                    selector,
                    style,
                    oldVal,
                    newVal,
                    "style_" + category,
                    magnitude
                ));
            }
        }
        
        // Position changes - detect layout shifts
        Map<String, Object> oldPosition = oldElement.getPosition();
        Map<String, Object> newPosition = newElement.getPosition();
        
        if (oldPosition != null && newPosition != null) {
            // Check for position changes (skip width/height if already detected as style changes)
            String[] positionProps = {"x", "y", "width", "height"};
            for (String prop : positionProps) {
                Object oldVal = oldPosition.get(prop);
                Object newVal = newPosition.get(prop);
                
                if (!nullSafeEquals(oldVal, newVal)) {
                    // Skip width/height changes if they were already detected as style dimension changes
                    if ((prop.equals("width") || prop.equals("height"))) {
                        boolean styleChangeExists = elementChanges.stream()
                            .anyMatch(change -> change.getProperty().equals(prop) && 
                                     change.getChangeType().equals("style_dimension"));
                        if (styleChangeExists) {
                            continue; // Skip this position change as it's redundant
                        }
                    }
                    
                    double magnitude = 0.5; // Default magnitude for position changes
                    
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
                    
                    elementChanges.add(new ElementChange(
                        selector,
                        "position_" + prop,
                        oldVal != null ? oldVal.toString() : "null",
                        newVal != null ? newVal.toString() : "null",
                        "layout",
                        magnitude
                    ));
                }
            }
        }
        
        return elementChanges;
    }
    
    /**
     * Categorize a CSS property
     * 
     * @param property CSS property name
     * @return Category name
     */
    private String getStyleCategory(String property) {
        if (property == null) return "other";
        
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
        String element = change.getElement().toLowerCase();
        String property = change.getProperty();
        String changeType = change.getChangeType();
        double magnitude = change.getMagnitude();
        
        // Interactive elements (buttons, links, forms) - changes are critical
        String[] interactiveElements = {"button", "input", "form", "a", "select"};
        for (String keyword : interactiveElements) {
            if (element.contains(keyword)) {
                return "critical";
            }
        }
        
        // Structural changes are always critical
        if ("structural".equals(changeType)) {
            return "critical";
        }
        
        // Significant text changes
        if ("text".equals(property) && magnitude > 0.5) {
            return "critical";
        }
        
        // Visual property changes
        if (property.equals("color") || property.equals("background-color") || property.equals("font-family")) {
            return magnitude > 0.3 ? "cosmetic" : "noise";
        }
        
        // Layout changes
        if (changeType.equals("layout") || property.startsWith("position_")) {
            return magnitude > 0.1 ? "cosmetic" : "noise";
        }
        
        // Minor style changes
        if (property.contains("font-size") || property.contains("margin") || property.contains("padding")) {
            return magnitude < 0.3 ? "noise" : "cosmetic";
        }
        
        // Default to noise for minor changes
        return "noise";
    }
    
    /**
     * Create a unique identifier for an element
     * Uses selector + position (but NOT text content) to handle duplicate selectors
     * Text content is excluded because it may change and we want to detect those changes
     */
    private String createElementId(ElementData element) {
        StringBuilder id = new StringBuilder();
        id.append(element.getSelector());
        
        // Add position info to distinguish elements with same selector
        if (element.getPosition() != null) {
            Object x = element.getPosition().get("x");
            Object y = element.getPosition().get("y");
            if (x != null && y != null) {
                id.append("::pos=").append(x).append(",").append(y);
            }
        }
        
        // If we still don't have enough uniqueness, add some stable attributes
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
        
        return id.toString();
    }

    /**
     * Compare two sets of elements and detect all changes
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
    
    /**
     * Get the current list of detected changes
     * 
     * @return List of changes
     */
    public List<ElementChange> getChanges() {
        return changes;
    }
}
