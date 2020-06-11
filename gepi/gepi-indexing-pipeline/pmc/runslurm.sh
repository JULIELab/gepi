#!/bin/bash
#SBATCH --mem 15G
#SBATCH --cpus-per-task 7 
#SBATCH -J GepiIndexPMC 
$JAVA_HOME/bin/java -jar ~/bin/jcore-pipeline-runner-base* run.xml

