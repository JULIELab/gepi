#!/bin/bash
#SBATCH --mem 50G
#SBATCH --cpus-per-task 7 
#SBATCH -J GEPIINDEX 
$JAVA_HOME/bin/java -jar ~/bin/jcore-pipeline-runner-base* run.xml

