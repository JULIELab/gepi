package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.services.IGeneIdService;

import java.util.*;
import java.util.stream.Collectors;

import static de.julielab.gepi.core.services.IGeneIdService.IdType.UNKNOWN;

public class IdConversionResult {
    private IGeneIdService.IdType from;
    private IGeneIdService.IdType to;
    private List<String> sourceIds;
    private Map<String, String> convertedItems;
    private Set<String> unconvertedItems;

    public IdConversionResult(List<String> sourceIds, Map<String, String> idMapping, IGeneIdService.IdType from, IGeneIdService.IdType to) {
        convertedItems = idMapping;
        this.sourceIds = sourceIds;
        this.from = from;
        this.to = to;
    }

    public static IdConversionResult of(List<String> targetIds) {
        Map<String, String> idMapping = new HashMap<>();
        for (String id : targetIds)
            idMapping.put("dummysrc", id);
        return new IdConversionResult(Collections.emptyList(), idMapping, UNKNOWN, IGeneIdService.IdType.GEPI_AGGREGATE);
    }

    public static IdConversionResult of(String... targetIds) {
        List<String> ids = List.of(targetIds);
        return of(ids);
    }

    public Set<String> getTargetIds() {
        return convertedItems.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
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

    public Map<String, String> getConvertedItems() {
        return convertedItems;
    }

    public void setConvertedItems(Map<String, String> convertedItems) {
        this.convertedItems = convertedItems;
    }

    public Set<String> getUnconvertedItems() {
        return unconvertedItems;
    }

    public void setUnconvertedItems(Set<String> unconvertedItems) {
        this.unconvertedItems = unconvertedItems;
    }
}
