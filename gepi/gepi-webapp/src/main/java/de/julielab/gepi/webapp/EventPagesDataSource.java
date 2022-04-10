package de.julielab.gepi.webapp;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EventPagesDataSource implements GridDataSource {
    private final IEventRetrievalService eventRetrievalService;
    private final GepiRequestData requestData;
    private CompletableFuture<EventRetrievalResult> events;
    private int start;

    public EventPagesDataSource(IEventRetrievalService eventRetrievalService, GepiRequestData requestData) {
        this.eventRetrievalService = eventRetrievalService;
        this.requestData = requestData;
    }

    @Override
    public int getAvailableRows() {
        int availableRows = 0;
        try {
            availableRows = (int) eventRetrievalService.getEvents(requestData, 0, 0).get().getNumTotalRows();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
//        System.out.println("Available rows: " + availableRows);
        return availableRows;
    }

    @Override
    public void prepare(int i, int i1, List<SortConstraint> list) {
        // TODO support the sort constraints
        events = eventRetrievalService.getEvents(requestData, i, i1-i+1);
        try {
            System.out.println("Received " + events.get().getEventList().size() + " events where " + (i1-i+1) + " events were requested.");
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
            System.out.println("Get row value " + i + " in list of size " + events.get().getEventList().size());
            return new BeanModelEvent(events.get().getEventList().get(i-start));
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Class getRowType() {
        return BeanModelEvent.class;
    }
}
