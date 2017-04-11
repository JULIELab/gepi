package de.julielab.gepi.core.services;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.ImportModule;

import de.julielab.elastic.query.services.ElasticQueryComponentsModule;
import de.julielab.gepi.core.retrieval.services.EventResponseProcessingService;
import de.julielab.gepi.core.retrieval.services.EventRetrievalService;
import de.julielab.gepi.core.retrieval.services.IEventResponseProcessingService;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;

@ImportModule({ElasticQueryComponentsModule.class})
public class GepiCoreModule {
	public static void bind (ServiceBinder binder) {
		binder.bind(IEventRetrievalService.class, EventRetrievalService.class);
		binder.bind(IEventResponseProcessingService.class, EventResponseProcessingService.class);
		binder.bind(IGeneIdService.class, GeneIdService.class);
		binder.bind(IGoogleChartsDataManager.class, GoogleChartsDataManager.class);
	}
}
