#!/bin/bash
#SBATCH --mem 35G
#SBATCH --cpus-per-task 7 
#SBATCH -J PmcIdx 
$JAVA_HOME/bin/java -jar ~/bin/jcore-pipeline-runner-base* run.xml 

