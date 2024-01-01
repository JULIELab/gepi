# GePI Preprocessing Pipelines

The NLP pipelines in this directory perform the linguistic processing and, eventually, the extraction of interactions between genes, gene products, gene groups and gene families. Since PubMed Central documents are much larger and more complex than PubMed abstracts, there are a few small differences in the processing pipelines - e.g. batch sizes, max sentence lengths etc. - which is why there are two very similar pipelines located in the respective subdirectories, `pmc` and `pubmed`. The largest difference is the reader component that parses the respective XML formats. Apart from this, the pipelines are very similar and perform the same processing steps.

## UIMA pipeline basics
These information are not required to run the pipelines. However, they help to understand the contents of the pipeline directories and the NLP processing design.

Both pipelines are [UIMA](https://uima.apache.org/) pipelines. This means that the pipelines are comprised of a series of processing components, e.g. sentence boundary detection, tokenization, abbreviation resolution, gene tagging and interaction extraction. These components are called **annotators** in UIMA. Each annotator has an **XML descriptor** stored in the `desc` and `descAll` subdirectories. The descriptor holds metadata of each annotator such as its name, description, input and output annotation types, parameters and others. Each annotator also consists of Java code that realizes the processing logic (the annotator code is not contained in this repository; see [Preparing the pipelines to run](#preparing-the-pipelines-to-run) for more information). For example, the tokenizer component expects that it is preceded by a sentence splitter in the pipeline. It reads the sentences, applies an ML model to split the sentence into tokens and returns the tokens. The descriptor specifies where the model is found and whether to use actually expect sentences or just tokenize the whole document text.

UIMA uses a sophisticated type system formalism to specify annotation types like sentences and tokens. In their most simple form, annotation types are just named types - e.g. `Sentence` - whose instances span characters in the document text. However, types can have attributes called `features` that express properties of type instances. Types can also extend other types which allows for taxonomic type systems. For example, the JulieLab types `de.julielab.jcore.types.Gene` extends `de.julielab.jcore.types.BioEntityMention` and inherits a feature named `species` which may refer to the species name that `BioEntityMention` refers to.

The document text and its annotations a held in a Common Analysis System (CAS) object. That is basically a container for the text, its annotation, annotation indices for efficient access to annotations are more functionality. Each annotator receives the CAS of a documents, reads required input and writes its output into the CAS. Thus, the CAS is the UIMA object that is passed through the pipeline components and always holds the current processing state until it can be consumed at the end of the pipeline into some external output.

## JCoRe basics
The annotator components used for the pipelines come from [JCoRe](https://github.com/JULIELab/jcore-base), the JULIE Lab Component Repository. All components in this repository are UIMA components tailored for our own type system. 

## Viewing and editing the pipelines
The pipelines have been created with the [JCoRe Pipeline Builder CLI](https://github.com/JULIELab/jcore-pipeline-modules/tree/master/jcore-pipeline-builder-cli) which is recommended to view or edit the pipelines. There is also a pipeline runner component, see [below](#running-the-pipelines).
For installation and usage instructions please refer to [the README for the JCoRe Pipeline Modules](https://github.com/JULIELab/jcore-pipeline-modules/tree/master).

## Preparing the pipelines to run

## Running the pipelines