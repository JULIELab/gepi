package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class GeneFilterBoard extends FilterBoard {

    @ExternalResource(key = "egid2tid")
    Map<String, String[]> egid2tid;
    @ExternalResource(key = "tid2atid")
    Map<String, String[]> tid2atid;
    @ExternalResource(key = "tid2prefName")
    Map<String, String> tid2prefName;
    @ExternalResource(key = "tid2tophomo")
    Map<String, String> tid2tophomo;
    AddonTermsFilter egid2tidAddonFilter;
    Filter gene2tid2atidAddonFilter;
    Filter egid2prefNameReplaceFilter;
    ReplaceFilter eg2tidReplaceFilter;
    Filter eg2tophomoFilter;

    @Override
    public void setupFilters() {
        egid2tidAddonFilter = new AddonTermsFilter(egid2tid);
        gene2tid2atidAddonFilter = new FilterChain(egid2tidAddonFilter, new AddonTermsFilter(tid2atid));
        Map<String, String> egid2tidSimpleMap = egid2tid.entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue()[0]));
        egid2prefNameReplaceFilter = new FilterChain(new ReplaceFilter(egid2tidSimpleMap), new ReplaceFilter(tid2prefName));
        eg2tidReplaceFilter = new ReplaceFilter(egid2tidSimpleMap);
        eg2tophomoFilter = new FilterChain(eg2tidReplaceFilter, new ReplaceFilter(tid2tophomo));
    }
}
