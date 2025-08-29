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
        comparisonSettings.setTextSimilarityThreshold(0.95);
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
        
        return config;
    }
}
