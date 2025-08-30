package com.uitester.report;

import com.uitester.diff.ElementChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes element changes to provide meaningful, human-readable insights
 * instead of raw CSS dumps.
 */
public class ChangeAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ChangeAnalyzer.class);
    
    // Patterns to extract specific CSS values
    private static final Pattern COLOR_PATTERN = Pattern.compile("background-color=rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
    private static final Pattern BORDER_RADIUS_PATTERN = Pattern.compile("border-radius=([\\d.]+px)");
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("font-size=([\\d.]+px)");
    private static final Pattern WIDTH_PATTERN = Pattern.compile("width=([\\d.]+px)");
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("height=([\\d.]+px)");
    private static final Pattern PADDING_PATTERN = Pattern.compile("padding=([\\d.px\\s]+)");
    private static final Pattern MARGIN_PATTERN = Pattern.compile("margin=([\\d.px\\s]+)");
    
    /**
     * Enhanced change description with human-readable summary
     */
    public static class EnhancedChange {
        private String element;
        private String summary;
        private String details;
        private String impact;
        private double confidence;
        private String changeType;
        
        public EnhancedChange(String element, String summary, String details, String impact, double confidence, String changeType) {
            this.element = element;
            this.summary = summary;
            this.details = details;
            this.impact = impact;
            this.confidence = confidence;
            this.changeType = changeType;
        }
        
        // Getters
        public String getElement() { return element; }
        public String getSummary() { return summary; }
        public String getDetails() { return details; }
        public String getImpact() { return impact; }
        public double getConfidence() { return confidence; }
        public String getChangeType() { return changeType; }
    }
    
    /**
     * Analyze an element change and create a human-readable description
     */
    public static EnhancedChange analyzeChange(ElementChange change) {
        String element = change.getElement();
        String changeType = change.getChangeType();
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        String property = change.getProperty();
        
        // Calculate better confidence score
        double confidence = calculateConfidence(change);
        
        String summary;
        String details;
        String impact;
        
        if (("CSS_MODIFICATION".equals(changeType) && "styles".equals(property)) ||
            "style".equals(changeType) || changeType.startsWith("style_")) {
            return analyzeCSSChange(element, oldValue, newValue, confidence);
        } else if ("TEXT_MODIFICATION".equals(changeType) || "text".equals(changeType)) {
            return analyzeTextChange(element, oldValue, newValue, confidence);
        } else if ("ELEMENT_ADDED".equals(changeType)) {
            summary = "New element added";
            details = "A new " + extractTagName(element) + " element was added to the page";
            impact = "Content addition - may affect layout";
        } else if ("ELEMENT_REMOVED".equals(changeType)) {
            summary = "Element removed";
            details = "The " + extractTagName(element) + " element was removed from the page";
            impact = "Content removal - may affect layout";
        } else if ("attribute".equals(changeType)) {
            return analyzeAttributeChange(element, property, oldValue, newValue, confidence);
        } else if ("layout".equals(changeType)) {
            return analyzeLayoutChange(element, property, oldValue, newValue, confidence);
        } else {
            summary = "Unknown change";
            details = "Change type: " + changeType;
            impact = "Impact unknown";
        }
        
        return new EnhancedChange(element, summary, details, impact, confidence, changeType);
    }
    
    /**
     * Analyze CSS style changes to extract meaningful differences
     */
    private static EnhancedChange analyzeCSSChange(String element, String oldValue, String newValue, double confidence) {
        List<String> changes = new ArrayList<>();
        String impact = "Visual change";
        
        // Extract and compare background colors
        String oldColor = extractColor(oldValue);
        String newColor = extractColor(newValue);
        if (!oldColor.equals(newColor)) {
            changes.add(String.format("Background color changed from %s to %s", oldColor, newColor));
            impact = "Color change - high visual impact";
        }
        
        // Extract and compare border radius
        String oldRadius = extractValue(oldValue, BORDER_RADIUS_PATTERN);
        String newRadius = extractValue(newValue, BORDER_RADIUS_PATTERN);
        if (!oldRadius.equals(newRadius)) {
            changes.add(String.format("Border radius changed from %s to %s", oldRadius, newRadius));
        }
        
        // Extract and compare font size
        String oldFontSize = extractValue(oldValue, FONT_SIZE_PATTERN);
        String newFontSize = extractValue(newValue, FONT_SIZE_PATTERN);
        if (!oldFontSize.equals(newFontSize)) {
            changes.add(String.format("Font size changed from %s to %s", oldFontSize, newFontSize));
            impact = "Typography change - affects readability";
        }
        
        // Extract and compare dimensions
        String oldWidth = extractValue(oldValue, WIDTH_PATTERN);
        String newWidth = extractValue(newValue, WIDTH_PATTERN);
        if (!oldWidth.equals(newWidth)) {
            changes.add(String.format("Width changed from %s to %s", oldWidth, newWidth));
        }
        
        String oldHeight = extractValue(oldValue, HEIGHT_PATTERN);
        String newHeight = extractValue(newValue, HEIGHT_PATTERN);
        if (!oldHeight.equals(newHeight)) {
            changes.add(String.format("Height changed from %s to %s", oldHeight, newHeight));
        }
        
        // Check for box-shadow addition
        if (newValue.contains("box-shadow") && !oldValue.contains("box-shadow")) {
            changes.add("Box shadow effect added");
            impact = "Visual enhancement - shadow effect added";
        } else if (oldValue.contains("box-shadow") && !newValue.contains("box-shadow")) {
            changes.add("Box shadow effect removed");
        }
        
        String summary;
        String details;
        
        if (changes.isEmpty()) {
            summary = "Style modified";
            details = "CSS properties changed but specific differences not detected";
        } else if (changes.size() == 1) {
            summary = changes.get(0);
            details = "Single style change detected in " + extractTagName(element);
        } else {
            summary = String.format("%d style changes", changes.size());
            details = "Multiple changes: " + String.join(", ", changes);
        }
        
        return new EnhancedChange(element, summary, details, impact, confidence, "CSS_MODIFICATION");
    }
    
    /**
     * Analyze text content changes
     */
    private static EnhancedChange analyzeTextChange(String element, String oldValue, String newValue, double confidence) {
        String summary = "Text content changed";
        String details = String.format("Text changed from '%s' to '%s'", 
                                      truncateText(oldValue), truncateText(newValue));
        String impact = "Content change - may affect user understanding";
        
        return new EnhancedChange(element, summary, details, impact, confidence, "TEXT_MODIFICATION");
    }
    
    /**
     * Extract color information from CSS
     */
    private static String extractColor(String cssValue) {
        Matcher matcher = COLOR_PATTERN.matcher(cssValue);
        if (matcher.find()) {
            int r = Integer.parseInt(matcher.group(1));
            int g = Integer.parseInt(matcher.group(2));
            int b = Integer.parseInt(matcher.group(3));
            
            // Convert to hex and add color name if known
            String hex = String.format("#%02X%02X%02X", r, g, b);
            String colorName = getColorName(r, g, b);
            return colorName != null ? colorName + " (" + hex + ")" : hex;
        }
        return "unknown";
    }
    
    /**
     * Extract value using regex pattern
     */
    private static String extractValue(String cssValue, Pattern pattern) {
        Matcher matcher = pattern.matcher(cssValue);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
    
    /**
     * Get human-readable color name for common colors
     */
    private static String getColorName(int r, int g, int b) {
        if (r == 66 && g == 133 && b == 244) return "Blue";
        if (r == 52 && g == 168 && b == 83) return "Green";
        if (r == 255 && g == 0 && b == 0) return "Red";
        if (r == 255 && g == 255 && b == 255) return "White";
        if (r == 0 && g == 0 && b == 0) return "Black";
        if (r == 255 && g == 165 && b == 0) return "Orange";
        if (r == 255 && g == 255 && b == 0) return "Yellow";
        if (r == 128 && g == 0 && b == 128) return "Purple";
        return null;
    }
    
    /**
     * Extract tag name from selector
     */
    private static String extractTagName(String selector) {
        if (selector == null) return "unknown";
        
        // Handle common patterns
        if (selector.startsWith("button")) return "button";
        if (selector.startsWith("div")) return "div";
        if (selector.startsWith("h1")) return "heading";
        if (selector.startsWith("h2")) return "heading";
        if (selector.startsWith("p")) return "paragraph";
        if (selector.startsWith("header")) return "header";
        if (selector.startsWith("footer")) return "footer";
        if (selector.startsWith("#")) return "element";
        if (selector.startsWith(".")) return "element";
        
        return "element";
    }
    
    /**
     * Truncate text for display
     */
    private static String truncateText(String text) {
        if (text == null) return "";
        return text.length() > 50 ? text.substring(0, 47) + "..." : text;
    }
    
    /**
     * Calculate better confidence score based on change characteristics
     */
    private static double calculateConfidence(ElementChange change) {
        // Use existing confidence if available and reasonable
        if (change.getMatchConfidence() != null && change.getMatchConfidence() > 0.1) {
            return change.getMatchConfidence();
        }
        
        // Calculate confidence based on change type
        String changeType = change.getChangeType();
        if ("CSS_MODIFICATION".equals(changeType) || "style".equals(changeType) || changeType.startsWith("style_")) {
            return 0.9; // High confidence for style changes
        } else if ("TEXT_MODIFICATION".equals(changeType) || "text".equals(changeType)) {
            return 0.95; // Very high confidence for text changes
        } else if ("ELEMENT_ADDED".equals(changeType) || "ELEMENT_REMOVED".equals(changeType)) {
            return 1.0; // Certain for structural changes
        } else if ("attribute".equals(changeType)) {
            return 0.85; // High confidence for attribute changes
        } else if ("layout".equals(changeType)) {
            return 0.8; // Good confidence for layout changes
        }
        
        return 0.7; // Default moderate confidence
    }
    
    /**
     * Analyze attribute changes
     */
    private static EnhancedChange analyzeAttributeChange(String element, String property, String oldValue, String newValue, double confidence) {
        String attributeName = property.startsWith("attr_") ? property.substring(5) : property;
        String summary;
        String details;
        String impact;
        
        if ("src".equals(attributeName)) {
            summary = "Image source changed";
            details = String.format("Image source changed from %s to %s", 
                truncateUrl(oldValue), truncateUrl(newValue));
            impact = "Image content change - may affect visual appearance";
        } else if ("href".equals(attributeName)) {
            summary = "Link destination changed";
            details = String.format("Link target changed from %s to %s", 
                truncateUrl(oldValue), truncateUrl(newValue));
            impact = "Navigation change - affects user experience";
        } else if ("class".equals(attributeName)) {
            summary = "CSS classes changed";
            details = String.format("CSS classes changed from '%s' to '%s'", oldValue, newValue);
            impact = "Styling change - may affect appearance";
        } else if ("id".equals(attributeName)) {
            summary = "Element ID changed";
            details = String.format("Element ID changed from '%s' to '%s'", oldValue, newValue);
            impact = "Identity change - may affect functionality";
        } else {
            summary = String.format("%s attribute changed", attributeName);
            details = String.format("Attribute '%s' changed from '%s' to '%s'", 
                attributeName, truncateText(oldValue), truncateText(newValue));
            impact = "Attribute modification - impact varies";
        }
        
        return new EnhancedChange(element, summary, details, impact, confidence, "attribute");
    }
    
    /**
     * Analyze layout/position changes
     */
    private static EnhancedChange analyzeLayoutChange(String element, String property, String oldValue, String newValue, double confidence) {
        String summary;
        String details;
        String impact;
        
        if (property.startsWith("position_")) {
            String direction = property.substring(9); // Remove "position_"
            summary = String.format("Element moved (%s)", direction);
            details = String.format("Position %s changed from %s to %s pixels", 
                direction, oldValue, newValue);
            
            // Calculate movement magnitude
            try {
                int oldPos = Integer.parseInt(oldValue);
                int newPos = Integer.parseInt(newValue);
                int movement = Math.abs(newPos - oldPos);
                
                if (movement > 50) {
                    impact = String.format("Significant layout shift - moved %d pixels", movement);
                } else if (movement > 10) {
                    impact = String.format("Minor layout adjustment - moved %d pixels", movement);
                } else {
                    impact = String.format("Slight position change - moved %d pixels", movement);
                }
            } catch (NumberFormatException e) {
                impact = "Position change detected";
            }
        } else {
            summary = "Layout property changed";
            details = String.format("Layout property '%s' changed from %s to %s", 
                property, oldValue, newValue);
            impact = "Layout change - may affect positioning";
        }
        
        return new EnhancedChange(element, summary, details, impact, confidence, "layout");
    }
    
    /**
     * Truncate URL for display
     */
    private static String truncateUrl(String url) {
        if (url == null) return "null";
        if (url.length() <= 50) return url;
        
        // Try to keep domain visible
        try {
            if (url.startsWith("http")) {
                String domain = url.substring(url.indexOf("//") + 2);
                if (domain.contains("/")) {
                    domain = domain.substring(0, domain.indexOf("/"));
                }
                return domain + "/..." + url.substring(url.length() - 20);
            }
        } catch (Exception e) {
            // Fall back to simple truncation
        }
        
        return url.substring(0, 25) + "..." + url.substring(url.length() - 15);
    }
}
