package de.julielab.gepi.core.retrieval.services;

import java.util.List;

import de.julielab.gepi.core.retrieval.data.Event;

public interface IEventPostProcessingService {

	List<Event> getPreferredNameFromAtid(List<Event> event);
		
}
