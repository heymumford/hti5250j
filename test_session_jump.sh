#!/bin/bash

# Simple test runner for SessionJumpEvent Record conversion

echo "=========================================="
echo "SessionJumpEvent Record Conversion Tests"
echo "=========================================="
echo ""

# Compile SessionJumpEvent and test
javac -source 21 \
  -cp ".:build/classes/java/main:lib/runtime/*:lib/development/*" \
  -d build/classes/java/test \
  tests/org/hti5250j/event/SessionJumpEventRecordTest.java \
  src/org/hti5250j/event/SessionJumpEvent.java \
  src/org/hti5250j/event/SessionJumpListener.java 2>&1 | grep -v "Annotation processing"

if [ $? -eq 0 ]; then
  echo "✓ Compilation successful"
  echo ""

  # Run tests
  java -cp "build/classes/java/test:build/classes/java/main:lib/runtime/*:lib/development/*" \
    org.junit.platform.console.ConsoleLauncher \
    --scan-classpath build/classes/java/test \
    --include-classname "SessionJumpEventRecordTest" 2>&1
else
  echo "✗ Compilation failed"
  exit 1
fi
