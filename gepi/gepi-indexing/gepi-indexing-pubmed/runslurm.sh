#!/bin/bash
#SBATCH --mem 17G
#SBATCH --cpus-per-task 7 
#SBATCH -J PmIdx 
$JAVA_HOME/bin/java -jar ~/bin/jcore-pipeline-runner-base* run.xml

