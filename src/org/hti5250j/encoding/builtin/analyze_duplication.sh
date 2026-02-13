#!/bin/bash

echo "CCSID File Duplication Analysis"
echo "================================="
echo ""

baseline="CCSID37.java"
baseline_lines=$(wc -l < "$baseline")

echo "Baseline file: $baseline ($baseline_lines lines)"
echo ""
echo "| File | Lines | Diff Lines | Duplicate % |"
echo "|------|-------|-----------|-------------|"

for file in CCSID*.java; do
    if [ "$file" = "$baseline" ]; then
        echo "| $file | $baseline_lines | (baseline) | - |"
    else
        total_lines=$(wc -l < "$file")
        diff_lines=$(diff -u "$baseline" "$file" 2>/dev/null | grep "^[+-]" | grep -v "^[+-][+-][+-]" | wc -l)
        
        # Calculate unique lines (diff_lines / 2 because diff shows +/- pairs)
        unique_lines=$((diff_lines / 2))
        duplicate_lines=$((total_lines - unique_lines))
        
        # Calculate percentage
        if [ $total_lines -gt 0 ]; then
            duplicate_pct=$((duplicate_lines * 100 / total_lines))
        else
            duplicate_pct=0
        fi
        
        echo "| $file | $total_lines | $diff_lines | $duplicate_pct% |"
    fi
done
