package com.uitester.report;

import com.uitester.diff.ElementChange;
import com.uitester.core.ElementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple HTML report generator that produces human-readable change summaries
 * instead of raw CSS dumps.
 */
public class SimpleReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SimpleReportGenerator.class);
    
    /**
     * Generate a simple, readable HTML report
     */
    public void generateReport(List<ElementChange> changes, 
                              List<ElementData> oldElements, 
                              List<ElementData> newElements, 
                              String outputPath) throws IOException {
        
        StringBuilder html = new StringBuilder();
        
        // HTML header
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n<head>\n")
            .append("<title>UI Change Analysis Report</title>\n")
            .append("<style>\n")
            .append("body { font-family: Arial, sans-serif; margin: 20px; }\n")
            .append(".summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n")
            .append(".change { border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 5px; }\n")
            .append(".change.critical { border-left: 5px solid #ff4444; }\n")
            .append(".change.cosmetic { border-left: 5px solid #4444ff; }\n")
            .append(".change.structural { border-left: 5px solid #ff8800; }\n")
            .append(".confidence { color: #666; font-size: 0.9em; }\n")
            .append(".element-name { font-weight: bold; color: #333; }\n")
            .append(".summary-text { font-size: 1.1em; margin-bottom: 5px; }\n")
            .append(".details { color: #555; }\n")
            .append(".impact { font-style: italic; color: #777; }\n")
            .append("</style>\n")
            .append("</head>\n<body>\n");
        
        // Report title and timestamp
        html.append("<h1>UI Change Analysis Report</h1>\n")
            .append("<p><strong>Generated:</strong> ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</p>\n");
        
        // Summary section
        html.append("<div class='summary'>\n")
            .append("<h2>Summary</h2>\n")
            .append("<p><strong>Total Changes:</strong> ").append(changes.size()).append("</p>\n")
            .append("<p><strong>Old Elements:</strong> ").append(oldElements.size()).append("</p>\n")
            .append("<p><strong>New Elements:</strong> ").append(newElements.size()).append("</p>\n");
        
        // Change type breakdown
        Map<String, Long> changeTypeCount = changes.stream()
            .collect(Collectors.groupingBy(
                change -> change.getChangeType() != null ? change.getChangeType() : "unknown",
                Collectors.counting()
            ));
        
        html.append("<h3>Change Types:</h3>\n<ul>\n");
        for (Map.Entry<String, Long> entry : changeTypeCount.entrySet()) {
            html.append("<li><strong>").append(entry.getKey()).append(":</strong> ")
                .append(entry.getValue()).append(" changes</li>\n");
        }
        html.append("</ul>\n</div>\n");
        
        // Individual changes
        html.append("<h2>Detailed Changes</h2>\n");
        
        if (changes.isEmpty()) {
            html.append("<p>No changes detected.</p>\n");
        } else {
            for (int i = 0; i < changes.size(); i++) {
                ElementChange change = changes.get(i);
                ChangeAnalyzer.EnhancedChange analyzed = ChangeAnalyzer.analyzeChange(change);
                
                String changeClass = getChangeClass(change);
                
                html.append("<div class='change ").append(changeClass).append("'>\n")
                    .append("<h3>Change #").append(i + 1).append("</h3>\n")
                    .append("<div class='element-name'>Element: ").append(escapeHtml(analyzed.getElement())).append("</div>\n")
                    .append("<div class='summary-text'>").append(escapeHtml(analyzed.getSummary())).append("</div>\n")
                    .append("<div class='details'>").append(escapeHtml(analyzed.getDetails())).append("</div>\n")
                    .append("<div class='impact'>Impact: ").append(escapeHtml(analyzed.getImpact())).append("</div>\n")
                    .append("<div class='confidence'>Confidence: ")
                    .append(String.format("%.1f%%", analyzed.getConfidence() * 100))
                    .append(" | Type: ").append(escapeHtml(analyzed.getChangeType())).append("</div>\n")
                    .append("</div>\n");
            }
        }
        
        // Close HTML
        html.append("</body>\n</html>");
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html.toString());
        }
        
        logger.info("Simple report generated: {}", outputPath);
    }
    
    /**
     * Determine CSS class for change styling
     */
    private String getChangeClass(ElementChange change) {
        String classification = change.getClassification();
        if ("critical".equals(classification)) {
            return "critical";
        } else if ("structural".equals(change.getChangeType()) || 
                   "ELEMENT_ADDED".equals(change.getChangeType()) || 
                   "ELEMENT_REMOVED".equals(change.getChangeType())) {
            return "structural";
        } else {
            return "cosmetic";
        }
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}
