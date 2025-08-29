# Phase 1: Configuration-Driven Approach

## Goal
The primary goal of this phase is to decouple the application's behavior from the source code by introducing an external JSON configuration file. This will allow for easy adjustments to element capture and comparison logic without requiring code modifications and recompilation.

## Key Steps

1.  **Create a `ProjectConfig` Model with Validation:**
    *   To avoid conflict with the existing `Configuration.java` (which manages runtime settings), we will create a new class named `ProjectConfig.java`.
    *   This class will be a POJO designed to mirror the structure of the `config.json` file with Jackson annotations.
    *   It will contain nested classes for `CaptureSettings` and `ComparisonSettings`.
    *   **Add validation methods** to ensure loaded values are within acceptable ranges (e.g., thresholds between 0.0-1.0).

2.  **Implement a Robust Configuration Loader:**
    *   A utility class, `ConfigLoader.java`, will handle loading and validation of `config.json`.
    *   **Comprehensive error handling:** Handle malformed JSON, missing files, and invalid values gracefully.
    *   **Fallback strategy:** If config loading fails, log warnings and use hardcoded defaults.
    *   **Configuration validation:** Validate all loaded values and log warnings for invalid configurations.

3.  **Create a Comprehensive Default `config.json`:**
    *   A `config.json` file will be added to `src/main/resources` with detailed settings:
    *   **Capture settings:** Specific CSS properties (color, font-size, width, height, etc.), HTML attributes (id, class, src, href), and ignore patterns for dynamic attributes.
    *   **Comparison settings:** Thresholds for text similarity (0.95), numeric changes (0.05), and style categorization.
    *   **Fingerprinting settings:** Properties to use for element identification and weights.

4.  **Safely Refactor the existing `Configuration.java`:**
    *   Add a `ProjectConfig` field to the existing `Configuration` class.
    *   **Backward compatibility:** Ensure command-line args and TestNG parameters still override config file values.
    *   **Migration safety:** Preserve all existing default values as fallbacks if config loading fails.
    *   Add getter methods to access the new configuration settings.

5.  **Gradually Integrate Configuration:**
    *   **Start with `DOMCSSCrawler`:** Replace hardcoded `CSS_PROPERTIES` with configurable lists.
    *   **Update `ChangeDetector`:** Use comparison thresholds from configuration.
    *   **Maintain compatibility:** Ensure existing functionality works unchanged if config is missing.

## Testing Strategy for Phase 1
*   **Unit tests** for `ConfigLoader` with various JSON scenarios (valid, invalid, missing file).
*   **Integration tests** to ensure command-line args still override config values.
*   **Validation tests** to verify configuration validation works correctly.
*   **Backward compatibility tests** to ensure existing TestNG scenarios work unchanged.

## Risk Mitigation
*   **Graceful degradation:** If config loading fails, application continues with hardcoded defaults.
*   **Comprehensive logging:** All configuration loading steps are logged for debugging.
*   **Validation with user feedback:** Invalid config values are logged with helpful error messages.

## Deliverables
*   `src/main/java/com/uitester/core/ProjectConfig.java` (New file with validation)
*   `src/main/java/com/uitester/core/ConfigLoader.java` (With error handling)
*   `src/main/resources/config.json` (Comprehensive default configuration)
*   Updated `Configuration.java` to safely load from `ProjectConfig`
*   Updated `DOMCSSCrawler.java` and `ChangeDetector.java` with gradual integration
*   Test suite for configuration loading and validation
