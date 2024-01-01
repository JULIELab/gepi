# GePI Preprocessing Pipelines

The NLP pipelines in this directory perform the linguistic processing and, eventually, the extraction of interactions between genes, gene products, gene groups and gene families. Since PubMed Central documents are much larger and more complex than PubMed abstracts, there are a few small differences in the processing pipelines - e.g. batch sizes, max sentence lengths etc. - which is why there are two very similar pipelines located in the respective subdirectories, `pmc` and `pubmed`. The largest difference is the reader component that parses the respective XML formats. Apart from this, the pipelines are very similar and perform the same processing steps.

## UIMA pipeline basics
Both pipelines are [UIMA](https://uima.apache.org/) pipelines. This means that the pipelines are comprised of a series of processing components, e.g. sentence boundary detection, tokenization, abbreviation resolution, gene tagging and interaction extraction. These components are called **annotators** in UIMA. Each annotator has an **XML descriptor** stored in the `desc` and `descAll` subdirectories. The descriptor holds metadata of each annotator such as its name, description, input and output annotation types, parameters and others. Each annotator also consists of Java code that realizes the processing logic (the annotator code is not contained in this repository; see [Preparing the pipelines to run](#preparing-the-pipelines-to-run) for more information). For example, the tokenizer component expects that it is preceded by a sentence splitter in the pipeline. It reads the sentences, applies an ML model to split the sentence into tokens and returns the tokens. The descriptor specifies where the model is found and whether to use actually expect sentences or just tokenize the whole document text.

## Viewing and editing the pipelines
The pipelines have been created with the [JCoRe Pipeline Builder CLI](https://github.com/JULIELab/jcore-pipeline-modules/tree/master/jcore-pipeline-builder-cli) which is recommended to view or edit the pipelines. There is also a pipeline runner component, see [below](#running-the-pipelines).


## Preparing the pipelines to run

## Running the pipelines