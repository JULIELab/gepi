package de.julielab.jcore.ae.bionlpgenesmerger;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Protein;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for jcore-bionlpgold-pred-gene-merge-ae.
 */
public class BioNLPGeneMergerTest {
    private final static Logger log = LoggerFactory.getLogger(BioNLPGeneMergerTest.class);

    @Test
    void process() throws Exception {
        final AnalysisEngine ae = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.bionlpgenesmerger.desc.jcore-bionlpgold-pred-gene-merge-ae");
        final JCas jCas = ae.newJCas();

        Gene tpCorrectOffsets = new Gene(jCas, 5, 12);
        tpCorrectOffsets.setId("T1");
        tpCorrectOffsets.addToIndexes();
        Gene tpWrongOffsets = new Gene(jCas, 20, 25);
        tpWrongOffsets.setId("T2");
        tpWrongOffsets.addToIndexes();
        Gene fn = new Gene(jCas, 4711, 4712);
        fn.setId("T3");
        fn.addToIndexes();

        Protein protTpCorrectOffsets = new Protein(jCas, 5, 12);
        protTpCorrectOffsets.addToIndexes();
        Protein protTpWrongOffsets = new Protein(jCas, 18, 28);
        protTpWrongOffsets.addToIndexes();
        Protein fp = new Protein(jCas, 30, 40);
        fp.addToIndexes();

        // Check that there are all 3 genes present in the test CAS.
        assertEquals(3, JCasUtil.select(jCas, Gene.class).size(), "Wrong number of genes before processing.");

        ae.process(jCas);

        // There should still be 3 genes in the CAS. The false negative should be gone and the false positive should be added.
        final Collection<Gene> genesInCas = JCasUtil.select(jCas, Gene.class);
        assertEquals(3, genesInCas.size(), "Wrong number of genes after processing");

        // The two TPs should still be there. The offsets of the second one should be adjusted.
        assertTrue(genesInCas.contains(tpCorrectOffsets));
        assertTrue(genesInCas.contains(tpWrongOffsets));
//        assertEquals(protTpWrongOffsets.getBegin(), tpWrongOffsets.getBegin());
//        assertEquals(protTpWrongOffsets.getEnd(), tpWrongOffsets.getEnd());

        // There should be a gene now for the false positive protein.
        boolean fpFound = false;
        for (Gene g : genesInCas) {
            if (g.getBegin() == fp.getBegin() && g.getEnd() == fp.getEnd()) {
                // this should only happen once
                assertFalse(fpFound);
                assertEquals("T4", g.getId());
                fpFound = true;
            }
        }
        assertTrue(fpFound);

    }
}
