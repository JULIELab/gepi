package de.julielab.gepi.core.services;

import de.julielab.gepi.core.retrieval.services.*;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.ImportModule;

import de.julielab.elastic.query.services.ElasticQueryComponentsModule;

@ImportModule({ElasticQueryComponentsModule.class})
public class GepiCoreModule {
	public static void bind (ServiceBinder binder) {
		binder.bind(IEventRetrievalService.class, EventRetrievalService.class);
		binder.bind(IAggregatedEventsRetrievalService.class, AggregatedEventsRetrievalService.class);
		binder.bind(IEventResponseProcessingService.class, EventResponseProcessingService.class);
		binder.bind(IEventPostProcessingService.class, EventPostProcessingService.class);
		binder.bind(IGeneIdService.class, GeneIdService.class);
		binder.bind(IGePiDataService.class, GePiDataService.class);
	}
}
