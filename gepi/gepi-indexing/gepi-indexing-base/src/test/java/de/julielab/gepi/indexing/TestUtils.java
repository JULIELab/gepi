package de.julielab.gepi.indexing;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.List;

public class TestUtils {
    public static ArgumentMention createGeneArgument(JCas jCas, int begin, int end, String... ids) {
        Gene g = new Gene(jCas, begin, end);
        g.setComponentId("SomeGeneTagger");
        for (String id : ids) {
            ResourceEntry re = new ResourceEntry(jCas);
            re.setEntryId(id);
            re.setComponentId("SomeNormalizer");
            FSArray resourceEntryList = JCoReTools.addToFSArray(g.getResourceEntryList(), re);
            g.setResourceEntryList(resourceEntryList);
        }
        g.addToIndexes();
        ArgumentMention am = new ArgumentMention(jCas, begin, end);
        am.setRef(g);
        am.addToIndexes();
        return am;
    }

    public static ArgumentMention createGeneArgument(JCas jCas, int begin, int end, String geneComponentId, List<String> ids, List<String> resourceEntryComponentIds) {
        Gene g = new Gene(jCas, begin, end);
        g.setComponentId(geneComponentId);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            ResourceEntry re = new ResourceEntry(jCas);
            re.setEntryId(id);
            re.setComponentId(resourceEntryComponentIds.get(i));
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
