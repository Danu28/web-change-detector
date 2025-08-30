package com.uitester.core;

/** Centralized default constants to remove scattered literals. */
public final class Defaults {
    private Defaults() {}

    // Matching weights
    public static final double MATCH_TAG_WEIGHT = 0.3;
    public static final double MATCH_TEXT_WEIGHT = 0.4;
    public static final double MATCH_STRUCTURAL_WEIGHT = 0.2;
    public static final double MATCH_CONTENT_WEIGHT = 0.1;

    // Matching thresholds
    public static final double MATCH_FUZZY_MIN_CONF = 0.6;
    public static final double MATCH_SEMANTIC_PRICE_CONF = 0.75;

    // Classification thresholds (legacy fallbacks)
    public static final double CLASS_TEXT_CRITICAL = 0.5;
    public static final double CLASS_COLOR_COSMETIC = 0.3;
    public static final double CLASS_LAYOUT_COSMETIC = 0.1;
    public static final double CLASS_STYLE_CRITICAL = 0.6; // new planned externalization
    public static final double CLASS_STYLE_COSMETIC = 0.2; // cosmetic style lower bound
    public static final double CLASS_POSITION_BASE_MAG = 0.5; // planned externalization

    // Detection fallback thresholds / weights
    public static final double TEXT_SIMILARITY_DEFAULT = 0.95;
    public static final double COLOR_STYLE_IMPORTANCE = 0.7; // base magnitude for color change when different
    public static final double ATTRIBUTE_CHANGE_BASE = 0.5; // magnitude for changed attribute when both present
    public static final double POSITION_CHANGE_BASE = 0.5; // default position magnitude when no numeric delta
    public static final double LOW_MATCH_CONFIDENCE = 0.8; // threshold to flag potential matches
    public static final double STRUCT_FINGERPRINT_PARTIAL = 0.5; // partial structural fingerprint score

    // Structural analysis defaults
    public static final int STRUCT_LIST_MIN = 3;
    public static final int STRUCT_GRID_MIN = 4;
    public static final int STRUCT_TABLE_MIN = 2;
    public static final int STRUCT_PARENT_SEARCH_MAX_DEPTH = 10;
    public static final int STRUCT_SELECTOR_SIMILARITY_CAP_DEPTH = 10;

    // Output defaults
    public static final String OUTPUT_DIR_TEMPLATE = "{section}_{baselineHost}-vs-{currentHost}";
    public static final String OUTPUT_BASELINE_FILE = "baseline.json";
    public static final String OUTPUT_CURRENT_FILE = "current.json";
    public static final String OUTPUT_CHANGES_FILE = "changes.json";
    public static final String OUTPUT_REPORT_FILE = "report.html";
    public static final String OUTPUT_REPORT_SIMPLE_FILE = "report-simple.html"; // new key candidate
}
