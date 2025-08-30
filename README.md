# Web Change Detector (Enhanced)

A Java-based DOM/UI change detection tool with configurable crawling, matching, classification, structural analysis, and reporting.

Key highlights:
- Modular JSON configuration with automatic backfill of new sections.
- Centralized defaults in `Defaults.java` (removes scattered magic numbers).
- Structural pattern confidence weighting & themed HTML reporting.
- Dual reports: full detail + lightweight simple report (`reportSimpleFile`).

## Quick Start

1. Build
```
mvn -DskipTests package
```
2. Run
```
mvn exec:java -Dexec.mainClass=com.uitester.main.EnhancedUITesterMain -Dexec.args="--baseline <baselineUrlOrFile> --current <currentUrlOrFile> --section-name sample"
```
3. Output appears under `output/` in a timestamped directory (customizable via `outputSettings.directoryTemplate`).

## Configuration (`src/main/resources/config.json`)
The system loads once via `ConfigLoader` and applies defaults for any missing sections.

### Sections Overview
- captureSettings: What attributes/styles to extract.
- comparisonSettings: Change thresholds, ignored styles, style categories, weighting.
- fingerprintSettings: Element fingerprint composition.
- performanceSettings: Limits & performance constraints.
- crawlerSettings: DOM extraction behavior (cssProperties, attributesToExtract, visibilityFilter, throttleMs).
- matchingSettings: Element matching weights, fuzzy & semantic thresholds (`enableSemanticPrice`, price confidence).
- classificationSettings: Interactive & accessibility keywords, magnitude thresholds (text/color/layout/style/position), optional custom rules.
- structuralAnalysisSettings: Pattern detection thresholds, depth caps, and `patternConfidence` map (navigation, list, form, table, css-grid) that scales structural change confidence.
- reportingSettings: Severity order, badge colors, theme colors (`themeColors`), confidence bar & dynamic labels (`confidenceLevels`), auto-scroll target.
- outputSettings: Directory & file naming templates including explicit `reportSimpleFile` for the compact HTML report.
- flags: Feature toggles (structural analysis, semantic matching, advanced classification).

### Example Snippet
```json
{
  "matchingSettings": {
    "tagWeight": 0.25,
    "textWeight": 0.45,
    "structuralWeight": 0.2,
    "contentWeight": 0.1,
    "fuzzyMinConfidence": 0.55,
    "semanticPriceConfidence": 0.8,
    "enableSemanticPrice": true
  },
  "classificationSettings": {
    "interactiveKeywords": ["button","a","input"],
    "magnitudeThresholds": {"textCritical": 0.55, "colorCosmetic": 0.25, "layoutCosmetic": 0.08},
    "rules": [ {"propertyContains": "aria-", "classification": "critical"} ]
  }
}
```
Only include sections you wish to override; missing values use internal defaults.

## CLI Overrides
Override any nested config value (dot notation) without editing JSON:
```
--override matchingSettings.tagWeight=0.2 --override classificationSettings.magnitudeThresholds.textCritical=0.6 \
--override reportingSettings.themeColors.accentPrimary=#1d4ed8 --override reportingSettings.confidenceLevels[0].min=0.92 \
--override outputSettings.reportSimpleFile=summary.html
```
Multiple `--override` flags allowed. Numeric & boolean values auto-parsed; others treated as strings.

## Output Directory & Reports
`outputSettings.directoryTemplate` supports placeholders:
- {section} – CLI `--section-name`
- {baselineHost} / {currentHost}
- {timestamp} – run start ISO-like compact (yyyyMMdd_HHmmss)

Example: `"{section}_{baselineHost}-vs-{currentHost}_{timestamp}"`

Report filenames:
- `reportFile` – Full detailed report
- `reportSimpleFile` – Lightweight summary (severity grouping & essentials)

Customize independently; both emit to the run directory.

## Feature Flags
| Flag | Purpose | Default |
|------|---------|---------|
| enableStructuralAnalysis | Enables structural pattern detection & context classification | true |
| enableSemanticMatching | Allows semantic element matching extensions (e.g., price context) | true |
| enableAdvancedClassification | Activates rule engine for custom classification rules | false |

## Classification Logic (Config-Driven)
Centralized in `ChangeDetector`:
1. Structural / element-level anomalies.
2. Accessibility attribute changes (aria*, alt, role).
3. Interactive keyword presence.
4. Magnitude thresholds: textCritical, styleCritical/styleCosmetic, colorCosmetic, layoutCosmetic, positionBase.
5. Rule engine (`flags.enableAdvancedClassification`).
6. Otherwise derive severity (text or cosmetic) or fallback -> noise.
Threshold defaults in `classificationSettings.magnitudeThresholds` or `Defaults.java` if omitted.

## Structural Analysis
Tunable via `structuralAnalysisSettings`:
- listMinItems, gridMinItems, tableMinRows
- maxDepthForParentSearch / selectorSimilarityCapDepth
- patternConfidence map weights (navigation, list, form, table, css-grid) scaling structural context confidence.
Honors `flags.enableStructuralAnalysis`.

## Reporting Customization
- Sort order: `reportingSettings.severityOrder`.
- Badge colors: `badgeColors` map.
- Theme: `themeColors` (accentPrimary, accentSecondary, backgroundSoft, panelBackground, borderColor).
- Confidence labels: ordered `confidenceLevels` list (`min` + `label`); adjust ranges without code.
- Confidence bar toggled via `enableConfidenceBar`.
- Simple report mirrors theming but omits verbose sections.

## Adding Rules (classificationSettings.rules)
Each rule is a JSON object with optional keys:
- propertyContains (substring match on `property`)
- changeType (exact match)
- minMagnitude (numeric >=)
- classification (result label)
First matching rule returns its classification.

## Development Notes
- Central config object: `ProjectConfig` with validation (`validate()`).
- Runtime wrapper: `Configuration` for overrides & derived paths (expands directory template & report file names including simple report).
- Centralized constants: `Defaults.java` (single source for fallback weights & thresholds, eliminating magic numbers).
- Refactored components: `ElementMatcher`, `ChangeDetector`, `StructuralAnalyzer`, `SimpleReportGenerator` consume config-driven values.
- Dot-notation overrides applied pre path resolution.

## Future Enhancements
- Unit tests for matcher weighting, classification thresholds, structural confidences.
- JSON schema for config.
- Severity filtering CLI (`--min-severity`).
- Persist run metrics (JSON) for historical trend analysis.
- Externalize additional report gradient & background tokens.

## Troubleshooting
| Symptom | Cause | Fix |
|---------|-------|-----|
| No report file | Wrong `reportFile` or output dir template | Check `outputSettings` + logs |
| Missing structural patterns | Structural analysis disabled | Set `flags.enableStructuralAnalysis=true` |
| All changes noise | Thresholds too high | Lower `magnitudeThresholds` values |
| Unexpected unmatched elements | Fuzzy threshold too high | Reduce `matchingSettings.fuzzyMinConfidence` |

## License
Internal / TBD.
