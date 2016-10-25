package de.julielab.semedico.consumer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jules.utility.JulesFeaturePath;
import de.julielab.neo4j.plugins.TermManager;
import de.julielab.neo4j.plugins.datarepresentation.JsonSerializer;

/**
 * Sends term writing variants to the Neo4j Semedico term database using a Neo4j
 * server plugin. The address of the plugin is hardcoded into this component.
 * 
 * WARNING: Uses a static cache, it is NOT possible to deploy multiple,
 * differently configured AEs of this class into the same pipeline running in
 * the same JVM! If that would be necessary, the sharing of the cache could
 * probably be archieved by a SharedResource object (the issue is just that it
 * seems one has always to deliver a "resource file" which doesn't exist in this
 * case; one could use a dummy...)
 * 
 * @author faessler
 * 
 */
public class ConceptTermToNeo4jConsumer extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(ConceptTermToNeo4jConsumer.class);

	public final static String PARAM_TYPES_AND_ID_FEATURE = "Types";
	@Deprecated
	public final static String PARAM_CONCURRENCY_LEVEL = "ConcurrencyLevel";
	public final static String PARAM_INIT_CAPACITY = "InitialCacheCapacity";
	@Deprecated
	public final static String PARAM_MAX_CACHE_SIZE = "MaxCacheSize";
	public final static String PARAM_NEO4J_ADDRESS = "Neo4jAddress";
	public final static String PARAM_ENABLE_ACRONYMS = "EnableAcronyms";
	public final static String PARAM_ENABLE_WRITING_VARIANTS = "EnableWritingVariants";
	public final static String PARAM_INCLUDE_ACRONYMS_IN_VARIANTS = "IncludeAcronymsInVariants";

	@Deprecated
	private static Cache<String, Set<String>> cache = null;

	@ConfigurationParameter(name = PARAM_TYPES_AND_ID_FEATURE, description = "An array that maps type system type names to their respective concept ID feature path. Examples: de.julielab.jules.types.Gene=/resourceEntryList/entryId or de.julielab.jules.types.OntMention=/specificType")
	private String[] types;
	@ConfigurationParameter(name = PARAM_CONCURRENCY_LEVEL, mandatory = false, description = "An optional setting for the cache used internally to avoid unnecessary submissions of already known term writing variants when possible. If not set, defaults to 4.", defaultValue = "4")
	private static Integer concurrencyLevel;
	@ConfigurationParameter(name = PARAM_NEO4J_ADDRESS, description = "The protocol, host and port parts of the Neo4j server URL. E.g. http://myhost:7474")
	private String neo4jAddress;
	@ConfigurationParameter(name = PARAM_INIT_CAPACITY, mandatory = false, defaultValue = "1000", description = "Initial size of the concept term variant cache. The size is the number of terms for which writing variants are cached. Defaults to 1000.")
	private Integer initCapacity;
	@ConfigurationParameter(name = PARAM_MAX_CACHE_SIZE, mandatory = false, defaultValue = "100000", description = "The maximum size of the concept term variant cache before unfrequently used entries are evicted from the cache. Cache size denotes the number of concepts for which writing variants are cached. Defaults to 100000.")
	private Integer maxCacheSize;
	@ConfigurationParameter(name = PARAM_ENABLE_ACRONYMS)
	private Boolean acronymsEnabled;
	@ConfigurationParameter(name = PARAM_ENABLE_WRITING_VARIANTS)
	private Boolean writingVariantsEnabled;
	@ConfigurationParameter(name = PARAM_INCLUDE_ACRONYMS_IN_VARIANTS)
	private Boolean acronymsIncludedInWritingVariants;

	private Map<Type, JulesFeaturePath> fpMap;
	@Deprecated
	private Map<String, List<String>> newVariantMap;

	/**
	 * Maps concept IDs to their writing variants (possibly includes acronyms)
	 * and the observed number of occurrences for each variant per document:
	 * Map&lt;TermId, Map&lt;DocId, Map&lt;Variants, Count&gt;&gt;&gt;
	 */
	private Map<String, Map<String, Map<String,Integer>>> variantsMap;
	/**
	 * Maps concept IDs to their acronyms and the observed number of occurrences
	 * for each acronym per document:
	 * Map&lt;TermId, Map&lt;DocId, Map&lt;Acronyms, Count&gt;&gt;&gt;
	 */
	private Map<String, Map<String, Map<String,Integer>>> acronymsMap;

	private HttpClient client;

	public static final String ADD_WRITING_VARIANTS_ENDPOINT = "db/data/ext/" + TermManager.class.getSimpleName()
			+ "/graphdb/add_term_variants";
	private int numNewVariants = 0;
	private int numNewAcronyms = 0;

	private String authorizationToken;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		types = (String[]) aContext.getConfigParameterValue(PARAM_TYPES_AND_ID_FEATURE);
		neo4jAddress = ((String) aContext.getConfigParameterValue(PARAM_NEO4J_ADDRESS)).trim();
		initCapacity = (Integer) aContext.getConfigParameterValue(PARAM_INIT_CAPACITY);
		acronymsEnabled = (Boolean) aContext.getConfigParameterValue(PARAM_ENABLE_ACRONYMS);
		writingVariantsEnabled = (Boolean) aContext.getConfigParameterValue(PARAM_ENABLE_WRITING_VARIANTS);
		acronymsIncludedInWritingVariants = (Boolean) aContext
				.getConfigParameterValue(PARAM_INCLUDE_ACRONYMS_IN_VARIANTS);
		maxCacheSize = (Integer) aContext.getConfigParameterValue(PARAM_MAX_CACHE_SIZE);
		setConcurrencyLevel(aContext.getConfigParameterValue(PARAM_CONCURRENCY_LEVEL));
		setDefaults();
		initCache(initCapacity, maxCacheSize);
		newVariantMap = new HashMap<>();

		variantsMap = new HashMap<>(initCapacity);
		acronymsMap = new HashMap<>(initCapacity);

		client = HttpClients.createDefault();

		log.info("Extracting concept term variants according to the following mappings");
		for (int i = 0; i < types.length; i++) {
			String typeMapping = types[i];
			log.info("\t{}", typeMapping);
		}
		this.authorizationToken = "Basic " + Base64.encodeBase64URLSafeString(("neo4j" + ":" + "julielab").getBytes());
		log.info("Sending extracted variants to Neo4j server at {}", neo4jAddress);
		if (!isAddressReachable(neo4jAddress))
			log.warn(
					"Neo4j is currently not reachable at {}. It will be tried to reach the server again at each batch. Terms that could not be sent to the server with their batch will expire.");

	}

	@Deprecated
	private void setDefaults() {
		if (null == initCapacity)
			initCapacity = 1000;
		if (null == maxCacheSize)
			maxCacheSize = 10000;
		if (!neo4jAddress.endsWith("/"))
			neo4jAddress += "/";
	}

	private void buildFeaturePathMap(JCas aJCas) throws CASException {
		fpMap = new HashMap<>();
		TypeSystem ts = aJCas.getTypeSystem();
		for (int i = 0; i < types.length; i++) {
			String typeMapping = types[i];
			String[] split = typeMapping.split("=");
			if (split.length != 2)
				throw new IllegalArgumentException("The type to ID feature mapping \"" + typeMapping
						+ "\" is not valid. The mappings have to take the form <qualified name>=<ID feature path>");
			Type type = ts.getType(split[0]);
			if (null == type)
				throw new IllegalArgumentException("The type \"" + split[0] + "\" was not found in the type system.");
			JulesFeaturePath fp = new JulesFeaturePath();
			fp.initialize(split[1]);
			fpMap.put(type, fp);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
//		{
//			FSIterator<Annotation> iterator = aJCas.getAnnotationIndex(Header.type).iterator();
//			String docId = "<unknown>";
//			if (iterator.hasNext()) {
//				Header h = (Header) iterator.next();
//				docId = h.getDocId();
//			}
//			
//		Set<String> ids = new HashSet<>();
//		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Gene.type).iterator();
//		while (it.hasNext()) {
//			Gene gene = (Gene) it.next();
//			FSArray entryList = gene.getResourceEntryList();
//			if (null != entryList) {
//				for (int i = 0; i<entryList.size();++i) {
//					ResourceEntry entry = (ResourceEntry) entryList.get(i);
//					ids.add(entry.getEntryId());
//				}
//			}
//		}
//		for (String id : ids)
//			System.out.println("HIER: " + id + " " + docId );
//		}
		
		
		FSIterator<Annotation> iterator = aJCas.getAnnotationIndex(Header.type).iterator();
		String docId = "<unknown>";
		if (iterator.hasNext()) {
			Header h = (Header) iterator.next();
			docId = h.getDocId();
		}
		
		
		try {
			if (null == fpMap)
				buildFeaturePathMap(aJCas);

			for (Type type : fpMap.keySet()) {
				FSIterator<Annotation> it = aJCas.getAnnotationIndex(type).iterator();
				JulesFeaturePath idFp = fpMap.get(type);
				while (it.hasNext()) {
					Annotation a = it.next();
					if (a.getBegin() < 0 || a.getEnd() < 0 || a.getBegin() == a.getEnd()
							|| a.getEnd() > aJCas.getDocumentText().length() - 1) {
						log.debug(
								"An annotation of document {} has begin={} and end={} (document length {}); skipping this token",
								new Object[] { docId, a.getBegin(), a.getEnd(), aJCas.getDocumentText().length() });

						continue;
					}
					String[] ids = idFp.getValueAsStringArray(a);
					// String id = idFp.getValueAsString(a);
					if (null == ids || ids.length == 0) {
						log.debug("Annotation without a value for its ID occurred, skipping. The annotation was {}", a);
						continue;
					}

					boolean isAcronym = JCoReAnnotationTools.getAnnotationAtMatchingOffsets(aJCas, a,
							Abbreviation.class) != null;

					String termVariant = a.getCoveredText();
					for (int i = 0; i < ids.length; i++) {
						String id = ids[i];
						if (writingVariantsEnabled && (!isAcronym || acronymsIncludedInWritingVariants)) {
							recordCoveredText(termVariant, id, docId, variantsMap);
							++numNewVariants;
						}
						if (acronymsEnabled && isAcronym) {
							recordCoveredText(termVariant, id, docId, acronymsMap);
							++numNewAcronyms;
						}
					}
				}
			}
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

	}

	private void recordCoveredText(String termVariant, String id, String docId, Map<String, Map<String, Map<String, Integer>>> map) {
		Map<String, Map<String, Integer>> variants = map.get(id);
		if (null == variants) {
			variants = new HashMap<>();
			map.put(id, variants);
		}
		Map<String, Integer> countsInDocs = variants.get(docId);
		if (null == countsInDocs) {
			countsInDocs = new HashMap<>();
			variants.put(docId, countsInDocs);
		}
		Integer count = countsInDocs.get(termVariant);
		if (null == count)
			count = 0;
		++count;
		countsInDocs.put(termVariant, count);
	}

	/**
	 * To be sure that no side effects are happening, a synchronized method to
	 * set the static concurrency level.
	 * 
	 * @param tmp
	 */
	private static synchronized void setConcurrencyLevel(Object tmp) {
		concurrencyLevel = (Integer) (tmp == null ? 4 : tmp);
	}

	/**
	 * Synchronized method to create the static (i.e. shared among threads!)
	 * term writing variants cache
	 * 
	 * @param maxCacheSize
	 * @param initCapacity
	 */
	@Deprecated
	private static synchronized void initCache(Integer initCapacity, Integer maxCacheSize) {
		if (null == cache)
			cache = CacheBuilder.newBuilder().concurrencyLevel(concurrencyLevel).initialCapacity(initCapacity)
					.maximumSize(maxCacheSize).build();
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();
		if (isAddressReachable(neo4jAddress)) {
			commitWritingVariants();
		} else {
			log.warn(
					"Neo4j is currently not reachable at {}. It will be tried to reach the server again at each batch. Terms that could not be sent to the server with their batch will expire but could be added later if the server becomes reachable.");
			// remove the cached terms since they are not really known to the
			// server now
			removeTermsFromCache(newVariantMap);
		}
		newVariantMap.clear();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		if (isAddressReachable(neo4jAddress))
			commitWritingVariants();
		else
			log.warn("Neo4j is currently not reachable at {}. The final term batch will not be sent to the server.");
	}

	@Deprecated
	private void removeTermsFromCache(Map<String, List<String>> newVariantMap) {
		for (String conceptId : newVariantMap.keySet())
			cache.invalidate(conceptId);
	}

	private void commitWritingVariants() {
		// if (newVariantMap.isEmpty()) {
		if (variantsMap.isEmpty() && acronymsMap.isEmpty()) {
			log.info("No new concept term variants or acronyms found in this document batch.");
			return;
		}
		long time = System.currentTimeMillis();
		log.info(
				"Committing {} writing variants for {} concepts and {} acronyms for {} concepts to Neo4j server at {}.",
				new Object[] { numNewVariants, variantsMap.size(), numNewAcronyms, acronymsMap.size(), neo4jAddress });
		HttpPost post = new HttpPost(neo4jAddress + ADD_WRITING_VARIANTS_ENDPOINT);
		post.setHeader("Content-type", "application/json");
		post.setHeader("Authorization", authorizationToken);
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("termVariants", JsonSerializer.toJson(variantsMap));
			data.put("termAcronyms", JsonSerializer.toJson(acronymsMap));
			post.setEntity(new StringEntity(JsonSerializer.toJson(data), Charset.forName("UTF-8")));
			HttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			// We take all 200 values with us, because 204 is not really an
			// error. To get specific return codes, see HttpStatus
			// constants.
			if (response.getStatusLine().getStatusCode() >= 300) {
				String responseString = EntityUtils.toString(entity);
				log.error("Error when posting a request to the server: {}",
						null != entity && !StringUtils.isBlank(responseString) ? responseString
								: response.getStatusLine());
			} else
				EntityUtils.consume(entity);
		} catch (IOException e) {
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		log.info("Committing concept variants / acronyms to Neo4j took {}ms ({}s)", time, time / 1000);
		numNewVariants = 0;
		numNewAcronyms = 0;
		variantsMap.clear();
		acronymsMap.clear();
	}

	private boolean isAddressReachable(String address) {
		boolean reachable = false;
		try {
			URLConnection connection = new URL(address).openConnection();
			connection.connect();
			// If we've come this far without an exception, the connection is
			// available.
			reachable = true;
		} catch (ConnectException e) {
			// don't do anything, the warning will be logged below.
		} catch (IOException e) {
			e.printStackTrace();
		}
		return reachable;
	}

}
