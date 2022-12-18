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
    @ExternalResource(key = "fplxhgncgtid2atiddirect")
    Map<String, String> fplxhgncgtid2atiddirect;
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
        // We use a unique filter to handle the FPLX/HGNCG duplication issue. The GePiFamplexIdAssigner concatenates FPLX/HGNCG IDs
        // when both databases have an entry for a entity. If we don't do de-duplication on the top aggregation level,
        // we end up with more than two arguments in such cases.
        fplxHgncConcatenatedIdSplitFilter = new RegExSplitFilter("---");
        // Due to the FPLX/HGNCG duplication issue (see comment above) we come up with more than two concept IDs in the
        // described case. The unique filter doesn't help here because the two concepts have to different conceptIds.
        // We thus replace the FPLX/HGNCG conceptIds - and only those, not gene IDs - with their aggregate. Because
        // when the two have the same name, then they also have an equal-name aggregate. While this shifts the
        // abstraction level sort of unwanted, this is a way out of the duplication issue.
        orgid2tidReplaceFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, new ReplaceFilter(orgid2tid), new ReplaceFilter(fplxhgncgtid2atiddirect), new UniqueFilter());
        conceptid2prefNameFilter = new ReplaceFilter(conceptid2prefName);
        orgid2tidAddonFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, new SingleAddonTermsFilter(orgid2tid));
        egid2taxidReplaceFilter = new ReplaceFilter(egid2taxid);
        tid2atidAddonFilter = new AddonTermsFilter(tid2atid);
        orgid2tid2atidAddonFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidAddonFilter, tid2atidAddonFilter);
        orgid2prefNameReplaceFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, conceptid2prefNameFilter, new UniqueFilter());
        orgid2topaggprefname = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, new ReplaceFilter(tid2topaggPrefName), conceptid2prefNameFilter, new UniqueFilter());
        orgid2topaggFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2tidReplaceFilter, new ReplaceFilter(tid2atiddirect), new UniqueFilter());
        eg2famplexFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2topaggFilter, new UniqueFilter(), new AddonTermsFilter(tid2famplex, true, true));
        eg2hgncFilter = new FilterChain(fplxHgncConcatenatedIdSplitFilter, orgid2topaggFilter, new UniqueFilter(), new AddonTermsFilter(tid2hgncgroups, true, true));
        eg2gotidFilter = new FilterChain(orgid2tidReplaceFilter, new AddonTermsFilter(genetid2gotid, true, true));
        gotid2gohypertidFilter = new AddonTermsFilter(gotid2hypertid);
        eg2gohypertidFilter = new FilterChain(eg2gotidFilter, gotid2gohypertidFilter);
        eg2goprefnameFilter = new FilterChain(eg2gotidFilter, conceptid2prefNameFilter);
    }
}
