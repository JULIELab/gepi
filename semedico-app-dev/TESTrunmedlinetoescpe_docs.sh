#!/bin/bash

#SBATCH -p express
#SBATCH --mem 23000
#SBATCH --cpus-per-task 1

#cd target

export CLASSPATH=target/classes:conf:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xms2g -Xmx10g"

if [ -z $JAVA_BIN ]; then
	JAVA_BIN=java
fi

$JAVA_BIN $JVM_OPTS -cp $CLASSPATH de.julielab.jules.cpe.DBCPERunner -d desc/CPEMedlineToES_Docs.xml $*
