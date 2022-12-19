package de.julielab.gepi.webapp;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.services.EventRetrievalService;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.data.FilteredGepiRequestData;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import static de.julielab.gepi.core.services.GeneIdService.CONCEPT_ID_PATTERN;

public class EventPagesDataSource implements GridDataSource {
    private final IEventRetrievalService eventRetrievalService;
    private final FilteredGepiRequestData requestData;
    private IGeneIdService geneIdService;
    private Logger log;
    private Future<EventRetrievalResult> events;
    private int start;

    public EventPagesDataSource(Logger log, Future<EventRetrievalResult> events, IEventRetrievalService eventRetrievalService, IGeneIdService geneIdService, FilteredGepiRequestData requestData) {
        this.log = log;
        this.events = events;
        this.eventRetrievalService = eventRetrievalService;
        this.geneIdService = geneIdService;
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
                final EventRetrievalResult eventRetrievalResult = events.get();
                // Retrieving all geneInfos at once only requires a single call to Neo4j instead of one call for
                // each argument. Since we use an internal cache, the geneInfo items can then be accessed in the TableResultWidget
                // where we need to know the labels.
                long time = System.currentTimeMillis();
                // Catch the case where the ID is "none" due to unary event or because of an Event.EMPTY due to an exception.
                // Also catch gene IDs that GNormPlus assigned but which are not in our database because of discontinuation and thus could not be mapped to a concept ID during indexing.
                geneIdService.getGeneInfo(() -> eventRetrievalResult.getEventList().stream().map(Event::getArguments).flatMap(Collection::stream).map(Argument::getConceptId).filter(Predicate.not(id -> EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT.equals(id))).filter(Objects::nonNull).filter(Predicate.not(String::isBlank)).filter(id -> CONCEPT_ID_PATTERN.matcher(id).matches()).iterator());
                time = System.currentTimeMillis() - time;
                log.debug("Pre-fetched the geneInfo of arguments for {} events in ms", events.get().getEventList().size(), time);
                log.debug("Received {} events where {} events were requested. From {} to {}.", events.get().getEventList().size(), i1 - i + 1, i, i1);
                log.info("Returning events from {} to {}", i, i1);
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
        } catch (Exception e) {
            log.error("Error when trying to retrieve table row {} with start value {}. Returning empty event as a surrogate", i, start, e);
        }
        return new BeanModelEvent(Event.EMPTY);
    }

    @Override
    public Class getRowType() {
        return BeanModelEvent.class;
    }
}
