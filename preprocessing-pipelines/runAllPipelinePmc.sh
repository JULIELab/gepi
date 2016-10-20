#!/bin/bash

#SBATCH --mem 24000
#SBATCH --cpus-per-task 4

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi
source ~/.bashrc
# Absolute path to this script, e.g. /home/user/bin/run.sh
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-all

export CLASSPATH=resources:conf:../jpp-semedico-metadata:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xmx20g"

$JAVA_BIN $JVM_OPTS -Dfile.encoding=UTF-8 -cp $CLASSPATH de.julielab.jules.jpp.JPPDBCPERunner -d desc/CPEAllPMC.xml $PIPELINE_PARAMS $*
