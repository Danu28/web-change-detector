package com.uitester.report;

import com.uitester.diff.ElementChange;
import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
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
    private final ProjectConfig config;

    public SimpleReportGenerator() { this.config = null; }
    public SimpleReportGenerator(ProjectConfig config) { this.config = config; }
    
    /**
     * Generate a simple, readable HTML report
     */
    public void generateReport(List<ElementChange> changes,
                               List<ElementData> oldElements,
                               List<ElementData> newElements,
                               String outputPath) throws IOException {
        
        StringBuilder html = new StringBuilder();
        
        // Resolve theming
        Map<String,String> theme = config != null && config.getReportingSettings() != null ?
            config.getReportingSettings().getThemeColors() : null;
        String accent1 = theme != null && theme.get("accentPrimary") != null ? theme.get("accentPrimary") : "#667eea";
        String accent2 = theme != null && theme.get("accentSecondary") != null ? theme.get("accentSecondary") : "#764ba2";
        String bgSoft = theme != null && theme.get("backgroundSoft") != null ? theme.get("backgroundSoft") : "#f8fafc";
        String panelBg = theme != null && theme.get("panelBackground") != null ? theme.get("panelBackground") : "#ffffff";
        String borderCol = theme != null && theme.get("borderColor") != null ? theme.get("borderColor") : "#e2e8f0";

        // HTML header with enhanced styling
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n<head>\n")
            .append("<title>UI Change Analysis Report</title>\n")
            .append("<meta charset='UTF-8'>\n")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
            .append("<style>\n")
            // Base styles
            .append("* { box-sizing: border-box; }\n")
            .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: ").append(bgSoft).append("; color: #1a202c; line-height: 1.6; }\n")
            .append("h1 { color: #2d3748; margin-bottom: 8px; font-size: 2.2em; }\n")
            .append("h2 { color: #4a5568; margin-top: 30px; margin-bottom: 15px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }\n")
            .append("h3 { color: #2d3748; margin-top: 0; margin-bottom: 10px; }\n")
            
            // Header and navigation
            .append(".header { background: linear-gradient(135deg, ").append(accent1).append(" 0%, ").append(accent2).append(" 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; text-align: center; }\n")
            .append(".header h1 { color: white; margin: 0; font-size: 2.5em; text-shadow: 0 2px 4px rgba(0,0,0,0.3); }\n")
            .append(".header .subtitle { opacity: 0.9; font-size: 1.1em; margin-top: 8px; }\n")
            
            // Summary cards
            .append(".summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n")
            .append(".summary-card { background: ").append(panelBg).append("; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-left: 4px solid ").append(accent1).append("; }\n")
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
            .append(".change { background: ").append(panelBg).append("; border: 1px solid ").append(borderCol).append("; margin: 15px 0; padding: 20px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: transform 0.2s, box-shadow 0.2s; }\n")
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
        
    // Sort changes by severity order if configured
    List<String> severityOrder = config != null && config.getReportingSettings() != null ?
        config.getReportingSettings().getSeverityOrder() : null;
    if (severityOrder != null && !severityOrder.isEmpty()) {
        changes.sort((a, b) -> severityCompare(a, b, severityOrder));
    }

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
                
                String changeClass = getEnhancedChangeClass(change);
                String changeIcon = getChangeIcon(change);
                String confidencePercent = change.getMatchConfidence() != null ? 
                    String.format("%.1f", change.getMatchConfidence() * 100) : "N/A";
                
                // Determine base type tag separate from severity (so critical text still matches 'text')
                String baseTypeTag;
                if ("TEXT_MODIFICATION".equals(change.getChangeType())) {
                    baseTypeTag = "text";
                } else if ("ELEMENT_ADDED".equals(change.getChangeType()) || "ELEMENT_REMOVED".equals(change.getChangeType()) || "structural".equals(change.getChangeType())) {
                    baseTypeTag = "structural";
                } else if ("critical".equals(changeClass)) { // fallback if only classification provided
                    baseTypeTag = "critical"; // will be expanded below
                } else {
                    baseTypeTag = "cosmetic"; // default bucket
                }
                // Build data-groups list (space-separated) for filtering
                StringBuilder groups = new StringBuilder(baseTypeTag);
                if (!changeClass.equals(baseTypeTag)) {
                    groups.append(' ').append(changeClass);
                }
                html.append("<div class='change ").append(changeClass).append("' data-type='").append(baseTypeTag)
                    .append("' data-groups='").append(groups).append("'>\n")
                    .append("<div class='change-header'>\n")
                    .append("<div class='change-title'>").append(changeIcon).append(" Change #").append(i + 1).append("</div>\n")
                    .append("<div class='change-type-badge' style='background: ").append(getBadgeColor(changeClass)).append("; color: white;'>")
                    .append(escapeHtml(change.getChangeType())).append("</div>\n")
                    .append("</div>\n")
                    
                    .append("<div class='element-name'>&#128205; ").append(escapeHtml(change.getElement())).append("</div>\n")
                    .append("<div class='summary-text'>").append(escapeHtml(change.getProperty() + " changed")).append("</div>\n");
                
                // Enhanced details section
                String details = getChangeDetails(change);
                if (details != null && !details.trim().isEmpty()) {
                    html.append("<div class='details'>\n")
                        .append("<strong>Details:</strong><br>\n")
                        .append(escapeHtml(details).replace("\n", "<br>\n"))
                        .append("</div>\n");
                }
                
                // Impact assessment
                html.append("<div class='impact'>\n")
                    .append("<strong>&#128161; Impact Assessment:</strong> ")
                    .append(escapeHtml(getChangeImpact(change)))
                    .append("</div>\n");
                
                // Confidence visualization
        boolean showConfidence = config == null || config.getReportingSettings() == null ||
            config.getReportingSettings().getEnableConfidenceBar() == null ||
            Boolean.TRUE.equals(config.getReportingSettings().getEnableConfidenceBar());
        if (showConfidence) {
            html.append("<div class='confidence-bar'>\n")
                .append("<div class='confidence-fill' style='width: ").append(confidencePercent).append("%;'></div>\n")
                .append("</div>\n")
                .append("<div class='confidence-text'>\n")
                .append("<span>Confidence Level</span>\n")
                .append("<span><strong>").append(confidencePercent).append("%</strong> ")
                .append(getConfidenceLabel(change.getMatchConfidence() != null ? change.getMatchConfidence() : 0.0)).append("</span>\n")
                .append("</div>\n");
        }
        html.append("</div>\n");
            }
        }
        
        // JavaScript for interactivity
        String autoScrollTarget = null;
        if (config != null && config.getReportingSettings() != null) {
            autoScrollTarget = config.getReportingSettings().getAutoScrollTo();
        }
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
            .append("    if (type === 'all') { change.style.display='block'; return; }\n")
            .append("    const groups = (change.getAttribute('data-groups')||'').split(/\\s+/);\n")
            .append("    change.style.display = groups.includes(type) ? 'block' : 'none';\n")
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
            .append("// Auto-scroll based on config target\n")
            .append("document.addEventListener('DOMContentLoaded', function() {\n")
            .append("  const targetClass = '").append(autoScrollTarget != null ? autoScrollTarget : "critical").append("';\n")
            .append("  const first = document.querySelector('.change.' + targetClass);\n")
            .append("  if (first) { setTimeout(()=>{ first.scrollIntoView({behavior:'smooth',block:'center'}); first.style.animation='pulse 2s ease-in-out'; },1000);}\n")
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
        if (config != null && config.getReportingSettings() != null &&
                config.getReportingSettings().getBadgeColors() != null) {
            String custom = config.getReportingSettings().getBadgeColors().get(changeClass);
            if (custom != null && !custom.isEmpty()) return custom;
        }
        switch (changeClass) {
            case "critical": return "#e53e3e";
            case "text": return "#38a169";
            case "structural": return "#dd6b20";
            case "cosmetic": return "#3182ce";
            default: return "#718096";
        }
    }

    private int severityCompare(ElementChange a, ElementChange b, List<String> order) {
        return Integer.compare(severityIndex(a, order), severityIndex(b, order));
    }
    private int severityIndex(ElementChange c, List<String> order) {
        String cls = getEnhancedChangeClass(c);
        int idx = order.indexOf(cls);
        return idx >= 0 ? idx : Integer.MAX_VALUE;
    }
    
    /**
     * Get confidence level label
     */
    private String getConfidenceLabel(double confidence) {
        if (config != null && config.getReportingSettings() != null && config.getReportingSettings().getConfidenceLevels() != null) {
            // Expect list ordered by descending min
            List<Map<String,Object>> levels = config.getReportingSettings().getConfidenceLevels();
            for (Map<String,Object> lvl : levels) {
                Object minObj = lvl.get("min"); Object labelObj = lvl.get("label");
                double min = minObj instanceof Number ? ((Number)minObj).doubleValue() : 0.0;
                if (confidence >= min) {
                    return labelObj != null ? labelObj.toString() : "Level";
                }
            }
        }
        // Fallback static mapping
        if (confidence >= 0.9) return "Very High";
        if (confidence >= 0.75) return "High";
        if (confidence >= 0.5) return "Medium";
        if (confidence >= 0.25) return "Low";
        return "Very Low";
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
    
    /**
     * Extract details from change object
     */
    private String getChangeDetails(ElementChange change) {
        StringBuilder details = new StringBuilder();
        
        if (change.getOldValue() != null && change.getNewValue() != null) {
            details.append("Changed from: ").append(change.getOldValue())
                   .append("\nTo: ").append(change.getNewValue());
        } else if (change.getOldValue() != null) {
            details.append("Removed: ").append(change.getOldValue());
        } else if (change.getNewValue() != null) {
            details.append("Added: ").append(change.getNewValue());
        }
        
        return details.toString();
    }
    
    /**
     * Generate impact assessment based on change type
     */
    private String getChangeImpact(ElementChange change) {
        String changeType = change.getChangeType();
        
        if (changeType == null) {
            return "Unknown impact";
        }
        
        if (changeType.contains("ADDED")) {
            return "New element added to the interface";
        } else if (changeType.contains("REMOVED")) {
            return "Element removed from the interface";
        } else if (changeType.contains("CSS")) {
            return "Visual style changes that might affect user experience";
        } else if (changeType.contains("TEXT")) {
            return "Content changes that might affect user understanding";
        } else if (changeType.contains("ATTRIBUTE")) {
            return "Attribute changes that might affect functionality";
        } else {
            return "Changes detected that might affect user experience";
        }
    }
}
