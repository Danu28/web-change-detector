package com.uitester.main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.report.ReportGenerator;

/**
 * Main application class for UI Tester.
 * This is the entry point for the application.
 */
public class UITesterApplication {
    private static final Logger logger = LoggerFactory.getLogger(UITesterApplication.class);
    
    private Configuration config;
    private ObjectMapper objectMapper;
    
    public UITesterApplication(Configuration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Main method to run the application
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            CommandLine cmd = parseCommandLine(args);
            
            // Create configuration
            Configuration config = createConfiguration(cmd);
            
            // Create and run application
            UITesterApplication app = new UITesterApplication(config);
            app.run();
            
            logger.info("UI Tester completed successfully");
            System.exit(0);
        } catch (Exception e) {
            logger.error("Error running UI Tester: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Parse command line arguments
     * 
     * @param args Command line arguments
     * @return Parsed command line
     * @throws ParseException If there's an error parsing arguments
     */
    private static CommandLine parseCommandLine(String[] args) throws ParseException {
        Options options = new Options();
        
        // URLs
        options.addOption(Option.builder("b")
                .longOpt("baseline")
                .hasArg()
                .desc("Baseline URL to compare against")
                .build());
        
        options.addOption(Option.builder("c")
                .longOpt("current")
                .hasArg()
                .desc("Current URL to compare to baseline")
                .build());
        
        // Crawler settings
        options.addOption(Option.builder("m")
                .longOpt("max-elements")
                .hasArg()
                .type(Number.class)
                .desc("Maximum number of elements to extract")
                .build());
        
        options.addOption(Option.builder("w")
                .longOpt("wait-time")
                .hasArg()
                .type(Number.class)
                .desc("Time in seconds to wait for page load")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("scroll-time")
                .hasArg()
                .type(Number.class)
                .desc("Time in seconds to wait after scrolling")
                .build());
        
        options.addOption(Option.builder("h")
                .longOpt("headless")
                .desc("Run in headless mode")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("no-scrolling")
                .desc("Disable automatic scrolling")
                .build());
        
        options.addOption(Option.builder("x")
                .longOpt("xpath")
                .hasArg()
                .desc("XPath to container element to limit extraction")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("no-parallel")
                .desc("Disable parallel crawling")
                .build());
        
        // Viewport settings
        options.addOption(Option.builder()
                .longOpt("viewport-width")
                .hasArg()
                .type(Number.class)
                .desc("Browser viewport width in pixels")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("viewport-height")
                .hasArg()
                .type(Number.class)
                .desc("Browser viewport height in pixels")
                .build());
        
        // Detection settings
        options.addOption(Option.builder()
                .longOpt("max-changes")
                .hasArg()
                .type(Number.class)
                .desc("Maximum number of changes to detect")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("detect-structural")
                .desc("Enable detection of structural changes (element additions/removals)")
                .build());
        
        // Section info
        options.addOption(Option.builder("s")
                .longOpt("section-name")
                .hasArg()
                .desc("Section name for output")
                .build());
        
        // Help
        options.addOption(Option.builder("?")
                .longOpt("help")
                .desc("Show help")
                .build());
        
        // Parse arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        // Show help if requested
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar uitester.jar", options);
            System.exit(0);
        }
        
        return cmd;
    }
    
    /**
     * Create configuration from command line arguments
     * 
     * @param cmd Parsed command line
     * @return Configuration object
     */
    private static Configuration createConfiguration(CommandLine cmd) {
        Configuration config = new Configuration();
        
        // URLs
        if (cmd.hasOption("baseline")) {
            config.setBaselineUrl(cmd.getOptionValue("baseline"));
        }
        
        if (cmd.hasOption("current")) {
            config.setCurrentUrl(cmd.getOptionValue("current"));
        }
        
        // Crawler settings
        if (cmd.hasOption("max-elements")) {
            config.setMaxElements(Integer.parseInt(cmd.getOptionValue("max-elements")));
        }
        
        if (cmd.hasOption("wait-time")) {
            config.setWaitTime(Integer.parseInt(cmd.getOptionValue("wait-time")));
        }
        
        if (cmd.hasOption("scroll-time")) {
            config.setScrollTime(Double.parseDouble(cmd.getOptionValue("scroll-time")));
        }
        
        if (cmd.hasOption("headless")) {
            config.setHeadless(true);
        }
        
        if (cmd.hasOption("no-scrolling")) {
            config.setEnableScrolling(false);
        }
        
        if (cmd.hasOption("xpath")) {
            config.setContainerXpath(cmd.getOptionValue("xpath"));
        }
        
        if (cmd.hasOption("no-parallel")) {
            config.setParallelCrawling(false);
        }
        
        // Viewport settings
        if (cmd.hasOption("viewport-width")) {
            config.setViewportWidth(Integer.parseInt(cmd.getOptionValue("viewport-width")));
        }
        
        if (cmd.hasOption("viewport-height")) {
            config.setViewportHeight(Integer.parseInt(cmd.getOptionValue("viewport-height")));
        }
        
        // Detection settings
        if (cmd.hasOption("max-changes")) {
            config.setMaxChanges(Integer.parseInt(cmd.getOptionValue("max-changes")));
        }
        
        if (cmd.hasOption("detect-structural")) {
            config.setDetectStructuralChanges(true);
        }
        
        // Section info
        if (cmd.hasOption("section-name")) {
            config.setSectionName(cmd.getOptionValue("section-name"));
        }
        
        // Update output paths based on new settings
        config.updateOutputPaths();
        
        return config;
    }
    
    /**
     * Run the UI testing process
     * 
     * @throws Exception If there's an error during execution
     */
    public void run() throws Exception {
        logger.info("Starting UI testing process");
        
        List<ElementData> baselineElements;
        List<ElementData> currentElements;
        
        // Crawl pages in parallel or sequentially
        if (config.isParallelCrawling()) {
            logger.info("Using parallel crawling for baseline and current pages");
            ExecutorService executor = Executors.newFixedThreadPool(2);
            
            Callable<List<ElementData>> baselineTask = () -> crawlPage(config.getBaselineUrl(), "baseline");
            Callable<List<ElementData>> currentTask = () -> crawlPage(config.getCurrentUrl(), "current");
            
            Future<List<ElementData>> baselineFuture = executor.submit(baselineTask);
            Future<List<ElementData>> currentFuture = executor.submit(currentTask);
            
            baselineElements = baselineFuture.get();
            currentElements = currentFuture.get();
            
            executor.shutdown();
        } else {
            logger.info("Using sequential crawling for baseline and current pages");
            baselineElements = crawlPage(config.getBaselineUrl(), "baseline");
            currentElements = crawlPage(config.getCurrentUrl(), "current");
        }
        
        // Detect changes
        logger.info("Detecting changes between baseline and current snapshots");
        ChangeDetector changeDetector = new ChangeDetector(config);
        List<ElementChange> changes = changeDetector.detectChanges(baselineElements, currentElements, config.getMaxChanges());
        
        // Save changes to file
        saveChangesToFile(changes, config.getChangesFile());
        
        // Generate report
        logger.info("Generating HTML report");
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.generateReport(changes, config.getCurrentUrl(), config.getReportFile());
        
        logger.info("Testing complete. Report available at: {}", config.getReportFile());
    }
    
    /**
     * Crawl a page and extract elements
     * 
     * @param url URL to crawl
     * @param label Label for logging ("baseline" or "current")
     * @return List of extracted elements
     * @throws Exception If there's an error during crawling
     */
    private List<ElementData> crawlPage(String url, String label) throws Exception {
        logger.info("Crawling {} page: {}", label, url);
        
        List<ElementData> elements;
        File snapshotFile = new File(label.equals("baseline") ? 
                config.getBaselineSnapshot() : config.getCurrentSnapshot());
        
        // Check if we already have a snapshot file
        if (snapshotFile.exists() && !url.startsWith("file:")) {
            logger.info("Loading existing {} snapshot from {}", label, snapshotFile.getAbsolutePath());
            elements = loadElementsFromFile(snapshotFile.getAbsolutePath());
        } else {
            // Create crawler and extract elements
            DOMCSSCrawler crawler = new DOMCSSCrawler(
                    config.isHeadless(),
                    null,
                    config.getViewportWidth(),
                    config.getViewportHeight());
            
            try {
                // Navigate and extract elements
                crawler.navigate(url, config.getWaitTime());
                elements = crawler.extractElements(config.getContainerXpath(), config.getMaxElements());
                
                // Save elements to file
                saveElementsToFile(elements, snapshotFile.getAbsolutePath());
            } finally {
                crawler.close();
            }
        }
        
        return elements;
    }
    
    /**
     * Save elements to JSON file
     * 
     * @param elements List of elements
     * @param filePath Path to save to
     * @throws IOException If there's an error writing the file
     */
    private void saveElementsToFile(List<ElementData> elements, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, elements);
        }
        
        logger.info("Saved {} elements to {}", elements.size(), filePath);
    }
    
    /**
     * Load elements from JSON file
     * 
     * @param filePath Path to load from
     * @return List of elements
     * @throws IOException If there's an error reading the file
     */
    private List<ElementData> loadElementsFromFile(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            ElementData[] elements = objectMapper.readValue(reader, ElementData[].class);
            List<ElementData> elementList = new ArrayList<>();
            for (ElementData element : elements) {
                elementList.add(element);
            }
            
            logger.info("Loaded {} elements from {}", elementList.size(), filePath);
            return elementList;
        }
    }
    
    /**
     * Save changes to JSON file
     * 
     * @param changes List of changes
     * @param filePath Path to save to
     * @throws IOException If there's an error writing the file
     */
    private void saveChangesToFile(List<ElementChange> changes, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, changes);
        }
        
        logger.info("Saved {} changes to {}", changes.size(), filePath);
    }
}
