package com.uitester.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a DOM element with its properties, attributes, styles, and position.
 * This is a data class to store information about elements extracted by the crawler.
 */
public class ElementData {
    private String tagName;
    private String text;
    private String selector;
    private Map<String, String> attributes;
    private Map<String, String> styles;
    private Map<String, Object> position;
    private boolean inViewport;
    
    /**
     * Default constructor
     */
    public ElementData() {
        this.attributes = new HashMap<>();
        this.styles = new HashMap<>();
        this.position = new HashMap<>();
    }
    
    /**
     * Constructor with all fields
     */
    public ElementData(String tagName, String text, String selector, 
                       Map<String, String> attributes, Map<String, String> styles, 
                       Map<String, Object> position, boolean inViewport) {
        this.tagName = tagName;
        this.text = text;
        this.selector = selector;
        this.attributes = attributes;
        this.styles = styles;
        this.position = position;
        this.inViewport = inViewport;
    }

    // Getters and setters
    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getStyles() {
        return styles;
    }

    public void setStyles(Map<String, String> styles) {
        this.styles = styles;
    }

    public Map<String, Object> getPosition() {
        return position;
    }

    public void setPosition(Map<String, Object> position) {
        this.position = position;
    }

    public boolean isInViewport() {
        return inViewport;
    }

    public void setInViewport(boolean inViewport) {
        this.inViewport = inViewport;
    }
    
    @Override
    public String toString() {
        return String.format("%s[selector=%s, text=%s]", 
                tagName, 
                selector, 
                text != null && text.length() > 30 ? text.substring(0, 27) + "..." : text);
    }
}
