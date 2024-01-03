# GePI Preprocessing Pipelines

The NLP pipelines in this directory perform the linguistic processing and, eventually, the extraction of interactions between genes, gene products, gene groups and gene families. Since PubMed Central documents are much larger and more complex than PubMed abstracts, there are a few small differences in the processing pipelines - e.g. batch sizes, max sentence lengths etc. - which is why there are two very similar pipelines located in the respective subdirectories, `pmc` and `pubmed`. The largest difference is the reader component that parses the respective XML formats. Apart from this, the pipelines are very similar and perform the same processing steps.

## UIMA pipeline basics
These information are not required to run the pipelines. However, they help to understand the contents of the pipeline directories and the NLP processing design.

Both pipelines are [UIMA](https://uima.apache.org/) pipelines. This means that the pipelines are comprised of a series of processing components, e.g. sentence boundary detection, tokenization, abbreviation resolution, gene tagging and interaction extraction. These components are called **annotators** in UIMA. Each annotator has an **XML descriptor** stored in the `desc` and `descAll` subdirectories. The descriptor holds metadata of each annotator such as its name, description, input and output annotation types, parameters and others. Each annotator also consists of Java code that realizes the processing logic (the annotator code is not contained in this repository; see [Preparing the pipelines to run](#preparing-the-pipelines-to-run) for more information). For example, the tokenizer component expects that it is preceded by a sentence splitter in the pipeline. It reads the sentences, applies an ML model to split the sentence into tokens and returns the tokens. The descriptor specifies where the model is found and whether to use actually expect sentences or just tokenize the whole document text.

UIMA uses a sophisticated type system formalism to specify annotation types like sentences and tokens. In their most simple form, annotation types are just named types - e.g. `Sentence` - whose instances span characters in the document text. However, types can have attributes called `features` that express properties of type instances. Types can also extend other types which allows for taxonomic type systems. For example, the JulieLab types `de.julielab.jcore.types.Gene` extends `de.julielab.jcore.types.BioEntityMention` and inherits a feature named `species` which may refer to the species name that `BioEntityMention` refers to.

The document text and its annotations a held in a Common Analysis System (CAS) object. That is basically a container for the text, its annotation, annotation indices for efficient access to annotations are more functionality. Each annotator receives the CAS of a documents, reads required input and writes its output into the CAS. Thus, the CAS is the UIMA object that is passed through the pipeline components and always holds the current processing state until it can be consumed at the end of the pipeline into some external output.

## JCoRe basics
The annotator components used for the pipelines come from [JCoRe](https://github.com/JULIELab/jcore-base), the JULIE Lab Component Repository. All components in this repository are UIMA components tailored for our own type system. To view, edit and use the GePI pipelines, no deeper knowledge of JCoRe is required. By using the JCoRe Pipeline Builder ([see below](#viewing-and-editing-the-pipelines)), direct access to the JCoRe components is established.

## Viewing and editing the pipelines
The pipelines have been created with the [JCoRe Pipeline Builder CLI](https://github.com/JULIELab/jcore-pipeline-modules/tree/master/jcore-pipeline-builder-cli) which is recommended to view or edit the pipelines. There is also a pipeline runner component, see [below](#running-the-pipelines).
For installation and usage instructions please refer to [the README for the JCoRe Pipeline Modules](https://github.com/JULIELab/jcore-pipeline-modules/tree/master).

## Preparing the pipelines to run
To run the pipelines, two steps are necessary:
1. Install GNormPlus in a separate location and create symbolic links to required resources in the preprocessing pipeline directories. **This is done using the `linkGNormPlusResources.sh` script in this directory.** More details and information are found in the `../../linkGNormPlusResourcesFunction.sh` script that is used by `linkGNormPlusResources.sh`.
2. Download the JCoRe annotator code. Use the JCoRe Pipeline Builder CLI for this task. Either load the pipeline with the CLI tool, save the pipeline and choose to store the dependencies or run the CLI tool with the `-s` switch to directly download the dependencies. Upon successful download, there will be a new `lib/` subdirectory containing the JAR files.
3. Create a `pubmed` and a `pmc` database in your PostgreSQL DBMS. You can omit a database for a corpus you don't want to work with (e.g. if you won't use PubMed Central, don't create the `pmc` database).
4. Copy the `config/costosys_template.xml` files to `config/costosys.xml`. Edit the `config/costosys.xml` to point to your PostgreSQL server and the respective databases. Make sure that each pipeline works with its own database. The preprocessing pipelines expect their specific PubMed or PMC XML format and won't work with other data.

## Running the pipelines
The pipelines need MEDLINE/PubMed and/or PubMed Central XML data imported in the PostgreSQL database with the [CoStoSys](https://github.com/JULIELab/costosys) tool. The pipelines will read from the database, parse the XML into pure text, add annotations to the text and store the result back into the database.

It is therefore necessary to obtain PubMed and/or PubMed Central XML files from the NIH. Instructions for PubMed can be found [here](https://pubmed.ncbi.nlm.nih.gov/download/) and instructions for PubMed Central can be found [here](https://www.ncbi.nlm.nih.gov/pmc/tools/ftp/). CoStoSys and the JCoRe pipeline readers expect the bulk XML formats. Baseline and update files are supported. The PubMed update files may mark documents for deletion which is also performed by CoStoSys PubMed updates when the `<documentdeletions>` element in the `pubmed/pubmedImport.xml` points to tables. This is the default, so deletions will be performed. 

After downloading at least one bulk XML file, its contents can be imported into the database. Note that all document XML will be imported into the database and that the output of the pipeline runs will also be stored in the database. The whole of PubMed will take around 100GB of disk space. **PubMed Central will take up to 4TB** due to the high number of sentences, tokens and other basic annotations. In principle, these space requirements can be alleviated by preprocessing a batch of XML files, indexing the results into the GePI ElasticSearch instance, clearing the database and then processing the next batch. However, this approach is currently not employed in our own internal processes. Thus, no supporting code or scripts are currently available.

Importing and preprocessing of the data happens in the following steps:

1. Build the CoStoSys tool according to the instructions in its README to obtain the `costosys-<version>-cli-assembly.jar` file.
2. Rename the file to `costosys.jar` (just for simplicity) and place it next to this README. It could be placed anywhere but will be expected in this directory for the sake of these instructions.
3. Import the PubMed / PubMed Central data downloaded previously (mind the `-im` switch for MEDLINE/PubMed and `-ip` for PMC):
   1. PubMed: `java -jar costosys.jar -im pubmed/pubmedImport.xml -dbc pubmed/preprocessing/config/costosys.xml`
   2. PMC: `java -jar costosys.jar -ip pmc/pubmedImport.xml -dbc pmc/preprocessing/config/costosys.xml`
4. Create **subset tables** for processing  ([see below](#details-and-tips-for-running-the-pipelines) for an explanation):
   1. PubMed: `java -jar costosys.xml -s _data._data_mirror -m -dbc pubmed/preprocessing/config/costosys.xml`
   2. PMC: `java -jar costosys.xml -s _data._data_mirror -m -dbc pmc/preprocessing/config/costosys.xml`
5. Use the JCoRe Pipeline CPE Runner to run the pipelines. You should already have the runner after following the instructions to [install the JCoRe Pipeline Modules](https://github.com/JULIELab/jcore-pipeline-modules/tree/master):
   1. Enter the `preprocessing` directory on a console you would like to run.
   2. Start the pipeline using `runpipeline run.xml` (`runpipeline` is the alias created during in the installation of the JCoRe Pipeline Modules).
6. Successfully running a pipeline will create a table `_data_xmi.documents` in the respective PostgreSQL database. This table contains the document itself in the UIMA serialization format XMI as well as the annotations. The indexing pipeline reads from this table.

## Details and tips for running the pipelines
- The pipelines scale vertically by specifying the amount of threads in the `run.xml` file. The pipelines are thread safe and have been run with 10 to 24 threads.
- The pipelines scale horizontally by running the very same pipeline on multiple machines that all have access to the PostgreSQL DBMS. This is where the subset tables from above come into play:
  - The data import creates the table `_data._data`, i.e. the `_data` table in the schema `_data`. The XML contents are stored in this table one document per row.
  - While it is possible to read from this so-called *data table* directly, the usage of a subset table is recommended due to some quality of life features that come along with it. **For horizontal scaleout a subset table is required**.
  - A subset table is a CoStoSys concept and serves two main purposes:
    1. Represent a subset of a data table, i.e. a corpus, by referencing document IDs in the data table via foreign keys; the XML data is not duplicated.
    2. Subset tables have a specific column schema that contains information about the state of processing for each document, i.e. the boolean columns `in_process` and `is_processed`. This allows to query the state of processing with CoStoSys, e.g. `java -jar costosys.jar -st _data._data_mirror` where `-st` stands for *status*. Call `java -jar costosys.jar` without parameters for the full list of functions. See the [CoStoSys](https://github.com/JULIELab/costosys) for details on the concepts.
    3. Through the processing status, subset tables **synchronize multiple pipelines** accessing the same CoStoSys database. Each pipeline only reads documents that have not yet been marked as being `in_process`.