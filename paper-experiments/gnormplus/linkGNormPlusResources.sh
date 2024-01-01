#!/bin/bash
# This script creates links to GNormPlus resources required for
# the processing pipelines to run. GNormPlus
# base directory must be provided as the sole parameter. 
# 
# It uses another script to do the actual work. See the longer
# comment in that script for detailed information.

set -eu

source ../../linkGNormPlusResourcesFunction.sh

PIPELINE_DIRS=("bc2-processing" "bc3-processing" "nlm-gene-processing")
linkGnpResources $1 "${PIPELINE_DIRS[@]}"
