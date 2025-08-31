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

        // Unified capture precedence logic:
        // 1. If crawlerSettings unified fields (stylesToCapture / attributesToCapture) present -> base list.
        // 2. Merge in legacy crawlerSettings.cssProperties / attributesToExtract (deprecated names) if not already included.
        // 3. Merge in captureSettings.stylesToCapture / attributesToCapture.
        // 4. If still empty use defaults.
        Set<String> styleSet = new HashSet<>();
        Set<String> attrSet = new HashSet<>();

        if (crawlerSettings != null && crawlerSettings.getStylesToCapture() != null) {
            styleSet.addAll(crawlerSettings.getStylesToCapture());
        }
        if (crawlerSettings != null && crawlerSettings.getCssProperties() != null) {
            // deprecated alias; merge but log if introducing new props
            for (String p : crawlerSettings.getCssProperties()) {
                if (styleSet.add(p)) {
                    logger.debug("Added style from deprecated cssProperties: {}", p);
                }
            }
        }
    // legacy captureSettings removed – no longer merging here
        if (styleSet.isEmpty()) {
            styleSet.addAll(Arrays.asList(
                "color","background-color","font-size","font-weight","display","visibility",
                "position","width","height","margin","padding","border","text-align",
                "line-height","opacity","z-index","box-shadow","border-radius","gap"));
            logger.warn("Using default CSS properties (no explicit styles configured)");
        }
        this.cssProperties = styleSet;
        logger.info("Effective CSS properties ({}): {}", cssProperties.size(), String.join(",", cssProperties));

        if (crawlerSettings != null && crawlerSettings.getAttributesToCapture() != null) {
            attrSet.addAll(crawlerSettings.getAttributesToCapture());
        }
        if (crawlerSettings != null && crawlerSettings.getAttributesToExtract() != null) {
            for (String a : crawlerSettings.getAttributesToExtract()) {
                if (attrSet.add(a)) {
                    logger.debug("Added attribute from deprecated attributesToExtract: {}", a);
                }
            }
        }
    // legacy captureSettings removed – no longer merging attributes
        if (attrSet.isEmpty()) {
            attrSet.addAll(Arrays.asList("id","class","aria-label","alt","href","src","type","role","data-testid"));
            logger.warn("Using default attribute list (no explicit attributes configured)");
        }
        // Always ensure we capture generic aria-* and data-* even if not listed explicitly
        // They will be handled during extraction if pattern-based logic is added.
        this.attributesToExtract = new ArrayList<>(attrSet);
        logger.info("Effective attributes ({}): {}", attributesToExtract.size(), String.join(",", attributesToExtract));

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
            } else if (elementClass != null && !elementClass.isEmpty()) {
                // Try short form tag.firstClass only if unique
                String shortSelector = tagName + "." + elementClass.split(" ")[0];
                try {
                    String uniquenessScript = "var sel='" + shortSelector.replace("'","\\'") + "';" +
                        "try { return document.querySelectorAll(sel).length; } catch(e){ return 999; }";
                    Long count = (Long) ((JavascriptExecutor) driver).executeScript(uniquenessScript);
                    if (count != null && count == 1L) {
                        return shortSelector; // unique enough
                    }
                } catch (Exception ignore) {}
                // Fall through to full path if not unique
            }
            {
                // Deterministic full path with nth-of-type at each level for guaranteed uniqueness
                String script = "function fullPath(el){" +
                        " if(!el||el.nodeType!==1) return '';" +
                        " var parts=[];" +
                        " while(el && el.nodeType===1 && el.tagName!=='HTML'){" +
                        "   if(el.id){ parts.unshift('#'+cssEscape(el.id)); break; }" +
                        "   var tag=el.tagName.toLowerCase();" +
                        "   var cls=(el.className||'').trim().split(/\\s+/)[0]||'';" +
                        "   var nth=1, sib=el; while((sib=sib.previousElementSibling)!=null){ if(sib.nodeName===el.nodeName) nth++; }" +
                        "   var seg=tag+(cls?'.'+cssEscape(cls):'')+':nth-of-type('+nth+')';" +
                        "   parts.unshift(seg);" +
                        "   el=el.parentElement;" +
                        " }" +
                        " return parts.join(' > ');" +
                        " function cssEscape(s){ return s.replace(/([:#.>+~\\\\[\\\\]\\\\s])/g,'\\\\$1'); }" +
                        "}; return fullPath(arguments[0]);";
                try {
                    String path = (String) ((JavascriptExecutor) driver).executeScript(script, element);
                    if (path != null && !path.isEmpty()) return path;
                } catch (Exception e) {
                    logger.warn("Full path selector JS failed: {}", e.getMessage());
                }
                // Fallback simple nth-of-type
                String fallbackScript = "var e=arguments[0]; var n=1, p=e; while((p=p.previousElementSibling)!=null){ if(p.tagName===e.tagName) n++; } return n;";
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
            if (directText != null && !directText.isEmpty()) {
                return directText; // simple case
            }

            // Fallback logic: handle cases where original text was wrapped in a new inline element (e.g., <strong>)
            // We want the parent element to still expose a stable text value so text diffs are not misclassified as removals.
            String fullText = element.getText() != null ? element.getText().trim() : "";
            if (fullText.isEmpty()) {
                return ""; // nothing to do
            }

            // Determine if all child elements are inline formatting wrappers so we can safely flatten.
            // Treat only formatting wrappers (exclude interactive <A>) as inline for flattening.
            String allInlineScript = "var el=arguments[0];" +
                "if(!el.children || el.children.length===0) return false;" +
                "var inline=['STRONG','EM','B','I','SPAN','SMALL','SUP','SUB','MARK','CODE'];" +
                "for(var i=0;i<el.children.length;i++){" +
                "  var tag=el.children[i].tagName;" +
                "  if(inline.indexOf(tag)===-1) return false;" +
                "}" +
                "return true;";
            Boolean allInline = (Boolean) ((JavascriptExecutor) driver).executeScript(allInlineScript, element);

            String hasAnchorChildScript = "var el=arguments[0]; if(!el.children) return false; for(var i=0;i<el.children.length;i++){ if(el.children[i].tagName==='A') return true;} return false;";
            Boolean hasAnchorChild = (Boolean) ((JavascriptExecutor) driver).executeScript(hasAnchorChildScript, element);

            // Also detect if children have text at all
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

            // If children have text and are all inline wrappers, or total text length is short (likely a label/price), use fullText.
            final int INLINE_TEXT_FALLBACK_MAX_LEN = 120; // guard against large container duplication
            if (!Boolean.TRUE.equals(hasAnchorChild) && (Boolean.TRUE.equals(allInline) || (Boolean.TRUE.equals(hasTextChildren) && fullText.length() <= INLINE_TEXT_FALLBACK_MAX_LEN))) {
                return fullText;
            }

            // Else keep it empty to avoid large duplicated ancestor text
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
