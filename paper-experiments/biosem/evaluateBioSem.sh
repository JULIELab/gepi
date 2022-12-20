#!/bin/bash
# This script can train and test BioSem on the BioNLP Shared Task data of 2009, 2011 and 2013.
# The BioSem code must have been built before via 'mvn clean package'.
# Parameters: BioNLP Edition (2009, 2011, 2013) ; Training data (train or mixed (train+devel)) ; Test data (devel or test)


set -euo pipefail

function mergeBioNlpSTDataWithGNPGenes {
  ORIGINAL_DATA_LOC=$(realpath $1)

  RUNNER_VERSION=0.5.1
  if [[ ! -f "jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar" ]] || [[ ! -f "jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar" ]]; then
        echo "Downloading JCoRe Pipeline Runner";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-base/$RUNNER_VERSION/jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-cpe/$RUNNER_VERSION/jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar";
  fi
  # Run the pipeline that will apply GNormPlus on the text data after
  # removing the gold gene annotations.
  echo "Running JULIE Lab version of GNormPlus on $ORIGINAL_DATA_LOC"
  # The pipeline has fixed input- and output directories. Copy the data to and from there.
  INPUT_DIR=bionlp-st-ge-gene-recognition/data/BioNLPinData
  OUTPUT_DIR=bionlp-st-ge-gene-recognition/data/BioNLPoutData
  if [[ -d $INPUT_DIR ]]; then
    rm -rf $INPUT_DIR
  fi
  if [[ -d $OUTPUT_DIR ]]; then
    rm -rf $OUTPUT_DIR
  fi
  mkdir -p $INPUT_DIR
  mkdir -p $OUTPUT_DIR
  cp -s $ORIGINAL_DATA_LOC/* bionlp-st-ge-gene-recognition/data/BioNLPinData/
  
  # This lets the pipeline run
  cd bionlp-st-ge-gene-recognition
  java -jar ../jcore-pipeline-runner-base* run.xml
  cd ..
}

function downloadBioSemResources {
  if [[ ! -d jcore-dependencies ]] || [[ ! -d nl ]]; then
    git clone --depth 1 --single-branch -b master --filter=blob:none --sparse https://github.com/JULIELab/jcore-dependencies
    cd jcore-dependencies
    git sparse-checkout set biosem-event-extractor
    cd ..
  fi
  mkdir -p nl/uva/biosem/
  cp jcore-dependencies/biosem-event-extractor/lib/model/* nl/uva/biosem
}


if [[ $# -eq 0 ]]; then
  echo "Parameters: <BioNLP ST Edition: '2009', '2011', '2013'> <training data: 'train' or 'mixed' (mixed=train+devel)> <test data: 'devel' or 'test'>"
  exit 1
fi

DATA=evaldata
DOWNLOAD=$DATA/download

# 2009, 2011 or 2013
EDITION=$1
# Train or mixed (train + devel)
TRAIN_SOURCE=$2
# devel or test
EVAL_TARGET=$3
TASK="-ge"
# The git repository for 2009 does not carry a suffix like 'ge'
if [[ "$EDITION" = "2009" ]]; then
  TASK=""
fi

TEST_OUTPUT=$DATA/test-output

STBASE=$DOWNLOAD/bionlp-st-$EDITION$TASK/original-data
STTRAIN=$STBASE/train
STDEVEL=$STBASE/devel
# mixed = train + devel
STMIXED=$STBASE/mixed
STDBS=$DATA/dbs/st$EDITION

mkdir -p $DOWNLOAD
mkdir -p $TEST_OUTPUT

if [[ "$EDITION" == "2009" ]]; then

  if [[ ! -d "$STBASE" ]]; then
    git -C $DOWNLOAD clone https://github.com/openbiocorpora/bionlp-st-2009.git
  fi

elif [[ "$EDITION" == "2011" ]]; then

  if [[ ! -d "$STBASE" ]]; then
    git -C $DOWNLOAD clone https://github.com/openbiocorpora/bionlp-st-2011-ge.git
  fi

elif [[ "$EDITION" == "2013" ]]; then

  if [[ ! -d "$STBASE" ]]; then
    git -C $DOWNLOAD clone https://github.com/openbiocorpora/bionlp-st-2013-ge.git
  fi

fi

if [[ "$EVAL_TARGET" == "test" ]]; then

    TEST_DATA="$STBASE"/test
    TEST_DB="$STDBS"/test

  elif [[ "$EVAL_TARGET" == "devel" ]]; then

    TEST_DATA="$STBASE"/devel
    TEST_DB="$STDBS"/devel

  fi

 # Prepare mixed training data
if [[ $TRAIN_SOURCE == "mixed" ]]; then

  TRAIN_DATA="$STBASE"/mixed
  TRAIN_DB="$STDBS"/mixed

  if [[ ! -d "$STMIXED" ]]; then

    echo "Copy train and dev data into $STMIXED"
    mkdir "$STMIXED"
    cp "$STTRAIN"/* "$STMIXED"
    cp "$STDEVEL"/* "$STMIXED"

  fi

elif [[ $TRAIN_SOURCE == "train" ]]; then

  TRAIN_DATA="$STBASE"/train
  TRAIN_DB="$STDBS"/train

fi

echo "Looking for existing training database $TRAIN_DB to clear."
if ls $TRAIN_DB* &>/dev/null; then

   rm -r $TRAIN_DB.*

fi

echo "Looking for existing test database $TEST_DB to clear."
if ls $TEST_DB.* &>/dev/null; then

  rm -r $TEST_DB.*

fi

echo "Train data: $TRAIN_DATA"
echo "Test data: $TEST_DATA"

# Train the model so we can use it with the JCoRe BioSem UIMA component together with GNormPlus below
BIOSEM_JAR=biosem-event-extractor-1.1.8-with-dependencies.jar
if [[ ! -f "$BIOSEM_JAR" ]]; then
    echo "Downloading the BioSem event extraction program";
    wget https://repo1.maven.org/maven2/de/julielab/biosem-event-extractor/1.1.8/biosem-event-extractor-1.1.8-with-dependencies.jar
fi
export CLASSPATH=.:$BIOSEM_JAR

echo "Creating database for train texts"
java corpora.DataLoader "$TRAIN_DATA" "$TRAIN_DB" true
echo "Created database for train texts"
echo "Learning triggers"
java relations.TriggerLearner "$TRAIN_DB" "$TRAIN_DB"
echo "Learned triggers"
echo "Learning event patterns"
java relations.RuleLearner "$TRAIN_DB"
echo "Learned event patterns"


echo "Copy the train model to the pipeline and rename the files to the name set in the pipeline configuration (config/biossemconfig.properties)"
rm -rf bionlp-st-ge-gene-recognition/resources/relexmodel
mkdir -p bionlp-st-ge-gene-recognition/resources/relexmodel
cp $TRAIN_DB* bionlp-st-ge-gene-recognition/resources/relexmodel
curdir=$(pwd)
cd bionlp-st-ge-gene-recognition/resources/relexmodel;
# https://linuxgazette.net/18/bash.html
for i in *; do ext=${i#*.}; mv $i trainedmodel.$ext; done
cd $curdir
echo "Trained model copied to pipeline resources."

# Adapt the test data by merging GNormPlus-detected gene mentions into it
# and use that instead of the original gold genes for event prediction.
mergeBioNlpSTDataWithGNPGenes $TEST_DATA
MERGED_TEST_DATA=bionlp-st-ge-gene-recognition/data/BioNLPoutData/

if [[ ! -d nl ]]; then
  echo "Obtaining BioSem executable code and required resources."
  downloadBioSemResources;
fi

PIPELINE_INPUT_DIR=bionlp-st-ge-gene-recognition/data/BioNLPinData
PIPELINE_OUTPUT_DIR=bionlp-st-ge-gene-recognition/data/BioNLPoutData


if [[ "$EVAL_TARGET" == "devel" ]]; then

    if [[ "$EDITION" == "2009" || "$EDITION" == "2011" ]]; then

      # Use the local evaluation script that works with FP genes due to
      # the usage of GNormPlus
      EVALSCRIPT=./a2-evaluate-2011.pl

    elif [[ "$EDITION" == "2013" ]]; then

     # Use the local evaluation script that works with FP genes due to
      # the usage of GNormPlus
      EVALSCRIPT=./a2-evaluate-2013.pl

    fi


  # Evalution takes place on the original TEST_DATA with gold gene annotations
  echo "perl $EVALSCRIPT -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/*.a2"
  perl "$EVALSCRIPT" -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/*.a2
  echo "This evaluation result is for BioNLP ST $EDITION, trained on $TRAIN_SOURCE and tested on $EVAL_TARGET using genes recognized by GNormPlus instead of the gold gene annotations."

  if [[ $EDITION == 2011 ]]; then
    echo "Evaluating abstracts only: perl "$EVALSCRIPT" -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/PMID-*.a2";
    perl "$EVALSCRIPT" -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/PMID-*.a2
    echo "$EDITION abstract-only evaluation score."
    echo "Evaluating fulltext passages only: perl "$EVALSCRIPT" -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/PMC-*.a2"
    perl "$EVALSCRIPT" -s -p -g $PIPELINE_INPUT_DIR -t1 $PIPELINE_OUTPUT_DIR/PMC-*.a2
    echo "$EDITION fulltext-only evaluation score."
  fi

elif [[ "$EVAL_TARGET" == "test" && "$EDITION" == "2013" ]]; then
  # the 2013 edition has encoding issues in some file names about NFκB; the test evaluation will not work
  # unless this is fixed
  for i in $PIPELINE_OUTPUT_DIR/*; do mv $i `echo $i | sed 's/+%A6/κ/'`; done

fi

if [[ "$EVAL_TARGET" == "test" ]]; then

  echo "Making tar ball of test data output named 'test-output-$EDITION.tar.gz' that can be sent to the online evaluation at http://bionlp-st.dbcls.jp/GE/$EDITION/eval-test/"
  TEST_DATA_DIR=$PIPELINE_OUTPUT_DIR
  if [[ "$EDITION" == "2009" ]]; then
    # The data we download for the 2009 edition does not specify the "PMID-" prefix that is expected by the evaluation service
    rm -rf evaltmp;
    mkdir evaltmp;
    cp $PIPELINE_OUTPUT_DIR/*.a2 evaltmp;
    cd evaltmp;
    for i in *; do
      mv $i "PMID-$i";
    done
    cd ..
    TEST_DATA_DIR=evaltmp
  fi
  # from https://stackoverflow.com/a/39530409/1314955
  find $TEST_DATA_DIR -name '*.a2' -printf "%P\n" | tar -czf test-output-$EDITION.tar.gz --no-recursion -C $TEST_DATA_DIR -T -
  if [[ -d evaltmp ]]; then rm -rf evaltmp; fi

fi
