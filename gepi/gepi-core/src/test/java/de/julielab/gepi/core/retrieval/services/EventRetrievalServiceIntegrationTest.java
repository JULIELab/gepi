package de.julielab.gepi.core.retrieval.services;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.GePiCoreTestModule;

// TODO this should be a self-contained integration test
//@Ignore
public class EventRetrievalServiceIntegrationTest {
	private static Registry registry;

	@BeforeClass
	public static void setup() {
		registry = RegistryBuilder.buildAndStartupRegistry(GePiCoreTestModule.class);
	}

	@AfterClass
	public static void shutdown() {
		registry.shutdown();
	}
	
	@Test
	public void testGetOutsideEvents() throws Exception {
		IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
		CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(Arrays.asList("5327").stream());
		assertTrue(0 < outsideEvents.get().getEventList().size());
		
		for (Event e : outsideEvents.get().getEventList()) {
			System.out.println(e.getDocumentId() + ", " + e.getDocumentType());
		}
	}
}
