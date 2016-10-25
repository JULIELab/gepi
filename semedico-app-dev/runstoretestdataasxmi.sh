#!/bin/bash

#SBATCH -p express
#SBATCH --mem 5000
#SBATCH --cpus-per-task 2

#cd target

export CLASSPATH=target/classes:conf:`echo lib/*.jar | tr ' ' ':'`
export JVM_OPTS="-Xms23g -Xmx23g"

if [ -z $JAVA_BIN ]; then
	JAVA_BIN=java
fi

$JAVA_BIN $JVM_OPTS -cp $CLASSPATH de.julielab.jules.cpe.DBCPERunner -d desc/CPEStoreTestDataAsXMI.xml $*
