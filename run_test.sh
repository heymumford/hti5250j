#!/bin/bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
CLASSPATH="build:$(ls lib/development/*.jar | paste -sd: -):$(ls lib/runtime/*.jar | paste -sd: -)"
java -cp "$CLASSPATH" org.junit.runner.JUnitCore org.tn5250j.framework.tn5250.InsertCharModePairwiseTest
