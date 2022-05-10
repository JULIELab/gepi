package de.julielab.jcore.ae.genemerge;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.index.JCoReAnnotationIndex;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;
import java.util.stream.Collectors;

@ResourceMetaData(name="GePi Gene Merger", description = "Merges gene annotations from multiple sources/taggers. All overlapping gene mentions are fused into the longest one. The component IDs are concatenated. Resource entries are collected into a single list.")
@TypeCapability(inputs= {"de.julielab.jcore.types.GeneMention", "de.julielab.jcore.types.ResourceEntry"},outputs= {"de.julielab.jcore.types.GeneMention", "de.julielab.jcore.types.ResourceEntry"})
public class GepiGeneMerger extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        JCoReOverlapAnnotationIndex<Gene> geneIndex = new JCoReOverlapAnnotationIndex<>(jCas, Gene.type);
        List<Gene> toRemove = new ArrayList<>();
        Set<Gene> alreadyHandled = new HashSet<>();
        for (Gene g : jCas.<Gene>getAnnotationIndex(Gene.type)) {
            // Get the overlapping genes and filter out those we have already seen (which will happen with overlapping genes)
            final List<Gene> overlappingGenes = geneIndex.search(g).stream().filter(alreadyHandled::add).collect(Collectors.toList());
            if (!overlappingGenes.isEmpty()) {
                final Gene largestGene = getLargestGene(overlappingGenes);
                assert largestGene != null : "There was no largest gene determined.";
                mergeOverlappingGenes(largestGene, overlappingGenes, jCas);
                // Mark all but the longest annotation for removal
                // We can use != here instead of equals, that's not a glitch
                overlappingGenes.stream().filter(x -> x != largestGene).forEach(toRemove::add);
            }
        }
        // Remove smaller overlapped genes.
        toRemove.forEach(Annotation::removeFromIndexes);
    }

    /**
     * Merge component IDs and resource entries into the longest gene annotation.
     * @param largestGene
     * @param overlappingGenes
     * @param jCas
     */
    private void mergeOverlappingGenes(Gene largestGene, List<Gene> overlappingGenes, JCas jCas) {
        String componentIds = overlappingGenes.stream().map(Annotation::getComponentId).distinct().collect(Collectors.joining(","));
        largestGene.setComponentId(componentIds);
        // resource entries contain the ID mapping
        final List<ResourceEntry> resourceEntries = overlappingGenes.stream().map(Gene::getResourceEntryList).filter(Objects::nonNull).flatMap(list -> Arrays.stream(list.toArray())).filter(Objects::nonNull).map(ResourceEntry.class::cast).collect(Collectors.toList());
        final FSArray resourceEntryList = new FSArray(jCas, resourceEntries.size());
        for (int i = 0; i < resourceEntries.size(); i++) {
            ResourceEntry re = resourceEntries.get(i);
            resourceEntryList.set(i, re);
        }
        largestGene.setResourceEntryList(resourceEntryList);
    }

    private Gene getLargestGene(Collection<Gene> genes) {
        int maxLength = -1;
        Gene maxGene = null;
        for (Gene g : genes) {
            int length = g.getEnd() - g.getBegin();
            if (length > maxLength) {
                maxLength = length;
                maxGene = g;
            }
        }
        return maxGene;
    }
}
