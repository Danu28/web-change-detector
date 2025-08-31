package com.uitester.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Utility class for loading and validating project configuration from config.json.
 * Provides robust error handling and fallback to default values.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_CONFIG_PATH = "config.json";
    
    private static ProjectConfig cachedConfig = null;
    private static final Object configLock = new Object();
    
    /**
     * Load configuration from the default config.json file in classpath.
     * 
     * @return ProjectConfig object with validated settings
     */
    public static ProjectConfig loadConfig() {
        return loadConfig(DEFAULT_CONFIG_PATH);
    }
    
    /**
     * Load configuration from specified path.
     * 
     * @param configPath Path to configuration file
     * @return ProjectConfig object with validated settings
     */
    public static ProjectConfig loadConfig(String configPath) {
        synchronized (configLock) {
            if (cachedConfig != null) {
                return cachedConfig;
            }
            
            ProjectConfig config = null;
            
            try {
                config = loadConfigFromClasspath(configPath);
                logger.info("Successfully loaded configuration from {}", configPath);
                // Populate defaults for any newly introduced sections/fields
                applyNewSectionDefaults(config);
            } catch (Exception e) {
                logger.warn("Failed to load configuration from {}: {}. Using default configuration.", 
                           configPath, e.getMessage());
                config = createDefaultConfig();
            }
            
            // Validate configuration
            ProjectConfig.ValidationResult validation = config.validate();
            if (validation.hasErrors()) {
                logger.error("Configuration validation failed with errors: {}", validation.getErrors());
                logger.warn("Falling back to default configuration");
                config = createDefaultConfig();
            } else if (!validation.getWarnings().isEmpty()) {
                logger.warn("Configuration validation warnings: {}", validation.getWarnings());
            }
            
            cachedConfig = config;
            return config;
        }
    }
    
    /**
     * Clear cached configuration (useful for testing).
     */
    public static void clearCache() {
        synchronized (configLock) {
            cachedConfig = null;
        }
    }
    
    /**
     * Load configuration from classpath resource.
     * 
     * @param resourcePath Path to resource
     * @return Loaded ProjectConfig
     * @throws IOException If loading fails
     */
    private static ProjectConfig loadConfigFromClasspath(String resourcePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Configuration file not found in classpath: " + resourcePath);
            }
            
            return objectMapper.readValue(inputStream, ProjectConfig.class);
        }
    }
    
    /**
     * Create a default configuration with sensible fallback values.
     * 
     * @return Default ProjectConfig
     */
    private static ProjectConfig createDefaultConfig() {
        logger.info("Creating default configuration");
        
        ProjectConfig config = new ProjectConfig();
        
        // Default capture settings
        ProjectConfig.CaptureSettings captureSettings = new ProjectConfig.CaptureSettings();
        captureSettings.setAttributesToCapture(Arrays.asList(
            "id", "class", "name", "href", "src", "alt", "title", "type", "value", "placeholder"
        ));
        captureSettings.setStylesToCapture(Arrays.asList(
            "color", "background-color", "font-size", "font-weight", "display", "visibility",
            "position", "width", "height", "margin", "padding", "border", "text-align",
            "line-height", "opacity", "z-index"
        ));
        captureSettings.setIgnoreAttributePatterns(Arrays.asList(
            "data-reactid.*", "data-react-.*", "_ngcontent-.*", "_nghost-.*"
        ));
        captureSettings.setCaptureOnlyViewport(true);
        captureSettings.setMaxTextLength(1000);
        captureSettings.setIgnoreEmptyText(true);
        config.setCaptureSettings(captureSettings);
        
        // Default comparison settings
        ProjectConfig.ComparisonSettings comparisonSettings = new ProjectConfig.ComparisonSettings();
    comparisonSettings.setTextSimilarityThreshold(Defaults.TEXT_SIMILARITY_DEFAULT);
        comparisonSettings.setNumericChangeThreshold(0.05);
        comparisonSettings.setColorChangeThreshold(0.1);
        comparisonSettings.setIgnoreStyleChanges(Arrays.asList("font-family"));
        
        Map<String, List<String>> styleCategories = new HashMap<>();
        styleCategories.put("layout", Arrays.asList("width", "height", "margin", "padding", "display", "position"));
        styleCategories.put("typography", Arrays.asList("font-size", "font-weight", "font-family", "line-height", "text-align"));
        styleCategories.put("appearance", Arrays.asList("color", "background-color", "border", "opacity"));
        styleCategories.put("positioning", Arrays.asList("z-index", "visibility"));
        comparisonSettings.setStyleCategories(styleCategories);
        
        Map<String, Double> changeMagnitudeWeights = new HashMap<>();
        changeMagnitudeWeights.put("layout", 0.8);
        changeMagnitudeWeights.put("typography", 0.6);
        changeMagnitudeWeights.put("appearance", 0.7);
        changeMagnitudeWeights.put("positioning", 0.5);
        comparisonSettings.setChangeMagnitudeWeights(changeMagnitudeWeights);
        
        config.setComparisonSettings(comparisonSettings);
        
        // Default fingerprint settings
        ProjectConfig.FingerprintSettings fingerprintSettings = new ProjectConfig.FingerprintSettings();
        fingerprintSettings.setUseTagName(true);
        fingerprintSettings.setUseTextContent(true);
        fingerprintSettings.setUseAttributes(Arrays.asList("id", "class", "name", "data-testid"));
        fingerprintSettings.setTextContentMaxLength(100);
        fingerprintSettings.setNormalizeWhitespace(true);
        fingerprintSettings.setCaseSensitive(false);
        fingerprintSettings.setIncludePosition(false);
        config.setFingerprintSettings(fingerprintSettings);
        
        // Default performance settings
        ProjectConfig.PerformanceSettings performanceSettings = new ProjectConfig.PerformanceSettings();
        performanceSettings.setMaxElements(10000);
        performanceSettings.setMaxProcessingTimeMs(300000);
        performanceSettings.setEnableParallelProcessing(true);
        performanceSettings.setMemoryWarningThresholdMb(512);
        config.setPerformanceSettings(performanceSettings);

        // Populate defaults for new modular sections
        applyNewSectionDefaults(config);
        
        return config;
    }

    /**
     * Apply defaults to newly added configuration sections when missing to maintain backward compatibility.
     */
    private static void applyNewSectionDefaults(ProjectConfig config) {
        if (config.getCrawlerSettings() == null) {
            ProjectConfig.CrawlerSettings cs = new ProjectConfig.CrawlerSettings();
            // Migrate legacy captureSettings if present
            if (config.getCaptureSettings() != null) {
                cs.setStylesToCapture(config.getCaptureSettings().getStylesToCapture());
                cs.setAttributesToCapture(config.getCaptureSettings().getAttributesToCapture());
                cs.setIgnoreAttributePatterns(config.getCaptureSettings().getIgnoreAttributePatterns());
                cs.setCaptureOnlyViewport(config.getCaptureSettings().getCaptureOnlyViewport());
                cs.setMaxTextLength(config.getCaptureSettings().getMaxTextLength());
                cs.setIgnoreEmptyText(config.getCaptureSettings().getIgnoreEmptyText());
            }
            cs.setVisibilityFilter(Boolean.TRUE);
            cs.setThrottleMs(0);
            config.setCrawlerSettings(cs);
        } else if (config.getCaptureSettings() != null) {
            // Both present: prefer crawlerSettings and log deprecation by adding a warning classificationSettings rules list.
            // We can't log here without logger; consider future injection. For now we migrate missing pieces only.
            ProjectConfig.CrawlerSettings cs = config.getCrawlerSettings();
            if (cs.getStylesToCapture() == null) cs.setStylesToCapture(config.getCaptureSettings().getStylesToCapture());
            if (cs.getAttributesToCapture() == null) cs.setAttributesToCapture(config.getCaptureSettings().getAttributesToCapture());
            if (cs.getIgnoreAttributePatterns() == null) cs.setIgnoreAttributePatterns(config.getCaptureSettings().getIgnoreAttributePatterns());
            if (cs.getCaptureOnlyViewport() == null) cs.setCaptureOnlyViewport(config.getCaptureSettings().getCaptureOnlyViewport());
            if (cs.getMaxTextLength() == null) cs.setMaxTextLength(config.getCaptureSettings().getMaxTextLength());
            if (cs.getIgnoreEmptyText() == null) cs.setIgnoreEmptyText(config.getCaptureSettings().getIgnoreEmptyText());
        }
        // Null out legacy block to avoid downstream ambiguity
        config.setCaptureSettings(null);

        if (config.getMatchingSettings() == null) {
            ProjectConfig.MatchingSettings ms = new ProjectConfig.MatchingSettings();
            ms.setTagWeight(Defaults.MATCH_TAG_WEIGHT);
            ms.setTextWeight(Defaults.MATCH_TEXT_WEIGHT);
            ms.setStructuralWeight(Defaults.MATCH_STRUCTURAL_WEIGHT);
            ms.setContentWeight(Defaults.MATCH_CONTENT_WEIGHT);
            ms.setFuzzyMinConfidence(Defaults.MATCH_FUZZY_MIN_CONF);
            ms.setSemanticPriceConfidence(Defaults.MATCH_SEMANTIC_PRICE_CONF);
            ms.setEnableSemanticPrice(Boolean.TRUE);
            config.setMatchingSettings(ms);
        }

        if (config.getClassificationSettings() == null) {
            ProjectConfig.ClassificationSettings cls = new ProjectConfig.ClassificationSettings();
            cls.setInteractiveKeywords(Arrays.asList("button", "input", "form", "a", "select"));
            cls.setAccessibilityKeywords(Arrays.asList("aria", "alt", "role"));
            Map<String, Double> thresholds = new HashMap<>();
            thresholds.put("textCritical", Defaults.CLASS_TEXT_CRITICAL); // magnitude threshold for critical text change
            thresholds.put("colorCosmetic", Defaults.CLASS_COLOR_COSMETIC);
            thresholds.put("layoutCosmetic", Defaults.CLASS_LAYOUT_COSMETIC);
            thresholds.put("styleCritical", Defaults.CLASS_STYLE_CRITICAL); // externalized critical style magnitude
            thresholds.put("styleCosmetic", Defaults.CLASS_STYLE_COSMETIC); // cosmetic style lower bound
            thresholds.put("positionBase", Defaults.CLASS_POSITION_BASE_MAG); // base magnitude for position changes
            cls.setMagnitudeThresholds(thresholds);
            cls.setRules(new ArrayList<>()); // placeholder for future rule engine
            config.setClassificationSettings(cls);
        }
        // For existing configs missing new fields
        if (config.getClassificationSettings() != null) {
            ProjectConfig.ClassificationSettings cls = config.getClassificationSettings();
            if (cls.getAccessibilityKeywords() == null) {
                cls.setAccessibilityKeywords(Arrays.asList("aria", "alt", "role"));
            }
            if (cls.getMagnitudeThresholds() != null) {
                Map<String, Double> mt = cls.getMagnitudeThresholds();
                if (!mt.containsKey("styleCritical")) mt.put("styleCritical", Defaults.CLASS_STYLE_CRITICAL);
                if (!mt.containsKey("styleCosmetic")) mt.put("styleCosmetic", Defaults.CLASS_STYLE_COSMETIC);
                if (!mt.containsKey("positionBase")) mt.put("positionBase", Defaults.CLASS_POSITION_BASE_MAG);
            }
        }

        if (config.getStructuralAnalysisSettings() == null) {
            ProjectConfig.StructuralAnalysisSettings sas = new ProjectConfig.StructuralAnalysisSettings();
            sas.setListMinItems(3);
            sas.setGridMinItems(4);
            sas.setTableMinRows(2);
            sas.setMaxDepthForParentSearch(10);
            sas.setSelectorSimilarityCapDepth(10);
            Map<String, Double> pc = new HashMap<>();
            pc.put("navigation", 0.8);
            pc.put("list", 0.9);
            pc.put("form", 0.85);
            pc.put("table", 0.9);
            pc.put("css-grid", 0.7);
            sas.setPatternConfidence(pc);
            config.setStructuralAnalysisSettings(sas);
        }
        // Populate missing pattern confidences for existing configs
        if (config.getStructuralAnalysisSettings() != null && config.getStructuralAnalysisSettings().getPatternConfidence() == null) {
            Map<String, Double> pc = new HashMap<>();
            pc.put("navigation", 0.8);
            pc.put("list", 0.9);
            pc.put("form", 0.85);
            pc.put("table", 0.9);
            pc.put("css-grid", 0.7);
            config.getStructuralAnalysisSettings().setPatternConfidence(pc);
        }

        if (config.getReportingSettings() == null) {
            ProjectConfig.ReportingSettings rs = new ProjectConfig.ReportingSettings();
            rs.setSeverityOrder(Arrays.asList("critical", "structural", "text", "cosmetic", "noise"));
            Map<String, String> colors = new HashMap<>();
            colors.put("critical", "#e53e3e");
            colors.put("structural", "#dd6b20");
            colors.put("text", "#38a169");
            colors.put("cosmetic", "#3182ce");
            rs.setBadgeColors(colors);
            rs.setAutoScrollTo("critical");
            rs.setEnableConfidenceBar(Boolean.TRUE);
            Map<String, String> theme = new HashMap<>();
            theme.put("accentPrimary", "#667eea");
            theme.put("accentSecondary", "#764ba2");
            theme.put("backgroundSoft", "#f8fafc");
            theme.put("panelBackground", "#ffffff");
            theme.put("borderColor", "#e2e8f0");
            rs.setThemeColors(theme);
            List<Map<String, Object>> confLevels = new ArrayList<>();
            confLevels.add(mapOf("min", 0.9, "label", "Very High"));
            confLevels.add(mapOf("min", 0.75, "label", "High"));
            confLevels.add(mapOf("min", 0.5, "label", "Medium"));
            confLevels.add(mapOf("min", 0.25, "label", "Low"));
            confLevels.add(mapOf("min", 0.0, "label", "Very Low"));
            rs.setConfidenceLevels(confLevels);
            config.setReportingSettings(rs);
        }
        // Populate missing reporting enhancements
        if (config.getReportingSettings() != null) {
            ProjectConfig.ReportingSettings rs = config.getReportingSettings();
            if (rs.getThemeColors() == null) {
                Map<String, String> theme = new HashMap<>();
                theme.put("accentPrimary", "#667eea");
                theme.put("accentSecondary", "#764ba2");
                theme.put("backgroundSoft", "#f8fafc");
                theme.put("panelBackground", "#ffffff");
                theme.put("borderColor", "#e2e8f0");
                rs.setThemeColors(theme);
            }
            if (rs.getConfidenceLevels() == null) {
                List<Map<String, Object>> confLevels = new ArrayList<>();
                confLevels.add(mapOf("min", 0.9, "label", "Very High"));
                confLevels.add(mapOf("min", 0.75, "label", "High"));
                confLevels.add(mapOf("min", 0.5, "label", "Medium"));
                confLevels.add(mapOf("min", 0.25, "label", "Low"));
                confLevels.add(mapOf("min", 0.0, "label", "Very Low"));
                rs.setConfidenceLevels(confLevels);
            }
        }

        if (config.getOutputSettings() == null) {
            ProjectConfig.OutputSettings os = new ProjectConfig.OutputSettings();
            os.setDirectoryTemplate("{section}_{baselineHost}-vs-{currentHost}");
            os.setBaselineFile("baseline.json");
            os.setCurrentFile("current.json");
            os.setChangesFile("changes.json");
            os.setReportFile("report.html");
            os.setReportSimpleFile("report-simple.html");
            config.setOutputSettings(os);
        }
        if (config.getOutputSettings() != null && config.getOutputSettings().getReportSimpleFile() == null) {
            config.getOutputSettings().setReportSimpleFile("report-simple.html");
        }

        if (config.getFlags() == null) {
            ProjectConfig.Flags flags = new ProjectConfig.Flags();
            flags.setEnableStructuralAnalysis(Boolean.TRUE);
            flags.setEnableSemanticMatching(Boolean.TRUE);
            flags.setEnableAdvancedClassification(Boolean.FALSE);
            config.setFlags(flags);
        }
    }

    // Helper to build simple map entries for confidenceLevels
    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1); m.put(k2, v2); return m;
    }
}
