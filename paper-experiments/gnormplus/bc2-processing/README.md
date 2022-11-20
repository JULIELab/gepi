# JULIE Lab GNormPlus Evaluation on BioCreative II GN Test Dataset
This directory contains a UIMA pipeline built with the JCoRe Pipeline Builder, see https://github.com/JULIELab/jcore-pipeline-modules.
It is readily configured to run the JULIE Lab GNormPlus version on the BC2 test set. It reads the data from the central `corpus`
directory, see the parent directory's README.md file for details. The output is written to `data/output-bioc/`. The pipeline is
meant to be used with the `runEvaluation.sh` script in the parent directory.

## Notes for BC2
The BioCreative II Gene Normalization data sets only contain human genes. Thus, the GNormPlus component used in this pipeline
is configured to only assign the NCBI Taxonomy ID 9606, homo sapiens, for all gene mentions.

