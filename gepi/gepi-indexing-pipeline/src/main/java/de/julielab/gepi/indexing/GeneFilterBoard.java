package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;
import java.util.Set;

public class GeneFilterBoard extends FilterBoard {

    @ExternalResource(key = "egid2tid")
    Map<String, String[]> egid2tid;
    @ExternalResource(key = "tid2atid")
    Map<String, String[]> tid2atid;
    AddonTermsFilter egid2tidAddonFilter;
    Filter gene2tid2atidAddonFilter;

    @Override
    public void setupFilters() {
        egid2tidAddonFilter = new AddonTermsFilter(egid2tid);
        gene2tid2atidAddonFilter = new FilterChain(egid2tidAddonFilter, new AddonTermsFilter(tid2atid));
    }
}
