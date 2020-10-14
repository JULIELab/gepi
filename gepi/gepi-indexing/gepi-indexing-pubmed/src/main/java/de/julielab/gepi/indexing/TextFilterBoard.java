package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;
import java.util.Set;

public class TextFilterBoard extends FilterBoard {


    @ExternalResource(key = "tid2atid")
    private Map<String, String[]> tid2atid;
    @ExternalResource(key = "stopwords")
    Set<String> stopwords;
    Filter textTokensFilter;
    AddonTermsFilter tid2atidAddonFilter;

    @Override
    public void setupFilters() {
        tid2atidAddonFilter = new AddonTermsFilter(tid2atid);
        textTokensFilter = new FilterChain(new RegExFilter("\\p{P}", true), new StopWordFilter(stopwords, true), new LuceneStandardTokenizerFilter(), new LowerCaseFilter(), new SnowballFilter());
    }
}
