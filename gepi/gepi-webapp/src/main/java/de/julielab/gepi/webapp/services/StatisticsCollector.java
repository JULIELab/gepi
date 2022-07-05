package de.julielab.gepi.webapp.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.ElasticSearchCarrier;
import de.julielab.elastic.query.components.data.ElasticServerResponse;
import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.elastic.query.components.data.query.MatchAllQuery;
import de.julielab.elastic.query.services.IElasticServerResponse;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.services.IEventResponseProcessingService;
import de.julielab.gepi.webapp.data.GepiEventStatistics;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

public class StatisticsCollector implements IStatisticsCollector{
    private final String documentIndex;
    private final Logger log;
    private final ISearchServerComponent searchServerComponent;
    private boolean initialized;

    private GepiEventStatistics stats;

    public StatisticsCollector(@Symbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS) String documentIndex, Logger log,
                               ISearchServerComponent searchServerComponent) {
        this.documentIndex = documentIndex;
        this.log = log;
        this.searchServerComponent = searchServerComponent;
    }

    @Override
    public void run() {
        GepiEventStatistics newStats = new GepiEventStatistics();
        getNumTotalInteractions(newStats);



        this.stats = newStats;
        initialized = true;
    }

    private void getNumTotalInteractions(GepiEventStatistics stats) {
        SearchServerRequest serverRequest = new SearchServerRequest();
        serverRequest.query = new MatchAllQuery();
        serverRequest.index = documentIndex;
        serverRequest.rows = 0;
        serverRequest.isCountRequest = true;

        ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier<>("BipartiteEvents");
        carrier.addSearchServerRequest(serverRequest);
        searchServerComponent.process(carrier);

        final IElasticServerResponse response = carrier.getSingleSearchServerResponse();
        stats.setNumTotalInteractions(response.getNumFound());
    }

    @Override
    public GepiEventStatistics getStats() {
        return stats;
    }
}
