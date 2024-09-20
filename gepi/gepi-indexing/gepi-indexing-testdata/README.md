# GePI Testdata Creation Pipeline

This pipeline creates molecular interaction examples for the creation of the Maven artifact <code>de.julielab:gepi-test-data:&lt;version></code>.
The project for the artifact itself is located under <code>gepi/gepi-test-data</code> in this repository.

This is a JCoRe pipeline that currently expects an existing Postgres database that contains 100 specific PubMed documents pre-processed with the PubMed preprocessing pipeline found at <code>gepi/gepi-preprocessing</code>. The list of documents that should be pre-processed is found in <code>gepi/gepi-test-data/src/main/resources/test-index-input/test_pmid.txt</code>
There resides the current test data. But if the index format is changed and fields should be added or adapted, the interaction data must be re-created with this pipeline. 

A run of this pipeline with an updated index schema follows these steps:

1) Clear GePI-Artifacts (because they contain the indexing code) from the <code>lib/</code> directory.
2) Clear the <code>data/output-json</code> directory of this project, if the directory exists.
3) Add the updated GePI-Artifacts with Maven:

       cd ..
       mvn clean pacakge -pl gepi-indexing-testdata --also-make
4) Run the pipeline with the JCoRe pipeline runner.
5) Now the new interaction data should be available in <code>data/output-json</code>. From there it can be copied into the <code>gepi-test-data</code> project to update the test data.



