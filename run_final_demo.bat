@echo off
REM Enhanced UI Tester - Final Demo
REM This demonstrates the complete enhanced system with all components

echo ==========================================
echo 🚀 ENHANCED UI TESTER - COMPLETE DEMO
echo ==========================================
echo.

echo 📦 Using packaged JAR with all dependencies...
echo.

REM Check if JAR exists
if not exist "target\java-ui-tester-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo ❌ JAR file not found! Running mvn package first...
    call mvn package -DskipTests -q
    if errorlevel 1 (
        echo ERROR: Build failed!
        pause
        exit /b 1
    )
)

echo ✅ Found packaged JAR
echo.

REM Convert test file paths to file URLs
set "BASELINE_FILE=file:///C:/Users/KaDh550/Desktop/java-ui-tester/test_files/baseline.html"
set "CURRENT_FILE=file:///C:/Users/KaDh550/Desktop/java-ui-tester/test_files/current.html"

echo 🔍 STARTING ENHANCED ANALYSIS
echo ==========================================
echo Baseline: %BASELINE_FILE%
echo Current:  %CURRENT_FILE%
echo Section:  TestFilesDemo
echo ==========================================
echo.

REM Run the enhanced UI tester
java -cp "target\java-ui-tester-1.0-SNAPSHOT-jar-with-dependencies.jar" ^
    com.uitester.main.EnhancedUITesterMain ^
    --baseline "%BASELINE_FILE%" ^
    --current "%CURRENT_FILE%" ^
    --section-name "TestFilesDemo" ^
    --max-elements 50 ^
    --wait-time 3 ^
    --confidence-threshold 0.7 ^
    --headless

if errorlevel 1 (
    echo.
    echo ❌ Enhanced analysis failed!
    echo Check the error messages above.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo ✅ ENHANCED ANALYSIS COMPLETE!
echo ==========================================
echo.

REM Check for generated reports
echo 📄 GENERATED REPORTS:
echo.

for /f "delims=" %%i in ('dir /b /s output\*TestFilesDemo*simple*.html 2^>nul') do (
    echo ✅ Simple Report: %%i
    set "SIMPLE_REPORT=%%i"
)

for /f "delims=" %%i in ('dir /b /s output\*TestFilesDemo*enhanced*.html 2^>nul') do (
    echo ✅ Enhanced Report: %%i
    set "ENHANCED_REPORT=%%i"
)

for /f "delims=" %%i in ('dir /b /s output\*TestFilesDemo*baseline*.json 2^>nul') do (
    echo ✅ Baseline Data: %%i
)

for /f "delims=" %%i in ('dir /b /s output\*TestFilesDemo*current*.json 2^>nul') do (
    echo ✅ Current Data: %%i
)

for /f "delims=" %%i in ('dir /b /s output\*TestFilesDemo*changes*.json 2^>nul') do (
    echo ✅ Changes Data: %%i
)

echo.
echo ==========================================
echo 🎯 QUICK REPORT PREVIEW
echo ==========================================

REM Try to show a quick preview of results
if defined SIMPLE_REPORT (
    echo 📖 Opening Simple Report in default browser...
    start "" "%SIMPLE_REPORT%"
    timeout /t 2 >nul
)

if defined ENHANCED_REPORT (
    echo 🎨 Enhanced Report available at: %ENHANCED_REPORT%
    echo    (You can open this manually for detailed technical analysis)
)

echo.
echo ==========================================
echo 🏆 DEMO COMPLETE - ENHANCED FEATURES SHOWN:
echo ==========================================
echo ✅ Phase 1: Enhanced Element Capture with Fingerprinting
echo ✅ Phase 2: Advanced Element Matching with Confidence Scores  
echo ✅ Phase 3: Structural Analysis and Context Enhancement
echo ✅ Phase 4: Simple Clean Report Generation
echo ✅ Human-Readable Change Descriptions
echo ✅ Performance Metrics and Analysis
echo ✅ Configurable Thresholds and Settings
echo ==========================================
echo.
echo 💡 TIP: Compare the new reports with old ones to see the improvements!
echo 📚 Check ENHANCED_README.md for complete usage documentation
echo.

pause
