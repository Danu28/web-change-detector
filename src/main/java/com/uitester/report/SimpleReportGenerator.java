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
        
        // HTML header with enhanced styling
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n<head>\n")
            .append("<title>UI Change Analysis Report</title>\n")
            .append("<meta charset='UTF-8'>\n")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
            .append("<style>\n")
            // Base styles
            .append("* { box-sizing: border-box; }\n")
            .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f8fafc; color: #1a202c; line-height: 1.6; }\n")
            .append("h1 { color: #2d3748; margin-bottom: 8px; font-size: 2.2em; }\n")
            .append("h2 { color: #4a5568; margin-top: 30px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }\n")
            .append("h3 { color: #2d3748; margin-top: 0; margin-bottom: 10px; }\n")
            
            // Header and navigation
            .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; text-align: center; }\n")
            .append(".header h1 { color: white; margin: 0; font-size: 2.5em; text-shadow: 0 2px 4px rgba(0,0,0,0.3); }\n")
            .append(".header .subtitle { opacity: 0.9; font-size: 1.1em; margin-top: 8px; }\n")
            
            // Summary cards
            .append(".summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n")
            .append(".summary-card { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-left: 4px solid #667eea; }\n")
            .append(".summary-card h3 { margin-top: 0; color: #4a5568; }\n")
            .append(".summary-number { font-size: 2.5em; font-weight: bold; color: #667eea; margin: 10px 0; }\n")
            .append(".summary-label { color: #718096; font-size: 0.9em; text-transform: uppercase; letter-spacing: 0.5px; }\n")
            
            // Change type badges
            .append(".change-types { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 15px; }\n")
            .append(".change-badge { background: #e2e8f0; color: #4a5568; padding: 8px 16px; border-radius: 20px; font-size: 0.9em; font-weight: 500; }\n")
            .append(".change-badge.critical { background: #fed7d7; color: #c53030; }\n")
            .append(".change-badge.structural { background: #feebc8; color: #dd6b20; }\n")
            .append(".change-badge.cosmetic { background: #bee3f8; color: #3182ce; }\n")
            .append(".change-badge.text { background: #d4edda; color: #38a169; }\n")
            
            // Individual changes
            .append(".change { background: white; border: 1px solid #e2e8f0; margin: 15px 0; padding: 20px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: transform 0.2s, box-shadow 0.2s; }\n")
            .append(".change:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(0,0,0,0.1); }\n")
            .append(".change.critical { border-left: 5px solid #f56565; background: linear-gradient(90deg, #fed7d7 0%, white 5%); }\n")
            .append(".change.cosmetic { border-left: 5px solid #4299e1; background: linear-gradient(90deg, #bee3f8 0%, white 5%); }\n")
            .append(".change.structural { border-left: 5px solid #ed8936; background: linear-gradient(90deg, #feebc8 0%, white 5%); }\n")
            .append(".change.text { border-left: 5px solid #48bb78; background: linear-gradient(90deg, #d4edda 0%, white 5%); }\n")
            
            // Change content
            .append(".change-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; }\n")
            .append(".change-title { font-size: 1.2em; font-weight: 600; color: #2d3748; }\n")
            .append(".change-type-badge { padding: 4px 12px; border-radius: 12px; font-size: 0.8em; font-weight: 500; text-transform: uppercase; }\n")
            .append(".element-name { font-weight: 600; color: #4a5568; background: #f7fafc; padding: 8px 12px; border-radius: 5px; margin-bottom: 10px; font-family: 'Courier New', monospace; }\n")
            .append(".summary-text { font-size: 1.1em; margin-bottom: 10px; color: #2d3748; font-weight: 500; }\n")
            .append(".details { color: #718096; background: #f7fafc; padding: 12px; border-radius: 5px; margin: 10px 0; border-left: 3px solid #cbd5e0; }\n")
            .append(".impact { font-style: italic; color: #4a5568; background: #edf2f7; padding: 10px; border-radius: 5px; margin-top: 10px; }\n")
            .append(".confidence-bar { background: #e2e8f0; height: 8px; border-radius: 4px; margin: 10px 0; overflow: hidden; }\n")
            .append(".confidence-fill { height: 100%; background: linear-gradient(90deg, #48bb78, #38a169); border-radius: 4px; transition: width 0.3s; }\n")
            .append(".confidence-text { font-size: 0.9em; color: #718096; margin-top: 5px; display: flex; justify-content: space-between; }\n")
            
            // Filters and search
            .append(".controls { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }\n")
            .append(".filter-group { display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }\n")
            .append(".filter-btn { padding: 8px 16px; border: 2px solid #e2e8f0; background: white; border-radius: 5px; cursor: pointer; transition: all 0.2s; }\n")
            .append(".filter-btn.active { background: #667eea; color: white; border-color: #667eea; }\n")
            .append(".search-box { padding: 10px; border: 2px solid #e2e8f0; border-radius: 5px; font-size: 1em; min-width: 200px; }\n")
            
            // Responsive design
            .append("@media (max-width: 768px) {\n")
            .append("  body { padding: 10px; }\n")
            .append("  .summary-grid { grid-template-columns: 1fr; }\n")
            .append("  .change-header { flex-direction: column; align-items: flex-start; }\n")
            .append("  .filter-group { flex-direction: column; align-items: stretch; }\n")
            .append("}\n")
            
            // Print styles
            .append("@media print {\n")
            .append("  body { background: white; }\n")
            .append("  .change:hover { transform: none; box-shadow: none; }\n")
            .append("  .controls { display: none; }\n")
            .append("}\n")
            .append("</style>\n")
            .append("</head>\n<body>\n");
        
        // Enhanced header section
        html.append("<div class='header'>\n")
            .append("<h1>UI Change Analysis Report</h1>\n")
            .append("<div class='subtitle'>Generated: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm:ss")))
            .append("</div>\n")
            .append("</div>\n");
        
        // Summary cards section
        Map<String, Long> changeTypeCount = changes.stream()
            .collect(Collectors.groupingBy(
                change -> change.getChangeType() != null ? change.getChangeType() : "unknown",
                Collectors.counting()
            ));
        
        // Calculate impact metrics
        long criticalChanges = changes.stream()
            .mapToLong(change -> "critical".equals(change.getClassification()) ? 1 : 0)
            .sum();
        
        long textChanges = changeTypeCount.getOrDefault("TEXT_MODIFICATION", 0L);
        long structuralChanges = changeTypeCount.getOrDefault("ELEMENT_ADDED", 0L) + 
                                changeTypeCount.getOrDefault("ELEMENT_REMOVED", 0L);
        long styleChanges = changeTypeCount.getOrDefault("STYLE_CHANGE", 0L);
        
        html.append("<div class='summary-grid'>\n")
            .append("<div class='summary-card'>\n")
            .append("<h3>Total Changes</h3>\n")
            .append("<div class='summary-number'>").append(changes.size()).append("</div>\n")
            .append("<div class='summary-label'>Detected Modifications</div>\n")
            .append("</div>\n")
            
            .append("<div class='summary-card'>\n")
            .append("<h3>Critical Issues</h3>\n")
            .append("<div class='summary-number'>").append(criticalChanges).append("</div>\n")
            .append("<div class='summary-label'>High Priority Changes</div>\n")
            .append("</div>\n")
            
            .append("<div class='summary-card'>\n")
            .append("<h3>Elements Scanned</h3>\n")
            .append("<div class='summary-number'>").append(oldElements.size()).append("</div>\n")
            .append("<div class='summary-label'>Baseline → ").append(newElements.size()).append(" Current</div>\n")
            .append("</div>\n")
            
            .append("<div class='summary-card'>\n")
            .append("<h3>Change Distribution</h3>\n")
            .append("<div class='change-types'>\n");
        
        if (textChanges > 0) {
            html.append("<span class='change-badge text'>Text: ").append(textChanges).append("</span>\n");
        }
        if (structuralChanges > 0) {
            html.append("<span class='change-badge structural'>Structure: ").append(structuralChanges).append("</span>\n");
        }
        if (styleChanges > 0) {
            html.append("<span class='change-badge cosmetic'>Style: ").append(styleChanges).append("</span>\n");
        }
        if (criticalChanges > 0) {
            html.append("<span class='change-badge critical'>Critical: ").append(criticalChanges).append("</span>\n");
        }
        
        html.append("</div>\n</div>\n</div>\n");
        
        // Interactive controls
        html.append("<div class='controls'>\n")
            .append("<div class='filter-group'>\n")
            .append("<span style='font-weight: 600; color: #4a5568;'>Filter by type:</span>\n")
            .append("<button class='filter-btn active' onclick='filterChanges(\"all\")'>All Changes</button>\n")
            .append("<button class='filter-btn' onclick='filterChanges(\"critical\")'>Critical Only</button>\n")
            .append("<button class='filter-btn' onclick='filterChanges(\"text\")'>Text Changes</button>\n")
            .append("<button class='filter-btn' onclick='filterChanges(\"structural\")'>Structural</button>\n")
            .append("<input type='text' class='search-box' placeholder='Search changes...' onkeyup='searchChanges(this.value)'>\n")
            .append("</div>\n</div>\n");
        
        // Individual changes with enhanced display
        html.append("<h2>Detailed Change Analysis</h2>\n");
        
        if (changes.isEmpty()) {
            html.append("<div style='text-align: center; padding: 40px; color: #718096;'>\n")
                .append("<h3>✅ No Changes Detected</h3>\n")
                .append("<p>The UI appears to be identical between baseline and current versions.</p>\n")
                .append("</div>\n");
        } else {
            for (int i = 0; i < changes.size(); i++) {
                ElementChange change = changes.get(i);
                ChangeAnalyzer.EnhancedChange analyzed = ChangeAnalyzer.analyzeChange(change);
                
                String changeClass = getEnhancedChangeClass(change);
                String changeIcon = getChangeIcon(change);
                String confidencePercent = String.format("%.1f", analyzed.getConfidence() * 100);
                
                html.append("<div class='change ").append(changeClass).append("' data-type='").append(changeClass).append("'>\n")
                    .append("<div class='change-header'>\n")
                    .append("<div class='change-title'>").append(changeIcon).append(" Change #").append(i + 1).append("</div>\n")
                    .append("<div class='change-type-badge' style='background: ").append(getBadgeColor(changeClass)).append("; color: white;'>")
                    .append(escapeHtml(analyzed.getChangeType())).append("</div>\n")
                    .append("</div>\n")
                    
                    .append("<div class='element-name'>&#128205; ").append(escapeHtml(analyzed.getElement())).append("</div>\n")
                    .append("<div class='summary-text'>").append(escapeHtml(analyzed.getSummary())).append("</div>\n");
                
                // Enhanced details section
                if (analyzed.getDetails() != null && !analyzed.getDetails().trim().isEmpty()) {
                    html.append("<div class='details'>\n")
                        .append("<strong>Details:</strong><br>\n")
                        .append(escapeHtml(analyzed.getDetails()).replace("\n", "<br>\n"))
                        .append("</div>\n");
                }
                
                // Impact assessment
                html.append("<div class='impact'>\n")
                    .append("<strong>&#128161; Impact Assessment:</strong> ")
                    .append(escapeHtml(analyzed.getImpact()))
                    .append("</div>\n");
                
                // Confidence visualization
                html.append("<div class='confidence-bar'>\n")
                    .append("<div class='confidence-fill' style='width: ").append(confidencePercent).append("%;'></div>\n")
                    .append("</div>\n")
                    .append("<div class='confidence-text'>\n")
                    .append("<span>Confidence Level</span>\n")
                    .append("<span><strong>").append(confidencePercent).append("%</strong> ")
                    .append(getConfidenceLabel(analyzed.getConfidence())).append("</span>\n")
                    .append("</div>\n")
                    .append("</div>\n");
            }
        }
        
        // JavaScript for interactivity
        html.append("<script>\n")
            .append("function filterChanges(type) {\n")
            .append("  const changes = document.querySelectorAll('.change');\n")
            .append("  const buttons = document.querySelectorAll('.filter-btn');\n")
            .append("  \n")
            .append("  // Update button states\n")
            .append("  buttons.forEach(btn => btn.classList.remove('active'));\n")
            .append("  event.target.classList.add('active');\n")
            .append("  \n")
            .append("  // Filter changes\n")
            .append("  changes.forEach(change => {\n")
            .append("    if (type === 'all') {\n")
            .append("      change.style.display = 'block';\n")
            .append("    } else {\n")
            .append("      const changeType = change.getAttribute('data-type');\n")
            .append("      change.style.display = changeType === type ? 'block' : 'none';\n")
            .append("    }\n")
            .append("  });\n")
            .append("}\n")
            .append("\n")
            .append("function searchChanges(query) {\n")
            .append("  const changes = document.querySelectorAll('.change');\n")
            .append("  const searchTerm = query.toLowerCase();\n")
            .append("  \n")
            .append("  changes.forEach(change => {\n")
            .append("    const text = change.textContent.toLowerCase();\n")
            .append("    change.style.display = text.includes(searchTerm) ? 'block' : 'none';\n")
            .append("  });\n")
            .append("}\n")
            .append("\n")
            .append("// Auto-scroll to first critical change\n")
            .append("document.addEventListener('DOMContentLoaded', function() {\n")
            .append("  const firstCritical = document.querySelector('.change.critical');\n")
            .append("  if (firstCritical) {\n")
            .append("    setTimeout(() => {\n")
            .append("      firstCritical.scrollIntoView({ behavior: 'smooth', block: 'center' });\n")
            .append("      firstCritical.style.animation = 'pulse 2s ease-in-out';\n")
            .append("    }, 1000);\n")
            .append("  }\n")
            .append("});\n")
            .append("</script>\n")
            .append("\n")
            .append("<style>\n")
            .append("@keyframes pulse {\n")
            .append("  0%, 100% { transform: scale(1); }\n")
            .append("  50% { transform: scale(1.02); }\n")
            .append("}\n")
            .append("</style>\n")
            .append("\n")
            .append("</body>\n</html>");
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html.toString());
        }
        
        logger.info("Simple report generated: {}", outputPath);
    }
    
    /**
     * Determine enhanced CSS class for change styling
     */
    private String getEnhancedChangeClass(ElementChange change) {
        String changeType = change.getChangeType();
        String classification = change.getClassification();
        
        if ("critical".equals(classification)) {
            return "critical";
        } else if ("TEXT_MODIFICATION".equals(changeType)) {
            return "text";
        } else if ("ELEMENT_ADDED".equals(changeType) || 
                   "ELEMENT_REMOVED".equals(changeType) || 
                   "structural".equals(changeType)) {
            return "structural";
        } else {
            return "cosmetic";
        }
    }
    
    /**
     * Get icon for change type
     */
    private String getChangeIcon(ElementChange change) {
        String changeType = change.getChangeType();
        String classification = change.getClassification();
        
        if ("critical".equals(classification)) {
            return "&#9888;"; // Warning symbol
        } else if ("TEXT_MODIFICATION".equals(changeType)) {
            return "&#9997;"; // Pencil
        } else if ("ELEMENT_ADDED".equals(changeType)) {
            return "&#10133;"; // Heavy plus sign
        } else if ("ELEMENT_REMOVED".equals(changeType)) {
            return "&#10134;"; // Heavy minus sign
        } else if ("STYLE_CHANGE".equals(changeType)) {
            return "&#127912;"; // Artist palette
        } else {
            return "&#8634;"; // Clockwise arrow
        }
    }
    
    /**
     * Get badge color for change type
     */
    private String getBadgeColor(String changeClass) {
        switch (changeClass) {
            case "critical": return "#e53e3e";
            case "text": return "#38a169";
            case "structural": return "#dd6b20";
            case "cosmetic": return "#3182ce";
            default: return "#718096";
        }
    }
    
    /**
     * Get confidence level label
     */
    private String getConfidenceLabel(double confidence) {
        if (confidence >= 0.9) {
            return "Very High";
        } else if (confidence >= 0.75) {
            return "High";
        } else if (confidence >= 0.5) {
            return "Medium";
        } else if (confidence >= 0.25) {
            return "Low";
        } else {
            return "Very Low";
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
