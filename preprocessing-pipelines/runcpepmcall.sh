#!/bin/bash

#SBATCH --mem 23000
#SBATCH --cpus-per-task 4

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi
source ~/.bashrc
# Absolute path to this script, e.g. /home/user/bin/run.sh
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-all

export CLASSPATH=resources:conf:`echo lib/*.jar | tr ' ' ':'`
export UIMA_JVM_OPTS="-Xmx20g -Dfile.encoding=UTF-8 "
runCPE.sh desc/CPEAllPMC.xml
