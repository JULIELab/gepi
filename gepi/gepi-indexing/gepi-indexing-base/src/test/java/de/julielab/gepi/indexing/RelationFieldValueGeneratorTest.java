package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.AddonTermsFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.ReplaceFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ace.Head;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class RelationFieldValueGeneratorTest {
    @Test
    public void generateFieldValue() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.eg2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.eg2tophomoFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2homoPrefNameReplaceFilter = new FilterChain();
        gfb.egid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.gene2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        TextFilterBoard tfb = new TextFilterBoard();
        FilterRegistry fr = Mockito.mock(FilterRegistry.class);
        Mockito.when(fr.getFilterBoard(GeneFilterBoard.class)).thenReturn(gfb);
        Mockito.when(fr.getFilterBoard(TextFilterBoard.class)).thenReturn(tfb);
        RelationFieldValueGenerator generator = new RelationFieldValueGenerator(fr);

        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");
        Header header = new Header(jCas);
        header.setDocId("doc1");
        header.addToIndexes();
        jCas.setDocumentText("Gene1 regulates gene2.");
        ArgumentMention a1 = createGeneArgument(jCas, 0, 5, "id1");
        ArgumentMention a2 = createGeneArgument(jCas, 16, 21, "id2");

        FlattenedRelation rel = new FlattenedRelation(jCas);
        rel.setArguments(JCoReTools.addToFSArray(null, List.of(a1, a2)));
        GeneralEventMention em = new EventMention(jCas);
        em.setSpecificType("regulation");
        rel.setRootRelation(em);
        rel.setRelations(JCoReTools.addToFSArray(null, em));
        rel.addToIndexes();

        ArrayFieldValue docs = (ArrayFieldValue) generator.generateFieldValue(rel);
        docs.stream().map(Document.class::cast).forEach(d -> d.remove("ARGUMENT_FS"));
        assertThat(docs).hasSize(1);
        Document doc = (Document) docs.get(0);

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id1, id2]");
        assertThat(doc.get("argument1geneid").toString()).isEqualTo("id1");
        assertThat(doc.get("argument2geneid").toString()).isEqualTo("id2");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
    }

    @Test
    public void generateFieldValueMultipleGeneIds() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.eg2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.eg2tophomoFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2homoPrefNameReplaceFilter = new FilterChain();
        gfb.egid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.gene2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        TextFilterBoard tfb = new TextFilterBoard();
        FilterRegistry fr = Mockito.mock(FilterRegistry.class);
        Mockito.when(fr.getFilterBoard(GeneFilterBoard.class)).thenReturn(gfb);
        Mockito.when(fr.getFilterBoard(TextFilterBoard.class)).thenReturn(tfb);
        RelationFieldValueGenerator generator = new RelationFieldValueGenerator(fr);

        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");
        Header header = new Header(jCas);
        header.setDocId("doc1");
        header.addToIndexes();
        jCas.setDocumentText("Gene1 regulates gene2.");
        ArgumentMention a1 = createGeneArgument(jCas, 0, 5, "id1", "id2");
        ArgumentMention a2 = createGeneArgument(jCas, 16, 21, "id3");

        FlattenedRelation rel = new FlattenedRelation(jCas);
        rel.setArguments(JCoReTools.addToFSArray(null, List.of(a1, a2)));
        GeneralEventMention em = new EventMention(jCas);
        em.setSpecificType("regulation");
        rel.setRootRelation(em);
        rel.setRelations(JCoReTools.addToFSArray(null, em));
        rel.addToIndexes();

        ArrayFieldValue docs = (ArrayFieldValue) generator.generateFieldValue(rel);
        docs.stream().map(Document.class::cast).forEach(d -> d.remove("ARGUMENT_FS"));
        assertThat(docs).hasSize(2);
        Document doc = (Document) docs.get(0);

        assertThat(doc.get("arguments").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argumentconceptids").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argumenttophomoids").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argumentcoveredtext").toString()).isEqualTo("[Gene1, gene2]");
        assertThat(doc.get("argumentprefnames").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argumenthomoprefnames").toString()).isEqualTo("[id1, id3]");
        assertThat(doc.get("argument1geneid").toString()).isEqualTo("id1");
        assertThat(doc.get("argument2geneid").toString()).isEqualTo("id3");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
    }

    private ArgumentMention createGeneArgument(JCas jCas, int begin, int end, String... ids) {
        Gene g = new Gene(jCas, begin, end);
        for (String id : ids) {
            ResourceEntry re = new ResourceEntry(jCas);
            re.setEntryId(id);
            FSArray resourceEntryList = JCoReTools.addToFSArray(g.getResourceEntryList(), re);
            g.setResourceEntryList(resourceEntryList);
        }
        g.addToIndexes();
        ArgumentMention am = new ArgumentMention(jCas, begin, end);
        am.setRef(g);
        am.addToIndexes();
        return am;
    }
}
