#!/bin/bash
# Parameter 1: Number of jobs to run in parallel (mandatory)
# Parameter 2: Number of CPUs (threads) per job (mandatory)
# Parameter 3: SLURM queue to push jobs into (mandatory)
# Parameter 4: Parameters to forward to the pipeline scripts (only tested with one parameter up to now)

function sendSubPipelineJobs {
	jobfile=$1
	dep_cmd=""
	partition=$4
	if [[ "<none>" != $3 ]]; then
		# Format: afterok:45:46:47:...:
		# The first : is already contained in the job-id-string.
		dep_cmd="-d afterok$3"
	fi
	echo "Sending jobs for $2 (jobfile: $1, dependencies: $dep_cmd)."
	for i in $(eval echo "{1..$NJ}"); do
		outfile=$LOGDIR/$2-$i-%j.log
		partitionParam=""
		if [ ! -z $partition ]; then
			partitionParam="-p $partition"
		fi
        	command="sbatch $partitionParam -o $outfile -e $outfile -J $2$i $dep_cmd --cpus-per-task=$NCPU $jobfile"
		answer=`$command`
		exitcode=$?
		if [[ $exitcode != 0 ]]; then
			echo "Non-0 exit value when calling SLURM, exiting."
                        exit $exitcode
                fi
        	# SLURM answeres in the form "Submitted batch job 48", thus the forth
        	# word ist the job id.
        	job_id=$(echo $answer | awk ' { print $4 }')
        	new_job_ids="$new_job_ids:$job_id"
	done
	# Save the job IDs just created.
	last_job_ids=$new_job_ids
	# Empty the list as to not accumulate new job IDs onto older job IDs.
	new_job_ids=""
	# Additionally, store the job IDs to file for external jobs which may
	# on these here.
	echo $last_job_ids > last_job_ids.txt
}

LOGDIR=log


if [ ! -d $LOGDIR ]; then
	mkdir $LOGDIR
fi

# Number of jobs per pipeline.
if [ -z $1 ]; then
	#echo "You must specify the number of jobs running in parallel." >&2
	echo "Usage: $0 <number of pipelines> <number of threads per pipeline> [SLURM queue] [pipeline parameters]"
	exit 1
fi
# Number of CPUs per pipeline (corresponding to the processingUnitThreadCount
# attribute in the CPE descriptor).
if [ -z $2 ]; then
	echo "You must specify the number of jobs - i.e. the number of piplines - to start." >&2
	exit 1
fi
# SLURM queue to execute the jobs on
if [ -z $2 ]; then
	echo "You must specify the number CPUs per job - i.e. the number of threads per pipeline." >&2
	exit 1
fi

NJ=$1
NCPU=$2
JPP_PARTITION=$3

# This environment variable has to be used by the pipeline scripts
export PIPELINE_PARAMS="$4 -t $2"

# Change into the script directory so that the relative paths to
# the pipeline scripts are correct.
SCRIPTDIR="`dirname \"$0\"`"
cd $SCRIPTDIR
jobfilesyntax="runSyntaxPipeline.sh"
jobfilegenesandrelations="runGenesAndRelationsPipeline.sh"
jobfilesemedicometadata="runSemedicoPipeline.sh"

# The list of job ids allocated to the last pipeline jobs.
last_job_ids="<none>"
# The list of job ids just created be sending a new pipeline to SLURM.
new_job_ids=""

# If a particular SLURM partition is required, the JPP_PARTITION variable
# has to be exported before running the script
sendSubPipelineJobs $jobfilesyntax "syntax" "<none>" $JPP_PARTITION
sendSubPipelineJobs $jobfilegenesandrelations "genRel" $last_job_ids $JPP_PARTITION
#sendSubPipelineJobs $jobfilesemedicometadata "smdco" $last_job_ids $JPP_PARTITION
