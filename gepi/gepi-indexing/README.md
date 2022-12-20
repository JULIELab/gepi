# GePI Indexing

## ElasticSearch Setup

The GePI mapping for ElasticSearch is located at `gepi-indexing-base/src/main/resources/elasticSearchMapping.json`. The number of shards was chosen following https://www.elastic.co/de/blog/how-many-shards-should-i-have-in-my-elasticsearch-cluster.

## Indexing Pipelines

This directory contains code and UIMA pipelines for indexing from a preprocessed [JeDIS](https://julielab.de/jedis/) database. The database must contain annotations as specified in the following table:

annotation level | UIMA type name | JCoRe component used for GePI
----------------|-----------------|---------------------------------
sentences   | `de.julielab.jcore.types.Sentence` | [JSBD](https://github.com/JULIELab/jcore-projects/tree/v2.6/jcore-jsbd-ae-biomedical-english)
tokens  |`de.julielab.jcore.types.Token` | [JTBD](https://github.com/JULIELab/jcore-projects/tree/v2.6/jcore-jtbd-ae-biomedical-english)
POS Tags | `de.julielab.jcore.types.PennBioIEPOSTag` (for lemmatization) | [OpenNLP POS Tagger](https://github.com/JULIELab/jcore-projects/tree/master/jcore-opennlp-postag-ae-biomedical-english)
lemmas   |`de.julielab.jcore.types.Lemma` (set as token feature)  | [BioLemmatizer](https://github.com/JULIELab/jcore-base/tree/master/jcore-biolemmatizer-ae)
event mentions | `de.julielab.jcore.types.EventMention` | [BioSem](https://github.com/JULIELab/jcore-projects/tree/master/jcore-biosem-ae-bionlp-st11)
event triggers | `de.julielab.jcore.types.EventTrigger` | [BioSem](https://github.com/JULIELab/jcore-projects/tree/master/jcore-biosem-ae-bionlp-st11)
event arguments | `de.julielab.jcore.types.Argument` (set as features for `EventMention`) | [BioSem](https://github.com/JULIELab/jcore-projects/tree/master/jcore-biosem-ae-bionlp-st11)
genes/gene products   | `de.julielab.jcore.types.Gene` (set as features of `Argument`) | [GNormPlus](https://github.com/JULIELab/jcore-projects/tree/master/jcore-gnormplus-pubmed-db-reader)

POS Tags are not needed for the actual indexing but only for lemma creation. Lemmas are used in the indexing pipeline to find hedging expressions (`may`, `suggest that`...)

The pipelines in this directory then read those annotations. Through the `de.julielab.gepi.indexing.RelationDocumentGenerator` and `de.julielab.gepi.indexing.RelationFieldValueGenerator`, UIMA CAS objects are converted into the ElasticSearch JSON format and sent to the ElasticSearch server(s).


