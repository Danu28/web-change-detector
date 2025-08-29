package com.uitester.core;

import com.uitester.diff.ElementChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 3: Advanced structural analysis for UI change detection.
 * Analyzes DOM structure, element relationships, and provides context-aware insights.
 */
public class StructuralAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(StructuralAnalyzer.class);
    
    private final ProjectConfig config;
    
    public StructuralAnalyzer(ProjectConfig config) {
        this.config = config;
    }
    
    /**
     * Represents a node in the DOM tree structure
     */
    public static class StructuralNode {
        private ElementData element;
        private StructuralNode parent;
        private List<StructuralNode> children;
        private int depth;
        private String path; // XPath-like structural path
        
        public StructuralNode(ElementData element) {
            this.element = element;
            this.children = new ArrayList<>();
            this.depth = 0;
            this.path = "";
        }
        
        // Getters and setters
        public ElementData getElement() { return element; }
        public void setElement(ElementData element) { this.element = element; }
        
        public StructuralNode getParent() { return parent; }
        public void setParent(StructuralNode parent) { 
            this.parent = parent; 
            if (parent != null) {
                this.depth = parent.depth + 1;
            }
        }
        
        public List<StructuralNode> getChildren() { return children; }
        public void addChild(StructuralNode child) { 
            children.add(child); 
            child.setParent(this);
        }
        
        public int getDepth() { return depth; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        /**
         * Get sibling nodes (nodes with same parent)
         */
        public List<StructuralNode> getSiblings() {
            if (parent == null) return new ArrayList<>();
            return parent.getChildren().stream()
                .filter(node -> node != this)
                .collect(Collectors.toList());
        }
        
        /**
         * Check if this node is an ancestor of another node
         */
        public boolean isAncestorOf(StructuralNode other) {
            StructuralNode current = other.getParent();
            while (current != null) {
                if (current == this) return true;
                current = current.getParent();
            }
            return false;
        }
        
        /**
         * Get all descendant nodes
         */
        public List<StructuralNode> getDescendants() {
            List<StructuralNode> descendants = new ArrayList<>();
            for (StructuralNode child : children) {
                descendants.add(child);
                descendants.addAll(child.getDescendants());
            }
            return descendants;
        }
    }
    
    /**
     * Represents structural analysis results
     */
    public static class StructuralAnalysis {
        private StructuralNode rootNode;
        private Map<String, StructuralNode> selectorToNode;
        private Map<Integer, List<StructuralNode>> depthToNodes;
        private List<StructuralPattern> patterns;
        private StructuralMetrics metrics;
        
        public StructuralAnalysis() {
            this.selectorToNode = new HashMap<>();
            this.depthToNodes = new HashMap<>();
            this.patterns = new ArrayList<>();
        }
        
        // Getters and setters
        public StructuralNode getRootNode() { return rootNode; }
        public void setRootNode(StructuralNode rootNode) { this.rootNode = rootNode; }
        
        public Map<String, StructuralNode> getSelectorToNode() { return selectorToNode; }
        public Map<Integer, List<StructuralNode>> getDepthToNodes() { return depthToNodes; }
        public List<StructuralPattern> getPatterns() { return patterns; }
        public StructuralMetrics getMetrics() { return metrics; }
        public void setMetrics(StructuralMetrics metrics) { this.metrics = metrics; }
    }
    
    /**
     * Represents identified structural patterns in the DOM
     */
    public static class StructuralPattern {
        private String patternType; // "navigation", "content-grid", "form", "list"
        private List<StructuralNode> nodes;
        private String description;
        private double confidence;
        
        public StructuralPattern(String patternType, List<StructuralNode> nodes, String description, double confidence) {
            this.patternType = patternType;
            this.nodes = nodes;
            this.description = description;
            this.confidence = confidence;
        }
        
        // Getters
        public String getPatternType() { return patternType; }
        public List<StructuralNode> getNodes() { return nodes; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
    }
    
    /**
     * Structural metrics for the analyzed DOM
     */
    public static class StructuralMetrics {
        private int totalNodes;
        private int maxDepth;
        private double averageDepth;
        private Map<String, Integer> tagCounts;
        private int leafNodes;
        private double branchingFactor;
        
        public StructuralMetrics() {
            this.tagCounts = new HashMap<>();
        }
        
        // Getters and setters
        public int getTotalNodes() { return totalNodes; }
        public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
        
        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
        
        public double getAverageDepth() { return averageDepth; }
        public void setAverageDepth(double averageDepth) { this.averageDepth = averageDepth; }
        
        public Map<String, Integer> getTagCounts() { return tagCounts; }
        
        public int getLeafNodes() { return leafNodes; }
        public void setLeafNodes(int leafNodes) { this.leafNodes = leafNodes; }
        
        public double getBranchingFactor() { return branchingFactor; }
        public void setBranchingFactor(double branchingFactor) { this.branchingFactor = branchingFactor; }
    }
    
    /**
     * Build structural analysis from a list of elements
     */
    public StructuralAnalysis analyzeStructure(List<ElementData> elements) {
        logger.info("Starting structural analysis of {} elements", elements.size());
        
        StructuralAnalysis analysis = new StructuralAnalysis();
        
        // Build the tree structure
        StructuralNode rootNode = buildStructuralTree(elements, analysis);
        analysis.setRootNode(rootNode);
        
        // Calculate metrics
        StructuralMetrics metrics = calculateMetrics(analysis);
        analysis.setMetrics(metrics);
        
        // Identify patterns
        List<StructuralPattern> patterns = identifyPatterns(analysis);
        analysis.getPatterns().addAll(patterns);
        
        logger.info("Structural analysis complete: {} nodes, max depth {}, {} patterns identified", 
                   metrics.getTotalNodes(), metrics.getMaxDepth(), patterns.size());
        
        return analysis;
    }
    
    /**
     * Build a tree structure from flat element list using selector analysis
     */
    private StructuralNode buildStructuralTree(List<ElementData> elements, StructuralAnalysis analysis) {
        // Create a virtual root node
        StructuralNode root = new StructuralNode(null);
        root.setPath("/");
        
        // Sort elements by selector complexity (simpler selectors are likely higher in tree)
        List<ElementData> sortedElements = elements.stream()
            .sorted((a, b) -> getSelectorComplexity(a.getSelector()) - getSelectorComplexity(b.getSelector()))
            .collect(Collectors.toList());
        
        // Build nodes and establish relationships
        for (ElementData element : sortedElements) {
            StructuralNode node = new StructuralNode(element);
            String path = generateStructuralPath(element);
            node.setPath(path);
            
            analysis.getSelectorToNode().put(element.getSelector(), node);
            
            // Find parent based on selector hierarchy
            StructuralNode parent = findParentNode(node, analysis.getSelectorToNode(), root);
            parent.addChild(node);
            
            // Group by depth
            analysis.getDepthToNodes()
                .computeIfAbsent(node.getDepth(), k -> new ArrayList<>())
                .add(node);
        }
        
        return root;
    }
    
    /**
     * Calculate structural complexity of a CSS selector
     */
    private int getSelectorComplexity(String selector) {
        if (selector == null) return 0;
        
        // Count various selector components
        int complexity = 0;
        complexity += selector.split("\\s+").length; // Space-separated parts
        complexity += selector.split(">").length - 1; // Direct child selectors
        complexity += selector.split("\\+").length - 1; // Adjacent sibling selectors
        complexity += selector.split("~").length - 1; // General sibling selectors
        complexity += (selector.length() - selector.replace(":", "").length()); // Pseudo selectors
        complexity += (selector.length() - selector.replace("[", "").length()); // Attribute selectors
        
        return complexity;
    }
    
    /**
     * Generate a structural path for an element based on its properties
     */
    private String generateStructuralPath(ElementData element) {
        StringBuilder path = new StringBuilder("/");
        
        if (element.getTagName() != null) {
            path.append(element.getTagName());
            
            // Add class information if available
            String classAttr = element.getAttributes() != null ? 
                element.getAttributes().get("class") : null;
            if (classAttr != null && !classAttr.isEmpty()) {
                path.append(".").append(classAttr.split("\\s+")[0]); // Use first class
            }
            
            // Add ID if available
            String idAttr = element.getAttributes() != null ? 
                element.getAttributes().get("id") : null;
            if (idAttr != null && !idAttr.isEmpty()) {
                path.append("#").append(idAttr);
            }
        }
        
        return path.toString();
    }
    
    /**
     * Find the most appropriate parent node for a given node
     */
    private StructuralNode findParentNode(StructuralNode node, Map<String, StructuralNode> selectorToNode, StructuralNode root) {
        // For now, use a simple heuristic based on selector similarity
        // In a real implementation, this would use DOM hierarchy information
        
        String nodeSelector = node.getElement().getSelector();
        StructuralNode bestParent = root;
        int bestSimilarity = 0;
        
        for (StructuralNode candidate : selectorToNode.values()) {
            if (candidate != node && candidate.getDepth() < 10) { // Avoid too deep nesting
                int similarity = calculateSelectorSimilarity(nodeSelector, candidate.getElement().getSelector());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestParent = candidate;
                }
            }
        }
        
        return bestParent;
    }
    
    /**
     * Calculate similarity between two CSS selectors
     */
    private int calculateSelectorSimilarity(String selector1, String selector2) {
        if (selector1 == null || selector2 == null) return 0;
        
        // Simple similarity based on common parts
        String[] parts1 = selector1.split("[\\s>+~]");
        String[] parts2 = selector2.split("[\\s>+~]");
        
        int commonParts = 0;
        for (String part1 : parts1) {
            for (String part2 : parts2) {
                if (part1.equals(part2)) {
                    commonParts++;
                    break;
                }
            }
        }
        
        return commonParts;
    }
    
    /**
     * Calculate structural metrics for the analyzed tree
     */
    private StructuralMetrics calculateMetrics(StructuralAnalysis analysis) {
        StructuralMetrics metrics = new StructuralMetrics();
        
        // Count total nodes
        int totalNodes = analysis.getSelectorToNode().size();
        metrics.setTotalNodes(totalNodes);
        
        // Calculate max depth and average depth
        int maxDepth = 0;
        int depthSum = 0;
        int leafCount = 0;
        
        for (StructuralNode node : analysis.getSelectorToNode().values()) {
            int depth = node.getDepth();
            maxDepth = Math.max(maxDepth, depth);
            depthSum += depth;
            
            if (node.getChildren().isEmpty()) {
                leafCount++;
            }
            
            // Count tag types
            if (node.getElement() != null && node.getElement().getTagName() != null) {
                String tagName = node.getElement().getTagName().toLowerCase();
                metrics.getTagCounts().merge(tagName, 1, Integer::sum);
            }
        }
        
        metrics.setMaxDepth(maxDepth);
        metrics.setAverageDepth(totalNodes > 0 ? (double) depthSum / totalNodes : 0);
        metrics.setLeafNodes(leafCount);
        
        // Calculate branching factor
        int totalBranches = 0;
        int nodesWithChildren = 0;
        for (StructuralNode node : analysis.getSelectorToNode().values()) {
            if (!node.getChildren().isEmpty()) {
                totalBranches += node.getChildren().size();
                nodesWithChildren++;
            }
        }
        metrics.setBranchingFactor(nodesWithChildren > 0 ? (double) totalBranches / nodesWithChildren : 0);
        
        return metrics;
    }
    
    /**
     * Identify common structural patterns in the DOM
     */
    private List<StructuralPattern> identifyPatterns(StructuralAnalysis analysis) {
        List<StructuralPattern> patterns = new ArrayList<>();
        
        // Identify navigation patterns
        patterns.addAll(identifyNavigationPatterns(analysis));
        
        // Identify list patterns
        patterns.addAll(identifyListPatterns(analysis));
        
        // Identify form patterns
        patterns.addAll(identifyFormPatterns(analysis));
        
        // Identify grid/table patterns
        patterns.addAll(identifyGridPatterns(analysis));
        
        return patterns;
    }
    
    /**
     * Identify navigation-related structural patterns
     */
    private List<StructuralPattern> identifyNavigationPatterns(StructuralAnalysis analysis) {
        List<StructuralPattern> patterns = new ArrayList<>();
        
        // Look for nav elements or elements with navigation-related classes/roles
        List<StructuralNode> navNodes = analysis.getSelectorToNode().values().stream()
            .filter(node -> {
                if (node.getElement() == null) return false;
                
                String tagName = node.getElement().getTagName();
                if ("nav".equalsIgnoreCase(tagName)) return true;
                
                Map<String, String> attrs = node.getElement().getAttributes();
                if (attrs != null) {
                    String classAttr = attrs.get("class");
                    String roleAttr = attrs.get("role");
                    
                    if (classAttr != null && (classAttr.contains("nav") || classAttr.contains("menu"))) {
                        return true;
                    }
                    if ("navigation".equals(roleAttr) || "menubar".equals(roleAttr)) {
                        return true;
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
        
        if (!navNodes.isEmpty()) {
            patterns.add(new StructuralPattern(
                "navigation",
                navNodes,
                "Navigation elements detected with " + navNodes.size() + " components",
                0.8
            ));
        }
        
        return patterns;
    }
    
    /**
     * Identify list-related structural patterns
     */
    private List<StructuralPattern> identifyListPatterns(StructuralAnalysis analysis) {
        List<StructuralPattern> patterns = new ArrayList<>();
        
        // Look for ul/ol elements with multiple li children
        for (StructuralNode node : analysis.getSelectorToNode().values()) {
            if (node.getElement() == null) continue;
            
            String tagName = node.getElement().getTagName();
            if ("ul".equalsIgnoreCase(tagName) || "ol".equalsIgnoreCase(tagName)) {
                int liChildren = (int) node.getChildren().stream()
                    .filter(child -> child.getElement() != null && 
                            "li".equalsIgnoreCase(child.getElement().getTagName()))
                    .count();
                
                if (liChildren >= 3) { // Consider it a pattern if 3+ items
                    patterns.add(new StructuralPattern(
                        "list",
                        Arrays.asList(node),
                        tagName.toUpperCase() + " with " + liChildren + " items",
                        0.9
                    ));
                }
            }
        }
        
        return patterns;
    }
    
    /**
     * Identify form-related structural patterns
     */
    private List<StructuralPattern> identifyFormPatterns(StructuralAnalysis analysis) {
        List<StructuralPattern> patterns = new ArrayList<>();
        
        // Look for form elements
        List<StructuralNode> formNodes = analysis.getSelectorToNode().values().stream()
            .filter(node -> node.getElement() != null && 
                          "form".equalsIgnoreCase(node.getElement().getTagName()))
            .collect(Collectors.toList());
        
        for (StructuralNode formNode : formNodes) {
            // Count form controls in the form
            int controlCount = (int) formNode.getDescendants().stream()
                .filter(node -> {
                    if (node.getElement() == null) return false;
                    String tag = node.getElement().getTagName();
                    return "input".equalsIgnoreCase(tag) || 
                           "select".equalsIgnoreCase(tag) || 
                           "textarea".equalsIgnoreCase(tag) ||
                           "button".equalsIgnoreCase(tag);
                })
                .count();
            
            if (controlCount >= 2) {
                patterns.add(new StructuralPattern(
                    "form",
                    Arrays.asList(formNode),
                    "Form with " + controlCount + " controls",
                    0.85
                ));
            }
        }
        
        return patterns;
    }
    
    /**
     * Identify grid/table structural patterns
     */
    private List<StructuralPattern> identifyGridPatterns(StructuralAnalysis analysis) {
        List<StructuralPattern> patterns = new ArrayList<>();
        
        // Look for table elements
        List<StructuralNode> tableNodes = analysis.getSelectorToNode().values().stream()
            .filter(node -> node.getElement() != null && 
                          "table".equalsIgnoreCase(node.getElement().getTagName()))
            .collect(Collectors.toList());
        
        for (StructuralNode tableNode : tableNodes) {
            int rowCount = (int) tableNode.getDescendants().stream()
                .filter(node -> node.getElement() != null && 
                              "tr".equalsIgnoreCase(node.getElement().getTagName()))
                .count();
            
            if (rowCount >= 2) {
                patterns.add(new StructuralPattern(
                    "table",
                    Arrays.asList(tableNode),
                    "Table with " + rowCount + " rows",
                    0.9
                ));
            }
        }
        
        // Look for CSS grid patterns (elements with display: grid)
        List<StructuralNode> gridNodes = analysis.getSelectorToNode().values().stream()
            .filter(node -> {
                if (node.getElement() == null || node.getElement().getStyles() == null) return false;
                String display = node.getElement().getStyles().get("display");
                return "grid".equals(display) || "flex".equals(display);
            })
            .collect(Collectors.toList());
        
        for (StructuralNode gridNode : gridNodes) {
            if (gridNode.getChildren().size() >= 4) { // Grid with multiple items
                String display = gridNode.getElement().getStyles().get("display");
                patterns.add(new StructuralPattern(
                    "css-grid",
                    Arrays.asList(gridNode),
                    display.toUpperCase() + " layout with " + gridNode.getChildren().size() + " items",
                    0.7
                ));
            }
        }
        
        return patterns;
    }
    
    /**
     * Analyze changes in structural context
     */
    public List<ElementChange> analyzeStructuralChanges(List<ElementChange> changes, 
                                                       StructuralAnalysis oldStructure, 
                                                       StructuralAnalysis newStructure) {
        logger.info("Analyzing {} changes in structural context", changes.size());
        
        List<ElementChange> enhancedChanges = new ArrayList<>();
        
        for (ElementChange change : changes) {
            ElementChange enhancedChange = enhanceChangeWithStructuralContext(change, oldStructure, newStructure);
            enhancedChanges.add(enhancedChange);
        }
        
        return enhancedChanges;
    }
    
    /**
     * Enhance a single change with structural context information
     */
    private ElementChange enhanceChangeWithStructuralContext(ElementChange change, 
                                                           StructuralAnalysis oldStructure, 
                                                           StructuralAnalysis newStructure) {
        // Find the node in both structures
        StructuralNode oldNode = oldStructure.getSelectorToNode().get(change.getElement());
        StructuralNode newNode = newStructure.getSelectorToNode().get(change.getElement());
        
        // Enhance classification based on structural context
        if (oldNode != null) {
            String structuralContext = determineStructuralContext(oldNode, oldStructure);
            if (structuralContext != null) {
                // Adjust change classification based on context
                String originalClassification = change.getClassification();
                String contextualClassification = adjustClassificationForContext(originalClassification, structuralContext);
                change.setClassification(contextualClassification);
            }
        }
        
        return change;
    }
    
    /**
     * Determine the structural context of a node (navigation, content, form, etc.)
     */
    private String determineStructuralContext(StructuralNode node, StructuralAnalysis structure) {
        // Check if node is part of any identified patterns
        for (StructuralPattern pattern : structure.getPatterns()) {
            if (pattern.getNodes().contains(node) || 
                pattern.getNodes().stream().anyMatch(patternNode -> patternNode.isAncestorOf(node))) {
                return pattern.getPatternType();
            }
        }
        
        // Fallback to tag-based context
        if (node.getElement() != null && node.getElement().getTagName() != null) {
            String tagName = node.getElement().getTagName().toLowerCase();
            switch (tagName) {
                case "nav":
                case "header":
                case "footer":
                    return "navigation";
                case "main":
                case "article":
                case "section":
                    return "content";
                case "form":
                case "input":
                case "button":
                    return "form";
                case "aside":
                    return "sidebar";
                default:
                    return "general";
            }
        }
        
        return "general";
    }
    
    /**
     * Adjust change classification based on structural context
     */
    private String adjustClassificationForContext(String originalClassification, String context) {
        // Navigation changes are more critical
        if ("navigation".equals(context)) {
            if ("cosmetic".equals(originalClassification)) {
                return "critical"; // Navigation changes affect UX significantly
            }
        }
        
        // Form changes are critical for functionality
        if ("form".equals(context)) {
            if ("noise".equals(originalClassification) || "cosmetic".equals(originalClassification)) {
                return "critical"; // Form changes affect functionality
            }
        }
        
        // Content area changes depend on magnitude
        if ("content".equals(context)) {
            // Keep original classification for content areas
            return originalClassification;
        }
        
        return originalClassification;
    }
}
