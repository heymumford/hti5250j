#!/bin/bash

echo "Detailed CCSID Duplication Analysis"
echo "===================================="
echo ""

# Get all regular (non-930) CCSID files except CCSID37 (baseline)
files=$(ls CCSID*.java | grep -v CCSID930.java | grep -v CCSID37.java)

total_files=0
total_lines=0
total_duplicate_lines=0
duplication_percentages=()

for file in $files; do
    lines=$(wc -l < "$file")
    diff_lines=$(diff -u CCSID37.java "$file" 2>/dev/null | grep "^[+-]" | grep -v "^[+-][+-][+-]" | wc -l)
    unique_lines=$((diff_lines / 2))
    duplicate_lines=$((lines - unique_lines))
    
    if [ $lines -gt 0 ]; then
        duplicate_pct=$((duplicate_lines * 100 / lines))
    else
        duplicate_pct=0
    fi
    
    total_files=$((total_files + 1))
    total_lines=$((total_lines + lines))
    total_duplicate_lines=$((total_duplicate_lines + duplicate_lines))
    duplication_percentages+=($duplicate_pct)
done

# Add baseline
total_files=$((total_files + 1))
total_lines=$((total_lines + 81))
total_duplicate_lines=$((total_duplicate_lines + 81))

# Calculate average duplication
avg_duplication=$((total_duplicate_lines * 100 / total_lines))

echo "Summary Statistics:"
echo "==================="
echo "Files analyzed (excluding CCSID930): $total_files"
echo "Total lines of code: $total_lines"
echo "Total duplicate lines: $total_duplicate_lines"
echo "Average duplication: $avg_duplication%"
echo ""

# Calculate median
IFS=$'\n' sorted=($(sort -n <<<"${duplication_percentages[*]}"))
unset IFS
n=${#sorted[@]}
if (( n % 2 == 1 )); then
    median=${sorted[n/2]}
else
    median=$(( (sorted[n/2-1] + sorted[n/2]) / 2 ))
fi

echo "Median duplication: $median%"
echo "Min duplication: ${sorted[0]}%"
echo "Max duplication: ${sorted[-1]}%"
