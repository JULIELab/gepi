# GNormPlus experiments carried out for GePI
For GePI, the JULIE Lab derived a GNormPlus version from the official September 2022 release. The JULIE Lab version outputs family names and can be used in UIMA components with multi-threading capabilities.

The experiments in this directory use the JULIE Lab version of GNormPlus within UIMA pipelines. Such pipelines are also used for GePI data processing and indexing. Thus, the conditions of the experiments are as close as to production code as possible.

There is experimental code for data sets from BioCreative II, BioCreative III and NLM-Gene. The data sets themselves are not included in this repository. They must be obtained from the official places:
* BioCreative data sets: https://biocreative.bioinformatics.udel.edu/
* For NLM Gene, we use a non-published version in PubTator format. The published version in BioC format is missing some annotations as of November 2022. The PubTator version used for GePI experiments was obtained via e-mail from the developers of GNormPlus.
The data set archives need to be extracted in the `corpus/` sub directory. The evaluation scripts expect this location and the original archive directory structure of the data sets.

To run the evaluation on one of the data sets, use the `runEvaluation.sh` script. Running the evaluations has the following requirements:
* a Unix/Linux operating system
* common Linux command line tools: wget, cut, awk and possibily others
* a bash-compatible shell
* Java >= 11

When these requirements are met the following command runs an evaluation:
```
runEvaluation.sh <one of bc2, bc3 or nlm-gene>
```
