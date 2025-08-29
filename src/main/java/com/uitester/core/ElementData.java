package com.uitester.core;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Represents a DOM element with its properties, attributes, styles, and position.
 * This is a data class to store information about elements extracted by the crawler.
 * Enhanced with fingerprinting capabilities for Phase 2.
 */
public class ElementData {
    private String tagName;
    private String text;
    private String selector;
    private Map<String, String> attributes;
    private Map<String, String> styles;
    private Map<String, Object> position;
    private boolean inViewport;
    
    // Phase 2: Fingerprinting fields
    private String fingerprint;
    private String structuralFingerprint;
    private String contentFingerprint;
    private Double fingerprintConfidence;
    
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
    
    // Phase 2: Fingerprinting getters and setters
    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getStructuralFingerprint() {
        return structuralFingerprint;
    }

    public void setStructuralFingerprint(String structuralFingerprint) {
        this.structuralFingerprint = structuralFingerprint;
    }

    public String getContentFingerprint() {
        return contentFingerprint;
    }

    public void setContentFingerprint(String contentFingerprint) {
        this.contentFingerprint = contentFingerprint;
    }

    public Double getFingerprintConfidence() {
        return fingerprintConfidence;
    }

    public void setFingerprintConfidence(Double fingerprintConfidence) {
        this.fingerprintConfidence = fingerprintConfidence;
    }

    /**
     * Phase 2: Generate a comprehensive fingerprint for this element based on configured settings.
     * This fingerprint helps match elements across baseline and current versions even when
     * selectors change due to DOM modifications.
     * 
     * @param config Configuration containing fingerprint settings
     * @return Generated fingerprint string
     */
    public String generateFingerprint(ProjectConfig config) {
        if (config == null || config.getFingerprintSettings() == null) {
            return generateBasicFingerprint();
        }
        
        ProjectConfig.FingerprintSettings settings = config.getFingerprintSettings();
        List<String> components = new ArrayList<>();
        
        // Add tag name if configured
        if (Boolean.TRUE.equals(settings.getUseTagName()) && tagName != null) {
            components.add("tag:" + tagName);
        }
        
        // Add text content if configured
        if (Boolean.TRUE.equals(settings.getUseTextContent()) && text != null && !text.trim().isEmpty()) {
            String normalizedText = text.trim().replaceAll("\\s+", " ");
            if (normalizedText.length() > 50) {
                normalizedText = normalizedText.substring(0, 50) + "...";
            }
            components.add("text:" + normalizedText);
        }
        
        // Add key attributes if configured
        if (attributes != null && settings.getUseAttributes() != null) {
            List<String> keyAttrs = settings.getUseAttributes();
            for (String attr : keyAttrs) {
                String value = attributes.get(attr);
                if (value != null && !value.trim().isEmpty()) {
                    components.add("attr:" + attr + "=" + value.trim());
                }
            }
        }
        
        // Add structural position if configured
        if (Boolean.TRUE.equals(settings.getIncludePosition())) {
            // Use a simplified structural signature
            if (selector != null) {
                String structuralSig = selector.replaceAll(":\\w+\\([^)]*\\)", "").replaceAll("#[^\\s>+~.]+", "");
                if (!structuralSig.trim().isEmpty()) {
                    components.add("struct:" + structuralSig.trim());
                }
            }
        }
        
        // Add parent context (simplified version)
        if (selector != null) {
            String[] parts = selector.split("\\s+");
            if (parts.length > 1) {
                String parentContext = String.join(" ", Arrays.copyOf(parts, Math.min(2, parts.length - 1)));
                components.add("parent:" + parentContext);
            }
        }
        
        // Combine components and hash
        String combined = String.join("|", components);
        this.fingerprint = hashString(combined);
        
        // Calculate confidence based on number of components
        this.fingerprintConfidence = Math.min(1.0, components.size() / 5.0);
        
        return this.fingerprint;
    }
    
    /**
     * Generate a basic fingerprint when no configuration is available
     */
    private String generateBasicFingerprint() {
        List<String> components = new ArrayList<>();
        
        if (tagName != null) {
            components.add("tag:" + tagName);
        }
        
        if (text != null && !text.trim().isEmpty()) {
            String normalizedText = text.trim().replaceAll("\\s+", " ");
            if (normalizedText.length() > 30) {
                normalizedText = normalizedText.substring(0, 30) + "...";
            }
            components.add("text:" + normalizedText);
        }
        
        if (attributes != null) {
            String id = attributes.get("id");
            if (id != null && !id.trim().isEmpty()) {
                components.add("id:" + id.trim());
            }
            
            String className = attributes.get("class");
            if (className != null && !className.trim().isEmpty()) {
                components.add("class:" + className.trim());
            }
        }
        
        String combined = String.join("|", components);
        this.fingerprint = hashString(combined);
        this.fingerprintConfidence = Math.min(1.0, components.size() / 3.0);
        
        return this.fingerprint;
    }
    
    /**
     * Generate structural fingerprint (focused on DOM structure)
     */
    public String generateStructuralFingerprint() {
        List<String> components = new ArrayList<>();
        
        if (tagName != null) {
            components.add(tagName);
        }
        
        if (attributes != null) {
            String id = attributes.get("id");
            if (id != null && !id.trim().isEmpty()) {
                components.add("id:" + id.trim());
            }
        }
        
        if (selector != null) {
            String simplified = selector.replaceAll(":\\w+\\([^)]*\\)", "").replaceAll("#[^\\s>+~.]+", "");
            if (!simplified.trim().isEmpty()) {
                components.add("path:" + simplified.trim());
            }
        }
        
        String combined = String.join("|", components);
        this.structuralFingerprint = hashString(combined);
        return this.structuralFingerprint;
    }
    
    /**
     * Generate content fingerprint (focused on text content and visible attributes)
     */
    public String generateContentFingerprint() {
        List<String> components = new ArrayList<>();
        
        if (text != null && !text.trim().isEmpty()) {
            String normalizedText = text.trim().replaceAll("\\s+", " ");
            components.add("text:" + normalizedText);
        }
        
        if (attributes != null) {
            String[] contentAttrs = {"alt", "title", "placeholder", "aria-label"};
            for (String attr : contentAttrs) {
                String value = attributes.get(attr);
                if (value != null && !value.trim().isEmpty()) {
                    components.add(attr + ":" + value.trim());
                }
            }
        }
        
        String combined = String.join("|", components);
        this.contentFingerprint = hashString(combined);
        return this.contentFingerprint;
    }
    
    /**
     * Hash a string using SHA-256 and return first 16 characters
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s[selector=%s, text=%s, fingerprint=%s]", 
                tagName, 
                selector, 
                text != null && text.length() > 30 ? text.substring(0, 27) + "..." : text,
                fingerprint != null ? fingerprint.substring(0, Math.min(8, fingerprint.length())) : "none");
    }
}
