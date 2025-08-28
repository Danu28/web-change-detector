package com.uitester.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.uitester.core.ElementData;

/**
 * DOM and CSS Crawler that extracts elements and their properties from web pages.
 * This is equivalent to the Python DOMCSSCrawler class.
 */
public class DOMCSSCrawler {
    private static final Logger logger = LoggerFactory.getLogger(DOMCSSCrawler.class);
    
    private WebDriver driver;
    private List<ElementData> elements;
    
    // CSS properties to extract from elements
    private static final Set<String> CSS_PROPERTIES = new HashSet<>(Arrays.asList(
        "color", "background-color", "font-size", "font-weight", "display",
        "visibility", "position", "width", "height", "margin", "padding",
        "border", "text-align", "line-height", "opacity", "z-index"
    ));
    
    /**
     * Initialize the DOM CSS Crawler
     * 
     * @param headless Whether to run the browser in headless mode
     * @param driver An existing WebDriver instance to use (optional)
     * @param viewportWidth Browser viewport width in pixels (optional)
     * @param viewportHeight Browser viewport height in pixels (optional)
     */
    public DOMCSSCrawler(boolean headless, WebDriver driver, Integer viewportWidth, Integer viewportHeight) {
        if (driver != null) {
            this.driver = driver;
            logger.info("Using provided WebDriver instance");
        } else {
            setupDriver(headless, viewportWidth, viewportHeight);
        }
        
        this.elements = new ArrayList<>();
    }
    
    /**
     * Initialize the DOM CSS Crawler with default settings
     */
    public DOMCSSCrawler() {
        this(false, null, null, null);
    }
    
    /**
     * Initialize Chrome WebDriver with optimized options
     * 
     * @param headless Whether to run browser in headless mode
     * @param viewportWidth Browser viewport width in pixels (optional)
     * @param viewportHeight Browser viewport height in pixels (optional)
     */
    private void setupDriver(boolean headless, Integer viewportWidth, Integer viewportHeight) {
        ChromeOptions chromeOptions = new ChromeOptions();
        
        if (headless) {
            chromeOptions.addArguments("--headless=new");
        }
        
        // Common performance flags for container environments
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-background-networking");
        chromeOptions.addArguments("--disable-sync");
        chromeOptions.addArguments("--disable-translate");
        
        // Add viewport size to Chrome options
        if (viewportWidth != null && viewportHeight != null) {
            chromeOptions.addArguments(String.format("--window-size=%d,%d", viewportWidth, viewportHeight));
            logger.info("Requested viewport size: {}×{}", viewportWidth, viewportHeight);
        }
        
        // Workaround for high-DPI issues
        if (headless && viewportWidth != null && viewportHeight != null) {
            chromeOptions.addArguments("--force-device-scale-factor=1");
            chromeOptions.addArguments("--high-dpi-support=1");
        }
        
        // Instantiate the driver
        this.driver = new ChromeDriver(chromeOptions);
        
        // Additional window sizing logic
        if (!headless && viewportWidth != null && viewportHeight != null) {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(viewportWidth, viewportHeight));
            logger.info("Set window size (headed): {}×{}", viewportWidth, viewportHeight);
        }
    }
    
    /**
     * Extract key attributes from an element
     * 
     * @param element The WebElement to extract attributes from
     * @return Map of attribute names to values
     */
    private Map<String, String> extractAttributes(WebElement element) {
        Map<String, String> attributes = new HashMap<>();
        String[] attributesToExtract = {"id", "class", "aria-label", "alt", "href", "src", "type", "role"};
        
        for (String attr : attributesToExtract) {
            try {
                String value = element.getAttribute(attr);
                if (value != null && !value.isEmpty()) {
                    // Truncate very long attribute values
                    if (value.length() > 500) {
                        value = value.substring(0, 497) + "...";
                    }
                    attributes.put(attr, value);
                }
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                // Skip attributes that can't be accessed
                continue;
            } catch (Exception e) {
                logger.error("Unexpected error getting attribute '{}': {}", attr, e.getMessage());
                continue;
            }
        }
        
        return attributes;
    }
    
    /**
     * Extract multiple CSS properties at once using JavaScript
     * 
     * @param element The WebElement to extract styles from
     * @param cssProperties List of CSS property names to extract
     * @return Map of CSS property names to values
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> extractStyles(WebElement element, Set<String> cssProperties) {
        try {
            // Build a JavaScript function to get all CSS properties at once
            String script = 
                "function getStyles(element, properties) {" +
                "    const styles = {};" +
                "    const computedStyle = window.getComputedStyle(element);" +
                "    " +
                "    for (const prop of properties) {" +
                "        const value = computedStyle.getPropertyValue(prop);" +
                "        if (value && value !== 'auto' && value !== 'none') {" +
                "            styles[prop] = value;" +
                "        }" +
                "    }" +
                "    " +
                "    return styles;" +
                "}" +
                "" +
                "return getStyles(arguments[0], arguments[1]);";
            
            return (Map<String, String>) ((JavascriptExecutor) driver).executeScript(script, element, cssProperties.toArray());
        } catch (Exception e) {
            logger.error("Error extracting styles: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get element position and determine if it's in viewport
     * 
     * @param element The WebElement to get position for
     * @return Map containing position information and viewport status
     */
    private Map<String, Object> getElementPosition(WebElement element) {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> position = new HashMap<>();
            
            // Get element location and size
            org.openqa.selenium.Point location = element.getLocation();
            org.openqa.selenium.Dimension size = element.getSize();
            
            // Calculate if element is in viewport
            Long viewportWidth = (Long) ((JavascriptExecutor) driver).executeScript("return window.innerWidth");
            Long viewportHeight = (Long) ((JavascriptExecutor) driver).executeScript("return window.innerHeight");
            
            boolean inViewport = (
                location.getX() >= 0 && 
                location.getY() >= 0 && 
                location.getX() < viewportWidth && 
                location.getY() < viewportHeight
            );
            
            position.put("x", location.getX());
            position.put("y", location.getY());
            position.put("width", size.getWidth());
            position.put("height", size.getHeight());
            
            result.put("position", position);
            result.put("inViewport", inViewport);
            
            return result;
        } catch (Exception e) {
            logger.error("Error getting element position: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> position = new HashMap<>();
            
            position.put("x", 0);
            position.put("y", 0);
            position.put("width", 0);
            position.put("height", 0);
            
            result.put("position", position);
            result.put("inViewport", false);
            
            return result;
        }
    }
    
    /**
     * Create a more reliable CSS selector or XPath for an element
     * 
     * @param element The WebElement to create selector for
     * @param attributes The element's attributes
     * @return A CSS selector string
     */
    private String createElementSelector(WebElement element, Map<String, String> attributes) {
        try {
            String tagName = element.getTagName();
            String elementId = attributes.getOrDefault("id", "");
            String elementClass = attributes.getOrDefault("class", "");
            
            if (!elementId.isEmpty()) {
                return "#" + elementId;
            } else if (!elementClass.isEmpty() && elementClass != null) {
                // Use first class as it's often most specific
                return tagName + "." + elementClass.split(" ")[0];
            } else {
                // Create a more unique selector by including parent context
                try {
                    // Get parent element info to create more specific selector
                    String parentScript = 
                        "var element = arguments[0];" +
                        "var parent = element.parentNode;" +
                        "if (!parent || parent.tagName === 'BODY' || parent.tagName === 'HTML') {" +
                        "    return null;" +
                        "}" +
                        "" +
                        "// Get parent's class or id" +
                        "var parentId = parent.id;" +
                        "var parentClass = parent.className;" +
                        "" +
                        "return {" +
                        "    tag: parent.tagName.toLowerCase()," +
                        "    id: parentId," +
                        "    class: parentClass ? parentClass.split(' ')[0] : ''" +
                        "};";
                    
                    Map<String, String> parentInfo = (Map<String, String>) ((JavascriptExecutor) driver).executeScript(parentScript, element);
                    
                    // Get position among siblings of same tag
                    String siblingsScript = 
                        "var element = arguments[0];" +
                        "var siblings = Array.from(element.parentNode.children).filter(e => e.tagName === element.tagName);" +
                        "return siblings.indexOf(element) + 1;";
                    
                    Long position = (Long) ((JavascriptExecutor) driver).executeScript(siblingsScript, element);
                    
                    // Create more specific selector with parent context
                    if (parentInfo != null) {
                        if (parentInfo.containsKey("id") && !parentInfo.get("id").isEmpty()) {
                            return "#" + parentInfo.get("id") + " > " + tagName + ":nth-of-type(" + position + ")";
                        } else if (parentInfo.containsKey("class") && !parentInfo.get("class").isEmpty()) {
                            return parentInfo.get("tag") + "." + parentInfo.get("class") + " > " + tagName + ":nth-of-type(" + position + ")";
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error creating parent-based selector: {}", e.getMessage());
                }
                
                // Fall back to simple tag with position
                String fallbackScript = 
                    "var element = arguments[0];" +
                    "var siblings = Array.from(element.parentNode.children).filter(e => e.tagName === element.tagName);" +
                    "return siblings.indexOf(element) + 1;";
                Long elementPosition = (Long) ((JavascriptExecutor) driver).executeScript(fallbackScript, element);
                return tagName + ":nth-of-type(" + elementPosition + ")";
            }
        } catch (Exception e) {
            logger.error("Error creating element selector: {}", e.getMessage());
            return element.getTagName() + ".unknown-element";
        }
    }
    
    /**
     * Navigate to a URL and wait for the page to load
     * 
     * @param url The URL to navigate to
     * @param waitTimeSeconds Time to wait for page to load
     */
    public void navigate(String url, int waitTimeSeconds) {
        driver.get(url);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(waitTimeSeconds));
        
        // Wait for DOM to be ready
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitTimeSeconds));
        wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        
        logger.info("Navigated to URL: {}", url);
    }
    
    /**
     * Extract all visible elements from the page with their attributes and styles
     * 
     * @param containerXPath Optional XPath to limit extraction to a container
     * @param maxElements Maximum number of elements to extract (null for unlimited)
     * @return List of ElementData objects with element information
     */
    public List<ElementData> extractElements(String containerXPath, Integer maxElements) {
        elements.clear();
        List<WebElement> allElements;
        
        if (containerXPath != null && !containerXPath.isEmpty()) {
            try {
                WebElement container = driver.findElement(By.xpath(containerXPath));
                allElements = container.findElements(By.xpath(".//*"));
                logger.info("Using container XPath: {}", containerXPath);
            } catch (NoSuchElementException e) {
                logger.error("Container element not found with XPath: {}", containerXPath);
                return elements;
            }
        } else {
            allElements = driver.findElements(By.xpath("//*"));
        }
        
        logger.info("Found {} elements on the page", allElements.size());
        
        int processedCount = 0;
        for (WebElement element : allElements) {
            try {
                if (maxElements != null && processedCount >= maxElements) {
                    logger.info("Reached max elements limit: {}", maxElements);
                    break;
                }
                
                // Skip invisible elements
                if (!element.isDisplayed()) {
                    continue;
                }
                
                // Extract element data
                Map<String, String> attributes = extractAttributes(element);
                Map<String, String> styles = extractStyles(element, CSS_PROPERTIES);
                Map<String, Object> positionData = getElementPosition(element);
                String selector = createElementSelector(element, attributes);
                
                // Extract text using JavaScript to get only direct text nodes (avoiding duplication)
                String text = extractDirectTextContent(element);
                String tagName = element.getTagName();
                
                // Create element data object
                ElementData elementData = new ElementData();
                elementData.setTagName(tagName);
                elementData.setText(text);
                elementData.setSelector(selector);
                elementData.setAttributes(attributes);
                elementData.setStyles(styles);
                elementData.setPosition((Map<String, Object>) positionData.get("position"));
                elementData.setInViewport((Boolean) positionData.get("inViewport"));
                
                elements.add(elementData);
                processedCount++;
                
            } catch (StaleElementReferenceException e) {
                // Element no longer exists in DOM, skip it
                continue;
            } catch (Exception e) {
                logger.error("Error processing element: {}", e.getMessage());
            }
        }
        
        logger.info("Extracted {} elements", elements.size());
        return elements;
    }
    
    /**
     * Extract only direct text content from an element, avoiding duplicating text from child elements
     * 
     * @param element WebElement to extract text from
     * @return String containing only direct text content
     */
    private String extractDirectTextContent(WebElement element) {
        try {
            // Get only direct text nodes (not text from child elements)
            String directTextScript = 
                "var element = arguments[0];" +
                "var directText = '';" +
                "for (var i = 0; i < element.childNodes.length; i++) {" +
                "    var node = element.childNodes[i];" +
                "    if (node.nodeType === Node.TEXT_NODE) {" +
                "        directText += node.textContent;" +
                "    }" +
                "}" +
                "return directText.trim();";
            
            String directText = (String) ((JavascriptExecutor) driver).executeScript(directTextScript, element);
            
            // If no direct text but element has text and no child elements with text, use full text
            if (directText == null || directText.isEmpty()) {
                String fullText = element.getText().trim();
                
                if (!fullText.isEmpty()) {
                    // Check if this element has child elements with text
                    String hasTextChildrenScript = 
                        "var element = arguments[0];" +
                        "var children = element.children;" +
                        "for (var i = 0; i < children.length; i++) {" +
                        "    if (children[i].textContent && children[i].textContent.trim()) {" +
                        "        return true;" +
                        "    }" +
                        "}" +
                        "return false;";
                    
                    Boolean hasTextChildren = (Boolean) ((JavascriptExecutor) driver).executeScript(hasTextChildrenScript, element);
                    
                    // Only use full text if no children have text (leaf text element)
                    if (hasTextChildren != null && !hasTextChildren) {
                        return fullText;
                    }
                }
            } else {
                return directText;
            }
            
            return "";
        } catch (Exception e) {
            logger.error("Error extracting direct text: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Close the WebDriver and release resources
     */
    public void close() {
        if (driver != null) {
            driver.quit();
            logger.info("WebDriver closed");
        }
    }
}
