package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.services.IGeneIdService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdConversionResult {
    private IGeneIdService.IdType from;
    private IGeneIdService.IdType to;

    public IdConversionResult(List<String> sourceIds, List<String> targetIds, IGeneIdService.IdType from, IGeneIdService.IdType to) {
        // TODO
    }

    public IGeneIdService.IdType getFrom() {
        return from;
    }

    public void setFrom(IGeneIdService.IdType from) {
        this.from = from;
    }

    public IGeneIdService.IdType getTo() {
        return to;
    }

    public void setTo(IGeneIdService.IdType to) {
        this.to = to;
    }

    private Map<String, String> convertedItems;
    private Set<String> unconvertedITems;

    public Map<String, String> getConvertedItems() {
        return convertedItems;
    }

    public void setConvertedItems(Map<String, String> convertedItems) {
        this.convertedItems = convertedItems;
    }

    public Set<String> getUnconvertedITems() {
        return unconvertedITems;
    }

    public void setUnconvertedITems(Set<String> unconvertedITems) {
        this.unconvertedITems = unconvertedITems;
    }
}
