@echo off
REM Enhanced UI Tester Launcher Script
REM This script demonstrates how to use the new EnhancedUITesterMain class

echo ========================================
echo Enhanced UI Tester - Demo Launcher
echo ========================================

echo Compiling project...
call mvn compile -q

if errorlevel 1 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running Enhanced UI Tester with test files...
echo.

REM Convert file paths to URLs for testing
set BASELINE_FILE=file:///C:/Users/KaDh550/Desktop/java-ui-tester/test_files/baseline.html
set CURRENT_FILE=file:///C:/Users/KaDh550/Desktop/java-ui-tester/test_files/current.html

REM Run the enhanced main class
java -cp "target/classes;target/dependency/*" ^
    com.uitester.main.EnhancedUITesterMain ^
    --baseline "%BASELINE_FILE%" ^
    --current "%CURRENT_FILE%" ^
    --section-name "TestFiles" ^
    --max-elements 50 ^
    --wait-time 3 ^
    --confidence-threshold 0.7 ^
    --headless

echo.
echo ========================================
echo Enhanced Analysis Complete!
echo ========================================
echo Check the output directory for reports:
echo - Simple Report: output/TestFiles*/report-simple.html
echo - Enhanced Report: output/TestFiles*/report-enhanced.html
echo ========================================

pause
