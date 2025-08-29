package com.uitester.test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.uitester.core.ElementData;
import com.uitester.diff.ChangeDetector;
import com.uitester.diff.ElementChange;

public class FixAllChanges {
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Process all comparison output directories
            File outputDir = new File("output");
            File[] subdirs = outputDir.listFiles(File::isDirectory);
            
            if (subdirs != null) {
                for (File subdir : subdirs) {
                    File baselineFile = new File(subdir, "baseline.json");
                    File currentFile = new File(subdir, "current.json");
                    File changesFile = new File(subdir, "changes.json");
                    
                    if (baselineFile.exists() && currentFile.exists()) {
                        System.out.println("\nProcessing: " + subdir.getName());
                        
                        // Read baseline and current data
                        List<ElementData> baselineElements = mapper.readValue(
                            new FileReader(baselineFile),
                            new TypeReference<List<ElementData>>() {}
                        );
                        
                        List<ElementData> currentElements = mapper.readValue(
                            new FileReader(currentFile), 
                            new TypeReference<List<ElementData>>() {}
                        );
                        
                        System.out.println("Baseline elements: " + baselineElements.size());
                        System.out.println("Current elements: " + currentElements.size());
                        
                        // Test change detection
                        ChangeDetector detector = new ChangeDetector();
                        List<ElementChange> changes = detector.detectChanges(baselineElements, currentElements, null);
                        
                        System.out.println("Total detected changes: " + changes.size());
                        
                        // Save changes to the changes.json file
                        try (FileWriter writer = new FileWriter(changesFile)) {
                            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, changes);
                            System.out.println("Changes saved to " + changesFile.getPath());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
