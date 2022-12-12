package de.julielab.gepi.core.retrieval.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.julielab.gepi.core.services.IdType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdConversionResult {
    private Map<String, IdType> inputIdTypeMapping;
    private IdType to;
    private Collection<String> sourceIds;
    private Multimap<String, String> convertedItems;
    private Set<String> unconvertedItems;

    /**
     * @param sourceIds The IDs or names provided by the user.
     * @param idMapping A mapping from user input ID to resolved target/search ID, i.e. concept or aggregate IDs.
     * @param inputIdTypeMapping
     * @param to The ID type that the user input has been mapped to for this IdConversionResult.
     */
    public IdConversionResult(Collection<String> sourceIds, Multimap<String, String> idMapping, Map<String, IdType> inputIdTypeMapping, @Deprecated IdType to) {
        this.convertedItems = idMapping;
        this.sourceIds = sourceIds;
        this.inputIdTypeMapping = inputIdTypeMapping;
        this.to = to;
    }

    public Map<String, IdType> getInputIdTypeMapping() {
        return inputIdTypeMapping;
    }

    public IdType getTypeOfInputId(String inputId) {
        if (!inputIdTypeMapping.containsKey(inputId))
            throw new IllegalArgumentException("The input user input element '" + inputId + "' is not contained in this IdConversionResult inputIdTypeMapping.");
        return inputIdTypeMapping.get(inputId);
    }

    public static IdConversionResult of(List<String> targetIds) {
        Multimap<String, String> idMapping = HashMultimap.create();
        for (String id : targetIds)
            idMapping.put("dummysrc", id);
        return new IdConversionResult(Collections.emptyList(), idMapping, Collections.emptyMap() , IdType.GEPI_AGGREGATE);
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



    public IdType getTo() {
        return to;
    }

    public void setTo(IdType to) {
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
