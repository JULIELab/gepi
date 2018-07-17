package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GepiFilterBoard extends FilterBoard {

    private AddonTermsFilter geneToDatabaseIdsFilter;
    Filter geneDatabaseIdsToAggregatesFilter;
    @ExternalResource(key = "geneToDatabaseIds")
    Map<String, String[]> geneToDatabaseIds;
    @ExternalResource(key = "geneDatabaseIdsToAggregates")
    Map<String, String[]> geneDatabaseIdsToAggregates;
    @ExternalResource(key = "stopwords")
    Set<String> stopwords;
    Filter unwantedTokensFilter;
    Filter textTokensFilter;

    @Override
    public void setupFilters() {
        geneToDatabaseIdsFilter = new AddonTermsFilter(geneToDatabaseIds);
        geneDatabaseIdsToAggregatesFilter = new FilterChain(geneToDatabaseIdsFilter, new AddonTermsFilter(geneDatabaseIdsToAggregates));
        unwantedTokensFilter = new FilterChain(new RegExFilter("\\p{P}", true), new StopWordFilter(stopwords, true));
        textTokensFilter = new FilterChain(new RegExFilter("\\p{P}", true), new StopWordFilter(stopwords, true), new LuceneStandardTokenizerFilter(), new LowerCaseFilter(), new SnowballFilter());
    }
}
