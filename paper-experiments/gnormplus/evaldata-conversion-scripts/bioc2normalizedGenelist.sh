#!/bin/bash
# Uses XPath expressions to extract the gene annotation data from BioC files.
# Then, converts these data into JULIE Lab Entity Evaluator format, including
# normalization of multiple IDs in one line to multiple lines with one ID.

set -eu

PRED_FILE=$1
java -Xmx4g -cp ../../julie-xml-tools*.jar de.julielab.xml.JulieXMLToolsCLIRecords $PRED_FILE "//annotation[infon='Gene']" ../../id "infon[@key='NCBI Gene']" location/@offset location/@length text "infon[@key='type']" > pred_not_normalized.genelist
awk -F "\t" -v OFS="\t" '{if($2 ~ /[;,]/) {split($2, ids, "[;,]"); for (i in ids) {print $1,ids[i],$3,$3+$4,$5,$6}} else {print $1,$2,$3,$3+$4,$5,$6}}' pred_not_normalized.genelist | sort -u | sed '/		/d'
rm pred_not_normalized.genelist