#!/bin/bash
# This script creates links to GNormPlus resources required for
# the processing pipelines to run. The resource directory must be
# provided as the sole parameter.
# 
# The pipelines in this directory employ the JULIE Lab GNormPlus
# version to find gene mentions for evaluation. 
# GNormPlus requires a range of resources available in the original
# GNormPlus download from https://www.ncbi.nlm.nih.gov/research/bionlp/Tools/gnormplus/.
# Note that you need the official distribution to build the CRF++ library on your
# local machine (more below).
#
# For the reproduction of GePI paper experiments results, the very
# files used for evaluation purposes can be obtained here:
# https://zenodo.org/record/7327191#.Y3T_y4KZOPw
# Use these files to reproduce the results shown in the paper. The files
# from the official GNormPlus download are not required - except the CRF++ program.
#
# This script takes as argument the folder that contains the GNormPlus
# resources. Those can come from the official download or from the JULIE Lab
# Zenodo repository, depending on whether you want to try another set
# of resources or reproduce the paper scores.
# The resources are then linked into the folders that contain the
# processing pipelines for the different gene normalization corpora so that
# the pipelines can be run to produce results.
#
# Special note on the CRF++ software: GNormPlus uses CRF++ for multiple
# tasks. Unfortunately, this is a C++ program and must be built depending
# on the running environment. You will need to download the official GNormPlus
# and follow the CRF++ build instructions. If you use the JULIE Lab resources,
# copy the "CRF" folder from the GNormPlus distribution into the extracted
# JULIE Lab GNormPlus resources directory after the CRF++ program was built.

set -eu

RES_DIR=$1
for pipeline_dir in bionlp-st-ge-gene-recognition/; do
	echo "Linking GNormPlus resources from $RES_DIR into $pipeline_dir"
	cd $pipeline_dir;
	for f in Ab3P BioC.dtd CRF Dictionary Library identify_abbr path_Ab3P tmBioC.key; do
		if [ -L "$f" ]; then
			rm "$f"
		fi;
		ln -s $RES_DIR/$f
	done;
	cd ..
done;
