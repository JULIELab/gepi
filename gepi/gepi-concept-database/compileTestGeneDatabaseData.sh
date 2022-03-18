#!/bin/bash
set -eo pipefail
# This script compiles excerpts from resource files required to build the Neo4j gene concept database.
# The base resources are:
# 1. gene_info.gz (downloaded from https://ftp.ncbi.nlm.nih.gov/gene/DATA/)
# 2. names.dmp (obtained from NCBI taxonomy via visiting https://ftp.ncbi.nih.gov/pub/taxonomy/ and downloading and extracting one of the taxdmp.* files)
# 3. gene_orthologs.gz (this file was previously part of gene_groups; downloaded from https://ftp.ncbi.nlm.nih.gov/gene/DATA/)
# 4. gene2summary (this must be compiled via the gene mapper resource creation; see the README.md of https://github.com/JULIELab/gene-name-mapping)
# The file paths are specified to this script as parameters in the given order.

if [ -z "$1" ]; then
  echo "Parameters: gene_info names_dmp gene_orthologs gene2summary"
  exit 1
fi

GENE_INFO=$1
NAMES_DMP=$2
GENE_ORTHOLOGS=$3
GENE2SUMMARY=$4

GENES_DIR=src/test/resources/geneconcepts/genes
ORGS_DIR=src/test/resources/geneconcepts/organisms

TEST_GENE_IDS_LIST=../gepi-core/src/test/resources/test-index-input/testEventGeneIds.txt

echo "Extracting gene_info_test from $GENE_INFO to $GENES_DIR/gene_info_test"
mkdir -p $GENES_DIR
mkdir -p $ORGS_DIR
CATCMD="cat"
if [ "${GENE_INFO: -3}" == ".gz" ]; then
  CATCMD="gzip -dc"
fi
$CATCMD $GENE_INFO | awk -v idlist=$TEST_GENE_IDS_LIST 'BEGIN{ FS="\t"; while(( getline line<idlist) > 0) ids[line] = 1; }{ if ($2 in ids) print $0 }' > $GENES_DIR/gene_info_test

echo "Extracting gene_orthologs_test from $GENE_ORTHOLOGS to $GENES_DIR/gene_orthologs_test"
CATCMD="cat"
if [ "${GENE_ORTHOLOGS: -3}" == ".gz" ]; then
  CATCMD="gzip -dc"
fi
$CATCMD $GENE_ORTHOLOGS | awk -v idlist=$TEST_GENE_IDS_LIST 'BEGIN{ FS="\t"; while(( getline line<idlist) > 0) ids[line] = 1; }{ if ($2 in ids || $5 in ids) print $0 }' > $GENES_DIR/gene_orthologs_test

echo "Extracting taxonomy IDs for test genes from $GENES_DIR/gene_info_test to $ORGS_DIR/taxIdsForTests.lst"
tail -n +2 $GENES_DIR/gene_info_test | cut -f1 > $ORGS_DIR/taxIdsForTests.lst

echo "Extracting names_test.dmp from $NAMES_DMP to $ORGS_DIR/names_test.dmp"
CATCMD="cat"
if [ "${NAMES_DMP: -3}" == ".gz" ]; then
  CATCMD="gzip -dc"
fi
$CATCMD $NAMES_DMP | awk -v idlist=$ORGS_DIR/taxIdsForTests.lst 'BEGIN{ FS="\t"; while(( getline line<idlist) > 0) ids[line] = 1; }{ if ($1 in ids) print $0 }' > $ORGS_DIR/names_test.dmp

echo "Extracting gene2summary_test from $GENE2SUMMARY to $GENES_DIR/gene2summary_test"
CATCMD="cat"
if [ "${GENE2SUMMARY: -3}" == ".gz" ] ; then
  CATCMD="gzip -dc"
fi
$CATCMD $GENE2SUMMARY | awk -v idlist=$TEST_GENE_IDS_LIST 'BEGIN{ FS="\t"; while(( getline line<idlist) > 0) ids[line] = 1; }{ if ($1 in ids) print $0 }' > $GENES_DIR/gene2summary_test