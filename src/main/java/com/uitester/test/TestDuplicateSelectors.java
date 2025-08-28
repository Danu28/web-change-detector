package com.uitester.test;

import java.io.FileReader;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.uitester.core.ElementData;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

public class TestDuplicateSelectors {
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Read baseline and current data with duplicate selectors
            List<ElementData> baselineElements = mapper.readValue(
                new FileReader("test_baseline_duplicate.json"),
                new TypeReference<List<ElementData>>() {}
            );
            
            List<ElementData> currentElements = mapper.readValue(
                new FileReader("test_current_duplicate.json"), 
                new TypeReference<List<ElementData>>() {}
            );
            
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
