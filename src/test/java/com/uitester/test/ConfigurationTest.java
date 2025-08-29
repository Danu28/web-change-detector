package com.uitester.test;

import com.uitester.core.ConfigLoader;
import com.uitester.core.Configuration;
import com.uitester.core.ProjectConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test class for Phase 1 configuration functionality.
 */
public class ConfigurationTest {
    
    @Test
    public void testConfigLoading() {
        // Test that config can be loaded
        ProjectConfig config = ConfigLoader.loadConfig();
        Assert.assertNotNull(config, "Configuration should not be null");
        
        // Test that capture settings are loaded
        Assert.assertNotNull(config.getCaptureSettings(), "Capture settings should not be null");
        Assert.assertNotNull(config.getCaptureSettings().getStylesToCapture(), "Styles to capture should not be null");
        Assert.assertTrue(config.getCaptureSettings().getStylesToCapture().size() > 0, "Should have styles to capture");
        
        // Test that comparison settings are loaded
        Assert.assertNotNull(config.getComparisonSettings(), "Comparison settings should not be null");
        Assert.assertNotNull(config.getComparisonSettings().getTextSimilarityThreshold(), "Text similarity threshold should not be null");
        
        System.out.println("✓ Configuration loaded successfully");
        System.out.println("  - Styles to capture: " + config.getCaptureSettings().getStylesToCapture().size());
        System.out.println("  - Text similarity threshold: " + config.getComparisonSettings().getTextSimilarityThreshold());
    }
    
    @Test
    public void testConfigurationIntegration() {
        // Test that Configuration class loads ProjectConfig
        Configuration configuration = new Configuration();
        Assert.assertNotNull(configuration.getProjectConfig(), "Project config should be loaded in Configuration");
        
        ProjectConfig projectConfig = configuration.getProjectConfig();
        Assert.assertNotNull(projectConfig.getCaptureSettings(), "Capture settings should be available");
        
        System.out.println("✓ Configuration integration works");
        System.out.println("  - Project config loaded: " + (projectConfig != null));
        System.out.println("  - Parallel crawling enabled: " + configuration.isParallelCrawling());
    }
    
    @Test
    public void testConfigValidation() {
        ProjectConfig config = ConfigLoader.loadConfig();
        ProjectConfig.ValidationResult validation = config.validate();
        
        Assert.assertFalse(validation.hasErrors(), "Configuration should not have validation errors: " + validation.getErrors());
        
        System.out.println("✓ Configuration validation passed");
        if (!validation.getWarnings().isEmpty()) {
            System.out.println("  - Warnings: " + validation.getWarnings());
        }
    }
    
    @Test
    public void testFallbackConfiguration() {
        // Clear cache to force reload
        ConfigLoader.clearCache();
        
        // Test with non-existent config file
        ProjectConfig config = ConfigLoader.loadConfig("non-existent-config.json");
        Assert.assertNotNull(config, "Should fall back to default config");
        
        // Verify default values are present
        Assert.assertNotNull(config.getCaptureSettings(), "Default capture settings should be present");
        Assert.assertTrue(config.getCaptureSettings().getStylesToCapture().size() > 0, "Default styles should be present");
        
        System.out.println("✓ Fallback configuration works");
        
        // Clear cache again for other tests
        ConfigLoader.clearCache();
    }
}
