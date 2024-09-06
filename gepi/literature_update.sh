#!/bin/bash
# Script that runs in an infinite loop and updates the GePI interaction index.
# CAUTION: This script is written for the specific situation in the JulieLab. You should adapt it for your environment.
# Takes one parameter:
# 1: Path to a file that defines the following environment variables:
# - GEPI_REPO_DIR: Directory to the GePI Maven project root (i.e. the gepi/ directory within the gepi repository)
# - GEPI_PREPROCESSING_PM: Path to the JCoRe Pipeline performing the NLP preprocessing of PubMed for GePI (e.g. the one at gepi-preprocessing/pubmed/preprocessing of this repository)
# - GEPI_PREPROCESSING_PMC: Path to the JCoRe Pipeline performing the NLP preprocessing of PMC for GePI (e.g. the one at gepi-preprocessing/pmc/preprocessing of this repository)
# - GEPI_INDEXING_PM: Path to the JCoRe Pipeline performing the ElasticSearch indexing of PubMed for GePI (e.g. the one at gepi-indexing/gepi-indexing-pubmed)
# - GEPI_INDEXING_PMC: Path to the JCoRe Pipeline performing the ElasticSearch indexing of PubMed for GePI (e.g. the one at gepi-indexing/gepi-indexing-pubmed)
set -e
# Stop via CTRL-Z followed by "kill %%""
source $1
TIME_CMD="/usr/bin/time -v"

# a day
SECONDS_BETWEEN_UPDATES=86400

# For security reasons, our GePI ElasticSearch does not accept remote
# connections. Thus, we need to tunnel to it for indexing.
function tunnel_to_es() {
	cmd1='ssh -i ~/.ssh/id_rsa -4 -f -N -L 9201:localhost:9200 gepi-vm'
	pid=`pgrep -f "${cmd1}" || true`
	if [ -n "$pid" ]; then
		kill $pid
	fi
	${cmd1}
	cmd2='ssh -i ~/.ssh/id_rsa -4 -f -N -L 9301:localhost:9300 gepi-vm'
	pid=`pgrep -f "${cmd2}" || true`
	if [ -n "$pid" ]; then
		kill $pid
	fi
	${cmd2}
}

# Do the update again and again. There will be at least $SECONDS_BETWEEN_UPDATES between two runs.
while [ 1 ]
do
	START_TIME=`date +%s`
	
	cd $HOME/bin
	echo "[LitUpdate] Importing new PubMed XML documents into the database `date`"
	$TIME_CMD java -jar costosys.jar -dbc $GEPI_PREPROCESSING_PM/config/costosys.xml -im $GEPI_PREPROCESSING_PM/../pubmedImport.xml
	echo "[LitUpdate] Finished importing new PubMed XML documents into the database `date`"
	echo "[LitUpdate] Importing new PMC XML documents into the database `date`"
	$TIME_CMD java -jar costosys.jar -dbc $GEPI_PREPROCESSING_PMC/config/costosys.xml -ip $GEPI_PREPROCESSING_PMC/../pmcImport.xml
	echo "[LitUpdate] Finished importing new PMC XML documents into the database `date`"

	# Run the NLP processing
	echo "[LitUpdate] Running PubMed preprocessing `date`"
	cd $GEPI_PREPROCESSING_PM
	$TIME_CMD ./run.sh
	echo "[LitUpdate] Finished running PubMed preprocessing `date`"
	echo "[LitUpdate] Running PMC preprocessing `date`"
	cd $GEPI_PREPROCESSING_PMC
	$TIME_CMD ./run.sh
	echo "[LitUpdate] Finished running PMC preprocessing `date`"

	# Run the indexing
	# Reset documents that have been stuck in "in_process" for some reason (e.g. broken ES tunnel in last processing)
	java -jar $HOME/bin/costosys.jar -dbc $GEPI_PREPROCESSING_PM/config/costosys.xml -re gepi._documents_mirror -np
	# Open tunnel to ES
	tunnel_to_es
	echo "[LitUpdate] Running PubMed indexing `date`"
	cd $GEPI_INDEXING_PM
	$TIME_CMD ./run.sh
	echo "[LitUpdate] Finished running PubMed indexing `date`"
	echo "[LitUpdate] Running PMC indexing `date`"
	# Reset documents that have been stuck in "in_process" for some reason (e.g. broken ES tunnel in last processing)
	java -jar $HOME/bin/costosys.jar -dbc $GEPI_PREPROCESSING_PMC/config/costosys.xml -re gepi._documents_mirror -np
	# Reset the tunnel or re-create it if it collapsed before
	tunnel_to_es
	cd $GEPI_INDEXING_PMC
	$TIME_CMD ./run.sh
	echo "[LitUpdate] Finished running PMC indexing `date`"

	END_TIME=`date +%s`
	ELAPSED_TIME=$(($END_TIME-$START_TIME))
	echo "[LitUpdate] Updated PubMed and PMC literature from XML to index in $ELAPSED_TIME seconds. `date`"
	if [ $ELAPSED_TIME -lt $SECONDS_BETWEEN_UPDATES ]; then
		SLEEP_TIME=$((SECONDS_BETWEEN_UPDATES-$ELAPSED_TIME))
		echo "[LitUpdate] Sleeping for $SLEEP_TIME seconds before starting next update. `date`"
		sleep $SLEEP_TIME
	else
		echo "[LitUpdate] Update took longer than the time between update runs. Starting with a new update. `date`"
	fi
done
