package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

public class GeneFilterBoard extends FilterBoard {

    @ExternalResource(key = "egid2tid")
    Map<String, String> egid2tid;
    @ExternalResource(key = "tid2atid")
    Map<String, String[]> tid2atid;
    @ExternalResource(key = "tid2prefName")
    Map<String, String> tid2prefName;
    @ExternalResource(key = "tid2topHomologyPrefName")
    Map<String, String> tid2homoPrefName;
    @ExternalResource(key = "tid2tophomo")
    Map<String, String> tid2tophomo;
    @ExternalResource(key = "tid2famplex")
    Map<String, String[]> tid2famplex;
    @ExternalResource(key = "tid2hgncgroups")
    Map<String, String[]> tid2hgncgroups;
    SingleAddonTermsFilter egid2tidAddonFilter;
    Filter gene2tid2atidAddonFilter;
    Filter egid2prefNameReplaceFilter;
    ReplaceFilter eg2tidReplaceFilter;
    Filter eg2tophomoFilter;
    FilterChain egid2homoPrefNameReplaceFilter;
    Filter eg2famplexFilter;
    Filter eg2hgncFilter;

    @Override
    public void setupFilters() {
        egid2tidAddonFilter = new SingleAddonTermsFilter(egid2tid);
        gene2tid2atidAddonFilter = new FilterChain(egid2tidAddonFilter, new AddonTermsFilter(tid2atid));
        egid2prefNameReplaceFilter = new FilterChain(new ReplaceFilter(egid2tid), new ReplaceFilter(tid2prefName));
        egid2homoPrefNameReplaceFilter = new FilterChain(new ReplaceFilter(egid2tid), new ReplaceFilter(tid2homoPrefName));
        eg2tidReplaceFilter = new ReplaceFilter(egid2tid);
        eg2tophomoFilter = new FilterChain(eg2tidReplaceFilter, new ReplaceFilter(tid2tophomo));
        eg2famplexFilter = new FilterChain(eg2tophomoFilter, new AddonTermsFilter(tid2famplex));
        eg2hgncFilter = new FilterChain(eg2tophomoFilter, new AddonTermsFilter(tid2hgncgroups));
    }
}
