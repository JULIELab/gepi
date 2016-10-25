package de.julielab.semedico.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDescription;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jules.ae.EventFlattener;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.RawToken;
import de.julielab.semedico.ae.HypernymsProvider;
import de.julielab.semedico.ae.ListProvider;
import de.julielab.semedico.ae.MapProvider;

public abstract class AbstractSemedicoFieldsGeneratorTest {

	protected static String documentPath;
	protected static Map<String, Object> jsonDoc;

	@SuppressWarnings("unchecked")
	public static void setup() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		XmiCasDeserializer.deserialize(new FileInputStream(documentPath), jcas.getCas());

		AnalysisEngine eventFlattener = AnalysisEngineFactory.createEngine(EventFlattener.class);
		eventFlattener.process(jcas);

		ExternalResourceDescription hypernyms = ExternalResourceFactory.createExternalResourceDescription(HypernymsProvider.class,
				"file:resources/LucasParams/hypernyms.tid");
		ExternalResourceDescription meshTermMapping = ExternalResourceFactory.createExternalResourceDescription(MapProvider.class,
				"file:src/test/resources/medline-fields-generator/meshdescriptors.2tid");
		ExternalResourceDescription stopwords = ExternalResourceFactory.createExternalResourceDescription(ListProvider.class,
				"file:resources/LucasParams/stopwords.txt");
		UimaContext uimaContext = UimaContextFactory.createUimaContext(SemedicoFilterBoard.RESOURCE_HYPERNYMS, hypernyms,
				SemedicoFilterBoard.RESOURCE_MESH_TERM_MAPPING, meshTermMapping, SemedicoFilterBoard.RESOURCE_STOPWORDS, stopwords);
		FilterRegistry fr = new FilterRegistry(uimaContext);
		fr.addFilterBoard(SemedicoFilterBoard.SEMEDICO_BOARD, new SemedicoFilterBoard());
		SemedicoMedlineFieldsGenerator fieldsGenerator = new SemedicoMedlineFieldsGenerator(fr);
		Document doc = fieldsGenerator.addFields(jcas, new Document());
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RawToken.class, new RawToken.RawTokenGsonAdapter());
		Gson gson = builder.create();
		String json = gson.toJson(doc);
		System.out.println(json);
		jsonDoc = gson.fromJson(json, Map.class);
	}


	@SuppressWarnings("unchecked")
	protected void assertIsArrayOfObjectsWithPreanalyzedTextValues(String fieldName, boolean stored) {
		List<Map<String, Object>> fieldContents = (List<Map<String, Object>>) jsonDoc.get(fieldName);
		assertNotNull("Field \"" + fieldName + "\" is null", fieldContents);
		assertFalse("Field \"" + fieldName + "\" is empty", fieldContents.isEmpty());
		for (Map<String, Object> arrayElement : fieldContents) {
			assertIsPreAnalyzedField("text", arrayElement, stored);
		}
	}

	/**
	 * A preanalyzed field must have a version and, for the sake of this test,
	 * must also have tokens. A field that is only stored does not take
	 * advantage of the preanalyzed format and should just be sent in its raw
	 * format.
	 * 
	 * @param stored
	 * @param fieldContents
	 */
	@SuppressWarnings("unchecked")
	protected void assertIsPreAnalyzedField(String fieldName, Map<String, Object> json, boolean stored) {
		Map<String, Object> fieldContents = (Map<String, Object>) json.get(fieldName);
		assertNotNull("Field \"" + fieldName + "\" is null", fieldContents);
		assertEquals("1", fieldContents.get("v"));
		List<Map<String, Object>> tokens = (List<Map<String, Object>>) fieldContents.get("tokens");
		assertNotNull(tokens);
		assertFalse(tokens.isEmpty());
		if (stored) {
			String stringValue = (String) fieldContents.get("str");
			assertNotNull("Field " + fieldName + " is supposed to be stored, yet its string value is null", stringValue);
			assertFalse("Field " + fieldName + " is supposed to be stored, yet its string value is empty", stringValue.length() == 0);
		}
	}

	@SuppressWarnings("unchecked")
	protected void assertValuesInField(String fieldName, Set<String> expectedValues, Map<String, Object> json) {
		List<String> fieldContents = (List<String>) json.get(fieldName);
		for (String token : fieldContents)
			assertTrue("Token \"" + token + "\" was not expected in field \"" + fieldName + "\"", expectedValues.remove(token));
		assertTrue("The values " + expectedValues + " were expected but not found in field \"" + fieldName + "\"", expectedValues.isEmpty());
	}

}
