package com.uitester.test;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.uitester.core.ElementData;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

public class UpdatePaperChanges {
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
            List<ElementChange> changes = detector.detectChanges(baselineElements, currentElements, null);
            
            System.out.println("Total detected changes: " + changes.size());
            
            for (ElementChange change : changes) {
                System.out.println("Change: " + change.getElement() + " -> " + change.getProperty() + 
                                 " from '" + change.getOldValue() + "' to '" + change.getNewValue() + 
                                 "' (type: " + change.getChangeType() + ", magnitude: " + change.getMagnitude() + 
                                 ", classification: " + change.getClassification() + ")");
            }
            
            // Save changes to the changes.json file
            try (FileWriter writer = new FileWriter("output/Paper_instantink.hpconnected.com-vs-instantink-pie1.hpconnectedpie.com/changes.json")) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, changes);
                System.out.println("\nChanges saved to changes.json file!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
