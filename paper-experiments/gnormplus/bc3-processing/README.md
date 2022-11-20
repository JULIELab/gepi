# JULIE Lab GNormPlus Evaluation on BioCreative III GN Test Dataset
This directory contains a UIMA pipeline built with the JCoRe Pipeline Builder, see https://github.com/JULIELab/jcore-pipeline-modules.
It is readily configured to run the JULIE Lab GNormPlus version on the BC3 gold 50 test set. It reads the data from the central `corpus`
directory, see the parent directory's README.md file for details. The output is written to `data/output-bioc/`. The pipeline is
meant to be used with the `runEvaluation.sh` script in the parent directory.

## Notes for BC3
The BioCreative III Gene Normalization evaluation data contains a large number of IDs that have been deprecated by re-sequencing of the
corresponding genomes. This leads to severly diminished evaluation scores on up-to-date gene normalization algorithms that do not
contain those IDs. In GNormPlus, the solution is to activate the `GeneIdMatch` option in the setup configuration file. This is done
for this pipeline in the `config/setup_do_id_match.txt` file. The GNormPlus component of the pipeline is configured to use that
file.
 
