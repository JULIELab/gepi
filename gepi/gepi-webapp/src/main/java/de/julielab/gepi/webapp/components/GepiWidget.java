package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.data.Neo4jAggregatedEventsRetrievalResult;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.concurrent.Future;

public class GepiWidget {

    @Inject
    private Logger log;

    @Inject
    private IGePiDataService dataService;

    @Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
    @Property
    private String classes;

    @InjectComponent
    private GepiWidgetLayout gepiWidgetLayout;

    @Parameter
    @Property
    protected GepiRequestData requestData;


    @InjectPage
    private Index index;

    public Future<EventRetrievalResult> getPagedEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getPagedResult();
    }

    public Future<EventRetrievalResult> getUnrolledResult4charts() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4charts();
    }

    public Future<EsAggregatedResult> getEsAggregatedResult() {
        return dataService.getData(requestData.getDataSessionId()).getEsAggregatedResult();
    }

    public Future<EventRetrievalResult> getUnrolledResult4download() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4download().get();
    }

    public Future<Neo4jAggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(requestData.getDataSessionId()).getNeo4jAggregatedResult();
    }

    public boolean isLargeView() {
        final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
        return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
    }
}
