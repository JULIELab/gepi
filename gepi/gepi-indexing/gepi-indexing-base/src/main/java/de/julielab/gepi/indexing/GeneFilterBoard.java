package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ExternalResource;
import de.julielab.jcore.consumer.es.FilterBoard;
import de.julielab.jcore.consumer.es.filter.*;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class GeneFilterBoard extends FilterBoard {

    @ExternalResource(key = "orgid2tid")
    Map<String, String> orgid2tid;
    @ExternalResource(key = "geneids2taxids")
    Map<String, String> egid2taxid;
    @ExternalResource(key = "tid2atid")
    Map<String, String[]> tid2atid;
    @ExternalResource(key = "conceptid2prefName")
    Map<String, String> conceptid2prefName;
    @ExternalResource(key = "tid2topaggprefname")
    Map<String, String> tid2topaggPrefName;
    @ExternalResource(key = "tid2atiddirect")
    Map<String, String> tid2atiddirect;
    @ExternalResource(key = "tid2famplex")
    Map<String, String[]> tid2famplex;
    @ExternalResource(key = "tid2hgncgroups")
    Map<String, String[]> tid2hgncgroups;
    @ExternalResource(key = "genetid2gotid")
    Map<String, String[]> genetid2gotid;
    @ExternalResource(key = "gotid2hypertid")
    Map<String, String[]> gotid2hypertid;
    Filter orgid2tidAddonFilter;
    Filter orgid2tid2atidAddonFilter;
    Filter orgid2prefNameReplaceFilter;
    Filter orgid2tidReplaceFilter;
    Filter orgid2topaggFilter;
    FilterChain orgid2topaggprefname;
    Filter eg2famplexFilter;
    Filter eg2hgncFilter;
    Filter conceptid2prefNameFilter;
    FilterChain eg2gotidFilter;
    Filter gotid2gohypertidFilter;
    Filter eg2gohypertidFilter;
    Filter egid2taxidReplaceFilter;
    Filter eg2goprefnameFilter;
    Filter tid2atidAddonFilter;
    Filter fplxHgncConcatenatedIdSplitFilter;

    @Override
    public void setupFilters() {
        fplxHgncConcatenatedIdSplitFilter = new RegExSplitFilter("---");
        orgid2tidReplaceFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, new ReplaceFilter(orgid2tid));
        conceptid2prefNameFilter = new ReplaceFilter(conceptid2prefName);
        orgid2tidAddonFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, new SingleAddonTermsFilter(orgid2tid));
        egid2taxidReplaceFilter = new ReplaceFilter(egid2taxid);
        tid2atidAddonFilter = new AddonTermsFilter(tid2atid);
        orgid2tid2atidAddonFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidAddonFilter, tid2atidAddonFilter);
        orgid2prefNameReplaceFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, conceptid2prefNameFilter);
        orgid2topaggprefname = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, new ReplaceFilter(tid2topaggPrefName), conceptid2prefNameFilter);
        orgid2topaggFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, new ReplaceFilter(tid2atiddirect));
        eg2famplexFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2topaggFilter, new AddonTermsFilter(tid2famplex, true, true));
        eg2hgncFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2topaggFilter, new AddonTermsFilter(tid2hgncgroups, true, true));
        eg2gotidFilter = new FilterChain(orgid2tidReplaceFilter, new AddonTermsFilter(genetid2gotid, true, true));
        gotid2gohypertidFilter = new AddonTermsFilter(gotid2hypertid);
        eg2gohypertidFilter = new FilterChain(eg2gotidFilter, gotid2gohypertidFilter);
        eg2goprefnameFilter = new FilterChain(eg2gotidFilter, conceptid2prefNameFilter);
    }
}
