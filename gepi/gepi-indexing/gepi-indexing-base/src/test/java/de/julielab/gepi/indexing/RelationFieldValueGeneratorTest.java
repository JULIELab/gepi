package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.AddonTermsFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.ReplaceFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static de.julielab.gepi.indexing.TestUtils.createGeneArgument;
import static org.assertj.core.api.Assertions.assertThat;

public class RelationFieldValueGeneratorTest {
    @Test
    public void generateFieldValue() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.orgid2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.orgid2topaggFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2topaggprefname = new FilterChain();
        gfb.orgid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2taxidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
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
        // We assign id2 first to test that the aggregation term creation successfully sorts the values
        ArgumentMention a1 = createGeneArgument(jCas, 0, 5, "id2");
        ArgumentMention a2 = createGeneArgument(jCas, 16, 21, "id1");

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

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id2, id1]");
        assertThat(doc.get("aggregationvalue").toString()).isEqualTo("id1---id2");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
    }

    @Test
    public void generateFieldValueMultipleGeneIds() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.orgid2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.orgid2topaggFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2topaggprefname = new FilterChain();
        gfb.orgid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2taxidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
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
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
    }

    @Test
    public void generateFieldValueMergedGenes() throws Exception {
        // The GepiGeneMerger creates componentIds that are actually multiple, comma separated IDs
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.orgid2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.orgid2topaggFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2topaggprefname = new FilterChain();
        gfb.orgid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2taxidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
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
        ArgumentMention a1 = createGeneArgument(jCas, 0, 5, "GeneTagger1,GeneTagger2", List.of("id1", "id2"), List.of("Normalizer1", "Normalizer2"));
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

        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger1, GeneTagger2, SomeGeneTagger]");
        assertThat(doc.get("genemappingsource").toString()).isEqualTo("[Normalizer1, SomeNormalizer]");

        doc = (Document) docs.get(1);

        assertThat(doc.get("genesource").toString()).isEqualTo("[GeneTagger1, GeneTagger2, SomeGeneTagger]");
        assertThat(doc.get("genemappingsource").toString()).isEqualTo("[Normalizer2, SomeNormalizer]");
    }

    @Test
    public void generateFieldValueWithFamily() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.orgid2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.orgid2topaggFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2topaggprefname = new FilterChain();
        gfb.orgid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2taxidReplaceFilter = new ReplaceFilter(Map.of("id1", "9606"));
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
        ArgumentMention a2 = createGeneArgument(jCas, 16, 21);
        ((Gene) a2.getRef()).setSpecificType("FamilyName");
        ((Gene) a2.getRef()).setSpecies(JCoReTools.newStringArray(jCas, "9606", "10090"));

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

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id1, gene2]");
        assertThat(doc.getAsArrayFieldValue("argumentgeneids").get(0).toString()).isEqualTo("id1");
        assertThat(doc.getAsArrayFieldValue("argumentgeneids").get(1).toString()).isEqualTo("gene2");
        assertThat(doc.getAsArrayFieldValue("argumenttaxids")).extracting(IFieldValue::toString).containsExactly("9606", "10090");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("regulation");
    }


    @Test
    public void generateUnaryFieldValue() throws Exception {
        GeneFilterBoard gfb = new GeneFilterBoard();
        gfb.orgid2tidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        gfb.orgid2topaggFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2topaggprefname = new FilterChain();
        gfb.orgid2prefNameReplaceFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.orgid2tid2atidAddonFilter = new AddonTermsFilter(Collections.emptyMap());
        gfb.egid2taxidReplaceFilter = new ReplaceFilter(Collections.emptyMap());
        TextFilterBoard tfb = new TextFilterBoard();
        FilterRegistry fr = Mockito.mock(FilterRegistry.class);
        Mockito.when(fr.getFilterBoard(GeneFilterBoard.class)).thenReturn(gfb);
        Mockito.when(fr.getFilterBoard(TextFilterBoard.class)).thenReturn(tfb);
        RelationFieldValueGenerator generator = new RelationFieldValueGenerator(fr);

        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");
        Header header = new Header(jCas);
        header.setDocId("doc1");
        header.addToIndexes();
        jCas.setDocumentText("Gene1 is phosphoryalated.");
        ArgumentMention a1 = createGeneArgument(jCas, 0, 5, "id1");

        FlattenedRelation rel = new FlattenedRelation(jCas);
        rel.setArguments(JCoReTools.addToFSArray(null, List.of(a1)));
        GeneralEventMention em = new EventMention(jCas);
        em.setSpecificType("phosphorylation");
        rel.setRootRelation(em);
        rel.setRelations(JCoReTools.addToFSArray(null, em));
        rel.addToIndexes();

        ArrayFieldValue docs = (ArrayFieldValue) generator.generateFieldValue(rel);
        docs.stream().map(Document.class::cast).forEach(d -> d.remove("ARGUMENT_FS"));
        assertThat(docs).hasSize(1);
        Document doc = (Document) docs.get(0);

        assertThat(doc.get("argumentgeneids").toString()).isEqualTo("[id1, none]");
        assertThat(doc.get("maineventtype").toString()).isEqualTo("phosphorylation");
    }

}
