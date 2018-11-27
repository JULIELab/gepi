#!/bin/bash
#SBATCH --mem-per-cpu 4G
#SBATCH --cpus-per-task 1
#SBATCH --ntasks 1
srun --exclusive java -Xmx3G -Dlogback.configurationFile=/home/faessler/Coding/git/gepi/gepi/gepi-indexing-pipeline/config/logback.xml -jar ~/bin/jcore-pipeline-runner* pipelinerunner.xml
