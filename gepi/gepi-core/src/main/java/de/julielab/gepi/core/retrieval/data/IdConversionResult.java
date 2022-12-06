package de.julielab.gepi.core.retrieval.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.julielab.gepi.core.services.IGeneIdService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdConversionResult {
    private IGeneIdService.IdType to;
    private Collection<String> sourceIds;
    private Multimap<String, String> convertedItems;
    private Set<String> unconvertedItems;
    public IdConversionResult(Collection<String> sourceIds, Multimap<String, String> idMapping, IGeneIdService.IdType to) {
        convertedItems = idMapping;
        this.sourceIds = sourceIds;
        this.to = to;
    }

    public static IdConversionResult of(List<String> targetIds) {
        Multimap<String, String> idMapping = HashMultimap.create();
        for (String id : targetIds)
            idMapping.put("dummysrc", id);
        return new IdConversionResult(Collections.emptyList(), idMapping, IGeneIdService.IdType.GEPI_AGGREGATE);
    }

    public static IdConversionResult of(String... targetIds) {
        List<String> ids = List.of(targetIds);
        return of(ids);
    }

    public Collection<String> getSourceIds() {
        return sourceIds;
    }

    public Set<String> getTargetIds() {
        return convertedItems.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }



    public IGeneIdService.IdType getTo() {
        return to;
    }

    public void setTo(IGeneIdService.IdType to) {
        this.to = to;
    }

    public Multimap<String, String> getConvertedItems() {
        return convertedItems;
    }

    public void setConvertedItems(Multimap<String, String> convertedItems) {
        this.convertedItems = convertedItems;
    }

    public Stream<String> getUnconvertedItems() {
        return sourceIds.stream().filter(Predicate.not(convertedItems::containsKey));
    }

    public void setUnconvertedItems(Set<String> unconvertedItems) {
        this.unconvertedItems = unconvertedItems;
    }
}
