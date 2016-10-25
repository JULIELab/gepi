#!/bin/bash

#SBATCH --mem 30000
#SBATCH --cpus-per-task 1


export CLASSPATH=target/classes:resources:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xmx2g"

if [ -z $JAVA_BIN ]; then
	JAVA_BIN=java
fi

$JAVA_BIN $JVM_OPTS -cp $CLASSPATH de.julielab.jules.cpe.DBCPERunner -d desc/CPE-Extract-Test-GeneIds.xml $*
