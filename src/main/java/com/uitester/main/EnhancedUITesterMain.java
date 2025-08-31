package com.uitester.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitester.core.Configuration;
import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
import com.uitester.crawler.DOMCSSCrawler;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;
import com.uitester.diff.ElementMatcher;
import com.uitester.report.SimpleReportGenerator;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enhanced UI Tester Main Application
 */
public class EnhancedUITesterMain {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedUITesterMain.class);

    private final WorkflowRunner workflowRunner;

    public EnhancedUITesterMain(Configuration config) {
        this.workflowRunner = new WorkflowRunner(config, config.getProjectConfig());
    }

    public static void main(String[] args) {
        logger.info("=".repeat(70));
        logger.info("🚀 ENHANCED UI TESTER - Advanced Change Detection System");
        logger.info("=".repeat(70));

        try {
            CommandLine cmd = CLIParser.parseCommandLine(args);
            Configuration config = ConfigFactory.createConfiguration(cmd);
            // Apply overrides AFTER base config created
            ConfigFactory.applyOverridesFromCLI(cmd, config);
            // Recompute output paths after potential sectionName/baseline/current updates
            config.updateOutputPaths();

            EnhancedUITesterMain app = new EnhancedUITesterMain(config);
            boolean compareOnly = cmd.hasOption("compare-only");
            app.workflowRunner.runEnhancedAnalysis(compareOnly);

            logger.info("✅ Enhanced UI Testing completed successfully!");
            logger.info("=".repeat(70));
            System.exit(0);
        } catch (Exception e) {
            logger.error("❌ Error running Enhanced UI Tester: {}", e.getMessage(), e);
            System.exit(1);
        }
    }


    // =====================================================================
    // INNER CLASSES
    // =====================================================================

    /**
     * CLI parsing
     */
    static class CLIParser {
        public static CommandLine parseCommandLine(String[] args) throws ParseException {
            Options options = new Options();

            options.addOption("b", "baseline", true, "Baseline URL");
            options.addOption("c", "current", true, "Current URL");
            options.addOption(null, "compare-only", false, "Use existing snapshots");
            options.addOption(null, "confidence-threshold", true, "Minimum text similarity threshold (maps to comparison.textSimilarityThreshold)");
            options.addOption("m", "max-elements", true, "Max elements");
            options.addOption("w", "wait-time", true, "Wait time (s)");
            options.addOption("h", "headless", false, "Headless mode");
            options.addOption(null, "xpath", true, "Container XPath");
            options.addOption("s", "section-name", true, "Section name");
            options.addOption(Option.builder()
                .longOpt("override")
                .hasArgs()
                .desc("Override config value(s) key=value (repeatable). Example: --override matching.fuzzyMinConfidence=0.7 --override performance.maxElements=2000")
                .build());
            options.addOption("?", "help", false, "Show help");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("java -jar enhanced-ui-tester.jar", options);
                System.exit(0);
            }
            return cmd;
        }
    }

    /**
     * Config creation
     */
    static class ConfigFactory {
        public static Configuration createConfiguration(CommandLine cmd) {
            Configuration config = new Configuration();
            if (cmd.hasOption("baseline")) config.setBaselineUrl(cmd.getOptionValue("baseline"));
            if (cmd.hasOption("current")) config.setCurrentUrl(cmd.getOptionValue("current"));
            if (cmd.hasOption("max-elements")) config.setMaxElements(Integer.parseInt(cmd.getOptionValue("max-elements")));
            if (cmd.hasOption("wait-time")) config.setWaitTime(Integer.parseInt(cmd.getOptionValue("wait-time")));
            if (cmd.hasOption("headless")) config.setHeadless(true);
            if (cmd.hasOption("xpath")) config.setContainerXpath(cmd.getOptionValue("xpath"));
            if (cmd.hasOption("section-name")) config.setSectionName(cmd.getOptionValue("section-name"));
            return config;
        }

        public static void applyOverridesFromCLI(CommandLine cmd, Configuration config) {
            Map<String, String> overrides = new java.util.HashMap<>();
            // Map legacy specific options to dot-notation overrides
            if (cmd.hasOption("confidence-threshold")) {
                overrides.put("comparison.textSimilarityThreshold", cmd.getOptionValue("confidence-threshold"));
            }
            if (cmd.hasOption("max-elements")) {
                overrides.put("performance.maxElements", cmd.getOptionValue("max-elements"));
            }
            // Generic overrides
            if (cmd.hasOption("override")) {
                String[] raw = cmd.getOptionValues("override");
                if (raw != null) {
                    for (String r : raw) {
                        if (r == null) continue;
                        int idx = r.indexOf('=');
                        if (idx > 0 && idx < r.length() - 1) {
                            String k = r.substring(0, idx).trim();
                            String v = r.substring(idx + 1).trim();
                            if (!k.isEmpty() && !v.isEmpty()) overrides.put(k, v);
                        } else {
                            logger.warn("Ignoring malformed override '{}'. Expected key=value", r);
                        }
                    }
                }
            }
            if (!overrides.isEmpty()) {
                logger.info("Applying {} override(s): {}", overrides.size(), overrides.keySet());
                config.applyOverrides(overrides);
            }
        }
    }

    /**
     * File handling
     */
    static class FileManager {
        private final ObjectMapper objectMapper = new ObjectMapper();

        public void saveElements(List<ElementData> elements, String filePath) throws IOException {
            writeToFile(elements, filePath);
            logger.info("💾 Saved {} elements to {}", elements.size(), filePath);
        }

        public List<ElementData> loadElements(String filePath) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) throw new IOException("File not found: " + filePath);
            List<ElementData> elements = objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ElementData.class));
            logger.info("📂 Loaded {} elements from {}", elements.size(), filePath);
            return elements;
        }

        public void saveChanges(List<ElementChange> changes, String filePath) throws IOException {
            writeToFile(changes, filePath);
            logger.info("💾 Saved {} changes to {}", changes.size(), filePath);
        }

        private <T> void writeToFile(T data, String filePath) throws IOException {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
            try (FileWriter writer = new FileWriter(filePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, data);
            }
        }
    }

    /**
     * Workflow
     */
    static class WorkflowRunner {
        private final Configuration config;
        private final ProjectConfig projectConfig;
        private final FileManager fileManager = new FileManager();

        WorkflowRunner(Configuration config, ProjectConfig projectConfig) {
            this.config = config;
            this.projectConfig = projectConfig;
        }

        public void runEnhancedAnalysis(boolean compareOnly) throws Exception {
            logger.info("🔍 Starting Enhanced UI Analysis Workflow");

            List<ElementData> baselineElements;
            List<ElementData> currentElements;

            if (compareOnly) {
                baselineElements = fileManager.loadElements(config.getBaselineSnapshot());
                currentElements = fileManager.loadElements(config.getCurrentSnapshot());
            } else {
                ElementCapture capture = new ElementCapture(config, projectConfig);
                if (config.isParallelCrawling()) {
                    logger.info("⚡ Parallel crawling enabled (baseline & current) ...");
                    java.util.concurrent.CompletableFuture<List<ElementData>> baseF = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return capture.captureEnhancedElements(config.getBaselineUrl(), "baseline"); } catch (Exception e) { throw new RuntimeException(e); }
                    });
                    java.util.concurrent.CompletableFuture<List<ElementData>> currF = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return capture.captureEnhancedElements(config.getCurrentUrl(), "current"); } catch (Exception e) { throw new RuntimeException(e); }
                    });
                    try {
                        baselineElements = baseF.get();
                        currentElements = currF.get();
                    } catch (Exception ex) {
                        logger.warn("Parallel capture failed, falling back to sequential: {}", ex.getMessage());
                        baselineElements = capture.captureEnhancedElements(config.getBaselineUrl(), "baseline");
                        currentElements = capture.captureEnhancedElements(config.getCurrentUrl(), "current");
                    }
                } else {
                    logger.info("🔁 Parallel crawling disabled; capturing sequentially...");
                    baselineElements = capture.captureEnhancedElements(config.getBaselineUrl(), "baseline");
                    currentElements = capture.captureEnhancedElements(config.getCurrentUrl(), "current");
                }
                fileManager.saveElements(baselineElements, config.getBaselineSnapshot());
                fileManager.saveElements(currentElements, config.getCurrentSnapshot());
            }

            ChangeWorkflow changeWorkflow = new ChangeWorkflow(projectConfig, config);
            List<ElementChange> changes = changeWorkflow.detectAndEnhanceChanges(baselineElements, currentElements);

            fileManager.saveChanges(changes, config.getChangesFile());

            ReportGenerator reportGen = new ReportGenerator(config);
            reportGen.generate(changes, baselineElements, currentElements);

            PerformanceSummary.print(baselineElements, currentElements, changes);
        }
    }

    /**
     * Capture elements
     */
    static class ElementCapture {
        private final Configuration config;
        private final ProjectConfig projectConfig;

        ElementCapture(Configuration config, ProjectConfig projectConfig) {
            this.config = config;
            this.projectConfig = projectConfig;
        }

        List<ElementData> captureEnhancedElements(String url, String label) throws Exception {
            logger.info("🌐 Capturing elements from {} page: {}", label, url);
            DOMCSSCrawler crawler = new DOMCSSCrawler(config.isHeadless(), null,
                    config.getViewportWidth(), config.getViewportHeight(), config);
            List<ElementData> elements;
            try {
                crawler.navigate(url, config.getWaitTime());
                elements = crawler.extractElements(config.getContainerXpath(), config.getMaxElements());
                for (ElementData e : elements) e.generateFingerprint(projectConfig);
            } finally {
                crawler.close();
            }
            logger.info("✅ Captured {} elements", elements.size());
            return elements;
        }
    }

    /**
     * Change workflow
     */
    static class ChangeWorkflow {
        private final ProjectConfig projectConfig;
        private final Configuration config;

        ChangeWorkflow(ProjectConfig projectConfig, Configuration config) {
            this.projectConfig = projectConfig;
            this.config = config;
        }

        List<ElementChange> detectAndEnhanceChanges(List<ElementData> baseline, List<ElementData> current) {
            logger.info("🎯 Matching elements...");
            ElementMatcher matcher = new ElementMatcher(projectConfig);
            ElementMatcher.MatchResult matchResult = matcher.matchElements(baseline, current);

            ChangeDetector detector = new ChangeDetector(config);
            List<ElementChange> changes = new ArrayList<>();

            // matched pairs
            matchResult.getMatchedPairs().forEach((b, c) ->
                    changes.addAll(detector.detectElementChanges(b, c)));

            // added
            for (ElementData e : matchResult.getAddedElements()) {
                ElementChange change = new ElementChange();
                change.setElement(e.getSelector());
                change.setChangeType("ELEMENT_ADDED");
                change.setNewValue(e.getTagName());
                changes.add(change);
            }

            // removed
            for (ElementData e : matchResult.getRemovedElements()) {
                ElementChange change = new ElementChange();
                change.setElement(e.getSelector());
                change.setChangeType("ELEMENT_REMOVED");
                change.setOldValue(e.getTagName());
                changes.add(change);
            }

            // Prune duplicate ancestor text modifications (noise reduction)
            pruneAncestorTextDuplicates(changes);

            logger.info("✅ {} changes detected", changes.size());
            return changes;
        }

        private void pruneAncestorTextDuplicates(List<ElementChange> changes) {
            if (changes == null || changes.isEmpty()) return;
            List<ElementChange> textChanges = new ArrayList<>();
            for (ElementChange c : changes) if ("TEXT_MODIFICATION".equals(c.getChangeType())) textChanges.add(c);
            List<ElementChange> toRemove = new ArrayList<>();
            java.util.function.Function<String,String> normSel = sel -> sel == null ? "" : sel.replaceAll(":nth-of-type\\(\\d+\\)", "").replaceAll("\\s+", " ").trim();
            java.util.function.BiFunction<String,String,Boolean> isAncestor = (a,b) -> {
                if (a == null || b == null) return false; if (a.equals(b)) return false; String na = normSel.apply(a); String nb = normSel.apply(b); if (nb.contains(na) && !na.isEmpty()) return true; return nb.startsWith(na + " >") || nb.startsWith(na + " "); };
            for (int i=0;i<textChanges.size();i++) {
                ElementChange a = textChanges.get(i); String aOld = a.getOldValue()==null?"":a.getOldValue(); String aNew = a.getNewValue()==null?"":a.getNewValue(); if (aOld.isEmpty()||aNew.isEmpty()) continue;
                for (int j=0;j<textChanges.size();j++){ if(i==j) continue; ElementChange b=textChanges.get(j); String bOld=b.getOldValue()==null?"":b.getOldValue(); String bNew=b.getNewValue()==null?"":b.getNewValue(); if(bOld.isEmpty()||bNew.isEmpty()) continue; boolean aAnc=isAncestor.apply(a.getElement(), b.getElement()); boolean bAnc=isAncestor.apply(b.getElement(), a.getElement()); if(!aAnc && !bAnc) continue; if(aAnc && aOld.contains(bOld) && aNew.contains(bNew) && (aOld.length()>bOld.length()||aNew.length()>bNew.length())) { toRemove.add(a); continue;} if(bAnc && bOld.contains(aOld) && bNew.contains(aNew) && (bOld.length()>aOld.length()||bNew.length()>aNew.length())) { toRemove.add(b);} }
            }
            // Token-based second pass
            List<ElementChange> survivors = new ArrayList<>(textChanges); survivors.removeAll(toRemove);
            for (ElementChange leaf : new ArrayList<>(survivors)) { String leafOld=leaf.getOldValue(); String leafNew=leaf.getNewValue(); if(leafOld==null||leafNew==null) continue; if(leafOld.length()>60||leafNew.length()>60) continue; for(ElementChange other:survivors){ if(leaf==other) continue; if(toRemove.contains(other)) continue; String oOld=other.getOldValue(); String oNew=other.getNewValue(); if(oOld==null||oNew==null) continue; if(oOld.length()<=leafOld.length()) continue; if(oOld.contains(leafOld) && oNew.contains(leafNew)) toRemove.add(other); } }
            if(!toRemove.isEmpty()){ changes.removeAll(new java.util.HashSet<>(toRemove)); logger.info("🧹 Pruned {} ancestor/aggregate duplicate text changes", toRemove.size()); }
        }
    }

    /**
     * Report generation
     */
    static class ReportGenerator {
        private final Configuration config;

        ReportGenerator(Configuration config) {
            this.config = config;
        }

        void generate(List<ElementChange> changes,
                      List<ElementData> baseline,
                      List<ElementData> current) throws IOException {
            String simplePath = config.getReportSimpleFile() != null ? config.getReportSimpleFile() : config.getReportFile().replace(".html", "-simple.html");
            new SimpleReportGenerator(config.getProjectConfig()).generateReport(changes, baseline, current, simplePath);
            logger.info("📄 Report generated: {}", simplePath);
        }
    }

    /**
     * Performance summary
     */
    static class PerformanceSummary {
        static void print(List<ElementData> baseline, List<ElementData> current, List<ElementChange> changes) {
            logger.info("📊 PERFORMANCE SUMMARY");
            logger.info("   Elements Analyzed: {} baseline + {} current = {}",
                    baseline.size(), current.size(), baseline.size() + current.size());
            logger.info("   Changes Detected: {}", changes.size());
        }
    }
}
