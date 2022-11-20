#!/bin/bash
# Takes in a complete PubTator file. Extracts the Gene entity annotations
# and re-formats them to adhere to the JULIE Lab Entity Evaluator format
# (docId, gene-id, begin, end, entity text, entity type).

# Extract the gene annotations from a PubTator file.
grep '^[0-9]*	[0-9]*.*Gene.*' $1 > geneannotationsfrompubtator.txt
# Re-arrange the columns to fit the JULIE Lab Entity Evaluator format.
# Note that this is what we call 'unnormalized' in the sense that
# each row may contain multiple IDs separated by commata or semicoli.
# The Entity Evaluator does not handle this and requires the application
# of the normalizeGenelist.sh script. Alternatively, use pubtator2jlgenelist.sh	
# which calls this script and normalizeGenelist.sh.
awk -v FS="\t" -v OFS="\t" '{print $1,$6,$2,$3,$4,$5}' geneannotationsfrompubtator.txt
rm geneannotationsfrompubtator.txt

