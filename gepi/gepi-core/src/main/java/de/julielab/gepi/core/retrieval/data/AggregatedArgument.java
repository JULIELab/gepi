package de.julielab.gepi.core.retrieval.data;

/**
 * Class to represent event arguments from ElasticSearch aggregations.
 *
 * There are no new fields or methods except a more appropriate constructor. The aggregation of events results in
 * very condensed information, only the preferred name of the top aggregate is known. No IDs. The usage of this
 * class should make clear where the event came from that has arguments of this type - an aggregation.
 */
public class AggregatedArgument extends Argument{
    public AggregatedArgument(String topAggregateName) {
        // aggregated events from the aggregationvalue ES field only know the highest aggregate name (FamPlex / HGNCG / gene orthology)
        super(null, null, null, null);
        setTopHomologyPreferredName(topAggregateName);
        // Since the top homology name is the only information we set, we also need to make it the comparison item.
        // Otherwise, all the arguments and, in consequence, the events will appear to be the same in HashMaps.
        setComparisonMode(ComparisonMode.TOP_HOMOLOGY_PREFERRED_NAME);
    }
}
