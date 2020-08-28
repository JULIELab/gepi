package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.GePiDataService;
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
    private long dataSessionId;

    @InjectPage
    private Index index;

    public Future<EventRetrievalResult> getEsResult() {
        return dataService.getData(dataSessionId).getUnrolledResult();
    }

    public Future<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(dataSessionId).getAggregatedResult();
    }

    public boolean isLargeView() {
        final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
        return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
    }
}
