#!/bin/bash

export CLASSPATH=`echo lib/* | tr ' ' ':'`:conf:target/classes:resources
export UIMA_JVM_OPTS="-Xmx10g -Dfile.encoding=UTF-8"
runCPE.sh desc/CPE.xml
