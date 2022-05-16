package de.julielab.jcore.ae.genemerge;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Expects as input Gene annotations. Finds those Gene annotations that have been created by the Gazetteer component and whose specific type starts matches "tid[0-9]+", indicating a Julielab Neo4j Concept database ID.
 * Also finds those Gene annotations whose specificType points to a gene family or group. When there is an overlap between group annotations and gazetteer annotations, the gazetteer-assigned database concept ID is added as a ResourceEntry to the family gene.
 * Finally, all gazetteer concept DB gene annotations are removed from CAS indexes.
 */
public class GePiFamplexIdAssigner extends JCasAnnotator_ImplBase {
    private Pattern tidP = Pattern.compile("tid[0-9]+");
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        final JCoReOverlapAnnotationIndex<Gene> conceptGazetteerGenes = new JCoReOverlapAnnotationIndex<>();
        List<Gene> familyGenes = new ArrayList<>();
        // collect gazetteer and family genes
        for (Gene g : jCas.<Gene>getAnnotationIndex(Gene.type)) {
            if (g.getComponentId() != null && g.getComponentId().toLowerCase().contains("gazetteer") && tidP.matcher(g.getSpecificType()).matches())
                conceptGazetteerGenes.index(g);
            // the "familiy" is a consistent typo in ProGene
            else if (g.getSpecificType() != null && (g.getSpecificType().equalsIgnoreCase("familyname") || g.getSpecificType().equalsIgnoreCase("protein_familiy_or_group"))) {
                familyGenes.add(g);
            }
        }
        conceptGazetteerGenes.freeze();
        // find families overlapping gazetteer annotations and transfer the found ID.
        for (Gene familyGene : familyGenes) {
            final List<Gene> search = conceptGazetteerGenes.search(familyGene);
            Gene longestGene = getLongestMatch(search);
            if (longestGene != null) {
                final ResourceEntry entry = new ResourceEntry(jCas, familyGene.getBegin(), familyGene.getEnd());
                entry.setComponentId(getClass().getSimpleName());
                entry.setEntryId(longestGene.getSpecificType());
                final FSArray resourceEntryList = new FSArray(jCas, 1);
                resourceEntryList.set(0, entry);
                familyGene.setResourceEntryList(resourceEntryList);
            }
        }
        // Remove gazetteer genes
        conceptGazetteerGenes.getBeginIndex().forEach(Gene::removeFromIndexes);
    }

    private Gene getLongestMatch(Iterable<Gene> genes) {
        int maxLength = -1;
        Gene longestGene = null;
        for (Gene g : genes) {
            final int length = g.getEnd() - g.getBegin();
            if (length > maxLength) {
                maxLength = length;
                longestGene = g;
            }
        }
        return longestGene;
    }
}
