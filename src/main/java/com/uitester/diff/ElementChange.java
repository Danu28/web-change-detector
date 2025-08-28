package com.uitester.diff;

/**
 * Represents a single detected change between elements.
 * This is a data class to store information about detected changes.
 */
public class ElementChange {
    private String element;
    private String property;
    private String oldValue;
    private String newValue;
    private String changeType;
    private double magnitude;
    private String classification;
    
    /**
     * Default constructor
     */
    public ElementChange() {
    }
    
    /**
     * Constructor with all fields
     */
    public ElementChange(String element, String property, String oldValue, String newValue, 
                        String changeType, double magnitude) {
        this.element = element;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
        this.magnitude = magnitude;
        
        // Automatically classify changes based on type and magnitude
        this.classification = classifyChange(changeType, property, magnitude);
    }

    /**
     * Classify a change based on its type and magnitude
     * 
     * @param changeType The type of change (e.g., text, attribute, style_color)
     * @param property The property that changed
     * @param magnitude The magnitude of the change (0.0 to 1.0)
     * @return Classification as "critical", "cosmetic", or "noise"
     */
    private String classifyChange(String changeType, String property, double magnitude) {
        // Critical changes
        if (changeType.equals("structural") || 
            (changeType.equals("text") && magnitude > 0.5) || 
            (property.equals("display") || property.equals("visibility"))) {
            return "critical";
        }
        
        // Cosmetic changes
        if (changeType.startsWith("style_") && magnitude > 0.2) {
            return "cosmetic";
        }
        
        if (changeType.equals("attribute") && 
            (property.contains("aria") || property.contains("alt") || property.contains("role"))) {
            return "critical"; // Accessibility changes are critical
        }
        
        // Default to noise for minor changes
        return "noise";
    }

    // Getters and setters
    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s changed from '%s' to '%s' (%s, magnitude: %.2f)", 
                element, property, 
                truncateValue(oldValue), truncateValue(newValue),
                classification, magnitude);
    }
    
    private String truncateValue(String value) {
        if (value == null) return "null";
        if (value.length() > 20) {
            return value.substring(0, 17) + "...";
        }
        return value;
    }
}
