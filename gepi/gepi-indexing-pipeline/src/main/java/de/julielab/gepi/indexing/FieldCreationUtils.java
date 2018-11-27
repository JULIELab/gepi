package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldCreationUtils {
    protected static final HashMap<String, Integer> likelihoodValues;
    protected static final HashMap<Integer, String> inverseLikelihoodValues = new HashMap<>();

    static {
        likelihoodValues = new HashMap<>();
        likelihoodValues.put("negation", 1);
        likelihoodValues.put("low", 2);
        likelihoodValues.put("investigation", 3);
        likelihoodValues.put("moderate", 4);
        likelihoodValues.put("high", 5);
        likelihoodValues.put("assertion", 6);
        for (Map.Entry<String, Integer> e : likelihoodValues.entrySet())
            inverseLikelihoodValues.put(e.getValue(), e.getKey());
    }

    public static int getMeanLikelihood(AnnotationFS annotation) throws FieldGenerationException {
        List<Integer> likelihoods = new ArrayList<>();
        try {
            // here we get all the likelihoods explicitly stated in the text
            // covered by the annotation parameter
            FSIterator<Annotation> it = annotation.getCAS().getJCas().getAnnotationIndex(LikelihoodIndicator.type)
                    .subiterator(annotation);
            while (it.hasNext()) {
                LikelihoodIndicator likelihood = (LikelihoodIndicator) it.next();
                if (null != likelihood) {
                    Integer likelihoodOrdinal = likelihoodValues.get(likelihood.getLikelihood());
                    likelihoods.add(likelihoodOrdinal);
                }
            }

            // assertions are mostly not expressed explicitly but hold by the
            // absence of another likelihood indicator; technically, the
            // LikelihoodAssignment component creates one assertion-likelihood
            // and places it at begin=0, end=0. Thus, we check if some
            // ConceptMention in the sought text span refers to this specific
            // out-of-bounds-likelihood, since all in-bounds-likelhoods have
            // been covered by the loop above.
            it = annotation.getCAS().getJCas().getAnnotationIndex(ConceptMention.type).subiterator(annotation);
            while (it.hasNext()) {
                ConceptMention concept = (ConceptMention) it.next();
                LikelihoodIndicator likelihood = concept.getLikelihood();
                if (null != likelihood && (likelihood.getBegin() >= annotation.getEnd()
                        || likelihood.getEnd() <= annotation.getBegin())) {
                    Integer likelihoodOrdinal = likelihoodValues.get(likelihood.getLikelihood());
                    likelihoods.add(likelihoodOrdinal);
                }
            }
        } catch (CASRuntimeException | CASException e) {
            throw new FieldGenerationException(
                    "Exception occurred while determining mean likelihood of text covered by annotation " + annotation
                            + ".",
                    e);
        }

        // if there is no likelihood indicator whatsoever, we assume certain
        // facts in the text passage
        if (likelihoods.isEmpty())
            return likelihoodValues.get("assertion");

        // mean of ordinals is contentious but in our case it should result in
        // something like a "mean likelihood"
        double sum = 0;
        for (Integer likelihoodOrdinal : likelihoods) {
            sum += likelihoodOrdinal;
        }
        double mean = sum / likelihoods.size();
        int meanLikelihoodValue = (int) Math.round(mean);
        return meanLikelihoodValue;
        // String ret = inverseLikelihoodValues.get(meanLikelihoodValue);
        // return ret;
    }

    /**
     * Adds pmid and pmcid fields.
     * @param document
     * @param annotation
     * @throws CASException
     */
    public static void addDocumentId(Document document, AnnotationFS annotation) throws CASException {
        JCas jCas = annotation.getCAS().getJCas();
        String docId = JCoReTools.getDocId(jCas);
        if (docId.startsWith("PMC")) {
            document.addField("pmcid", docId);
            Header header = JCasUtil.selectSingle(jCas, Header.class);
            if (header.getOtherIDs() != null) {
                for (int i = 0; i < header.getOtherIDs().size(); i++) {
                    OtherID otherID = header.getOtherIDs(i);
                    if (otherID.getSource().equalsIgnoreCase("pubmed")) {
                        document.addField("pmid", otherID.getId());
                    }
                }

            }
        } else {
            // We currently only expect PMC or no prefix, no prefix meaning PubMed
            document.addField("pmid", docId);
            Header header = JCasUtil.selectSingle(jCas, Header.class);
            if (header.getOtherIDs() != null) {
                for (int i = 0; i < header.getOtherIDs().size(); i++) {
                    OtherID otherID = header.getOtherIDs(i);
                    if (otherID.getSource().equalsIgnoreCase("pmc") || otherID.getSource().equalsIgnoreCase("pmcid")) {
                        document.addField("pmcid", otherID.getId());
                    }
                }

            }
        }
    }
}
