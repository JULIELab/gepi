#!/bin/sh

sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 4 --exclude=stemnet1 runAllPipelinePmc.sh -t4 -u
sbatch --cpus-per-task 8 --nodelist=s17 runAllPipelinePmc.sh -t8 -u
sbatch --cpus-per-task 8 --nodelist=s17 runAllPipelinePmc.sh -t8 -u
sbatch --cpus-per-task 5 -p express runAllPipelinePmc.sh -t5 -u
sbatch --cpus-per-task 5 -p express runAllPipelinePmc.sh -t5 -u