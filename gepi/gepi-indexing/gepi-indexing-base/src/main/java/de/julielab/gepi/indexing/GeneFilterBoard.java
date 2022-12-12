package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class GeneFilterBoard extends FilterBoard {

    @ExternalResource(key = "egid2tid")
    Map<String, String> egid2tid;
    @ExternalResource(key = "geneids2taxids")
    Map<String, String> egid2taxid;
    @ExternalResource(key = "tid2atid")
    Map<String, String[]> tid2atid;
    @ExternalResource(key = "conceptid2prefName")
    Map<String, String> conceptid2prefName;
    @ExternalResource(key = "tid2topHomologyPrefName")
    Map<String, String> tid2homoPrefName;
    @ExternalResource(key = "tid2tophomo")
    Map<String, String> tid2tophomo;
    @ExternalResource(key = "tid2famplex")
    Map<String, String[]> tid2famplex;
    @ExternalResource(key = "tid2hgncgroups")
    Map<String, String[]> tid2hgncgroups;
    @ExternalResource(key = "genetid2gotid")
    Map<String, String[]> genetid2gotid;
    @ExternalResource(key = "gotid2hypertid")
    Map<String, String[]> gotid2hypertid;
    SingleAddonTermsFilter egid2tidAddonFilter;
    Filter gene2tid2atidAddonFilter;
    Filter egid2prefNameReplaceFilter;
    ReplaceFilter eg2tidReplaceFilter;
    Filter eg2tophomoFilter;
    FilterChain egid2homoPrefNameReplaceFilter;
    Filter eg2famplexFilter;
    Filter eg2hgncFilter;
    Filter conceptid2prefNameFilter;
    FilterChain eg2gotidFilter;
    Filter gotid2gohypertidFilter;
    Filter eg2gohypertidFilter;
    Filter egid2taxidReplaceFilter;
    Filter eg2goprefnameFilter;

    @Override
    public void setupFilters() {
        conceptid2prefNameFilter = new ReplaceFilter(conceptid2prefName);
        egid2tidAddonFilter = new SingleAddonTermsFilter(egid2tid);
        egid2taxidReplaceFilter = new ReplaceFilter(egid2taxid);
        gene2tid2atidAddonFilter = new FilterChain(egid2tidAddonFilter, new AddonTermsFilter(tid2atid));
        egid2prefNameReplaceFilter = new FilterChain(new ReplaceFilter(egid2tid), new ReplaceFilter(conceptid2prefName));
        egid2homoPrefNameReplaceFilter = new FilterChain(new ReplaceFilter(egid2tid), new ReplaceFilter(tid2homoPrefName),conceptid2prefNameFilter);
        eg2tidReplaceFilter = new ReplaceFilter(egid2tid);
        eg2tophomoFilter = new FilterChain(eg2tidReplaceFilter, new ReplaceFilter(tid2tophomo));
        eg2famplexFilter = new FilterChain(eg2tophomoFilter, new AddonTermsFilter(tid2famplex, true, true));
        eg2hgncFilter = new FilterChain(eg2tophomoFilter, new AddonTermsFilter(tid2hgncgroups, true, true));
        eg2gotidFilter = new FilterChain(eg2tidReplaceFilter, new AddonTermsFilter(genetid2gotid, true));
        gotid2gohypertidFilter = new AddonTermsFilter(gotid2hypertid);
        eg2gohypertidFilter = new FilterChain(eg2gotidFilter, gotid2gohypertidFilter);
        eg2goprefnameFilter = new FilterChain(eg2gotidFilter, conceptid2prefNameFilter);
    }
}
