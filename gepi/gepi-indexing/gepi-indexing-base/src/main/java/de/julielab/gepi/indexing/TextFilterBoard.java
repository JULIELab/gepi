package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Set;

public class TextFilterBoard extends FilterBoard {


    @ExternalResource(key = "stopwords")
    Set<String> stopwords;
    Filter textTokensFilter;

    @Override
    public void setupFilters() {
        textTokensFilter = new FilterChain(new RegExFilter("\\p{P}", true), new StopWordFilter(stopwords, true), new LuceneStandardTokenizerFilter(), new LowerCaseFilter(), new SnowballFilter());
    }
}
