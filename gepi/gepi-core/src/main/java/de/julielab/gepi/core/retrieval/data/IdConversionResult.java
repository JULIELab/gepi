package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.services.IGeneIdService;

import java.util.*;

import static de.julielab.gepi.core.services.IGeneIdService.IdType.UNKNOWN;

public class IdConversionResult {
    public static IdConversionResult of(List<String> targetIds) {
        return new IdConversionResult(targetIds, targetIds, UNKNOWN, IGeneIdService.IdType.GEPI_AGGREGATE);
    }
    public static IdConversionResult of(String... targetIds) {
        List<String> ids = List.of(targetIds);
        return of(ids);
    }
    private IGeneIdService.IdType from;
    private IGeneIdService.IdType to;
    private List<String> sourceIds;

    public IdConversionResult(List<String> sourceIds, List<String> targetIds, IGeneIdService.IdType from, IGeneIdService.IdType to) {
        convertedItems = new HashMap<>();
        for (String id : targetIds)
            convertedItems.put("dummysrc", id);
        this.sourceIds = sourceIds;
        this.from = from;
        this.to = to;
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
