package de.julielab.gepi.core.retrieval.services;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.GePiCoreTestModule;

public class EventRetrievalServiceTest {
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
	public void testGetOutsideEvents() {
		IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
		EventRetrievalResult outsideEvents = eventRetrievalService.getOutsideEvents(Arrays.asList("5327").stream());
		assertTrue(0 < outsideEvents.getEvents().count());
	}
}
