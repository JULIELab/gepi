#!/bin/bash
# To applied directly at a PubTator format file.
# Outputs the normalized genelist in JULIE Lab Entity Evaluator format.
#
# Uses other scripts as subroutines.
set -eu

bash pubtator2jlgenelist.sh $1 > pubtatorgeneannos.tmp
bash normalizeGNPGenelist.sh pubtatorgeneannos.tmp
rm pubtatorgeneannos.tmp
