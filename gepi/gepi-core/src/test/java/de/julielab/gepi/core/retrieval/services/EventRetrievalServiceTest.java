package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventRetrievalServiceTest {
	@Test
	public void testReorderBipartiteEventResultArguments() throws Exception {
		EventRetrievalService retrievalService = new EventRetrievalService(null,
				LoggerFactory.getLogger(EventRetrievalService.class), null, null, null);
		Method method = retrievalService.getClass().getDeclaredMethod("reorderClosedEventResultArguments", Set.class,
				Set.class, EventRetrievalResult.class);
		method.setAccessible(true);

		Set<String> idsA = new HashSet<>(Arrays.asList("1", "2", "3"));
		Set<String> idsB = new HashSet<>(Arrays.asList("4", "5", "6"));

		EventRetrievalResult result = new EventRetrievalResult();
		Builder<Event> streamBuilder = Stream.builder();
		Event e = new Event();
		e.setArguments(Arrays.asList(getArg("4"), getArg("1")));
		streamBuilder.accept(e);
		
		result.setEvents(streamBuilder.build());
		
		method.invoke(retrievalService, idsA, idsB, result);
		
		assertEquals("1", result.getEventList().get(0).getFirstArgument().getGeneId());
		assertEquals("4", result.getEventList().get(0).getSecondArgument().getGeneId());
	}

	// ignore because we decided to break all events down to two arguments
	@Ignore
	@Test
	public void testReorderBipartiteEventResultArguments2() throws Exception {
		EventRetrievalService retrievalService = new EventRetrievalService(null, LoggerFactory.getLogger(EventRetrievalService.class),
				null, null, null);
		Method method = retrievalService.getClass().getDeclaredMethod("reorderClosedEventResultArguments", Set.class,
				Set.class, EventRetrievalResult.class);
		method.setAccessible(true);

		Set<String> idsA = new HashSet<>(Arrays.asList("1", "2", "3"));
		Set<String> idsB = new HashSet<>(Arrays.asList("4", "5", "6"));

		EventRetrievalResult result = new EventRetrievalResult();
		Builder<Event> streamBuilder = Stream.builder();
		Event e = new Event();
		e.setArguments(Arrays.asList(getArg("4"), getArg("1"), getArg("4")));
		streamBuilder.accept(e);
		
		result.setEvents(streamBuilder.build());
		
		method.invoke(retrievalService, idsA, idsB, result);
		
		assertEquals(2, result.getEventList().size());
		
		assertEquals("1", result.getEventList().get(0).getFirstArgument().getGeneId());
		assertEquals("4", result.getEventList().get(0).getSecondArgument().getGeneId());
		
		assertEquals("1", result.getEventList().get(1).getFirstArgument().getGeneId());
		assertEquals("4", result.getEventList().get(1).getSecondArgument().getGeneId());
	}

	// ignore because we decided to break all events down to two arguments
	@Ignore
	@Test
	public void testReorderBipartiteEventResultArguments3() throws Exception {
		EventRetrievalService retrievalService = new EventRetrievalService(null,
				LoggerFactory.getLogger(EventRetrievalService.class), null, null, null);
		Method method = retrievalService.getClass().getDeclaredMethod("reorderClosedEventResultArguments", Set.class,
				Set.class, EventRetrievalResult.class);
		method.setAccessible(true);

		Set<String> idsA = new HashSet<>(Arrays.asList("1", "2", "3"));
		Set<String> idsB = new HashSet<>(Arrays.asList("4", "5", "6"));

		EventRetrievalResult result = new EventRetrievalResult();
		Builder<Event> streamBuilder = Stream.builder();
		Event e = new Event();
		e.setArguments(Arrays.asList(getArg("4"), getArg("1"), getArg("3"), getArg("6")));
		streamBuilder.accept(e);
		
		result.setEvents(streamBuilder.build());
		
		method.invoke(retrievalService, idsA, idsB, result);
		
		assertEquals(4, result.getEventList().size());
		
		for (Event event : result.getEventList()) {
			assertTrue(idsA.contains(event.getFirstArgument().getGeneId()));
			assertTrue(idsB.contains(event.getSecondArgument().getGeneId()));
		}
	}

	private Argument getArg(String id) {
		return new Argument(id, null, "id: " + id, null);
	}
}
