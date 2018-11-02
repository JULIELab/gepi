#!/bin/bash
# A small script to extract all argument gene IDs from the test JSON documents in this directory. This is used
# to build the gene-mapper resources for exactly these gene IDs (can be set in jcore-gene-mapper-resources metaScript.sh).
# These gene-mapper resources are used to build the gene concept database.
for i in *gz; do gzip -dc $i | grep -o '"allargumentgeneids":\[[^]]*\]'; done | tr ',' '\n' | sed -E 's/[^0-9]//g' | sort -u
