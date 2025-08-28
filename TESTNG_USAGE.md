# TestNG-Based UI Change Detection Tests

This project now includes a comprehensive TestNG-based test class for UI change detection with parameterized testing support.

## Features

✅ **Parameterized Testing**: XPath and section locators as optional parameters  
✅ **Flexible Configuration**: Support for baseline/current URLs, structural changes, headless mode  
✅ **Multiple Test Scenarios**: Regular tests, structural change tests, validation tests  
✅ **Comprehensive Reporting**: Detailed test summaries and validation  
✅ **TestNG XML Configuration**: Pre-configured test suites for different scenarios  

## Running Tests

### 1. Run All Tests Using TestNG XML
```bash
mvn test
```

### 2. Run Specific Test Class
```bash
mvn test -Dtest=UIChangeDetectionTest
```

### 3. Run with Custom Parameters
```bash
mvn test -Dtest=UIChangeDetectionTest \
  -Dbaseline.url="https://example.com/page1" \
  -Dcurrent.url="https://example.com/page2" \
  -Dxpath="//*[@id='content']" \
  -Dsection.name="content-section" \
  -Ddetect.structural="true" \
  -Dheadless="true"
```

### 4. Run TestNG Suite Directly
```bash
java -cp "target/classes:target/test-classes:target/dependency/*" \
  org.testng.TestNG src/test/resources/testng.xml
```

## Test Scenarios Included

### 1. LocalFileTest
- Tests with local HTML files
- XPath: Not specified (full page)
- Section: "full-page"
- Structural changes: Disabled by default, enabled for structural test

### 2. HeaderSectionTest
- Tests live websites (HP Instant Ink pages)
- XPath: `//*[@id='v2Header']`
- Section: "header"
- Structural changes: Disabled by default, enabled for structural test

### 3. FooterSectionTest
- Tests live websites (HP Instant Ink pages)
- XPath: `//*[@id='footer']`
- Section: "footer"
- Structural changes: Disabled

## Test Methods

### `testUIChangeDetection()`
Main UI change detection test that:
- Runs the complete UI testing process
- Verifies output files are created
- Loads and validates changes
- Prints detailed test summary

### `testUIChangeDetectionWithStructural()`
Extended test that:
- Runs the same test with structural changes enabled
- Compares results between regular and structural detection
- Validates that structural test finds equal or more changes

### `testChangeClassifications()`
Validation test that:
- Checks all change classifications are valid ("critical", "cosmetic", "noise")
- Verifies magnitude values are between 0.0 and 1.0

### `testReportGeneration()`
Report validation test that:
- Verifies HTML report file exists and is not empty
- Validates report contains expected HTML structure and content

## Parameters

| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `baseline.url` | Baseline URL to compare against | Local file | `https://example.com/old` |
| `current.url` | Current URL to compare | Local file | `https://example.com/new` |
| `xpath` | XPath selector for container element | Empty (full page) | `//*[@id='header']` |
| `section.name` | Section name for output | Empty | `header` |
| `detect.structural` | Enable structural change detection | `false` | `true` |
| `headless` | Run browser in headless mode | `true` | `false` |
| `enable.structural.test` | Enable structural comparison test | `false` | `true` |

## Test Output

The tests generate:
- Detailed console output with test progress
- Test summary with change counts by type and classification
- HTML reports in the `output/` directory
- JSON files with baseline, current, and changes data

## Example Test Summary Output

```
=== TEST SUMMARY ===
Total changes detected: 5
- Attribute changes: 2
- Text changes: 1
- Style changes: 2
- Layout changes: 0
- Structural changes: 0
- Critical changes: 5
- Cosmetic changes: 0
- Noise changes: 0
===================
```

## Customizing Tests

### Adding New Test Scenarios
Edit `src/test/resources/testng.xml` to add new test configurations:

```xml
<test name="CustomTest">
    <parameter name="baseline.url" value="your-baseline-url"/>
    <parameter name="current.url" value="your-current-url"/>
    <parameter name="xpath" value="your-xpath"/>
    <parameter name="section.name" value="your-section"/>
    <parameter name="detect.structural" value="true"/>
    <parameter name="headless" value="true"/>
    <classes>
        <class name="com.uitester.test.UIChangeDetectionTest"/>
    </classes>
</test>
```

### Creating Custom Test Classes
Extend or create new test classes following the same pattern:

```java
@Test
@Parameters({"custom.param"})
public void customTest(@Optional("default") String customParam) {
    // Your test logic here
}
```

This TestNG implementation provides a robust, flexible testing framework for UI change detection with comprehensive parameterization and validation capabilities.
