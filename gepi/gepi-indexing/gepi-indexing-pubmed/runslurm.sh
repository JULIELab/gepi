#!/bin/bash
#SBATCH --mem 25G
#SBATCH --cpus-per-task 7 
#SBATCH -J GEPIINDX 
$JAVA_HOME/bin/java -jar ~/bin/jcore-pipeline-runner-base* run.xml

