package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;

public class RelationFilterBoard extends FilterBoard {

    @ExternalResource(key = "eventName2tid")
    private Map<String, String[]> eventName2tid;
    AddonTermsFilter eventName2tidAddonFilter;

    @Override
    public void setupFilters() {
        eventName2tidAddonFilter = new AddonTermsFilter(eventName2tid);
    }
}
