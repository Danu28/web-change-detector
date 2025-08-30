package com.uitester.diff;

import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
import com.uitester.core.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 2: Enhanced element matching using fingerprints instead of fragile selectors.
 * This class provides resilient matching between baseline and current elements
 * even when DOM structure or selectors change.
 */
public class ElementMatcher {
    private static final Logger logger = LoggerFactory.getLogger(ElementMatcher.class);
    
    private final ProjectConfig config;
    // Config-driven weights & thresholds (fallback to legacy defaults)
    private double tagWeight = Defaults.MATCH_TAG_WEIGHT;
    private double textWeight = Defaults.MATCH_TEXT_WEIGHT;
    private double structuralWeight = Defaults.MATCH_STRUCTURAL_WEIGHT;
    private double contentWeight = Defaults.MATCH_CONTENT_WEIGHT;
    private double fuzzyMinConfidence = Defaults.MATCH_FUZZY_MIN_CONF;
    private double semanticPriceConfidence = Defaults.MATCH_SEMANTIC_PRICE_CONF;
    private boolean enableSemanticPrice = true;
    private boolean enableSemanticMatchingFlag = true; // from flags if present
    
    public ElementMatcher(ProjectConfig config) {
        this.config = config;
        loadMatchingSettings();
    }

    private void loadMatchingSettings() {
        if (config == null) return;
        ProjectConfig.MatchingSettings ms = config.getMatchingSettings();
        if (ms != null) {
            if (ms.getTagWeight() != null) tagWeight = ms.getTagWeight();
            if (ms.getTextWeight() != null) textWeight = ms.getTextWeight();
            if (ms.getStructuralWeight() != null) structuralWeight = ms.getStructuralWeight();
            if (ms.getContentWeight() != null) contentWeight = ms.getContentWeight();
            if (ms.getFuzzyMinConfidence() != null) fuzzyMinConfidence = ms.getFuzzyMinConfidence();
            if (ms.getSemanticPriceConfidence() != null) semanticPriceConfidence = ms.getSemanticPriceConfidence();
            if (ms.getEnableSemanticPrice() != null) enableSemanticPrice = ms.getEnableSemanticPrice();
        }
        if (config.getFlags() != null && config.getFlags().getEnableSemanticMatching() != null) {
            enableSemanticMatchingFlag = config.getFlags().getEnableSemanticMatching();
        }
        double sum = tagWeight + textWeight + structuralWeight + contentWeight;
        if (sum <= 0) {
            tagWeight = Defaults.MATCH_TAG_WEIGHT;
            textWeight = Defaults.MATCH_TEXT_WEIGHT;
            structuralWeight = Defaults.MATCH_STRUCTURAL_WEIGHT;
            contentWeight = Defaults.MATCH_CONTENT_WEIGHT;
            sum = tagWeight + textWeight + structuralWeight + contentWeight;
        }
        // Normalize to sum 1 to keep confidence scale stable
        tagWeight /= sum; textWeight /= sum; structuralWeight /= sum; contentWeight /= sum;
        logger.info("ElementMatcher weights -> tag: {}, text: {}, structural: {}, content: {}; fuzzyMinConfidence: {}, semanticPriceConfidence: {}, semanticEnabled: {}", 
            String.format("%.2f", tagWeight), String.format("%.2f", textWeight), 
            String.format("%.2f", structuralWeight), String.format("%.2f", contentWeight), 
            String.format("%.2f", fuzzyMinConfidence), String.format("%.2f", semanticPriceConfidence), (enableSemanticPrice && enableSemanticMatchingFlag));
    }
    
    /**
     * Result of element matching operation
     */
    public static class MatchResult {
        private final Map<ElementData, ElementData> matchedPairs;
        private final List<ElementData> addedElements;
        private final List<ElementData> removedElements;
        private final Map<ElementData, Double> matchConfidences;
        
        public MatchResult(Map<ElementData, ElementData> matchedPairs, 
                          List<ElementData> addedElements,
                          List<ElementData> removedElements,
                          Map<ElementData, Double> matchConfidences) {
            this.matchedPairs = matchedPairs;
            this.addedElements = addedElements;
            this.removedElements = removedElements;
            this.matchConfidences = matchConfidences;
        }
        
        public Map<ElementData, ElementData> getMatchedPairs() {
            return matchedPairs;
        }
        
        public List<ElementData> getAddedElements() {
            return addedElements;
        }
        
        public List<ElementData> getRemovedElements() {
            return removedElements;
        }
        
        public Map<ElementData, Double> getMatchConfidences() {
            return matchConfidences;
        }
    }
    
    /**
     * Match baseline elements to current elements using fingerprint-based matching
     * 
     * @param baselineElements Elements from baseline snapshot
     * @param currentElements Elements from current snapshot  
     * @return MatchResult containing matched pairs and unmatched elements
     */
    public MatchResult matchElements(List<ElementData> baselineElements, List<ElementData> currentElements) {
        logger.info("Starting fingerprint-based element matching: {} baseline -> {} current", 
                   baselineElements.size(), currentElements.size());
        
        // Generate fingerprints for all elements
        generateFingerprints(baselineElements);
        generateFingerprints(currentElements);
        
        Map<ElementData, ElementData> matchedPairs = new HashMap<>();
        Map<ElementData, Double> matchConfidences = new HashMap<>();
        Set<ElementData> usedCurrentElements = new HashSet<>();
        
        // Phase 1: Exact fingerprint matches
        for (ElementData baseline : baselineElements) {
            ElementData exactMatch = findExactFingerprintMatch(baseline, currentElements, usedCurrentElements);
            if (exactMatch != null) {
                matchedPairs.put(baseline, exactMatch);
                matchConfidences.put(baseline, 1.0);
                usedCurrentElements.add(exactMatch);
                logger.debug("Exact fingerprint match: {} -> {}", baseline.getSelector(), exactMatch.getSelector());
            }
        }
        
        // Phase 2: Fuzzy fingerprint matches for unmatched elements
        List<ElementData> unmatchedBaseline = baselineElements.stream()
            .filter(e -> !matchedPairs.containsKey(e))
            .collect(Collectors.toList());
            
        for (ElementData baseline : unmatchedBaseline) {
            FuzzyMatch fuzzyMatch = findBestFuzzyMatch(baseline, currentElements, usedCurrentElements);
            if (fuzzyMatch != null && fuzzyMatch.confidence >= fuzzyMinConfidence) {
                matchedPairs.put(baseline, fuzzyMatch.element);
                matchConfidences.put(baseline, fuzzyMatch.confidence);
                usedCurrentElements.add(fuzzyMatch.element);
                logger.debug("Fuzzy match (confidence: {:.2f}): {} -> {}", 
                           fuzzyMatch.confidence, baseline.getSelector(), fuzzyMatch.element.getSelector());
            }
        }
        
        // Phase 2.5: Semantic Content Matching (for price-like elements)
        if (enableSemanticPrice && enableSemanticMatchingFlag) {
            List<ElementData> stillUnmatchedBaseline = baselineElements.stream()
                .filter(e -> !matchedPairs.containsKey(e))
                .collect(Collectors.toList());
            for (ElementData baseline : stillUnmatchedBaseline) {
                ElementData semanticMatch = findSemanticContentMatch(baseline, currentElements, usedCurrentElements);
                if (semanticMatch != null) {
                    matchedPairs.put(baseline, semanticMatch);
                    matchConfidences.put(baseline, semanticPriceConfidence);
                    usedCurrentElements.add(semanticMatch);
                    logger.info("✨ Semantic content match: {} ('{}') -> {} ('{}') [conf={}]", 
                               baseline.getSelector(), baseline.getText(),
                               semanticMatch.getSelector(), extractAllText(semanticMatch), String.format("%.2f", semanticPriceConfidence));
                }
            }
        }
        
        // Phase 3: Identify added and removed elements
        List<ElementData> removedElements = baselineElements.stream()
            .filter(e -> !matchedPairs.containsKey(e))
            .collect(Collectors.toList());
            
        List<ElementData> addedElements = currentElements.stream()
            .filter(e -> !usedCurrentElements.contains(e))
            .collect(Collectors.toList());
        
        logger.info("Element matching complete: {} matched, {} removed, {} added", 
                   matchedPairs.size(), removedElements.size(), addedElements.size());
        
        return new MatchResult(matchedPairs, addedElements, removedElements, matchConfidences);
    }
    
    /**
     * Generate fingerprints for all elements in the list
     */
    private void generateFingerprints(List<ElementData> elements) {
        for (ElementData element : elements) {
            element.generateFingerprint(config);
            element.generateStructuralFingerprint();
            element.generateContentFingerprint();
        }
    }
    
    /**
     * Find an exact fingerprint match
     */
    private ElementData findExactFingerprintMatch(ElementData baseline, List<ElementData> candidates, Set<ElementData> used) {
        String targetFingerprint = baseline.getFingerprint();
        if (targetFingerprint == null) return null;
        
        return candidates.stream()
            .filter(e -> !used.contains(e))
            .filter(e -> targetFingerprint.equals(e.getFingerprint()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Fuzzy match result
     */
    private static class FuzzyMatch {
        final ElementData element;
        final double confidence;
        
        FuzzyMatch(ElementData element, double confidence) {
            this.element = element;
            this.confidence = confidence;
        }
    }
    
    /**
     * Find the best fuzzy match using multiple fingerprint types and similarity metrics
     */
    private FuzzyMatch findBestFuzzyMatch(ElementData baseline, List<ElementData> candidates, Set<ElementData> used) {
        List<ElementData> availableCandidates = candidates.stream()
            .filter(e -> !used.contains(e))
            .collect(Collectors.toList());
            
        if (availableCandidates.isEmpty()) return null;
        
        ElementData bestMatch = null;
        double bestConfidence = 0.0;
        
        for (ElementData candidate : availableCandidates) {
            double confidence = calculateMatchConfidence(baseline, candidate);
            if (confidence > bestConfidence) {
                bestMatch = candidate;
                bestConfidence = confidence;
            }
        }
        
        return bestMatch != null ? new FuzzyMatch(bestMatch, bestConfidence) : null;
    }
    
    /**
     * Calculate match confidence between two elements using multiple similarity metrics
     */
    private double calculateMatchConfidence(ElementData baseline, ElementData candidate) {
        double totalWeight = 0.0;
        double weightedScore = 0.0;
        
        // Tag name similarity
        if (baseline.getTagName() != null && candidate.getTagName() != null) {
            double tagScore = baseline.getTagName().equals(candidate.getTagName()) ? 1.0 : 0.0;
            weightedScore += tagScore * tagWeight;
            totalWeight += tagWeight;
        }
        
        // Text content similarity
        if (baseline.getText() != null && candidate.getText() != null) {
            double textScore = calculateTextSimilarity(baseline.getText(), candidate.getText());
            weightedScore += textScore * textWeight;
            totalWeight += textWeight;
        }
        
        // Structural fingerprint similarity
        if (baseline.getStructuralFingerprint() != null && candidate.getStructuralFingerprint() != null) {
            double structScore = baseline.getStructuralFingerprint().equals(candidate.getStructuralFingerprint()) ? 1.0 : Defaults.STRUCT_FINGERPRINT_PARTIAL;
            weightedScore += structScore * structuralWeight;
            totalWeight += structuralWeight;
        }
        
        // Content fingerprint similarity
        if (baseline.getContentFingerprint() != null && candidate.getContentFingerprint() != null) {
            double contentScore = baseline.getContentFingerprint().equals(candidate.getContentFingerprint()) ? 1.0 : 0.0;
            weightedScore += contentScore * contentWeight;
            totalWeight += contentWeight;
        }
        
        return totalWeight > 0 ? weightedScore / totalWeight : 0.0;
    }
    
    /**
     * Calculate text similarity using simple normalized edit distance
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        String normalized1 = text1.trim().toLowerCase().replaceAll("\\s+", " ");
        String normalized2 = text2.trim().toLowerCase().replaceAll("\\s+", " ");
        
        if (normalized1.equals(normalized2)) return 1.0;
        if (normalized1.isEmpty() && normalized2.isEmpty()) return 1.0;
        if (normalized1.isEmpty() || normalized2.isEmpty()) return 0.0;
        
        // Simple similarity based on common words
        Set<String> words1 = new HashSet<>(Arrays.asList(normalized1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(normalized2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Find semantic content match based on content patterns (e.g., price values)
     */
    private ElementData findSemanticContentMatch(ElementData baseline, 
                                               List<ElementData> currentElements, 
                                               Set<ElementData> usedElements) {
        String baselineText = baseline.getText();
        if (baselineText == null || baselineText.trim().isEmpty()) {
            return null;
        }
        
        // Check if this looks like a price element
        if (isPriceElement(baseline)) {
            return findPriceElementMatch(baseline, currentElements, usedElements);
        }
        
        // Add more semantic matchers here for other content types
        // e.g., dates, phone numbers, addresses, etc.
        
        return null;
    }
    
    /**
     * Check if an element appears to be a price element
     */
    private boolean isPriceElement(ElementData element) {
        String text = element.getText();
        String selector = element.getSelector();
        
        // Check text patterns for price
        if (text != null && text.matches(".*[$€£¥]\\d+.*")) {
            return true;
        }
        
        // Check selector patterns for price
        if (selector != null && selector.toLowerCase().contains("price")) {
            return true;
        }
        
        // Check class attributes for price
        String classAttr = element.getAttributes().get("class");
        if (classAttr != null && classAttr.toLowerCase().contains("price")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Find a matching price element in current elements
     */
    private ElementData findPriceElementMatch(ElementData baseline, 
                                            List<ElementData> currentElements, 
                                            Set<ElementData> usedElements) {
        String baselinePrice = extractPriceValue(baseline.getText());
        if (baselinePrice == null) return null;
        
        // Look for elements in similar structural context
        String baselineContext = getStructuralContext(baseline);
        
        for (ElementData current : currentElements) {
            if (usedElements.contains(current)) continue;
            
            // Check if current element or its children contain a price
            String currentPrice = findPriceInElementOrChildren(current, currentElements);
            if (currentPrice != null) {
                String currentContext = getStructuralContext(current);
                
                // Match if same structural context (same parent container)
                if (baselineContext.equals(currentContext)) {
                    logger.debug("Price semantic match: {} ('{}') -> {} (price: '{}')", 
                               baseline.getSelector(), baselinePrice,
                               current.getSelector(), currentPrice);
                    return current;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract price value from text (e.g., "$199.99" -> "199.99")
     */
    private String extractPriceValue(String text) {
        if (text == null) return null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[$€£¥]([0-9,]+\\.?[0-9]*)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Get structural context (parent container info)
     */
    private String getStructuralContext(ElementData element) {
        String selector = element.getSelector();
        if (selector == null) return "";
        
        // For elements with ID parents, use the ID as context
        if (selector.contains("#")) {
            // Extract the ID part
            String[] parts = selector.split("\\s+");
            for (String part : parts) {
                if (part.startsWith("#")) {
                    return part;
                }
            }
        }
        
        return "";
    }
    
    /**
     * Find price in element or its children
     */
    private String findPriceInElementOrChildren(ElementData element, List<ElementData> allElements) {
        // Check the element itself
        String price = extractPriceValue(element.getText());
        if (price != null) return price;
        
        // Check if this element is a container that might have child price elements
        String elementSelector = element.getSelector();
        if (elementSelector != null) {
            for (ElementData other : allElements) {
                String otherSelector = other.getSelector();
                if (otherSelector != null && otherSelector.startsWith(elementSelector) && 
                    !otherSelector.equals(elementSelector)) {
                    // This might be a child element
                    String childPrice = extractPriceValue(other.getText());
                    if (childPrice != null) return childPrice;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract all text from element and children
     */
    private String extractAllText(ElementData element) {
        // For now, just return the element's direct text
        // This could be enhanced to traverse child elements
        return element.getText() != null ? element.getText() : "";
    }
}
