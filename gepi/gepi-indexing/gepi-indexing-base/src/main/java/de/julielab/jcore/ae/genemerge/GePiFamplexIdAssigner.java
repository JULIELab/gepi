package de.julielab.jcore.ae.genemerge;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Expects as input Gene annotations. Finds those Gene annotations that have been created by the Gazetteer component and whose specific type starts matches "tid[0-9]+", indicating a Julielab Neo4j Concept database ID.
 * Also finds those Gene annotations whose specificType points to a gene family or group. When there is an overlap between group annotations and gazetteer annotations, the gazetteer-assigned database concept ID is added as a ResourceEntry to the family gene.
 * Finally, all gazetteer concept DB gene annotations are removed from CAS indexes.
 */
public class GePiFamplexIdAssigner extends JCasAnnotator_ImplBase {
    private Pattern famplexHgncGroupMatcher = Pattern.compile("(FPLX|HGNCG):.*");
    /**
     * Gene names that are not clearly a group or family. In those cases we let the GNormPlus class decide.
     */
    private Pattern difficultCases = Pattern.compile("(p[0-9]+)|(il-?[0-9]+)|(cd[0-9]+)|(stat[0-9]+)]", Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        final JCoReOverlapAnnotationIndex<Gene> conceptGazetteerGenes = new JCoReOverlapAnnotationIndex<>();
        List<Gene> familyGenes = new ArrayList<>();
        // collect gazetteer and family genes
        for (Gene g : jCas.<Gene>getAnnotationIndex(Gene.type)) {
            if (g.getComponentId() != null && g.getComponentId().toLowerCase().contains("gazetteer") && famplexHgncGroupMatcher.matcher(g.getSpecificType()).matches())
                conceptGazetteerGenes.index(g);
                // I decided to ignore the GNP tagging in most cases and just match the names from FamPlex and HGNC because it just looks right in the data.
                // the "familiy" is a consistent typo in ProGene
            else if (!difficultCases.matcher(g.getCoveredText()).matches() || (g.getSpecificType() != null && (g.getSpecificType().equalsIgnoreCase("familyname") || g.getSpecificType().equalsIgnoreCase("protein_familiy_or_group")))) {
                familyGenes.add(g);
            }
        }
        conceptGazetteerGenes.freeze();
        // find families overlapping gazetteer annotations and transfer the found ID.
        for (Gene familyGene : familyGenes) {
            final List<Gene> search = conceptGazetteerGenes.search(familyGene);
            List<Gene> longestGenes = getLongestMatches(search, familyGene);
            FSArray resourceEntryList;
            if (!longestGenes.isEmpty()) {
                resourceEntryList = new FSArray(jCas, 1);
                final String concatenatedIds = longestGenes.stream().map(Gene::getSpecificType).collect(Collectors.joining("---"));
                final ResourceEntry entry = new ResourceEntry(jCas, familyGene.getBegin(), familyGene.getEnd());
                entry.setComponentId(getClass().getSimpleName());
                entry.setEntryId(concatenatedIds);
                resourceEntryList.set(0, entry);
                familyGene.setResourceEntryList(resourceEntryList);
            }
        }
        // Remove gazetteer genes
        conceptGazetteerGenes.getBeginIndex().forEach(Gene::removeFromIndexes);
    }

    private List<Gene> getLongestMatches(Iterable<Gene> genes, Gene referenceGene) {
        int maxLength = -1;
        List<Gene> longestGenes = new ArrayList<>();
        for (Gene g : genes) {
            final int length = g.getEnd() - g.getBegin();
            // Only accept matches that completely cover the predicted gene. Otherwise, name parts of concrete
            // genes can be mistaken for families, e.g. c-Jun is then mapped to the JUN family
            if (length > maxLength && g.getBegin() <= referenceGene.getBegin() && g.getEnd() >= referenceGene.getEnd()) {
                maxLength = length;
            }
        }
        for (Gene g : genes) {
            if (g.getEnd() - g.getBegin() == maxLength)
                longestGenes.add(g);
        }
        return longestGenes;
    }
}
