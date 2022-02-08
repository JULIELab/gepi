package de.julielab.gepi.webapp.pages;

import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.webapp.EventPagesDataSource;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;

public class GridTest {

    public void setRequestData(GepiRequestData requestData) {
        this.requestData = requestData;
    }

    @Persist(PersistenceConstants.FLASH)
    private GepiRequestData requestData;

    @Property
    private GridDataSource events;

    @Inject
    private IEventRetrievalService eventRetrievalService;


    void setupRender() {
        events = new EventPagesDataSource(eventRetrievalService, requestData);
    }
}
