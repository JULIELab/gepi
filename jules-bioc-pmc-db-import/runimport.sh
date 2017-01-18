#!/bin/bash

if [ -z $JAVA_BIN ]; then
	JAVA_BIN="java"
fi

export CLASSPATH=`echo lib/* | tr ' ' ':'`:conf:target/classes:resources
export UIMA_JVM_OPTS="-Xmx10g -Dfile.encoding=UTF-8"

$JAVA_BIN $UIMA_JVM_OPTS -cp $CLASSPATH de.julielab.jules.cpe.CPERunner desc/CPE.xml
