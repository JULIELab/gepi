package de.julielab.gepi.webapp;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.webapp.data.FilteredGepiRequestData;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EventPagesDataSource implements GridDataSource {
    private final IEventRetrievalService eventRetrievalService;
    private final FilteredGepiRequestData requestData;
    private Logger log;
    private Future<EventRetrievalResult> events;
    private int start;

    public EventPagesDataSource(Logger log, Future<EventRetrievalResult> events, IEventRetrievalService eventRetrievalService, FilteredGepiRequestData requestData) {
        this.log = log;
        this.events = events;
        this.eventRetrievalService = eventRetrievalService;
        this.requestData = requestData;
    }

    @Override
    public int getAvailableRows() {
        int availableRows = 0;
        try {
            availableRows = (int) events.get().getNumTotalRows();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        log.debug("Available rows: " + availableRows);
        return availableRows;
    }

    @Override
    public void prepare(int i, int i1, List<SortConstraint> list) {
        try {
            if (events.get().getStartRow() != i || events.get().getEndRow() != i1) {
                events = eventRetrievalService.getEvents(requestData, i, i1 - i + 1, false);
                log.debug("Received {} events where {} events were requested. From {} to {}.", events.get().getEventList().size(), i1 - i + 1, i1, i);
            } else {
                log.debug("Used {} events from the existing result where {} events were requested. From {} to {}.", events.get().getEventList().size(), i1 - i + 1, i, i1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        start = i;
    }

    @Override
    public Object getRowValue(int i) {
        try {
            return new BeanModelEvent(events.get().getEventList().get(i - start));
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Class getRowType() {
        return BeanModelEvent.class;
    }
}
