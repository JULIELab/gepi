package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.ConceptName;
import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;
public interface IGeneIdService {
    Map<String, ConceptName> getAggregationRelevantConceptNames(Iterable<String> conceptIds);

	Set<String> getPossibleAggregationConceptNames(Iterable<String> conceptIds);

	Future<IdConversionResult> convert(Stream<String> stream, IdType from, IdType to);

	Future<IdConversionResult> convert(Stream<String> stream, IdType to);

	CompletableFuture<Multimap<String, String>> convertConceptNames2AggregateIds(Stream<String> geneNames);
	/**
	 * Tries to figure out which IDs are given.
	 *
	 * @param idStream
	 * @return
	 */
	IdType determineIdType(Stream<String> idStream);


	Multimap<IdType, String> determineIdTypes(Stream<String> idStream);

	Future<Multimap<String, String>> convert2Gepi(Stream<String> input, String label, String originalId);

	/**
	 * Upon given IDs or gene names converts the input into atids that resemble
	 * the top homology atid of the respective gene
	 *
	 * @param input
	 * @param label
	 * @param originalId
	 * @return
	 */
	CompletableFuture<Multimap<String, String>> convertConcept2AggregateGepiIds(Stream<String> input, String label, String originalId);

	Map<String, GepiConceptInfo> getGeneInfo(Iterable<String> conceptIds);

    Set<String> getGeneAggregateSymbolsForFamilyConcepts(Iterable<? extends String> conceptIds, String propertyName);

    Set<String> getFamilyAndOrthologyGroupNodeProperties(Iterable<? extends String> conceptIds, String propertyName);
}
