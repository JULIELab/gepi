package de.julielab.gepi.core.services;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
class ConceptName {
    // preferredNames, for genes orthology aggregates, this is the symbol
    private String name;
    /* links to other concepts that
    * 1. have a symbol that is used in the aggregationvalue field: FPLX, HGNCG and orthology clusters (genegroup)
    * 2. are connected to this concept via this-IS_BROADER_THAN*->(gene)<-[HAS-ELEMENT]-genegroup
    * The genes themselves are omitted/jumped over if they take part in a genegroup because then, they own symbol is not used
    * in the aggregationvalue field. In indexing, the top homology names are used for the aggregateionvalue.
    * AMPK(FPLX) -> AMPKA(FPLX) -> [prkaa1 (gene)] -> PRKAA1 (AGGREGATE_GENEGROUP)
    * protein kinase activity (GO) -> [prkaa1 (gene)] -> PRKAA1 (AGGREGATE_GENEGROUP)
    */
    private List<String> nextToOrthologyClusters;
}
