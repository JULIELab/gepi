#!/bin/bash
set -eu pipefail
# Directory where some downloads and temporary files go to
TMP_DIR=/tmp
# Single files that are downloaded directly to the given location and then read by the concept manager
UP_MAPPING=/data/data_resources/biology/idmapping/idmapping_selected.tab.gz
GENE_INFO=/data/data_resources/biology/entrez/gene/gene_info.gz
GENE_ORTHOLOGS=/data/data_resources/biology/entrez/gene/gene_orthologs.gz
# Directories of files or files that are derived from existing resources and need some processing.
# Some of these require the TMP_DIR
FAMPLEX_DIR=famplex-import-files
HGNC_GROUPS_DIR=hgnc-groups-import-files
# Only the Gene Ontology at the moment
ONTOLOGY_CONCEPTS=extracted-ontology-concepts

curdir=$(pwd)

if [[ ! -f $UP_MAPPING ]]; then
  mkdir -p $(basename $(dirname $UP_MAPPING))
  UP_MAPPING_URL=https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping_selected.tab.gz
  echo "Downloading UniProt ID mapping from $UP_MAPPING_URL"
  wget $UP_MAPPING_URL -O $UP_MAPPING
else
  echo "Found UniProt ID mapping at $UP_MAPPING"
fi

if [[ ! -f $GENE_INFO ]]; then
  mkdir -p $(basename $(dirname $GENE_INFO))
  GENE_INFO_URL=ftp://ftp.ncbi.nih.gov/gene/DATA/gene_info.gz
  echo "Downloading gene_info from $GENE_INFO_URL"
  wget $GENE_INFO_URL -O $GENE_INFO
else
  echo "Found gene_info at $GENE_INFO"
fi

if [[ ! -f $GENE_ORTHOLOGS ]]; then
  mkdir -p $(basename $(dirname $GENE_ORTHOLOGS))
  GENE_ORTHOLOGS_URL=https://ftp.ncbi.nih.gov/gene/DATA/gene_orthologs.gz
  echo "Downloading gene_orthologs from $GENE_GENE_ORTHOLOGS_URL"
  wget $GENE_GENE_GENE_ORTHOLOGS_URL $GENE_GENE_ORTHOLOGS
else
  echo "Found gene_orthologs at $GENE_ORTHOLOGS"
fi

if [[ ! -d $HGNC_GROUPS_DIR ]]; then
  mkdir -p $HGNC_GROUPS_DIR
  cd $HGNC_GROUPS_DIR
  echo "Downloading data from HGNC for the creation of HGNC gene group concepts."
  wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/family.csv
  wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/family_alias.csv
  wget -q http://ftp.ebi.ac.uk/pub/databases/genenames/hgnc/csv/genefamily_db_tables/hierarchy.csv
  wget -q "https://www.genenames.org/cgi-bin/download/custom?col=gd_hgnc_id&col=md_eg_id&col=family.id&col=gd_pub_eg_id&status=Approved&status=Entry%20Withdrawn&hgnc_dbtag=on&order_by=gd_app_sym_sort&format=text&submit=submit" -O gene_group_ncbi_map.tsv
  cd $curdir
else
  echo "Found HGNC Group directory at $HGNC_GROUPS_DIR"
fi

if [[ ! -d $ONTOLOGY_CONCEPTS ]]; then
  echo "Downloading tools for ontology class extraction."
  if [[ ! -f  julielab-bioportal-ontology-tools-1.1.1-cli.jar ]]; then
    wget https://repo1.maven.org/maven2/de/julielab/julielab-bioportal-ontology-tools/1.1.1/julielab-bioportal-ontology-tools-1.1.1-cli.jar
  fi
  mkdir -p $TMP_DIR/ontologies
  echo "Downloading Gene Ontology from BioPortal"
  wget http://www.geneontology.org/ontology/gene_ontology.obo -O $TMP_DIR/ontologies/gene_ontology.obo
  echo "Removing xrefs from OBO format because the OWL API will mix them up with the actual names of the class, possibly resulting in wrong preferred names."
  sed '/^xref:/d' $TMP_DIR/ontologies/gene_ontology.obo > $TMP_DIR/ontologies/gene_ontology_no_xrefs.obo
  echo "Extracting GO concepts from $TMP_DIR/ontologies/ to concept manager JSON format at $ONTOLOGY_CONCEPTS"
  java -jar julielab-bioportal-ontology-tools-*-cli.jar -eci $TMP_DIR/ontologies/ dummy $ONTOLOGY_CONCEPTS false true gene_ontology_no_xrefs
  mv $ONTOLOGY_CONCEPTS/gene_ontology_no_xrefs.cls.jsonlst.gz $ONTOLOGY_CONCEPTS/gene_ontology.cls.jsonlst.gz
else
  echo "Found directory for ontology concepts in JSON format at $ONTOLOGY_CONCEPTS"
fi

if [[ ! -d $FAMPLEX_DIR ]]; then

  echo "Downloading and creating FamPlex resources."

  mkdir -p $FAMPLEX_DIR
  if [[ ! -d "$TMP_DIR/famplex" ]]; then
    echo "Cloning the FamPlex GitHub repository for gene/protein families and complexes."
    git clone https://github.com/johnbachman/famplex.git $TMP_DIR/famplex
  fi
  entityFile="$TMP_DIR/famplex/entities.csv"
  groundingMapCsv="$TMP_DIR/famplex/grounding_map.csv"
  cat $entityFile | tr '\r' '\n' > famplexEntities.txt
  # exchange windows carriage return with linux newlines
  cat $groundingMapCsv | tr '\r' '\n' > t
  groundingMapTsv="$FAMPLEX_DIR/grounding_map.tsv"
  # convert to tab-separated format
  sed -E 's/("([^"]*)")?,/\2\t/g' t | sed '/^$/d' > $groundingMapTsv
  # print out the FPLX-prefixed entities as those are the families and complexes
  awk -v idlist=famplexEntities.txt 'BEGIN{ FS="\t"; while(( getline line<idlist) > 0) ids[line] = 1; }{ if ($3 in ids && $2 == "FPLX") print $1,"FPLX:"$3,"-1" }' OFS="\t" $groundingMapTsv > $TMP_DIR/famplex.dict

  # We use the SPECIALIST Lexicon to extend the FamPlex dictionary. It lacks some quite frequent synonyms in its raw form.
  if [[ ! -f "$TMP_DIR/LEXICON.xml" ]]; then
      echo "Downloading SPECIALIST LEXICON from https://data.lhncbc.nlm.nih.gov/public/lsg/lexicon/2023/release/LEX_DOC/XML/LEXICON.xml. WARNING This URL has changed in the past. Subsequent XML parsing errors might be caused by a wrong URL."
  	  wget https://data.lhncbc.nlm.nih.gov/public/lsg/lexicon/2023/release/LEX_DOC/XML/LEXICON.xml -O $TMP_DIR/LEXICON.xml
  fi
  if [[ ! -f "$TMP_DIR/jcore-gene-mapper-resources.jar" ]]; then
    wget https://search.maven.org/remotecontent?filepath=de/julielab/gene-mapper-resources/2.5.0/gene-mapper-resources-2.5.0-jar-with-dependencies.jar -O "$TMP_DIR/jcore-gene-mapper-resources.jar"
  fi
  # Extend the FamPlex grounding dictionary
  echo "java -cp $TMP_DIR/jcore-gene-mapper-resources.jar de.julielab.genemapper.resources.SpecialistLexiconNameExpansion $TMP_DIR/LEXICON.xml $FAMPLEX_DIR/familyrecords.txt $TMP_DIR/famplex.dict"
  java -cp $TMP_DIR/jcore-gene-mapper-resources.jar de.julielab.genemapper.resources.SpecialistLexiconNameExpansion $TMP_DIR/LEXICON.xml $FAMPLEX_DIR/specialist-extended-famplexrecords.txt $TMP_DIR/famplex.dict

  # Create an alternative FamPlex relations.csv file that does not use HGNC IDs but NCBI Gene IDs.
  if [[ ! -f $FAMPLEX_DIR/relations_egids.tsv ]]; then
    cat $TMP_DIR/famplex/relations.csv | tr ',' '\t' > $TMP_DIR/relations.tsv
    wget 'https://www.genenames.org/cgi-bin/download/custom?col=gd_hgnc_id&col=gd_app_sym&col=gd_status&col=gd_prev_sym&col=gd_pub_eg_id&col=md_eg_id&status=Approved&status=Entry%20Withdrawn&hgnc_dbtag=on&order_by=gd_app_sym_sort&format=text&submit=submit' -O $TMP_DIR/hgnc2eg.txt
    echo "Mapping FamPlex leaf concepts to NCBI Gene IDs instead of HGNC IDs."
    awk -v idlist=$TMP_DIR/hgnc2eg.txt -v FS="\t" -v OFS="\t" '
    # Read the HGNC file to map HGNC symbols - used in FamPlex as HGNC IDs - to NCBI Gene IDs
    BEGIN{
      while((getline line < idlist) > 0) {
        split(line, a, "\t");
        # Sometimes the ID is not given by HGNC but by NCBI Gene directly which is expressed in different columns
        egid = a[5]!="" ? a[5] : a[6];
        # There are deprecated entries without Gene ID that sometimes have the same symbol as a non-deprecated entry; thus, do not override
        # entries for which we already have an NCBI Gene ID
        if(hgnc2eg[a[2]]=="")
          hgnc2eg[a[2]]=egid;
        # FamPlex uses deprecated HGNC symbols in some places. So check the "previous symbols" column, too.
        split(a[4], prevsyms, ", ");
        for (i in prevsyms) {
          # again, do not override entries that already have an NCBI Gene ID assigned
          if (hgnc2eg[prevsyms[i]]=="")
            hgnc2eg[prevsyms[i]]=egid
          }
        }
      }
      {
        # Print out all the columns of FamPlex relations but substitute HGNC symbols with NBCI Gene IDs.
        if($1=="HGNC" && hgnc2eg[$2] != "")
          print "EG",hgnc2eg[$2],$3,$4,$5;
        else print $0
      }' $TMP_DIR/relations.tsv > $FAMPLEX_DIR/relations_egids.tsv
  fi

else
  echo "FamPlex resources directory already exists, skipping FamPlex resource creation."
fi



