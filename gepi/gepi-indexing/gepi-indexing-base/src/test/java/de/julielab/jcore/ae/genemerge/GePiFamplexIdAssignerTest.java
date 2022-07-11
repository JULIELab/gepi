package de.julielab.jcore.ae.genemerge;

import de.julielab.jcore.types.Gene;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
public class GePiFamplexIdAssignerTest {
    @Test
    public void process() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        jCas.setDocumentText("AMPK often appears with mTOR and AKT1.");
        final Gene ampkFamily = new Gene(jCas, 0, 4);
        ampkFamily.setSpecificType("FamilyName");
        ampkFamily.addToIndexes();
        final Gene ampkGazetteer = new Gene(jCas, 0, 4);
        ampkGazetteer.setComponentId("Gazetteer");
        ampkGazetteer.setSpecificType("tid12345");
        ampkGazetteer.addToIndexes();

        final Gene mtor = new Gene(jCas, 24, 28);
        mtor.setSpecificType("Gene");
        mtor.addToIndexes();

        final Gene akt1 = new Gene(jCas, 33, 37);
        akt1.setSpecificType("Gene");
        akt1.addToIndexes();

        final AnalysisEngine idAssigner = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.genemerge.desc.gepi-famplex-id-assigner");
        idAssigner.process(jCas);

        final Collection<Gene> genes = JCasUtil.select(jCas, Gene.class);
        // The gazetteer gene should have been removed
        assertThat(genes).hasSize(3);
        final Gene ampk = genes.iterator().next();
        assertThat(ampk).extracting(Gene::getCoveredText).isEqualTo("AMPK");
        assertThat(ampk.getResourceEntryList()).isNotNull().hasSize(1);
        assertThat(ampk.getResourceEntryList(0)).isNotNull().extracting("entryId").isEqualTo("tid12345");
    }
}