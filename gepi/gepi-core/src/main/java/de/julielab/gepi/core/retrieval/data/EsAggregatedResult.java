package de.julielab.gepi.core.retrieval.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EsAggregatedResult {
    private Map<String, Integer> aSymbolFrequencies;
    private Map<String, Integer> bSymbolFrequencies;
    private Map<Event, Integer> eventFrequencies;
}
