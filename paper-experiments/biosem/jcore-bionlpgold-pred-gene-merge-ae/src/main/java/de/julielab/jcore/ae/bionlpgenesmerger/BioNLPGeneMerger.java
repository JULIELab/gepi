package de.julielab.jcore.ae.bionlpgenesmerger;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Protein;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ResourceMetaData(name = "JCoRe BioNLP Gold and Predicted Genes Merge AE", description = "Given the gold BioNLP ST gene mentions and other gene mentions - possibly from gene recognizer - merges the two different sources of genes. For simplicity, this component employs two different types to represent genes. The BioNLP ST reader uses the de.julielab.jcore.types.Gene type. The other genes should be realized with de.julielab.jcore.types.Protein annotations.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {"de.julielab.jcore.types.Gene", "de.julielab.jcore.types.Protein"}, outputs = {"de.julielab.jcore.types.Gene"})
public class BioNLPGeneMerger extends JCasAnnotator_ImplBase {

    private final static Logger log = LoggerFactory.getLogger(BioNLPGeneMerger.class);

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        removeFamilyNameAnnotations(aJCas);
        // Get Ids of left genes so we can assign new Ids to the false positives
        OptionalInt maxGeneIdNumber = StreamSupport.stream(aJCas.<Gene>getAnnotationIndex(Gene.type).spliterator(), false).map(Gene::getId).mapToInt(id -> Integer.parseInt(id.substring(1))).max();
        handleTpsFns(aJCas);
        handleFps(aJCas, maxGeneIdNumber);
        removePredictions(aJCas);
    }

    private void removeFamilyNameAnnotations(JCas aJCas) {
        List<Protein> famProts = StreamSupport.stream(aJCas.<Protein>getAnnotationIndex(Protein.type).spliterator(), false).filter(p -> p.getSpecificType() != null ? p.getSpecificType().equals("FamilyName") || p.getSpecificType().equals("protein_familiy_or_group"): false).collect(Collectors.toList());
        famProts.forEach(Protein::removeFromIndexes);
    }

    /**
     * After tailoring the gold annotations to the predictions, we don't need the predictions anymore and remove them to avoid confusion.
     *
     * @param aJCas
     */
    private void removePredictions(JCas aJCas) {
        List<Protein> predProts = StreamSupport.stream(aJCas.<Protein>getAnnotationIndex(Protein.type).spliterator(), false).collect(Collectors.toList());
        predProts.forEach(Protein::removeFromIndexes);
    }

    /**
     * Create false gene annotations for predictions without a gold gene equivalent. Set a new ID outside of the
     * gold ID range to avoid correct events based on those IDs by chance.
     *
     * @param aJCas
     * @param maxGeneIdNumber
     */
    private void handleFps(JCas aJCas, OptionalInt maxGeneIdNumber) {
        if (maxGeneIdNumber.isPresent()) {
            log.debug("maxTID: {}", maxGeneIdNumber.getAsInt());
            int newIdCounter = maxGeneIdNumber.getAsInt() + 1;
            final JCoReOverlapAnnotationIndex<Gene> geneIndex = new JCoReOverlapAnnotationIndex<>(aJCas, Gene.type);
            for (Protein predProt : aJCas.<Protein>getAnnotationIndex(Protein.type)) {
                final List<Gene> genes4protein = geneIndex.search(predProt);
                if (genes4protein.isEmpty()) {
                    final Gene falsePositiveGene = new Gene(aJCas, predProt.getBegin(), predProt.getEnd());
                    falsePositiveGene.setComponentId("[FP] " + predProt.getComponentId() + " / " + getClass().getSimpleName());
                    falsePositiveGene.setId("T" + newIdCounter++);
                    log.debug("[FP] {}: {}-{}", falsePositiveGene.getCoveredText(), falsePositiveGene.getBegin(), falsePositiveGene.getEnd());
                    log.debug(falsePositiveGene.getId() + " " + falsePositiveGene.getCoveredText());
                    // We need this specific type or the BioNLP Format Writer won't include this annotation in its output.
                    falsePositiveGene.setSpecificType("protein");
                    falsePositiveGene.setResourceEntryList(predProt.getResourceEntryList());
                    falsePositiveGene.addToIndexes();
                }
            }
        }
    }

    /**
     * Delete genes that have been missed in the prediction. Adapt offsets of gold mentions that have a prediction to adapt the genes to the predictions.
     *
     * @param aJCas
     */
    private void handleTpsFns(JCas aJCas) {
        JCoReOverlapAnnotationIndex<Protein> proteinIndex = new JCoReOverlapAnnotationIndex<>(aJCas, Protein.type);
        // track missed gold genes for deletion
        List<Gene> falseNegatives = new ArrayList<>();
        // track found gold genes for potential offset adaption
//        List<Triple<Gene, Integer, Integer>> truePositives = new ArrayList<>();
        for (Gene goldGene : aJCas.<Gene>getAnnotationIndex(Gene.type)) {
            final List<Protein> proteins4gene = proteinIndex.search(goldGene);
            if (!proteins4gene.isEmpty()) {
                for (Protein predProt : proteins4gene) {
//                    if (goldGene.getBegin() != predProt.getBegin() || goldGene.getEnd() != predProt.getEnd()) {
                        // Store the new offsets
//                        truePositives.add(ImmutableTriple.of(goldGene, predProt.getBegin(), predProt.getEnd()));
//                    }
                    goldGene.setComponentId("[TP] " + goldGene.getComponentId() + " / " + getClass().getSimpleName());
                    goldGene.setResourceEntryList(predProt.getResourceEntryList());
                    log.debug("[TP] {}: {}-{}", goldGene.getCoveredText(), goldGene.getBegin(), goldGene.getEnd());
                }
            } else {
                // No prediction for this gold gene. Remove it after we are done iterating over its index.
                falseNegatives.add(goldGene);
                log.debug("[FN] {}: {}-{}", goldGene.getCoveredText(), goldGene.getBegin(), goldGene.getEnd());
            }
        }
        // remove the gold genes missed in the predicted genes
        falseNegatives.forEach(Gene::removeFromIndexes);
        // re-add position adapted genes for correct index sorting
//        truePositives.forEach(triple -> {
//            Gene gene = triple.getLeft();
//            gene.removeFromIndexes();
//            gene.setBegin(triple.getMiddle());
//            gene.setEnd(triple.getRight());
//            gene.addToIndexes();
//        });
    }

}
