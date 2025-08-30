package com.uitester.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.uitester.core.ElementData;
import com.uitester.core.Configuration;
import com.uitester.core.ProjectConfig;

/**
 * DOM and CSS Crawler that extracts elements and their properties from web pages.
 * This is equivalent to the Python DOMCSSCrawler class.
 */
public class DOMCSSCrawler {
    private static final Logger logger = LoggerFactory.getLogger(DOMCSSCrawler.class);
    
    private WebDriver driver;
    private List<ElementData> elements;
    private Set<String> cssProperties;
    private List<String> attributesToExtract; // configurable attribute list
    private boolean visibilityFilter = true;  // whether to skip invisible elements
    private Integer throttleMs;               // optional delay between element processing
    @SuppressWarnings("unused")
    private Configuration configuration;      // reserved for future use
    
    /**
     * Initialize the DOM CSS Crawler
     * 
     * @param headless Whether to run the browser in headless mode
     * @param driver An existing WebDriver instance to use (optional)
     * @param viewportWidth Browser viewport width in pixels (optional)
     * @param viewportHeight Browser viewport height in pixels (optional)
     */
    public DOMCSSCrawler(boolean headless, WebDriver driver, Integer viewportWidth, Integer viewportHeight) {
        initWithConfig(null, headless, driver, viewportWidth, viewportHeight);
    }
    
    /**
     * Initialize the DOM CSS Crawler with configuration
     * 
     * @param headless Whether to run the browser in headless mode
     * @param driver An existing WebDriver instance to use (optional)
     * @param viewportWidth Browser viewport width in pixels (optional)
     * @param viewportHeight Browser viewport height in pixels (optional)
     * @param configuration Configuration object containing CSS properties to capture
     */
    public DOMCSSCrawler(boolean headless, WebDriver driver, Integer viewportWidth, Integer viewportHeight, 
                        Configuration configuration) {
        initWithConfig(configuration, headless, driver, viewportWidth, viewportHeight);
    }
    
    /**
     * Initialize the DOM CSS Crawler with default settings
     */
    public DOMCSSCrawler() { this(false, null, null, null); }

    /**
     * Unified initialization reading from new crawlerSettings where available.
     */
    private void initWithConfig(Configuration configuration, boolean headless, WebDriver driver, Integer viewportWidth, Integer viewportHeight) {
        this.configuration = configuration;
        ProjectConfig projectConfig = configuration != null ? configuration.getProjectConfig() : null;
        ProjectConfig.CrawlerSettings crawlerSettings = projectConfig != null ? projectConfig.getCrawlerSettings() : null;

        // CSS properties precedence: crawlerSettings.cssProperties -> captureSettings.stylesToCapture -> default list
        if (crawlerSettings != null && crawlerSettings.getCssProperties() != null && !crawlerSettings.getCssProperties().isEmpty()) {
            this.cssProperties = new HashSet<>(crawlerSettings.getCssProperties());
            logger.info("Using {} CSS properties (crawlerSettings)", cssProperties.size());
        } else if (projectConfig != null && projectConfig.getCaptureSettings() != null &&
                projectConfig.getCaptureSettings().getStylesToCapture() != null &&
                !projectConfig.getCaptureSettings().getStylesToCapture().isEmpty()) {
            this.cssProperties = new HashSet<>(projectConfig.getCaptureSettings().getStylesToCapture());
            logger.info("Using {} CSS properties (captureSettings)", cssProperties.size());
        } else {
            this.cssProperties = new HashSet<>(Arrays.asList(
                    "color", "background-color", "font-size", "font-weight", "display",
                    "visibility", "position", "width", "height", "margin", "padding",
                    "border", "text-align", "line-height", "opacity", "z-index"));
            logger.warn("Using default CSS properties (no config provided)");
        }

        // Attributes to extract precedence: crawlerSettings.attributesToExtract -> captureSettings.attributesToCapture -> default set
        if (crawlerSettings != null && crawlerSettings.getAttributesToExtract() != null && !crawlerSettings.getAttributesToExtract().isEmpty()) {
            this.attributesToExtract = new ArrayList<>(crawlerSettings.getAttributesToExtract());
        } else if (projectConfig != null && projectConfig.getCaptureSettings() != null &&
                projectConfig.getCaptureSettings().getAttributesToCapture() != null &&
                !projectConfig.getCaptureSettings().getAttributesToCapture().isEmpty()) {
            this.attributesToExtract = new ArrayList<>(projectConfig.getCaptureSettings().getAttributesToCapture());
        } else {
            this.attributesToExtract = Arrays.asList("id", "class", "aria-label", "alt", "href", "src", "type", "role");
        }

        // Visibility filter & throttle
        if (crawlerSettings != null && crawlerSettings.getVisibilityFilter() != null) {
            this.visibilityFilter = crawlerSettings.getVisibilityFilter();
        }
        if (crawlerSettings != null) {
            this.throttleMs = crawlerSettings.getThrottleMs();
        }

        if (driver != null) {
            this.driver = driver;
            logger.info("Using provided WebDriver instance");
        } else {
            setupDriver(headless, viewportWidth, viewportHeight);
        }

        this.elements = new ArrayList<>();
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
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));

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
        for (String attr : this.attributesToExtract) {
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
        // Build a minimal unique CSS selector (shorter than full path)
        String script = "function uniqueSelector(el){" +
            " if(!el||el.nodeType!==1) return '';" +
            " var doc=el.ownerDocument;" +
            " if(el.id){return '#'+el.id;}" +
            " var segments=[];" +
            " var current=el; var depth=0;" +
            " while(current && current.nodeType===1 && depth<8){" +
            "   segments.unshift(segment(current));" +
            "   var sel=segments.join(' > ');" +
            "   try{ if(doc.querySelectorAll(sel).length===1) return sel; }catch(e){}" +
            "   current=current.parentElement; depth++;" +
            "   if(current && current.id){ segments.unshift('#'+current.id); sel=segments.join(' > '); try{ if(doc.querySelectorAll(sel).length===1) return sel;}catch(e){} break;}" +
            " }" +
            " return segments.join(' > ');" +
            " function segment(node){" +
            "   if(node.id) return '#'+node.id;" +
            "   var tag=node.nodeName.toLowerCase();" +
            "   var cls=(node.className||'').trim().split(/\\s+/)[0]||'';" +
            "   var base=tag+(cls?'.'+cls:'');" +
            "   try{ if(doc.querySelectorAll(base).length===1) return base; }catch(e){}" +
            "   var nth=1, sib=node; while((sib=sib.previousElementSibling)!=null){ if(sib.nodeName===node.nodeName) nth++; }" +
            "   return base+':nth-of-type('+nth+')';" +
            " }" +
            "}; return uniqueSelector(arguments[0]);";
                try {
                    String fullPath = (String) ((JavascriptExecutor) driver).executeScript(script, element);
                    if (fullPath != null && !fullPath.isEmpty()) {
                        return fullPath;
                    }
                } catch (Exception ignore) {
                    // ignore JS path failure, fallback below
                }
                // Ultimate fallback simple tag:nth-of-type
                String fallbackScript = "var e=arguments[0]; var sib= e, n=1; while((sib=sib.previousElementSibling)!=null){ if(sib.tagName===e.tagName) n++; } return n;";
                Long nth = (Long) ((JavascriptExecutor) driver).executeScript(fallbackScript, element);
                return tagName + ":nth-of-type(" + nth + ")";
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
                
                // Skip invisible elements if visibilityFilter enabled
                if (visibilityFilter && !element.isDisplayed()) {
                    continue;
                }
                
                // Extract element data
                Map<String, String> attributes = extractAttributes(element);
                Map<String, String> styles = extractStyles(element, this.cssProperties);
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
                @SuppressWarnings("unchecked")
                Map<String, Object> pos = (Map<String, Object>) positionData.get("position");
                elementData.setPosition(pos);
                elementData.setInViewport((Boolean) positionData.get("inViewport"));
                
                elements.add(elementData);
                processedCount++;

                // Throttle if configured
                if (throttleMs != null && throttleMs > 0) {
                    try { Thread.sleep(throttleMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                
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
