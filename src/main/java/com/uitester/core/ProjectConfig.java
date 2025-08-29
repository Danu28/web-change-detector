package com.uitester.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuration model for UI testing project settings.
 * This class represents the structure of config.json file.
 */
public class ProjectConfig {
    
    @JsonProperty("captureSettings")
    private CaptureSettings captureSettings;
    
    @JsonProperty("comparisonSettings")
    private ComparisonSettings comparisonSettings;
    
    @JsonProperty("fingerprintSettings")
    private FingerprintSettings fingerprintSettings;
    
    @JsonProperty("performanceSettings")
    private PerformanceSettings performanceSettings;
    
    // Default constructor
    public ProjectConfig() {
    }
    
    // Getters and setters
    public CaptureSettings getCaptureSettings() {
        return captureSettings;
    }
    
    public void setCaptureSettings(CaptureSettings captureSettings) {
        this.captureSettings = captureSettings;
    }
    
    public ComparisonSettings getComparisonSettings() {
        return comparisonSettings;
    }
    
    public void setComparisonSettings(ComparisonSettings comparisonSettings) {
        this.comparisonSettings = comparisonSettings;
    }
    
    public FingerprintSettings getFingerprintSettings() {
        return fingerprintSettings;
    }
    
    public void setFingerprintSettings(FingerprintSettings fingerprintSettings) {
        this.fingerprintSettings = fingerprintSettings;
    }
    
    public PerformanceSettings getPerformanceSettings() {
        return performanceSettings;
    }
    
    public void setPerformanceSettings(PerformanceSettings performanceSettings) {
        this.performanceSettings = performanceSettings;
    }
    
    /**
     * Validates the loaded configuration and returns validation results.
     * 
     * @return ValidationResult containing any errors or warnings
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        
        if (comparisonSettings != null) {
            validateRange(result, "textSimilarityThreshold", comparisonSettings.getTextSimilarityThreshold(), 0.0, 1.0);
            validateRange(result, "numericChangeThreshold", comparisonSettings.getNumericChangeThreshold(), 0.0, 1.0);
            validateRange(result, "colorChangeThreshold", comparisonSettings.getColorChangeThreshold(), 0.0, 1.0);
        }
        
        if (performanceSettings != null) {
            validatePositive(result, "maxElements", performanceSettings.getMaxElements());
            validatePositive(result, "maxProcessingTimeMs", performanceSettings.getMaxProcessingTimeMs());
            validatePositive(result, "memoryWarningThresholdMb", performanceSettings.getMemoryWarningThresholdMb());
        }
        
        return result;
    }
    
    private void validateRange(ValidationResult result, String field, Double value, double min, double max) {
        if (value != null && (value < min || value > max)) {
            result.addError(String.format("%s must be between %.2f and %.2f, got %.2f", field, min, max, value));
        }
    }
    
    private void validatePositive(ValidationResult result, String field, Integer value) {
        if (value != null && value <= 0) {
            result.addError(String.format("%s must be positive, got %d", field, value));
        }
    }
    
    /**
     * Nested class for capture-related settings
     */
    public static class CaptureSettings {
        @JsonProperty("attributesToCapture")
        private List<String> attributesToCapture;
        
        @JsonProperty("stylesToCapture")
        private List<String> stylesToCapture;
        
        @JsonProperty("ignoreAttributePatterns")
        private List<String> ignoreAttributePatterns;
        
        @JsonProperty("captureOnlyViewport")
        private Boolean captureOnlyViewport;
        
        @JsonProperty("maxTextLength")
        private Integer maxTextLength;
        
        @JsonProperty("ignoreEmptyText")
        private Boolean ignoreEmptyText;
        
        // Getters and setters
        public List<String> getAttributesToCapture() {
            return attributesToCapture;
        }
        
        public void setAttributesToCapture(List<String> attributesToCapture) {
            this.attributesToCapture = attributesToCapture;
        }
        
        public List<String> getStylesToCapture() {
            return stylesToCapture;
        }
        
        public void setStylesToCapture(List<String> stylesToCapture) {
            this.stylesToCapture = stylesToCapture;
        }
        
        public List<String> getIgnoreAttributePatterns() {
            return ignoreAttributePatterns;
        }
        
        public void setIgnoreAttributePatterns(List<String> ignoreAttributePatterns) {
            this.ignoreAttributePatterns = ignoreAttributePatterns;
        }
        
        public Boolean getCaptureOnlyViewport() {
            return captureOnlyViewport;
        }
        
        public void setCaptureOnlyViewport(Boolean captureOnlyViewport) {
            this.captureOnlyViewport = captureOnlyViewport;
        }
        
        public Integer getMaxTextLength() {
            return maxTextLength;
        }
        
        public void setMaxTextLength(Integer maxTextLength) {
            this.maxTextLength = maxTextLength;
        }
        
        public Boolean getIgnoreEmptyText() {
            return ignoreEmptyText;
        }
        
        public void setIgnoreEmptyText(Boolean ignoreEmptyText) {
            this.ignoreEmptyText = ignoreEmptyText;
        }
    }
    
    /**
     * Nested class for comparison-related settings
     */
    public static class ComparisonSettings {
        @JsonProperty("textSimilarityThreshold")
        private Double textSimilarityThreshold;
        
        @JsonProperty("numericChangeThreshold")
        private Double numericChangeThreshold;
        
        @JsonProperty("colorChangeThreshold")
        private Double colorChangeThreshold;
        
        @JsonProperty("ignoreStyleChanges")
        private List<String> ignoreStyleChanges;
        
        @JsonProperty("styleCategories")
        private Map<String, List<String>> styleCategories;
        
        @JsonProperty("changeMagnitudeWeights")
        private Map<String, Double> changeMagnitudeWeights;
        
        // Getters and setters
        public Double getTextSimilarityThreshold() {
            return textSimilarityThreshold;
        }
        
        public void setTextSimilarityThreshold(Double textSimilarityThreshold) {
            this.textSimilarityThreshold = textSimilarityThreshold;
        }
        
        public Double getNumericChangeThreshold() {
            return numericChangeThreshold;
        }
        
        public void setNumericChangeThreshold(Double numericChangeThreshold) {
            this.numericChangeThreshold = numericChangeThreshold;
        }
        
        public Double getColorChangeThreshold() {
            return colorChangeThreshold;
        }
        
        public void setColorChangeThreshold(Double colorChangeThreshold) {
            this.colorChangeThreshold = colorChangeThreshold;
        }
        
        public List<String> getIgnoreStyleChanges() {
            return ignoreStyleChanges;
        }
        
        public void setIgnoreStyleChanges(List<String> ignoreStyleChanges) {
            this.ignoreStyleChanges = ignoreStyleChanges;
        }
        
        public Map<String, List<String>> getStyleCategories() {
            return styleCategories;
        }
        
        public void setStyleCategories(Map<String, List<String>> styleCategories) {
            this.styleCategories = styleCategories;
        }
        
        public Map<String, Double> getChangeMagnitudeWeights() {
            return changeMagnitudeWeights;
        }
        
        public void setChangeMagnitudeWeights(Map<String, Double> changeMagnitudeWeights) {
            this.changeMagnitudeWeights = changeMagnitudeWeights;
        }
    }
    
    /**
     * Nested class for fingerprint-related settings
     */
    public static class FingerprintSettings {
        @JsonProperty("useTagName")
        private Boolean useTagName;
        
        @JsonProperty("useTextContent")
        private Boolean useTextContent;
        
        @JsonProperty("useAttributes")
        private List<String> useAttributes;
        
        @JsonProperty("textContentMaxLength")
        private Integer textContentMaxLength;
        
        @JsonProperty("normalizeWhitespace")
        private Boolean normalizeWhitespace;
        
        @JsonProperty("caseSensitive")
        private Boolean caseSensitive;
        
        @JsonProperty("includePosition")
        private Boolean includePosition;
        
        // Getters and setters
        public Boolean getUseTagName() {
            return useTagName;
        }
        
        public void setUseTagName(Boolean useTagName) {
            this.useTagName = useTagName;
        }
        
        public Boolean getUseTextContent() {
            return useTextContent;
        }
        
        public void setUseTextContent(Boolean useTextContent) {
            this.useTextContent = useTextContent;
        }
        
        public List<String> getUseAttributes() {
            return useAttributes;
        }
        
        public void setUseAttributes(List<String> useAttributes) {
            this.useAttributes = useAttributes;
        }
        
        public Integer getTextContentMaxLength() {
            return textContentMaxLength;
        }
        
        public void setTextContentMaxLength(Integer textContentMaxLength) {
            this.textContentMaxLength = textContentMaxLength;
        }
        
        public Boolean getNormalizeWhitespace() {
            return normalizeWhitespace;
        }
        
        public void setNormalizeWhitespace(Boolean normalizeWhitespace) {
            this.normalizeWhitespace = normalizeWhitespace;
        }
        
        public Boolean getCaseSensitive() {
            return caseSensitive;
        }
        
        public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
        
        public Boolean getIncludePosition() {
            return includePosition;
        }
        
        public void setIncludePosition(Boolean includePosition) {
            this.includePosition = includePosition;
        }
    }
    
    /**
     * Nested class for performance-related settings
     */
    public static class PerformanceSettings {
        @JsonProperty("maxElements")
        private Integer maxElements;
        
        @JsonProperty("maxProcessingTimeMs")
        private Integer maxProcessingTimeMs;
        
        @JsonProperty("enableParallelProcessing")
        private Boolean enableParallelProcessing;
        
        @JsonProperty("memoryWarningThresholdMb")
        private Integer memoryWarningThresholdMb;
        
        // Getters and setters
        public Integer getMaxElements() {
            return maxElements;
        }
        
        public void setMaxElements(Integer maxElements) {
            this.maxElements = maxElements;
        }
        
        public Integer getMaxProcessingTimeMs() {
            return maxProcessingTimeMs;
        }
        
        public void setMaxProcessingTimeMs(Integer maxProcessingTimeMs) {
            this.maxProcessingTimeMs = maxProcessingTimeMs;
        }
        
        public Boolean getEnableParallelProcessing() {
            return enableParallelProcessing;
        }
        
        public void setEnableParallelProcessing(Boolean enableParallelProcessing) {
            this.enableParallelProcessing = enableParallelProcessing;
        }
        
        public Integer getMemoryWarningThresholdMb() {
            return memoryWarningThresholdMb;
        }
        
        public void setMemoryWarningThresholdMb(Integer memoryWarningThresholdMb) {
            this.memoryWarningThresholdMb = memoryWarningThresholdMb;
        }
    }
    
    /**
     * Simple validation result class
     */
    public static class ValidationResult {
        private List<String> errors = new java.util.ArrayList<>();
        private List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}
