package de.julielab.jcore.ae.genemerge;


import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
public class GepiGeneMergerTest {
    @Test
    public void testProcess() throws UIMAException {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        jCas.setDocumentText("mTOR");
        final Gene g1 = new Gene(jCas, 0, 4);
        g1.setComponentId("Tagger1");
        final ResourceEntry re1 = new ResourceEntry(jCas);
        re1.setComponentId("Tagger1");
        re1.setEntryId("id1");
        g1.setResourceEntryList(JCoReTools.addToFSArray(null, re1));
        g1.addToIndexes();

        final Gene g2 = new Gene(jCas, 0, 4);
        g2.setComponentId("Tagger2");
        final ResourceEntry re2 = new ResourceEntry(jCas);
        re2.setComponentId("Tagger2");
        re2.setEntryId("id2");
        g2.setResourceEntryList(JCoReTools.addToFSArray(null, re2));
        g2.addToIndexes();

        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(GepiGeneMerger.class);
        engine.process(jCas);

        final Collection<Gene> genes = JCasUtil.select(jCas, Gene.class);
        assertThat(genes).hasSize(1);
        Gene g = genes.iterator().next();
        assertThat(g.getComponentId().split(",")).containsExactly("Tagger1", "Tagger2");
        assertThat(g.getResourceEntryList()).isNotNull();
        assertThat(Arrays.stream(g.getResourceEntryList().toArray()).map(ResourceEntry.class::cast).map(Annotation::getComponentId).collect(Collectors.toList())).containsExactly("Tagger1", "Tagger2");
        assertThat(Arrays.stream(g.getResourceEntryList().toArray()).map(ResourceEntry.class::cast).map(ResourceEntry::getEntryId).collect(Collectors.toList())).containsExactly("id1", "id2");
    }
}