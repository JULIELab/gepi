package de.julielab.gepi.indexing;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.AddonTermsFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.ReplaceFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static de.julielab.gepi.indexing.TestUtils.createGeneArgument;
import static org.assertj.core.api.Assertions.assertThat;

public class RelationDocumentGeneratorTest {

    @Test
    public void generateFieldValueFromDifferentSources() throws Exception {
        // This test simulates the creation of events by different processing pipelines
        // that are basically equal and are merged into a single index document.
        RelationDocumentGenerator generator = createRelationDocumentGenerator();

        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");
        Header header = new Header(jCas);
        header.setDocId("doc1");
        header.addToIndexes();
        jCas.setDocumentText("Gene1 regulates gene2.");
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        // Relation from the first extraction process
        ArgumentMention a11 = createGeneArgument(jCas, 0, 5, "id1");
        a11.getRef().setComponentId("GeneTagger1");
        ((ConceptMention)a11.getRef()).getResourceEntryList(0).setComponentId("GeneMapper1");
        ArgumentMention a12 = createGeneArgument(jCas, 16, 21, "id2");
        a12.getRef().setComponentId("GeneTagger1");
        ((ConceptMention)a12.getRef()).getResourceEntryList(0).setComponentId("GeneMapper1");

        FlattenedRelation rel1 = new FlattenedRelation(jCas);
        rel1.setArguments(JCoReTools.addToFSArray(null, List.of(a11, a12)));
        GeneralEventMention em = new EventMention(jCas);
        em.setSpecificType("regulation");
        rel1.setRootRelation(em);
        rel1.setRelations(JCoReTools.addToFSArray(null, em));
        rel1.getRootRelation().setComponentId("EventExtractor1");
        rel1.addToIndexes();

        // Relation from the second extraction process that equals the first
        ArgumentMention a21 = createGeneArgument(jCas, 0, 5, "id1");
        a21.getRef().setComponentId("GeneTagger2");
        ((ConceptMention)a21.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");
        ArgumentMention a22 = createGeneArgument(jCas, 16, 21, "id2");
        a22.getRef().setComponentId("GeneTagger2");
        ((ConceptMention)a22.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");

        FlattenedRelation rel2 = new FlattenedRelation(jCas);
        rel2.setArguments(JCoReTools.addToFSArray(null, List.of(a21, a22)));
        GeneralEventMention em2 = new EventMention(jCas);
        em2.setSpecificType("regulation");
        rel2.setRootRelation(em2);
        rel2.setRelations(JCoReTools.addToFSArray(null, em2));
        rel2.getRootRelation().setComponentId("EventExtractor2");
        rel2.addToIndexes();

        // Relation that does not equal the first two because the genes are mapped to different IDs
        ArgumentMention a31 = createGeneArgument(jCas, 0, 5, "id4");
        a31.getRef().setComponentId("GeneTagger2");
        ((ConceptMention)a31.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");
        ArgumentMention a32 = createGeneArgument(jCas, 16, 21, "id5");
        a32.getRef().setComponentId("GeneTagger2");
        ((ConceptMention)a32.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");

        FlattenedRelation rel3 = new FlattenedRelation(jCas);
        rel3.setArguments(JCoReTools.addToFSArray(null, List.of(a31, a32)));
        GeneralEventMention em3 = new EventMention(jCas);
        em3.setSpecificType("regulation");
        rel3.setRootRelation(em3);
        rel3.setRelations(JCoReTools.addToFSArray(null, em3));
        rel3.getRootRelation().setComponentId("EventExtractor2");
        rel3.addToIndexes();

        List<Document> docs = generator.createDocuments(jCas);
        docs.stream().map(Document.class::cast).forEach(d -> d.remove("ARGUMENT_FS"));
        // There should be two relation documents: one for the merged first two above and one for the last
        assertThat(docs).hasSize(2);
        Document doc = docs.get(0);

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id1, id2]");
        assertThat(doc.get("argument1geneid").toString()).isEqualTo("id1");
        assertThat(doc.get("argument2geneid").toString()).isEqualTo("id2");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
        // We should find both sources for genes, gene IDs and events in this document
        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger1, GeneTagger2]");
        assertThat(doc.get("genemappingsource").toString()).isEqualTo("[GeneMapper1, GeneMapper2]");
        assertThat(doc.get("relationsource").toString()).isEqualTo("[EventExtractor1, EventExtractor2]");

        doc = docs.get(1);

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id4, id5]");
        // This relation has only been extracted in this form by the second extraction process
        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger2]");
        assertThat(doc.get("genemappingsource").toString()).isEqualTo("[GeneMapper2]");
        assertThat(doc.get("relationsource").toString()).isEqualTo("EventExtractor2");
    }

//    @Test
//    public void generateFieldValueMixedGeneSource() throws Exception {
//        // In this test we have three relations. The first two are equal to one another and are expected to be
//        // merged. The third one should stay for itself.
//        // Additionally, the arguments of the first relation stem from different GeneTaggers. Thus, the resulting
//        // merged event document should be marked as having "mixedGeneSources"
//        RelationDocumentGenerator generator = createRelationDocumentGenerator();
//
//        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");
//        Header header = new Header(jCas);
//        header.setDocId("doc1");
//        header.addToIndexes();
//        jCas.setDocumentText("Gene1 regulates gene2.");
//        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
//        // Mixed gene argument sources for this event
//        ArgumentMention a11 = createGeneArgument(jCas, 0, 5, "id1");
//        a11.getRef().setComponentId("GeneTagger1");
//        ((ConceptMention)a11.getRef()).getResourceEntryList(0).setComponentId("GeneMapper1");
//        ArgumentMention a12 = createGeneArgument(jCas, 16, 21, "id2");
//        a12.getRef().setComponentId("GeneTagger1");
//        ((ConceptMention)a12.getRef()).getResourceEntryList(0).setComponentId("GeneMapper1");
//
//        FlattenedRelation rel1 = new FlattenedRelation(jCas);
//        rel1.setArguments(JCoReTools.addToFSArray(null, List.of(a11, a12)));
//        GeneralEventMention em = new EventMention(jCas);
//        em.setSpecificType("regulation");
//        rel1.setRootRelation(em);
//        rel1.setRelations(JCoReTools.addToFSArray(null, em));
//        rel1.getRootRelation().setComponentId("EventExtractor1");
//        rel1.addToIndexes();
//
//        // This relation equals the above one in gene IDs etc. but has a complete different tagger
//        ArgumentMention a21 = createGeneArgument(jCas, 0, 5, "id1");
//        a21.getRef().setComponentId("GeneTagger2");
//        ((ConceptMention)a21.getRef()).getResourceEntryList(0).setComponentId("GeneMapper3");
//        ArgumentMention a22 = createGeneArgument(jCas, 16, 21, "id2");
//        a22.getRef().setComponentId("GeneTagger3");
//        ((ConceptMention)a22.getRef()).getResourceEntryList(0).setComponentId("GeneMapper3");
//
//        FlattenedRelation rel2 = new FlattenedRelation(jCas);
//        rel2.setArguments(JCoReTools.addToFSArray(null, List.of(a21, a22)));
//        GeneralEventMention em2 = new EventMention(jCas);
//        em2.setSpecificType("regulation");
//        rel2.setRootRelation(em2);
//        rel2.setRelations(JCoReTools.addToFSArray(null, em2));
//        rel2.getRootRelation().setComponentId("EventExtractor2");
//        rel2.addToIndexes();
//
//        // Relation that does not equal the first two because the genes are mapped to different IDs
//        ArgumentMention a31 = createGeneArgument(jCas, 0, 5, "id4");
//        a31.getRef().setComponentId("GeneTagger2");
//        ((ConceptMention)a31.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");
//        ArgumentMention a32 = createGeneArgument(jCas, 16, 21, "id5");
//        a32.getRef().setComponentId("GeneTagger2");
//        ((ConceptMention)a32.getRef()).getResourceEntryList(0).setComponentId("GeneMapper2");
//
//        FlattenedRelation rel3 = new FlattenedRelation(jCas);
//        rel3.setArguments(JCoReTools.addToFSArray(null, List.of(a31, a32)));
//        GeneralEventMention em3 = new EventMention(jCas);
//        em3.setSpecificType("regulation");
//        rel3.setRootRelation(em3);
//        rel3.setRelations(JCoReTools.addToFSArray(null, em3));
//        rel3.getRootRelation().setComponentId("EventExtractor2");
//        rel3.addToIndexes();
//
//        List<Document> docs = generator.createDocuments(jCas);
//        docs.stream().map(Document.class::cast).forEach(d -> d.remove("ARGUMENT_FS"));
//        // There should be two relation documents: one for the merged first two above and one for the last
//        assertThat(docs).hasSize(2);
//        Document doc = docs.get(0);
//
//        // We should find three sources for genes
//        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger1, GeneTagger2, GeneTagger3]");
//        assertThat(doc.get("mixedgenesource").toString()).isEqualTo("true");
//        assertThat(doc.get("mixedgenemappingsource").toString()).isEqualTo("false");
//
//        doc = docs.get(1);
//
//        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id4, id5]");
//        // This relation has only been extracted in this form by the second extraction process
//        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger2]");
//        assertThat(doc.get("genemappingsource").toString()).isEqualTo("[GeneMapper2]");
//        assertThat(doc.get("relationsource").toString()).isEqualTo("EventExtractor2");
//    }

    @NotNull
    private RelationDocumentGenerator createRelationDocumentGenerator() {
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
        return new RelationDocumentGenerator(fr);
    }

    @Test
    public void testDocument() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
        XmiCasDeserializer.deserialize(FileUtilities.getInputStreamFromFile(new File("/Users/faessler/Coding/git/gepi/gepi/gepi-indexing/gepi-indexing-pubmed/data/output-xmi/19134118.xmi")), jCas.getCas());

        RelationDocumentGenerator generator = createRelationDocumentGenerator();

        final List<Document> documents = generator.createDocuments(jCas);
        assertThat(documents).hasSize(3);
        for (var doc : documents) {
            System.out.println(doc.get("argumentcoveredtext"));
            System.out.println(doc.get("genesource"));
            System.out.println(doc.get("genemappingsource"));
            System.out.println(doc.get("mixedgenesource"));
            System.out.println(doc.get("mixedgenemappingsource"));
            System.out.println();
        }
    }

}