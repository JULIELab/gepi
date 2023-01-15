# Changelog

## v0.12.1 (15/01/2023)
- [**bug**] Fix NPE when entering the page via FAQ or Help page [#223](https://github.com/JULIELab/gepi/issues/223)
- [**enhancement**] Create a (fav)icon  or our own [#214](https://github.com/JULIELab/gepi/issues/214)

---

## v0.12.0 (23/12/2022)

#### General Enhancements

- [**enhancement**] Offer a page for delayed results download [#213](https://github.com/JULIELab/gepi/issues/213)
- [**enhancement**] Add a service to delete temporary Excel download files after a given amount of time [#212](https://github.com/JULIELab/gepi/issues/212)

#### Bug Fixes

- [**bug**] Fix download file issue "file not found" in production container [#220](https://github.com/JULIELab/gepi/issues/220)
- [**bug**] Fix truncated download table due to event download restriction for charts [#215](https://github.com/JULIELab/gepi/issues/215)

#### Miscellaneous Changes

- [**closed**] Download functionality [#32](https://github.com/JULIELab/gepi/issues/32)

---

## v0.11.2 (22/12/2022)
- [**enhancement**] Add an input field to allow the user the specification of the number of interactions used for aggregations [#207](https://github.com/JULIELab/gepi/issues/207)

## What's Changed
* Version 0.11.2. Fixes #207,#208. by @khituras in https://github.com/JULIELab/gepi/pull/209


**Full Changelog**: https://github.com/JULIELab/gepi/compare/v0.11.1...v0.11.2
---

## v0.11.1 (22/12/2022)
## What's Changed
* Issue68atid fix by @SchSascha in https://github.com/JULIELab/gepi/pull/82
* Rather a formal pull request, just to keep the documentation style up and running by @SchSascha in https://github.com/JULIELab/gepi/pull/87
* Table Download now results in an xls file (realized with apache poi).… by @fmatthies in https://github.com/JULIELab/gepi/pull/90
* 0.2 sankey d3 by @khituras in https://github.com/JULIELab/gepi/pull/109
* 0.2 by @khituras in https://github.com/JULIELab/gepi/pull/110
* Tab sessions by @khituras in https://github.com/JULIELab/gepi/pull/137
* Result paging by @khituras in https://github.com/JULIELab/gepi/pull/142
* State of BioArxiv first version by @khituras in https://github.com/JULIELab/gepi/pull/148
* Add python to the docker image. Fixes #152. by @khituras in https://github.com/JULIELab/gepi/pull/153
* 0.9.0 beta by @khituras in https://github.com/JULIELab/gepi/pull/158
* Fix #200, the pure section heading search crash. by @khituras in https://github.com/JULIELab/gepi/pull/201
* Interactive help by @khituras in https://github.com/JULIELab/gepi/pull/206


**Full Changelog**: https://github.com/JULIELab/gepi/compare/v0.1.1...v0.11.1
---

## v0.11.0-beta (20/12/2022)

#### Speed Improvements

- [**performance**] Add an index document field for aggregations [#181](https://github.com/JULIELab/gepi/issues/181)

#### General Enhancements

- [**enhancement**] Provide search examples [#196](https://github.com/JULIELab/gepi/issues/196)
- [**enhancement**] Introduce separate waiting-for-data state for paged requests [#193](https://github.com/JULIELab/gepi/issues/193)
- [**enhancement**] Load geneInfo in batch instead of one-by-one [#192](https://github.com/JULIELab/gepi/issues/192)
- [**enhancement**] Add a mechanism for GePiFamplexIdAssigner  to let GNP decide whether a gene should be mapped to Famplex/HGNC groups or not [#191](https://github.com/JULIELab/gepi/issues/191)
- [**enhancement**] Switch FamPlex and HGNC IDs in the index from conceptIds to prefixed original IDs [#190](https://github.com/JULIELab/gepi/issues/190)
- [**enhancement**] Extend preferredName normalization to punctuation and white spaces [#188](https://github.com/JULIELab/gepi/issues/188)
- [**enhancement**] Add taxIds to index documents and filter on those [#185](https://github.com/JULIELab/gepi/issues/185)
- [**enhancement**] Show message when concrete gene IDs and tax IDs are given with contradictions [#184](https://github.com/JULIELab/gepi/issues/184)
- [**enhancement**] Make direct links possible [#182](https://github.com/JULIELab/gepi/issues/182)
- [**enhancement**] Create index documents for unary events [#180](https://github.com/JULIELab/gepi/issues/180)
- [**enhancement**] Filter out duplicates based on abbreviation introduction [#179](https://github.com/JULIELab/gepi/issues/179)
- [**enhancement**] Integrate factuality (level of likelihood that an interaction actually takes place) [#173](https://github.com/JULIELab/gepi/issues/173)
- [**enhancement**] Add hover effects and tooltips for sankey diagrams [#172](https://github.com/JULIELab/gepi/issues/172)
- [**enhancement**] Add 6th Bootstrap grid tier [#171](https://github.com/JULIELab/gepi/issues/171)
- [**enhancement**] Distribute widgets relative to breakpoints and available space [#170](https://github.com/JULIELab/gepi/issues/170)
- [**enhancement**] Display messages in widges when there is no data [#169](https://github.com/JULIELab/gepi/issues/169)
- [**enhancement**] Try to resolve genes by synonym if there is no match by preferredName/symbol [#168](https://github.com/JULIELab/gepi/issues/168)
- [**enhancement**] Add bar plot widget [#166](https://github.com/JULIELab/gepi/issues/166)
- [**enhancement**] Always show 'others' slice in pie charts when there are too many genes to show [#165](https://github.com/JULIELab/gepi/issues/165)
- [**enhancement**] Make sankey diagrams so that their contents are not cropped [#155](https://github.com/JULIELab/gepi/issues/155)
- [**enhancement**] Integrate Gene Name -> NCBI Gene ID mapping [#118](https://github.com/JULIELab/gepi/issues/118)

#### Bug Fixes

- [**bug**] Deactivate B list file upload as long as A is empty [#147](https://github.com/JULIELab/gepi/issues/147)
- [**bug**] Make the event type filter work for 'phosphorylation' [#144](https://github.com/JULIELab/gepi/issues/144)
- [**bug**] Fix issue where b-list cannot be read from file repeatedly [#134](https://github.com/JULIELab/gepi/issues/134)

#### Evaluation and Reproducibility

- [**evaluation**] Add a UIMA BioNLP ST gene merger component [#178](https://github.com/JULIELab/gepi/issues/178)
- [**evaluation**] Add code to evaluate BioSem given GNormPlus genes [#177](https://github.com/JULIELab/gepi/issues/177)
- [**evaluation**] Add code to reproduce GNormPlus evalution results in the GePI paper [#176](https://github.com/JULIELab/gepi/issues/176)

#### Miscellaneous Changes

- [**closed**] Common partners Sankey: hide non-common edges [#167](https://github.com/JULIELab/gepi/issues/167)
- [**closed**] Check if reflexive relations are returned [#131](https://github.com/JULIELab/gepi/issues/131)
- [**closed**] Integrate Ensemble ID -> NCBI Gene ID mapping [#119](https://github.com/JULIELab/gepi/issues/119)
- [**closed**] Remove the organism restriction of GeNo [#112](https://github.com/JULIELab/gepi/issues/112)
- [**closed**] Make diagrams always use up the available space [#101](https://github.com/JULIELab/gepi/issues/101)

---

## v0.10.1-beta (21/10/2022)
- [**bug**] Fix null context with paragraph filter [#164](https://github.com/JULIELab/gepi/issues/164)

---

## v0.10.0-beta (21/10/2022)

#### General Enhancements

- [**enhancement**] Add tabs to the pie charts widgets to switch between a- and b-counts [#162](https://github.com/JULIELab/gepi/issues/162)
- [**enhancement**] Scroll to top upon search submission [#161](https://github.com/JULIELab/gepi/issues/161)
- [**enhancement**] Implement AND/OR for sentence and paragraph filters [#160](https://github.com/JULIELab/gepi/issues/160)
- [**enhancement**] Fix Pie Chart coloring [#156](https://github.com/JULIELab/gepi/issues/156)

#### Bug Fixes

- [**bug**] Fix fulltext match column [#163](https://github.com/JULIELab/gepi/issues/163)
- [**bug**] Fix paged result download [#159](https://github.com/JULIELab/gepi/issues/159)

---

## v0.9.0-beta (18/07/2022)

#### Speed Improvements

- [**performance**] Implement paged result retrieval for closed search [#151](https://github.com/JULIELab/gepi/issues/151)
- [**performance**] Avoid complete result download after each search [#150](https://github.com/JULIELab/gepi/issues/150)

#### General Enhancements

- [**enhancement**] Avoid freezing on result download [#157](https://github.com/JULIELab/gepi/issues/157)
- [**enhancement**] Re-enable the common partners Sankey [#149](https://github.com/JULIELab/gepi/issues/149)
- [**enhancement**] Make the table Ajax-Enabled [#143](https://github.com/JULIELab/gepi/issues/143)
- [**enhancement**] Implement paged result retrieval from ES [#138](https://github.com/JULIELab/gepi/issues/138)

#### Bug Fixes

- [**bug**] Fix bug where events are duplicated [#133](https://github.com/JULIELab/gepi/issues/133)

#### Miscellaneous Changes

- [**closed**] Add highlighting to table [#129](https://github.com/JULIELab/gepi/issues/129)
- [**closed**] Allow sentence and paragraph filters to contain compounds/phrases [#125](https://github.com/JULIELab/gepi/issues/125)
- [**closed**] Add paragraph filter options to GepiInput [#124](https://github.com/JULIELab/gepi/issues/124)
- [**closed**] Make paragraph text information available for all events [#123](https://github.com/JULIELab/gepi/issues/123)

---

## v0.8.0-beta2 (13/07/2022)
- [**bug**] Fix download table creation [#152](https://github.com/JULIELab/gepi/issues/152)

---

## v0.8.0-beta (bioRxiv upload) (11/07/2022)
# GePI v0.8.0-beta

This is the state of GePI right after the publication of the first bioRxiv version.

#### bug

- [**bug**] references are missing (got deleted to aid sentence splitter) [#92](https://github.com/JULIELab/gepi/issues/92)
- [**bug**] atid shown in results instead of gene symbol [#68](https://github.com/JULIELab/gepi/issues/68)
- [**bug**] events.argumentsearch does not recognise atids.  [#60](https://github.com/JULIELab/gepi/issues/60)
- [**bug**] multiple line entries  [#15](https://github.com/JULIELab/gepi/issues/15)

#### closed

- [**closed**] Add pipeline to create test data. [#139](https://github.com/JULIELab/gepi/issues/139)
- [**closed**] Create specific argument1 and argument2 fields. [#132](https://github.com/JULIELab/gepi/issues/132)
- [**closed**] Allow all events to be retrieved filtered on sentence/paragraph [#126](https://github.com/JULIELab/gepi/issues/126)
- [**closed**] Update to Java 11 [#121](https://github.com/JULIELab/gepi/issues/121)
- [**closed**] Erik Changelog 0.1.18-43 [#99](https://github.com/JULIELab/gepi/issues/99)
- [**closed**] Erik Changelog 0.1.18-42 [#98](https://github.com/JULIELab/gepi/issues/98)
- [**closed**] Erik Changelog 0.1.18-41 [#96](https://github.com/JULIELab/gepi/issues/96)
- [**closed**] Integrate search for relation type [#94](https://github.com/JULIELab/gepi/issues/94)
- [**closed**] General changelog [#86](https://github.com/JULIELab/gepi/issues/86)
- [**closed**] Come up with a solution for the problem of gene db data export [#83](https://github.com/JULIELab/gepi/issues/83)
- [**closed**] Bernd changelog 0.1.17-49 [#80](https://github.com/JULIELab/gepi/issues/80)
- [**closed**] Sascha changelog 0.1.17-49  [#78](https://github.com/JULIELab/gepi/issues/78)
- [**closed**] explicitely evaluate Sankey plot with common partners [#77](https://github.com/JULIELab/gepi/issues/77)
- [**closed**] remove bar plot/widget [#75](https://github.com/JULIELab/gepi/issues/75)
- [**closed**] extract neo4jDB concept project [#73](https://github.com/JULIELab/gepi/issues/73)
- [**closed**] Add main event type to result table  [#71](https://github.com/JULIELab/gepi/issues/71)
- [**closed**] Add resource versioning [#70](https://github.com/JULIELab/gepi/issues/70)
- [**closed**] Allow to download table also in format xls [#64](https://github.com/JULIELab/gepi/issues/64)
- [**closed**] order of mentioned gene events [#62](https://github.com/JULIELab/gepi/issues/62)
- [**closed**] clean up gepi neo4j build up scripts [#57](https://github.com/JULIELab/gepi/issues/57)
- [**closed**] Download feature for table [#54](https://github.com/JULIELab/gepi/issues/54)
- [**closed**] provide filter possibilities to narrow down search / for search exploration [#49](https://github.com/JULIELab/gepi/issues/49)
- [**closed**] Provide article id in resulting table per sentence [#41](https://github.com/JULIELab/gepi/issues/41)
- [**closed**] Search box entries A|B should automatically be extended to all homologous genes   [#39](https://github.com/JULIELab/gepi/issues/39)
- [**closed**] proper Inject usage for googleChartManager [#38](https://github.com/JULIELab/gepi/issues/38)
- [**closed**] correct table resizing [#31](https://github.com/JULIELab/gepi/issues/31)
- [**closed**] Input list recoverable from left side [#30](https://github.com/JULIELab/gepi/issues/30)
- [**closed**] pie chart resizing [#29](https://github.com/JULIELab/gepi/issues/29)
- [**closed**] Full index gepi available on coling servers [#28](https://github.com/JULIELab/gepi/issues/28)
- [**closed**] complete medline & pmc index [#27](https://github.com/JULIELab/gepi/issues/27)
- [**closed**] working demo on Sascha's laptop [#26](https://github.com/JULIELab/gepi/issues/26)
- [**closed**] workable A - B Search [#25](https://github.com/JULIELab/gepi/issues/25)
- [**closed**] input field - responsiveness [#24](https://github.com/JULIELab/gepi/issues/24)
- [**closed**] bar diagram - numbers [#23](https://github.com/JULIELab/gepi/issues/23)
- [**closed**] Table pager [#22](https://github.com/JULIELab/gepi/issues/22)
- [**closed**] elasticsearch index: pmid/pmcid fields wrong? [#21](https://github.com/JULIELab/gepi/issues/21)
- [**closed**] event class - atid order in meaningful order? [#20](https://github.com/JULIELab/gepi/issues/20)
- [**closed**] Switch to an event-centric index structure [#19](https://github.com/JULIELab/gepi/issues/19)
- [**closed**] Add index field for number of arguments [#17](https://github.com/JULIELab/gepi/issues/17)
- [**closed**] es_query - no pmc hits  [#16](https://github.com/JULIELab/gepi/issues/16)
- [**closed**] atid's [#14](https://github.com/JULIELab/gepi/issues/14)
- [**closed**] Result Portal [#13](https://github.com/JULIELab/gepi/issues/13)
- [**closed**] PMC XMI Parsing issue: Maximum attribute size limit exceeded [#11](https://github.com/JULIELab/gepi/issues/11)
- [**closed**] Problematic Medline Document: 23700993 [#10](https://github.com/JULIELab/gepi/issues/10)
- [**closed**] Point to missing and useful documentation [#8](https://github.com/JULIELab/gepi/issues/8)
- [**closed**] Build Gene Database [#6](https://github.com/JULIELab/gepi/issues/6)
- [**closed**] jpp: jules-longdocument-skipper (jls) [#5](https://github.com/JULIELab/gepi/issues/5)
- [**closed**] Who does what? [#4](https://github.com/JULIELab/gepi/issues/4)
- [**closed**] Current status of modules relevant for GePi   [#3](https://github.com/JULIELab/gepi/issues/3)
- [**closed**] Write FieldsGenerator [#2](https://github.com/JULIELab/gepi/issues/2)
- [**closed**] result pane small [#1](https://github.com/JULIELab/gepi/issues/1)

#### enhancement

- [**enhancement**] Support multiple sources for relations and genes [#146](https://github.com/JULIELab/gepi/issues/146)
- [**enhancement**] Support event indexing for multiple IDs per gene [#145](https://github.com/JULIELab/gepi/issues/145)
- [**enhancement**] Add a PieChart again [#141](https://github.com/JULIELab/gepi/issues/141)
- [**enhancement**] Enable section heading filter [#140](https://github.com/JULIELab/gepi/issues/140)
- [**enhancement**] Add filter terms to the Excel frontpage [#135](https://github.com/JULIELab/gepi/issues/135)

#### urgent

- [**urgent**] Update data download to the format used in latest data deliveries [#115](https://github.com/JULIELab/gepi/issues/115)
