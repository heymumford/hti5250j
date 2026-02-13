#!/bin/bash
#
# Wave 1/2 Readiness Verification Script
# Usage: ./verify-wave-readiness.sh
#
# This script validates all quality gates for Wave 3 readiness

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0
PASSES=0

echo "========================================="
echo "Wave 1/2 Readiness Verification"
echo "========================================="
echo ""

# Gate 1: Compilation Check
echo "Gate 1: Checking compilation..."
if ./gradlew clean compileJava 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✅ PASS${NC} - Code compiles without errors"
    ((PASSES++))
else
    echo -e "${RED}❌ FAIL${NC} - Compilation errors detected"
    echo "Run: ./gradlew clean compileJava"
    ((ERRORS++))
fi
echo ""

# Gate 2: Test Compilation Check
echo "Gate 2: Checking test compilation..."
if ./gradlew clean compileTestJava 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✅ PASS${NC} - Tests compile without errors"
    ((PASSES++))
else
    echo -e "${RED}❌ FAIL${NC} - Test compilation errors detected"
    echo "Run: ./gradlew clean compileTestJava"
    ((ERRORS++))
fi
echo ""

# Gate 3: Test Execution Check
echo "Gate 3: Running test suite..."
if ./gradlew test 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✅ PASS${NC} - All tests passing"
    ((PASSES++))

    # Count test results
    TEST_COUNT=$(./gradlew test 2>&1 | grep -oP '\d+(?= tests completed)' | head -1)
    if [ -n "$TEST_COUNT" ]; then
        echo "   Test count: $TEST_COUNT tests"
    fi
else
    echo -e "${RED}❌ FAIL${NC} - Test failures detected"
    echo "Run: ./gradlew test --info"
    ((ERRORS++))
fi
echo ""

# Gate 4: Integration Tests Exist
echo "Gate 4: Checking integration tests..."
INTEGRATION_TESTS=$(find src/test/java -name "*IntegrationTest.java" 2>/dev/null | wc -l)
if [ "$INTEGRATION_TESTS" -ge 3 ]; then
    echo -e "${GREEN}✅ PASS${NC} - Integration tests found ($INTEGRATION_TESTS files)"
    ((PASSES++))
elif [ "$INTEGRATION_TESTS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  WARN${NC} - Some integration tests found ($INTEGRATION_TESTS files, need 3+)"
    ((WARNINGS++))
else
    echo -e "${RED}❌ FAIL${NC} - No integration tests found"
    echo "Expected: src/test/java/**/*IntegrationTest.java"
    ((ERRORS++))
fi
echo ""

# Gate 5: Warning Count Check
echo "Gate 5: Checking warning count..."
WARNING_COUNT=$(./gradlew build 2>&1 | grep -oP '\d+(?= warnings?)' | head -1)
if [ -z "$WARNING_COUNT" ]; then
    WARNING_COUNT=0
fi

if [ "$WARNING_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✅ PASS${NC} - No warnings"
    ((PASSES++))
elif [ "$WARNING_COUNT" -le 33 ]; then
    echo -e "${YELLOW}⚠️  WARN${NC} - $WARNING_COUNT warnings (budget: 33 max)"
    echo "   Warning count acceptable but should be reduced"
    ((PASSES++))  # Pass but note warning
else
    echo -e "${RED}❌ FAIL${NC} - $WARNING_COUNT warnings (exceeds budget of 33)"
    echo "   New warnings introduced - must fix"
    ((ERRORS++))
fi
echo ""

# Gate 6: Critical Files Check
echo "Gate 6: Checking critical file compilation..."
CRITICAL_FILES=(
    "src/org/hti5250j/GuiGraphicBuffer.java"
    "src/org/hti5250j/event/SessionConfigEvent.java"
    "src/org/hti5250j/framework/tn5250/Rect.java"
)

CRITICAL_ERRORS=0
for file in "${CRITICAL_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo -e "${RED}❌${NC} $file - FILE MISSING"
        ((CRITICAL_ERRORS++))
    else
        # Check if file compiles (by checking if class file exists after build)
        CLASS_FILE="${file%.java}.class"
        CLASS_FILE="build/classes/java/main/${CLASS_FILE#src/}"
        if [ -f "$CLASS_FILE" ]; then
            echo -e "${GREEN}✅${NC} $file"
        else
            echo -e "${RED}❌${NC} $file - COMPILATION FAILED"
            ((CRITICAL_ERRORS++))
        fi
    fi
done

if [ "$CRITICAL_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✅ PASS${NC} - All critical files compile"
    ((PASSES++))
else
    echo -e "${RED}❌ FAIL${NC} - $CRITICAL_ERRORS critical file(s) failed"
    ((ERRORS++))
fi
echo ""

# Gate 7: Specific Test Suites
echo "Gate 7: Running critical test suites..."
CRITICAL_TESTS=(
    "*RectTest"
    "*SessionConfigEventTest"
    "*GuiGraphicBufferTest"
)

TEST_FAILURES=0
for test in "${CRITICAL_TESTS[@]}"; do
    if ./gradlew test --tests "$test" 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        echo -e "${GREEN}✅${NC} $test"
    else
        echo -e "${RED}❌${NC} $test - FAILED"
        ((TEST_FAILURES++))
    fi
done

if [ "$TEST_FAILURES" -eq 0 ]; then
    echo -e "${GREEN}✅ PASS${NC} - All critical tests passing"
    ((PASSES++))
else
    echo -e "${RED}❌ FAIL${NC} - $TEST_FAILURES critical test suite(s) failed"
    ((ERRORS++))
fi
echo ""

# Summary
echo "========================================="
echo "Verification Summary"
echo "========================================="
echo -e "${GREEN}Passed:${NC}   $PASSES/7 gates"
echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
echo -e "${RED}Errors:${NC}   $ERRORS"
echo ""

# Final verdict
if [ "$ERRORS" -eq 0 ]; then
    echo "========================================="
    echo -e "${GREEN}✅ WAVE 3 READY${NC}"
    echo "========================================="
    echo ""
    echo "All quality gates passed!"
    echo "You may proceed to Wave 3 (file splitting)."
    echo ""
    if [ "$WARNINGS" -gt 0 ]; then
        echo "Note: $WARNINGS warning(s) detected - consider addressing in Wave 4"
    fi
    exit 0
else
    echo "========================================="
    echo -e "${RED}❌ NOT READY FOR WAVE 3${NC}"
    echo "========================================="
    echo ""
    echo "Action Required:"
    echo "1. Fix $ERRORS failing gate(s)"
    echo "2. Re-run this script to verify"
    echo "3. See IMMEDIATE_ACTION_CHECKLIST.md for fix steps"
    echo ""
    echo "Quick diagnosis:"
    ./gradlew build 2>&1 | grep -E "(error:|errors)" | head -10
    echo ""
    exit 1
fi
