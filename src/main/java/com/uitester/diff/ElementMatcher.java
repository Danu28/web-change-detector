package com.uitester.diff;

import com.uitester.core.ElementData;
import com.uitester.core.ProjectConfig;
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
    
    public ElementMatcher(ProjectConfig config) {
        this.config = config;
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
            if (fuzzyMatch != null && fuzzyMatch.confidence >= 0.6) {
                matchedPairs.put(baseline, fuzzyMatch.element);
                matchConfidences.put(baseline, fuzzyMatch.confidence);
                usedCurrentElements.add(fuzzyMatch.element);
                logger.debug("Fuzzy match (confidence: {:.2f}): {} -> {}", 
                           fuzzyMatch.confidence, baseline.getSelector(), fuzzyMatch.element.getSelector());
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
        
        // Tag name similarity (weight: 0.3)
        if (baseline.getTagName() != null && candidate.getTagName() != null) {
            double tagScore = baseline.getTagName().equals(candidate.getTagName()) ? 1.0 : 0.0;
            weightedScore += tagScore * 0.3;
            totalWeight += 0.3;
        }
        
        // Text content similarity (weight: 0.4)
        if (baseline.getText() != null && candidate.getText() != null) {
            double textScore = calculateTextSimilarity(baseline.getText(), candidate.getText());
            weightedScore += textScore * 0.4;
            totalWeight += 0.4;
        }
        
        // Structural fingerprint similarity (weight: 0.2)
        if (baseline.getStructuralFingerprint() != null && candidate.getStructuralFingerprint() != null) {
            double structScore = baseline.getStructuralFingerprint().equals(candidate.getStructuralFingerprint()) ? 1.0 : 0.5;
            weightedScore += structScore * 0.2;
            totalWeight += 0.2;
        }
        
        // Content fingerprint similarity (weight: 0.1)
        if (baseline.getContentFingerprint() != null && candidate.getContentFingerprint() != null) {
            double contentScore = baseline.getContentFingerprint().equals(candidate.getContentFingerprint()) ? 1.0 : 0.0;
            weightedScore += contentScore * 0.1;
            totalWeight += 0.1;
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
}
