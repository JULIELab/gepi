#!/bin/bash
# Expects a genelist with columns in JULIE Lab format (docId, gene-id, begin, end, text, entity type)
# where the gene-id column might contain multiple IDs separated by commata or semicoli.
# Outputs a JULIE Lab format genelist where records with multiple IDs have been multiplied to one row
# per ID. No multiple IDs are left.

cat $1 | sed 's/([tT]ax:[0-9]*)//g' | awk -F "\t" '{if($2 ~ /[;,]/) {split($2, ids, /[;,]/); for (i in ids) print $1 "\t" ids[i] "\t" $3 "\t" $4 "\t" $5 "\t" $6 "\t" $7} else {print $0}}' 

