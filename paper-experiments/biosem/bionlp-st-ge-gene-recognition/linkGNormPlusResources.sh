#!/bin/bash
# The pipeline in this directory employ GNormPlus to find gene
# mentions. Those gene mentions are then merged with the
# BioNLP-ST gold data genes in order to obtain more realistic
# evaluation scores on event extraction when the genes have to be
# recognized first.
# GNormPlus requires a range of resources available in the original
# GNormPlus Download from 