package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.*;
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
//    protected long dataSessionId;


    @InjectPage
    private Index index;

    public Future<EventRetrievalResult> getPagedEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getPagedResult();
    }

    public Future<EventRetrievalResult> getUnrolledResult4charts() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4charts();
    }

    public Future<EventRetrievalResult> getUnrolledResult4download() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4download().get();
    }

    public Future<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(requestData.getDataSessionId()).getAggregatedResult();
    }

    public boolean isLargeView() {
        final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
        return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
    }

    public String getChartAreaColumnSizeClass() {
        return isLargeView() ? "col-10" : "col-12";
    }
}
