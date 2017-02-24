#!/bin/bash

#SBATCH --mem 23000
#SBATCH --cpus-per-task 4

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi
source ~/.bashrc
# Absolute path to this script, e.g. /home/user/bin/run.sh
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-genes-and-relations

export CLASSPATH=resources:conf:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xmx20g"
$JAVA_BIN $JVM_OPTS -Dfile.encoding=UTF-8 -cp $CLASSPATH de.julielab.jules.jpp.JPPDBCPERunner -d desc/CPEGenesAndRelationsPmc.xml $PIPELINE_PARAMS $*
