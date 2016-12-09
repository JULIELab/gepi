package de.julielab.semedico.consumer;

import java.util.Map;
import java.util.Set;

import de.julielab.jules.consumer.elasticsearch.AbstractFilterBoard;
import de.julielab.jules.consumer.elasticsearch.ExternalResource;
import de.julielab.jules.consumer.esconsumer.filter.AdditionFilter;
import de.julielab.jules.consumer.esconsumer.filter.FilterChain;
import de.julielab.jules.consumer.esconsumer.filter.LatinTransliterationFilter;
import de.julielab.jules.consumer.esconsumer.filter.LowerCaseFilter;
import de.julielab.jules.consumer.esconsumer.filter.LuceneStandardTokenizerFilter;
import de.julielab.jules.consumer.esconsumer.filter.RegExSplitFilter;
import de.julielab.jules.consumer.esconsumer.filter.ReplaceFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoEventLikelihoodRemovalFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoFacetIdReplaceFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoTermFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoTermsHypernymsFilter;
import de.julielab.jules.consumer.esconsumer.filter.SnowballFilter;
import de.julielab.jules.consumer.esconsumer.filter.StopWordFilter;
import de.julielab.jules.consumer.esconsumer.filter.UniqueFilter;
import de.julielab.jules.consumer.esconsumer.filter.ValidWordFilter;

public class SemedicoFilterBoard extends AbstractFilterBoard {

	public static final String SEMEDICO_BOARD = "SemedicoFilterBoard";
	
	public final static String RESOURCE_HYPERNYMS = "Hypernyms";
	public final static String RESOURCE_MESH_TERM_MAPPING = "MeshTermMapping";
	public final static String RESOURCE_STOPWORDS = "Stopwords";
	public final static String RESOURCE_AGG_FACETS = "AggFacetIds";
	public static final String RESOURCE_EVENT_TERM_PATTERNS = "EventTermPatterns";
	public final static String RESOURCE_VALID_TERMS = "ValidTerms";
	public final static String RESOURCE_TERM_FACETS = "TermFacetIds";
	public final static String RESOURCE_ELEMENTS_AGGREGATES_ID_MAPPING = "ElementsAggregatesIdMapping";

	public StopWordFilter stopWordFilter;
	public SemedicoTermFilter semedicoTermFilter;
	/**
	 * Lowercasing, stopword-Removal, Stemming
	 */
	public FilterChain tokenFilterChain;
	/**
	 * Lowercasing and stemming, no stopword-removal.
	 */
	public FilterChain wordNormalizationChain;
	public FilterChain journalFilterChain;
	public SnowballFilter snowballFilter;
	@Deprecated
	public FilterChain eventFilterChain;
	@Deprecated
	public FilterChain facetRecommenderFilterChain;
	public SemedicoTermsHypernymsFilter hypernymsFilter;
	public ReplaceFilter meshTermReplaceFilter;
	public FilterChain meshFilterChain;
//	public ValidWordFilter validTermsFilter;
	@Deprecated
	public SemedicoFacetIdReplaceFilter eventTermCategoryReplaceFilter;
	public LatinTransliterationFilter latinTransliterationFilter;
	public ReplaceFilter elementsAggregateIdReplaceFilter;
	
	@ExternalResource(key=RESOURCE_HYPERNYMS, property="hypernyms")
	private String[][] hypernyms;
	
	@ExternalResource(key=RESOURCE_MESH_TERM_MAPPING)
	private Map<String, String> meshTermMapping;
	
	@ExternalResource(key=RESOURCE_STOPWORDS, methodName="getAsSet")
	private Set<String> stopwords;
	
	@ExternalResource(key=RESOURCE_ELEMENTS_AGGREGATES_ID_MAPPING)
	private Map<String, String> elementsToAggregatesIdMapping;

//	@ExternalResource(key=RESOURCE_AGG_FACETS)
//	public String[][] aggFacetIds;
//	
//	@ExternalResource(key=RESOURCE_EVENT_TERM_PATTERNS)
//	public Map<String, String> eventTermPatterns;
	
//	@ExternalResource(key=RESOURCE_VALID_TERMS, property="asSet")
//	private Set<String> validTerms;
	
//	@ExternalResource(key=RESOURCE_TERM_FACETS)
//	public String[][] termFacetIds;
	
	
//	@Override
//	public void addToRegistry(FilterRegistry filterRegistry) {
//		filterRegistry.addFilterBoard(SEMEDICO_BOARD, this);
//	}

	@Override
	public void setupFilters() {
		
		hypernymsFilter = new SemedicoTermsHypernymsFilter(hypernyms);
		meshTermReplaceFilter = new ReplaceFilter(meshTermMapping);
		stopWordFilter = new StopWordFilter(stopwords, true);
//		validTermsFilter = new ValidWordFilter(validTerms, false);
		semedicoTermFilter = new SemedicoTermFilter();
		snowballFilter = new SnowballFilter();
//		eventTermCategoryReplaceFilter = new SemedicoFacetIdReplaceFilter(eventTermPatterns, true);
		latinTransliterationFilter = new LatinTransliterationFilter(true);
//		RegExSplitFilter dashSplitFilter = new RegExSplitFilter("-");

		tokenFilterChain = new FilterChain();
//		tokenFilterChain.add(dashSplitFilter);
		tokenFilterChain.add(latinTransliterationFilter);
		tokenFilterChain.add(new LowerCaseFilter());
		tokenFilterChain.add(stopWordFilter);
		tokenFilterChain.add(new SnowballFilter());
		
		wordNormalizationChain = new FilterChain();
//		wordNormalizationChain.add(dashSplitFilter);
		wordNormalizationChain.add(latinTransliterationFilter);
		// this default stuff should be done by elasticsearch
//		wordNormalizationChain.add(new LowerCaseFilter());
//		wordNormalizationChain.add(new SnowballFilter());

		journalFilterChain = new FilterChain();
		journalFilterChain.add(new LuceneStandardTokenizerFilter());
		journalFilterChain.add(new LowerCaseFilter());
		journalFilterChain.add(stopWordFilter);
		journalFilterChain.add(snowballFilter);

		eventFilterChain = new FilterChain();
		eventFilterChain.add(new SemedicoEventLikelihoodRemovalFilter());
		eventFilterChain.add(new UniqueFilter());

		facetRecommenderFilterChain = new FilterChain();
		facetRecommenderFilterChain.add(new AdditionFilter("_n"));
		facetRecommenderFilterChain.add(new UniqueFilter());

		meshFilterChain = new FilterChain();
		meshFilterChain.add(meshTermReplaceFilter);
		meshFilterChain.add(semedicoTermFilter);
		meshFilterChain.add(hypernymsFilter);
		meshFilterChain.add(new UniqueFilter());
		
		elementsAggregateIdReplaceFilter = new ReplaceFilter(elementsToAggregatesIdMapping);
	}


	@Override
	public String getName() {
		return SEMEDICO_BOARD;
	}

}
