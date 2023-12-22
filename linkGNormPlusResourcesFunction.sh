#!/bin/bash
# This script provides a function that links GNormPlus
# resources required to run the GePI NLP pipelines that use GNormPlus.
# The GNormPlus base directory must be provided as the first parameter,
# the pipeline directories to link the GNormPlus files into as the
# second parameter.
# This file is not meant to be executed directly but is imported
# by other script files.
# 
# GNormPlus processing pipelines employ the JULIE Lab GNormPlus
# version to find gene mentions. 
# GNormPlus employs a range of resources available in the original
# GNormPlus download from https://www.ncbi.nlm.nih.gov/research/bionlp/Tools/gnormplus/
# or in the JULIE Lab-adapted version that was used to build GePI (see next paragraph).
# Note that you need the official distribution to build the CRF++ library on your
# local machine (more below).
#
# For the reproduction of GePI paper experiments results, the very
# files used for the original NAR Web Server Issue paper can be obtained here:
# https://zenodo.org/record/7327191#.Y3T_y4KZOPw
# Use these files to reproduce the results shown in the paper. The files
# from the official GNormPlus download are not required - except the CRF++ program.
#
# This script takes as argument the 'GNormPlus' folder that
# contains the 'Ab3P' program, the 'Dictionary' directory etc.
# This folder is part of the official download or from the JULIE Lab
# Zenodo repository, depending on whether you want to use the latest
# Gene database or reproduce the paper results.
# The resources are linked into the folders that contain the
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

# This function is meant to be called from another script
function linkGnpResources() {
	local GNP_RES_DIR=$1
	shift # Shift all arguments so $1 is removed and the other parameters can be put into an array
	local PIPELINE_DIRS=("$@")
	local BASE_DIR=`pwd`
	for pipeline_dir in "${PIPELINE_DIRS[@]}"; do
		echo "Linking GNormPlus resources from $GNP_RES_DIR into $pipeline_dir"
		cd $pipeline_dir;
		for f in Ab3P BioC.dtd CRF Dictionary Library identify_abbr path_Ab3P tmBioC.key; do
			if [ -L "$f" ]; then
				rm "$f"
			fi;
			ln -s $GNP_RES_DIR/$f
		done;
		cd $BASE_DIR
	done;
}