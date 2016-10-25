#!/bin/bash

#SBATCH -p express
#SBATCH --mem 32000
#SBATCH --cpus-per-task 1

#cd target

export CLASSPATH=target/classes:conf:resources:.:`echo lib/*.jar | tr ' ' ':'`

runCPE.sh desc/CPEFromXMI.xml