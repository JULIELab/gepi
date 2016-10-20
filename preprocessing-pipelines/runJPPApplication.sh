#!/bin/bash
# Absolute path to this script, e.g. /home/user/bin/run.sh
if [ -z $JAVA_BIN ]; then
        JAVA_BIN="java"
fi
SCRIPTDIR="`dirname \"$0\"`"
cd jpp-application

CLASSPATH=resources:target/classes:`echo lib/*.jar | tr ' ' ':'`
JVM_OPTS="-Xmx5g -Dfile.encoding=UTF-8"
$JAVA_BIN $JVM_OPTS -cp $CLASSPATH de.julielab.jules.jpp.JppApplication $*
