#!/bin/bash

DATA=evaldata
DOWNLOAD=$DATA/download
TASK="-ge"

PIPLINE_DIR=bionlp-corpus-gene-traindata-creation
PIPELINE_INPUT_DIR=$PIPLINE_DIR/data/BioNLPinData
rm -rf $PIPELINE_INPUT_DIR
mkdir -p $PIPELINE_INPUT_DIR

# We don't need the data from 2009 because it is included in 2011
echo "Downloading all BioNLP-ST data from 2011 and 2013"
for EDITION in 2011 2013; do
		
	STBASE=$DOWNLOAD/bionlp-st-$EDITION$TASK/original-data

	if [[ "$EDITION" == "2011" ]]; then

 	 if [[ ! -d "$STBASE" ]]; then
   		 git -C $DOWNLOAD clone https://github.com/openbiocorpora/bionlp-st-2011-ge.git
 	 fi

	elif [[ "$EDITION" == "2013" ]]; then

 	 if [[ ! -d "$STBASE" ]]; then
  	  git -C $DOWNLOAD clone https://github.com/openbiocorpora/bionlp-st-2013-ge.git
 	 fi
 	fi
 	echo "Creating symbolic links in $PIPELINE_INPUT_DIR for data in $base_dir/$STBASE/train/. Note that some files are shared among multiple challenge years so that a number of 'File exists' warnings will appear."
 	base_dir=$(pwd)
 	cd $PIPELINE_INPUT_DIR
 	for i in $base_dir/$STBASE/train/*; do
 		ln -s $i
 	done
 	cd $base_dir
done

 RUNNER_VERSION=0.5.1
  if [[ ! -f "jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar" ]] || [[ ! -f "jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar" ]]; then
        echo "Downloading JCoRe Pipeline Runner";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-base/$RUNNER_VERSION/jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-cpe/$RUNNER_VERSION/jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar";
  fi

cd $PIPLINE_DIR
java -jar ../jcore-pipeline-runner-base* run.xml
cd ..



