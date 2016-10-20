#!/bin/bash

#SBATCH --mem 18000
#SBATCH --cpus-per-task 4

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi
source ~/.bashrc
# Absolute path to this script, e.g. /home/user/bin/run.sh
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-syntax

echo $logconf
export CLASSPATH=resources:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xms8g -Xmx15g"
$JAVA_BIN $JVM_OPTS -Dfile.encoding=UTF-8 -Djava.util.logging.config.file=$logconf -cp $CLASSPATH de.julielab.jules.jpp.JPPDBCPERunner -d desc/CPESyntax.xml $PIPELINE_PARAMS $*
