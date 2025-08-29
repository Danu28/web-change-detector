package com.uitester.test;

import java.io.FileReader;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.uitester.core.ElementData;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

public class TestPaperChanges {
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Read baseline and current data from the Paper comparison output
            List<ElementData> baselineElements = mapper.readValue(
                new FileReader("output/Paper_instantink.hpconnected.com-vs-instantink-pie1.hpconnectedpie.com/baseline.json"),
                new TypeReference<List<ElementData>>() {}
            );
            
            List<ElementData> currentElements = mapper.readValue(
                new FileReader("output/Paper_instantink.hpconnected.com-vs-instantink-pie1.hpconnectedpie.com/current.json"), 
                new TypeReference<List<ElementData>>() {}
            );
            
            System.out.println("Baseline elements: " + baselineElements.size());
            System.out.println("Current elements: " + currentElements.size());
            
            // Test change detection with detailed logging
            ChangeDetector detector = new ChangeDetector();
            
            // Compare individual elements to see what's happening
            for (int i = 0; i < Math.min(baselineElements.size(), currentElements.size()); i++) {
                ElementData baseline = baselineElements.get(i);
                ElementData current = currentElements.get(i);
                
                System.out.println("\n--- Element " + i + " ---");
                System.out.println("Selector: " + baseline.getSelector());
                
                // Check text differences
                if (!equalStrings(baseline.getText(), current.getText())) {
                    System.out.println("TEXT CHANGE:");
                    System.out.println("  Baseline: '" + baseline.getText() + "'");
                    System.out.println("  Current:  '" + current.getText() + "'");
                }
                
                // Check position differences
                if (baseline.getPosition() != null && current.getPosition() != null) {
                    Object baselineY = baseline.getPosition().get("y");
                    Object currentY = current.getPosition().get("y");
                    if (!equalObjects(baselineY, currentY)) {
                        System.out.println("POSITION Y CHANGE:");
                        System.out.println("  Baseline: " + baselineY);
                        System.out.println("  Current:  " + currentY);
                    }
                }
                
                // Check attribute differences
                if (baseline.getAttributes() != null && current.getAttributes() != null) {
                    String baselineHref = baseline.getAttributes().get("href");
                    String currentHref = current.getAttributes().get("href");
                    if (!equalStrings(baselineHref, currentHref)) {
                        System.out.println("HREF CHANGE:");
                        System.out.println("  Baseline: " + baselineHref);
                        System.out.println("  Current:  " + currentHref);
                    }
                }
            }
            
            List<ElementChange> changes = detector.detectChanges(baselineElements, currentElements, null);
            
            System.out.println("\n=== DETECTED CHANGES ===");
            System.out.println("Total detected changes: " + changes.size());
            
            for (ElementChange change : changes) {
                System.out.println("Change: " + change.getElement() + " -> " + change.getProperty() + 
                                 " from '" + change.getOldValue() + "' to '" + change.getNewValue() + 
                                 "' (type: " + change.getChangeType() + ", magnitude: " + change.getMagnitude() + ")");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean equalStrings(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    private static boolean equalObjects(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
