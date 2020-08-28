package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GepiWidget {

    @Inject
    private Logger log;

    @Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
    @Property
    private String classes;

    @InjectComponent
    private GepiWidgetLayout gepiWidgetLayout;

    @InjectPage
    private Index index;

    public CompletableFuture<EventRetrievalResult> getEsResult() {
        try {
            log.debug("Trying to access index for ES result.");
            return index.getEsResult();
        } finally {
            log.debug("Retrieved ES result.");
        }
    }

    public CompletableFuture<AggregatedEventsRetrievalResult> getNeo4jResult() {
        try {
            log.debug("Trying to access index for Neo4j result.");
            return index.getNeo4jResult();
        } finally {
            log.debug("Retrieved Neo4j result.");
        }
    }

    public boolean isLargeView() {
        final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
        return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
    }
}
