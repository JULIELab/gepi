#!/bin/bash
# This script runs the complete process of evaluation of the JULIE Lab
# version of the GNormPlus program on the specified corpus.

if [[ -z "$1" ]]; then
        echo "Usage: runEvaluation <bc2/bc3/nlm-gene>";
        exit 1;
fi;


function makePubtator2GenelistConversionLinks {
        # Expects the current working directory to be
        # <corpus-processing-pipeline-dir>/evaluation
        if [[ ! -L pubtator2normalizedGenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/pubtator2normalizedGenelist.sh
        fi
        if [[ ! -L pubtator2jlgenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/pubtator2jlgenelist.sh
        fi
        if [[ ! -L normalizeGNPGenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/normalizeGNPGenelist.sh
        fi
}

function getGoldGenelist {
        # Expects the current working directory to be
        # <corpus-processing-pipeline-dir>/evaluation
        DATASET=$1
        if [[ ! -f "gold.genelist" ]]; then
                if [[ $DATASET == "bc2" ]]; then
                        ln -s ../../corpora/bc2GNtest/bc2GNtest.genelist gold.genelist
                elif [[ $DATASET == "bc3" ]]; then
                        awk -v FS="\t" -v OFS="\t" '{print "PMC"$1,$2,$3}' ../../corpora/GNTestEval/test50.gold.txt > gold.genelist
                elif [[ $DATASET == "nlm-gene" ]]; then
                        makePubtator2GenelistConversionLinks;
                        # There are a few cases where no ID is given, delete those with the sed command.
                        bash pubtator2normalizedGenelist.sh ../../corpora/NLMGene_Test.txt | sed '/		/d' > gold.genelist
                else 
                        echo "Unhandled dataset $DATASET";
                        exit 3;
                fi
        fi
}

function makeBioC2GenelistConversionLinks {
        # Expects the current working directory to be
        # <corpus-processing-pipeline-dir>/evaluation
        if [[ ! -L bioc2normalizedGenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/pubtator2normalizedGenelist.sh
        fi
        if [[ ! -L bioc2jlgenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/pubtator2jlgenelist.sh
        fi
        if [[ ! -L normalizeGNPGenelist.sh ]]; then
                ln -s ../../evaldata-conversion-scripts/normalizeGNPGenelist.sh
        fi
}

function evaluate {
        DATASET=$1;
        cd $DATASET-processing;
        # For BC3 we need to create the subset of 50 gold-annotated papers from the gold genelist. The data directory
        # contains all 507 documents used for evaluation, including silver-standard documents.
        if [[ "$DATASET" == "bc3" ]] && [[ ! -f resources/testgold50.pmid ]]; then
                mkdir -p resources
                cut -f1 ../corpora/GNTestEval/test50.gold.txt | sort -u > resources/testgold50.pmcid
        fi
        # Clear old data because it is not overwritten
        # by the BioC writer in the pipeline but
        # new directories and files will be created otherwise.
        if [[ ! -f "data/output-bioc/bioc_collections_0/bioc_collection_0_0.xml" ]]; then
                java -jar ../jcore-pipeline-runner-base* run.xml
        else
                echo "File data/output-bioc/bioc_collections_0/bioc_collection_0_0.xml does already exist, GNormPlus pipeline is not run again."
        fi
        
        mkdir -p evaluation
        cd evaluation

        if [[ -f pred.genelist ]]; then
        	rm pred.genelist;
        fi
        # The pipeline outputs multiple BioC XML files when multithreading is used. Collect the genelist from all of them.
        find ../data/output-bioc -name 'bioc_collection_*.xml' | xargs -I {} bash ../../evaldata-conversion-scripts/bioc2normalizedGenelist.sh {} >> pred.genelist
        getGoldGenelist $DATASET
	echo "$(pwd): java -jar ../../julielab-entity-evaluator* -g gold.genelist -p pred.genelist"
        java -jar ../../julielab-entity-evaluator* -g gold.genelist -p pred.genelist
}

DATASET=$1
case $DATASET in bc2*)
	DATASET=bc2;
esac
case $DATASET in bc3*)
	DATASET=bc3;
esac
case $DATASET in nlm-gene*)
	DATASET=nlm-gene;
esac

if [[ "$DATASET" != "bc2" ]] && [[ "$DATASET" != "bc3" ]] && [[ "$DATASET" != "nlm-gene" ]]; then
        echo "Unknown data set: $DATASET. Valid data set identifiers include 'bc2', 'bc3' and 'nlm-gene'.";
        exit 2;
fi

RUNNER_VERSION=0.5.1
EVAL_VERSION=1.3.0
XML_TOOLS_VERSION=0.6.5
if [[ ! -f "jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar" ]] || [[ ! -f "jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar" ]]; then
        echo "Downloading JCoRe Pipeline Runner";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-base/$RUNNER_VERSION/jcore-pipeline-runner-base-$RUNNER_VERSION-cli-assembly.jar";
        wget "https://repo1.maven.org/maven2/de/julielab/jcore-pipeline-runner-cpe/$RUNNER_VERSION/jcore-pipeline-runner-cpe-$RUNNER_VERSION-jar-with-dependencies.jar";
fi

if [[ ! -f "julielab-entity-evaluator-$EVAL_VERSION-executable.jar" ]]; then
        echo "Downloading JULIE Lab Entity Evaluator."
        wget https://repo1.maven.org/maven2/de/julielab/julielab-entity-evaluator/$EVAL_VERSION/julielab-entity-evaluator-$EVAL_VERSION-executable.jar
fi
if [[ ! -f "julie-xml-tools-$XML_TOOLS_VERSION-xml-tools-assembly.jar" ]]; then
        echo "Downloading JULIE Lab XML tools."
        wget https://repo1.maven.org/maven2/de/julielab/julie-xml-tools/$XML_TOOLS_VERSION/julie-xml-tools-$XML_TOOLS_VERSION-xml-tools-assembly.jar
fi

echo "Running JULIE Lab GNormPlus evaluation on datasets $DATASET";
evaluate $DATASET;
