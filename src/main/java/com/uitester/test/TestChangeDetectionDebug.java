package com.uitester.test;

import java.io.FileReader;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.uitester.core.ElementData;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

public class TestChangeDetectionDebug {
    
    private static String createElementId(ElementData element) {
        StringBuilder id = new StringBuilder();
        id.append(element.getSelector());
        
        // Add position info to distinguish elements with same selector
        if (element.getPosition() != null) {
            Object x = element.getPosition().get("x");
            Object y = element.getPosition().get("y");
            if (x != null && y != null) {
                id.append("::pos=").append(x).append(",").append(y);
            }
        }
        
        // If we still don't have enough uniqueness, add some stable attributes
        if (element.getAttributes() != null) {
            String classAttr = element.getAttributes().get("class");
            String idAttr = element.getAttributes().get("id");
            if (idAttr != null && !idAttr.trim().isEmpty()) {
                id.append("::id=").append(idAttr.trim());
            } else if (classAttr != null && !classAttr.trim().isEmpty()) {
                // Take first class name for stability
                String firstClass = classAttr.trim().split("\\s+")[0];
                id.append("::class=").append(firstClass);
            }
        }
        
        return id.toString();
    }
    
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Read baseline and current data
            List<ElementData> baselineElements = mapper.readValue(
                new FileReader("test_baseline.json"),
                new TypeReference<List<ElementData>>() {}
            );
            
            List<ElementData> currentElements = mapper.readValue(
                new FileReader("test_current.json"), 
                new TypeReference<List<ElementData>>() {}
            );
            
            System.out.println("=== BASELINE ELEMENTS ===");
            for (ElementData element : baselineElements) {
                String elementId = createElementId(element);
                System.out.println("ID: " + elementId);
                System.out.println("Text: '" + element.getText() + "'");
                System.out.println("Selector: " + element.getSelector());
                System.out.println("Position: " + element.getPosition());
                System.out.println();
            }
            
            System.out.println("=== CURRENT ELEMENTS ===");
            for (ElementData element : currentElements) {
                String elementId = createElementId(element);
                System.out.println("ID: " + elementId);
                System.out.println("Text: '" + element.getText() + "'");
                System.out.println("Selector: " + element.getSelector());
                System.out.println("Position: " + element.getPosition());
                System.out.println();
            }
            
            System.out.println("Baseline elements: " + baselineElements.size());
            System.out.println("Current elements: " + currentElements.size());
            
            // Test change detection
            ChangeDetector detector = new ChangeDetector();
            List<ElementChange> changes = detector.detectChanges(baselineElements, currentElements, null);
            
            System.out.println("Detected changes: " + changes.size());
            
            for (ElementChange change : changes) {
                System.out.println(change.toString());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
