package com.uitester.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uitester.diff.ElementChange;

/**
 * Generates HTML reports for UI changes.
 * This is equivalent to the Python ReportGenerator class.
 */
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    private TemplateEngine templateEngine;
    
    /**
     * Initialize the report generator
     */
    public ReportGenerator() {
        this.templateEngine = createTemplateEngine();
    }
    
    /**
     * Create a Thymeleaf template engine
     * 
     * @return Configured TemplateEngine
     */
    private TemplateEngine createTemplateEngine() {
        // Create and configure the template resolver
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        
        // Create template engine with the resolver
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);
        
        return engine;
    }
    
    /**
     * Calculate statistics for the summary section
     * 
     * @param changes List of detected changes
     * @return Map with statistics
     */
    public Map<String, Integer> calculateStats(List<ElementChange> changes) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("critical", 0);
        stats.put("cosmetic", 0);
        stats.put("noise", 0);
        stats.put("total", changes.size());
        
        for (ElementChange change : changes) {
            String classification = change.getClassification() != null ? change.getClassification().toLowerCase() : "noise";
            
            if ("critical".equals(classification)) {
                stats.put("critical", stats.get("critical") + 1);
            } else if ("cosmetic".equals(classification)) {
                stats.put("cosmetic", stats.get("cosmetic") + 1);
            } else {
                stats.put("noise", stats.get("noise") + 1);
            }
        }
        
        return stats;
    }
    
    /**
     * Generate HTML report from classified changes
     * 
     * @param changes List of detected changes
     * @param url The URL that was tested
     * @param outputFile Path to write the report to
     * @throws IOException If there's an error writing the file
     */
    public void generateReport(List<ElementChange> changes, String url, String outputFile) throws IOException {
        // Create report data
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("url", url);
        reportData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        reportData.put("report_timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " UTC");
        reportData.put("changes", changes);
        reportData.put("stats", calculateStats(changes));
        
        // Create Thymeleaf context and process template
        Context context = new Context();
        context.setVariables(reportData);
        
        String htmlContent = templateEngine.process("report_template", context);
        
        // Ensure output directory exists
        File outputDir = new File(outputFile).getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(htmlContent);
        }
        
        logger.info("Report generated: {}", outputFile);
    }
}
