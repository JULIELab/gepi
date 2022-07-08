#!/bin/bash
# This file's original source is the julielab-concept-db-manager/julielab-concept-creation-hgnc-groups
# project:
echo "Downloading data from HGNC for the creation of HGNC gene group concepts."
wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/family.csv
wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/family_alias.csv
wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/hierarchy.csv
wget -q "https://www.genenames.org/cgi-bin/download/custom?col=gd_hgnc_id&col=md_eg_id&col=family.id&col=gd_pub_eg_id&status=Approved&status=Entry%20Withdrawn&hgnc_dbtag=on&order_by=gd_app_sym_sort&format=text&submit=submit" -O gene_group_ncbi_map.tsv
echo "Done."