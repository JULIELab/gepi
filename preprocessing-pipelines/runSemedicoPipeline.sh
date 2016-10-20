#!/bin/bash
#SBATCH --mem 20000
#SBATCH --cpus-per-task 4

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi
source ~/.bashrc
# Absolute path to this script, e.g. /home/user/bin/run.sh
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-semedico-metadata

# include the 'desc' directory to resolve the entity descriptors in the 'entities' directory by the EntityAAE descriptor.
export CLASSPATH=resources:.:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xms10g -Xmx17g"
$JAVA_BIN $JVM_OPTS -Dfile.encoding=UTF-8 -cp $CLASSPATH de.julielab.jules.jpp.JPPDBCPERunner -d desc/CPESemedicoMetadata.xml $PIPELINE_PARAMS $*
