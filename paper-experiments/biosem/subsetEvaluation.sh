#!/bin/bash

set -eu

EDITION=$1

SUBSET="10089566"
PIPELINE_RUNNER_VERSION=0.5.0

DATA=evaldata
DOWNLOAD=$DATA/download

function evaluateOnTestdata {
	TRAIN_DB=$1
	TEST_DATA=$2
	TEST_DB=$3
	TEST_OUTPUT=$4
	
	export CLASSPATH=.:biosem-event-extractor-1.1.8-20221122.085511-4-with-dependencies.jar
	echo "Creating database for test data: java corpora.DataLoader $TEST_DATA "$TEST_DB" false"
	# Read the data with the GNormPlus gene mentions
	java corpora.DataLoader $TEST_DATA "$TEST_DB" false
	echo "Extracting events from test data to $TEST_OUTPUT: java relations.EventExtraction "$TRAIN_DB" "$TEST_DB" "$TEST_OUTPUT""
	rm -rf $TEST_OUTPUT
	mkdir -p $TEST_OUTPUT
	java relations.EventExtraction "$TRAIN_DB" "$TEST_DB" "$TEST_OUTPUT"

 if [[ "$EDITION" == "2009" || "$EDITION" == "2011" ]]; then

      EVALTOOLS_URL=http://bionlp-st.dbcls.jp/GE/2011/downloads/BioNLP-ST_2011_genia_tools_rev1.tar.gz
      EVALTOOLS_FILE=BioNLP-ST_2011_genia_tools_rev1.tar.gz
      EVALTOOLS=$DATA/BioNLP-ST_2011_genia_tools_rev1

    elif [[ "$EDITION" == "2013" ]]; then

      EVALTOOLS_URL=http://bionlp-st.dbcls.jp/GE/2013/downloads/BioNLP-ST-2013-GE-tools.tar.gz
      EVALTOOLS_FILE=BioNLP-ST-2013-GE-tools.tar.gz
      EVALTOOLS=$DATA/tools

    fi

    if [[ ! -d "$EVALTOOLS" ]]; then

      wget -P $DOWNLOAD $EVALTOOLS_URL
      tar -xzf $DOWNLOAD/$EVALTOOLS_FILE -C $DATA

    fi

  # Evalution takes place on the original TEST_DATA with gold gene annotations
  echo "perl $EVALTOOLS/a2-evaluate.pl -g $GOLD_TEST_DATA -t1 $TEST_OUTPUT/*.a2"
  perl "$EVALTOOLS"/a2-evaluate.pl -g $GOLD_TEST_DATA -t1 $TEST_OUTPUT/*.a2
}

function writeResultXmi {
	if [[ ! -f "jcore-pipeline-runner-base-$PIPELINE_RUNNER_VERSION-cli-assembly.jar" ]] || [[ ! -f "jcore-pipeline-runner-cpe-$PIPELINE_RUNNER_VERSION-jar-with-dependencies.jar" ]]; then
	      echo "Downloading JCoRe Pipeline Runner";
	      wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-base/$PIPELINE_RUNNER_VERSION/jcore-pipeline-runner-base-$PIPELINE_RUNNER_VERSION-cli-assembly.jar";
	      wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-cpe/$PIPELINE_RUNNER_VERSION/jcore-pipeline-runner-cpe-$PIPELINE_RUNNER_VERSION-jar-with-dependencies.jar";
	fi

	# The predicted evaluation .a2 files
	TEST_OUTPUT_DIR=$1
	# The .txt and a1 files from which relation was done
	TEST_INPUT_DIR=$2
	XMI_DESTINATION=$3
	PIPELINE_INPUT_DIR=result_visualization_pipeline/data/BioNLPinData
	PIPELINE_OUTPUT_DIR=result_visualization_pipeline/data/output-xmi

	rm -rf $PIPELINE_INPUT_DIR
	mkdir -p $PIPELINE_INPUT_DIR
	rm -rf $XMI_DESTINATION
	mkdir -p $XMI_DESTINATION

	for i in $TEST_OUTPUT_DIR/*; do
		filename=$(basename $i);
		basename="${filename%.*}"
		# Get the relation a2 file and its .txt and .a1 file so we can read the whole document with the
		# event predictions
		cp $i $PIPELINE_INPUT_DIR;
		cp $TEST_INPUT_DIR/$basename.txt $PIPELINE_INPUT_DIR
		cp $TEST_INPUT_DIR/$basename.a1 $PIPELINE_INPUT_DIR
	done

	cd result_visualization_pipeline;
	java -jar ../jcore-pipeline-runner-base* run.xml
	cd ..
	
	echo "Copy files from $PIPELINE_OUTPUT_DIR to $XMI_DESTINATION. The XMI files can be viewed using the UIMA annotation viewer using the jcore-all-types.xml type system. The annotation viewer is part of the UIMA download. The JCoRe type system is found in the jcore-base repository on GitHub."
	cp $PIPELINE_OUTPUT_DIR/* $XMI_DESTINATION

}

GOLD_TEST_DATA=subsetevaluation$EDITION/gold
MERGED_TEST_DATA=subsetevaluation$EDITION/merged
rm -rf $GOLD_TEST_DATA
rm -rf $MERGED_TEST_DATA
mkdir -p $GOLD_TEST_DATA
mkdir -p $MERGED_TEST_DATA
for i in $SUBSET; do
	cp bionlp-st-ge-gene-recognition/data/BioNLPinData/$i* $GOLD_TEST_DATA
	cp bionlp-st-ge-gene-recognition/data/BioNLPoutData/$i* $MERGED_TEST_DATA
done
TEST_DB=subsetevaluation$EDITION/db/test

TRAIN_DB=evaldata/dbs/st$EDITION/train
TEST_OUTPUT=subsetevaluation$EDITION/merged-relex-output
TEST_OUTPUT_ORG=subsetevaluation$EDITION/original-relex-output

evaluateOnTestdata $TRAIN_DB $MERGED_TEST_DATA $TEST_DB $TEST_OUTPUT
echo "Above score is for MERGED data evaluation"
evaluateOnTestdata $TRAIN_DB $GOLD_TEST_DATA $TEST_DB $TEST_OUTPUT_ORG
echo "Above score is for ORIGINAL data evaluation"

writeResultXmi $TEST_OUTPUT $MERGED_TEST_DATA subsetevaluation$EDITION/merged-relex-output-xmi
writeResultXmi $TEST_OUTPUT_ORG $GOLD_TEST_DATA subsetevaluation$EDITION/original-relex-output-xmi

echo "Find UIMA XMI files in the subsetevaluation$EDITION/merged-relex-output-xmi and subsetevaluation$EDITION/original-relex-output-xmi directories.";