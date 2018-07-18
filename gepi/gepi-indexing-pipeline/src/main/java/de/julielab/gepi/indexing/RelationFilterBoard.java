package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;

public class RelationFilterBoard extends FilterBoard {

    ReplaceFilter eventName2tidReplaceFilter;
    @ExternalResource(key = "eventName2tid")
    private Map<String, String> eventName2tid;

    @Override
    public void setupFilters() {
        eventName2tidReplaceFilter = new ReplaceFilter(eventName2tid);

    }
}
