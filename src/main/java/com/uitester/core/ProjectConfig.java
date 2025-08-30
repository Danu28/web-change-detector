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

    // --- New extensible sections (Phase 1 refactor) ---
    @JsonProperty("crawlerSettings")
    private CrawlerSettings crawlerSettings;

    @JsonProperty("matchingSettings")
    private MatchingSettings matchingSettings;

    @JsonProperty("classificationSettings")
    private ClassificationSettings classificationSettings;

    @JsonProperty("structuralAnalysisSettings")
    private StructuralAnalysisSettings structuralAnalysisSettings;

    @JsonProperty("reportingSettings")
    private ReportingSettings reportingSettings;

    @JsonProperty("outputSettings")
    private OutputSettings outputSettings;

    @JsonProperty("flags")
    private Flags flags;
    
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

    public CrawlerSettings getCrawlerSettings() { return crawlerSettings; }
    public void setCrawlerSettings(CrawlerSettings crawlerSettings) { this.crawlerSettings = crawlerSettings; }

    public MatchingSettings getMatchingSettings() { return matchingSettings; }
    public void setMatchingSettings(MatchingSettings matchingSettings) { this.matchingSettings = matchingSettings; }

    public ClassificationSettings getClassificationSettings() { return classificationSettings; }
    public void setClassificationSettings(ClassificationSettings classificationSettings) { this.classificationSettings = classificationSettings; }

    public StructuralAnalysisSettings getStructuralAnalysisSettings() { return structuralAnalysisSettings; }
    public void setStructuralAnalysisSettings(StructuralAnalysisSettings structuralAnalysisSettings) { this.structuralAnalysisSettings = structuralAnalysisSettings; }

    public ReportingSettings getReportingSettings() { return reportingSettings; }
    public void setReportingSettings(ReportingSettings reportingSettings) { this.reportingSettings = reportingSettings; }

    public OutputSettings getOutputSettings() { return outputSettings; }
    public void setOutputSettings(OutputSettings outputSettings) { this.outputSettings = outputSettings; }

    public Flags getFlags() { return flags; }
    public void setFlags(Flags flags) { this.flags = flags; }
    
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

        if (matchingSettings != null) {
            validateRange(result, "tagWeight", matchingSettings.getTagWeight(), 0.0, 1.0);
            validateRange(result, "textWeight", matchingSettings.getTextWeight(), 0.0, 1.0);
            validateRange(result, "structuralWeight", matchingSettings.getStructuralWeight(), 0.0, 1.0);
            validateRange(result, "contentWeight", matchingSettings.getContentWeight(), 0.0, 1.0);
            // Sum check (warn, not error)
            Double sum = safeSum(matchingSettings.getTagWeight(), matchingSettings.getTextWeight(), matchingSettings.getStructuralWeight(), matchingSettings.getContentWeight());
            if (sum != null && Math.abs(sum - 1.0) > 0.05) {
                result.addWarning(String.format("Matching weight sum ~= %.2f (recommended 1.0)", sum));
            }
            validateRange(result, "fuzzyMinConfidence", matchingSettings.getFuzzyMinConfidence(), 0.0, 1.0);
        }

        if (classificationSettings != null && classificationSettings.getMagnitudeThresholds() != null) {
            for (Map.Entry<String, Double> e : classificationSettings.getMagnitudeThresholds().entrySet()) {
                validateRange(result, "magnitudeThresholds." + e.getKey(), e.getValue(), 0.0, 1.0);
            }
        }

        if (structuralAnalysisSettings != null) {
            validatePositive(result, "listMinItems", structuralAnalysisSettings.getListMinItems());
            validatePositive(result, "gridMinItems", structuralAnalysisSettings.getGridMinItems());
            validatePositive(result, "tableMinRows", structuralAnalysisSettings.getTableMinRows());
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

    // ================= New Config Sections =================

    /** Crawler related settings (element extraction) */
    public static class CrawlerSettings {
        @JsonProperty("cssProperties") private List<String> cssProperties; // overrides stylesToCapture if present
        @JsonProperty("attributesToExtract") private List<String> attributesToExtract; // overrides attributesToCapture
        @JsonProperty("visibilityFilter") private Boolean visibilityFilter; // whether to skip invisible elements
        @JsonProperty("maxDepth") private Integer maxDepth; // optional future use
        @JsonProperty("throttleMs") private Integer throttleMs; // delay between element reads

        public List<String> getCssProperties() { return cssProperties; }
        public void setCssProperties(List<String> cssProperties) { this.cssProperties = cssProperties; }
        public List<String> getAttributesToExtract() { return attributesToExtract; }
        public void setAttributesToExtract(List<String> attributesToExtract) { this.attributesToExtract = attributesToExtract; }
        public Boolean getVisibilityFilter() { return visibilityFilter; }
        public void setVisibilityFilter(Boolean visibilityFilter) { this.visibilityFilter = visibilityFilter; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public Integer getThrottleMs() { return throttleMs; }
        public void setThrottleMs(Integer throttleMs) { this.throttleMs = throttleMs; }
    }

    /** Element matching weights & thresholds */
    public static class MatchingSettings {
        @JsonProperty("tagWeight") private Double tagWeight; // default 0.3
        @JsonProperty("textWeight") private Double textWeight; // default 0.4
        @JsonProperty("structuralWeight") private Double structuralWeight; // default 0.2
        @JsonProperty("contentWeight") private Double contentWeight; // default 0.1
        @JsonProperty("fuzzyMinConfidence") private Double fuzzyMinConfidence; // default 0.6
        @JsonProperty("semanticPriceConfidence") private Double semanticPriceConfidence; // default 0.75
        @JsonProperty("enableSemanticPrice") private Boolean enableSemanticPrice; // default true

        public Double getTagWeight() { return tagWeight; }
        public void setTagWeight(Double tagWeight) { this.tagWeight = tagWeight; }
        public Double getTextWeight() { return textWeight; }
        public void setTextWeight(Double textWeight) { this.textWeight = textWeight; }
        public Double getStructuralWeight() { return structuralWeight; }
        public void setStructuralWeight(Double structuralWeight) { this.structuralWeight = structuralWeight; }
        public Double getContentWeight() { return contentWeight; }
        public void setContentWeight(Double contentWeight) { this.contentWeight = contentWeight; }
        public Double getFuzzyMinConfidence() { return fuzzyMinConfidence; }
        public void setFuzzyMinConfidence(Double fuzzyMinConfidence) { this.fuzzyMinConfidence = fuzzyMinConfidence; }
        public Double getSemanticPriceConfidence() { return semanticPriceConfidence; }
        public void setSemanticPriceConfidence(Double semanticPriceConfidence) { this.semanticPriceConfidence = semanticPriceConfidence; }
        public Boolean getEnableSemanticPrice() { return enableSemanticPrice; }
        public void setEnableSemanticPrice(Boolean enableSemanticPrice) { this.enableSemanticPrice = enableSemanticPrice; }
    }

    /** Classification rule settings */
    public static class ClassificationSettings {
        @JsonProperty("interactiveKeywords") private List<String> interactiveKeywords; // default [button,input,form,a,select]
    @JsonProperty("accessibilityKeywords") private List<String> accessibilityKeywords; // default [aria,alt,role]
        @JsonProperty("magnitudeThresholds") private Map<String, Double> magnitudeThresholds; // per category thresholds
        @JsonProperty("rules") private List<Map<String, Object>> rules; // future rule engine (conditions -> label)

        public List<String> getInteractiveKeywords() { return interactiveKeywords; }
        public void setInteractiveKeywords(List<String> interactiveKeywords) { this.interactiveKeywords = interactiveKeywords; }
    public List<String> getAccessibilityKeywords() { return accessibilityKeywords; }
    public void setAccessibilityKeywords(List<String> accessibilityKeywords) { this.accessibilityKeywords = accessibilityKeywords; }
        public Map<String, Double> getMagnitudeThresholds() { return magnitudeThresholds; }
        public void setMagnitudeThresholds(Map<String, Double> magnitudeThresholds) { this.magnitudeThresholds = magnitudeThresholds; }
        public List<Map<String, Object>> getRules() { return rules; }
        public void setRules(List<Map<String, Object>> rules) { this.rules = rules; }
    }

    /** Structural analysis tuning */
    public static class StructuralAnalysisSettings {
        @JsonProperty("listMinItems") private Integer listMinItems; // default 3
        @JsonProperty("gridMinItems") private Integer gridMinItems; // default 4
        @JsonProperty("tableMinRows") private Integer tableMinRows; // default 2
        @JsonProperty("maxDepthForParentSearch") private Integer maxDepthForParentSearch; // heuristic cap
        @JsonProperty("selectorSimilarityCapDepth") private Integer selectorSimilarityCapDepth; // avoid deep similarity loops
    @JsonProperty("patternConfidence") private Map<String, Double> patternConfidence; // navigation,list,form,table,css-grid

        public Integer getListMinItems() { return listMinItems; }
        public void setListMinItems(Integer listMinItems) { this.listMinItems = listMinItems; }
        public Integer getGridMinItems() { return gridMinItems; }
        public void setGridMinItems(Integer gridMinItems) { this.gridMinItems = gridMinItems; }
        public Integer getTableMinRows() { return tableMinRows; }
        public void setTableMinRows(Integer tableMinRows) { this.tableMinRows = tableMinRows; }
        public Integer getMaxDepthForParentSearch() { return maxDepthForParentSearch; }
        public void setMaxDepthForParentSearch(Integer maxDepthForParentSearch) { this.maxDepthForParentSearch = maxDepthForParentSearch; }
        public Integer getSelectorSimilarityCapDepth() { return selectorSimilarityCapDepth; }
        public void setSelectorSimilarityCapDepth(Integer selectorSimilarityCapDepth) { this.selectorSimilarityCapDepth = selectorSimilarityCapDepth; }
    public Map<String, Double> getPatternConfidence() { return patternConfidence; }
    public void setPatternConfidence(Map<String, Double> patternConfidence) { this.patternConfidence = patternConfidence; }
    }

    /** Reporting customization */
    public static class ReportingSettings {
        @JsonProperty("severityOrder") private List<String> severityOrder; // e.g. [critical,structural,text,cosmetic,noise]
        @JsonProperty("badgeColors") private Map<String, String> badgeColors; // override colors
        @JsonProperty("autoScrollTo") private String autoScrollTo; // severity to focus
        @JsonProperty("enableConfidenceBar") private Boolean enableConfidenceBar; // default true
    @JsonProperty("themeColors") private Map<String, String> themeColors; // accentPrimary, accentSecondary, backgroundSoft, panelBackground, borderColor
    @JsonProperty("confidenceLevels") private List<Map<String, Object>> confidenceLevels; // [{min:0.9,label:Very High},...]

        public List<String> getSeverityOrder() { return severityOrder; }
        public void setSeverityOrder(List<String> severityOrder) { this.severityOrder = severityOrder; }
        public Map<String, String> getBadgeColors() { return badgeColors; }
        public void setBadgeColors(Map<String, String> badgeColors) { this.badgeColors = badgeColors; }
        public String getAutoScrollTo() { return autoScrollTo; }
        public void setAutoScrollTo(String autoScrollTo) { this.autoScrollTo = autoScrollTo; }
        public Boolean getEnableConfidenceBar() { return enableConfidenceBar; }
        public void setEnableConfidenceBar(Boolean enableConfidenceBar) { this.enableConfidenceBar = enableConfidenceBar; }
    public Map<String, String> getThemeColors() { return themeColors; }
    public void setThemeColors(Map<String, String> themeColors) { this.themeColors = themeColors; }
    public List<Map<String, Object>> getConfidenceLevels() { return confidenceLevels; }
    public void setConfidenceLevels(List<Map<String, Object>> confidenceLevels) { this.confidenceLevels = confidenceLevels; }
    }

    /** Output / path templating */
    public static class OutputSettings {
        @JsonProperty("directoryTemplate") private String directoryTemplate; // e.g. "{section}_{baselineHost}-vs-{currentHost}"
        @JsonProperty("baselineFile") private String baselineFile;
        @JsonProperty("currentFile") private String currentFile;
        @JsonProperty("changesFile") private String changesFile;
        @JsonProperty("reportFile") private String reportFile;
    @JsonProperty("reportSimpleFile") private String reportSimpleFile; // new explicit simple report name

        public String getDirectoryTemplate() { return directoryTemplate; }
        public void setDirectoryTemplate(String directoryTemplate) { this.directoryTemplate = directoryTemplate; }
        public String getBaselineFile() { return baselineFile; }
        public void setBaselineFile(String baselineFile) { this.baselineFile = baselineFile; }
        public String getCurrentFile() { return currentFile; }
        public void setCurrentFile(String currentFile) { this.currentFile = currentFile; }
        public String getChangesFile() { return changesFile; }
        public void setChangesFile(String changesFile) { this.changesFile = changesFile; }
        public String getReportFile() { return reportFile; }
        public void setReportFile(String reportFile) { this.reportFile = reportFile; }
    public String getReportSimpleFile() { return reportSimpleFile; }
    public void setReportSimpleFile(String reportSimpleFile) { this.reportSimpleFile = reportSimpleFile; }
    }

    /** Feature flags */
    public static class Flags {
        @JsonProperty("enableStructuralAnalysis") private Boolean enableStructuralAnalysis; // default true
        @JsonProperty("enableSemanticMatching") private Boolean enableSemanticMatching; // default true
        @JsonProperty("enableAdvancedClassification") private Boolean enableAdvancedClassification; // default false

        public Boolean getEnableStructuralAnalysis() { return enableStructuralAnalysis; }
        public void setEnableStructuralAnalysis(Boolean enableStructuralAnalysis) { this.enableStructuralAnalysis = enableStructuralAnalysis; }
        public Boolean getEnableSemanticMatching() { return enableSemanticMatching; }
        public void setEnableSemanticMatching(Boolean enableSemanticMatching) { this.enableSemanticMatching = enableSemanticMatching; }
        public Boolean getEnableAdvancedClassification() { return enableAdvancedClassification; }
        public void setEnableAdvancedClassification(Boolean enableAdvancedClassification) { this.enableAdvancedClassification = enableAdvancedClassification; }
    }

    // ================= Helper =================
    private Double safeSum(Double... vals) {
        double sum = 0.0; boolean any = false;
        for (Double v : vals) { if (v != null) { sum += v; any = true; } }
        return any ? sum : null;
    }
}
