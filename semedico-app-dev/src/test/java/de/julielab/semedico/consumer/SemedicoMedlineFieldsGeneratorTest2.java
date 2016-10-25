package de.julielab.semedico.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * To test abstract sections and keywords
 * @author faessler
 *
 */
public class SemedicoMedlineFieldsGeneratorTest2 extends AbstractSemedicoFieldsGeneratorTest {

	@BeforeClass
	public static void setup() throws Exception {
		documentPath = "src/test/resources/medline-fields-generator/19270793.xmi";
		AbstractSemedicoFieldsGeneratorTest.setup();
	}
	@Test
	public void testTitle() {
		assertIsPreAnalyzedField("title", jsonDoc, true);
	}

	@Test
	public void testAbstract() {
		assertIsPreAnalyzedField("abstract", jsonDoc, true);
	}

	@Test
	public void testSentences() {
		assertIsArrayOfObjectsWithPreanalyzedTextValues("sentences", true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> fieldContents = (List<Map<String, Object>>) jsonDoc.get("sentences");
		for (Map<String, Object> arrayElement : fieldContents) {
			String likelihood = (String) arrayElement.get("likelihood");
			assertNotNull("Sentence likelihood is null", likelihood);
			assertFalse("Sentence likelihood is the empty string", likelihood.length() == 0);
		}
	}

	@Test
	public void testAbstractSections() {
		assertIsArrayOfObjectsWithPreanalyzedTextValues("abstractsections", true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> fieldContents = (List<Map<String, Object>>) jsonDoc.get("abstractsections");
		for (Map<String, Object> arrayElement : fieldContents) {
			String likelihood = (String) arrayElement.get("likelihood");
			assertNotNull("AbstractSection likelihood is null", likelihood);
			assertFalse("AbstractSection likelihood is the empty string", likelihood.length() == 0);
			
			String label = (String) arrayElement.get("label");
			assertFalse(StringUtils.isBlank(label));
			String nlmcategory = (String) arrayElement.get("nlmcategory");
			assertFalse(StringUtils.isBlank(nlmcategory));
		}
	}

	@Test
	public void testPubmedID() {
		String pubmedID = (String) jsonDoc.get("pubmedID");
		assertEquals("19270793", pubmedID);
	}
	
	@Test
	public void testPmcID() {
		String pubmedID = (String) jsonDoc.get("pmcID");
		assertEquals("2649225", pubmedID);
	}

	@Test
	public void testKeywords() {
		Set<String> keywords = new HashSet<>(Arrays.asList("RDX","carcinogenesis","gene regulation","miRNA","microRNA","microarray","qRT-PCR","toxicant","toxicity","toxicogenomics"));
		assertValuesInField("keywords", keywords, jsonDoc);
	}

	@Test
	public void testAuthors() {
		Set<String> authors = new HashSet<>(Arrays.asList("Zhang, Baohong", "Pan, Xiaoping"));
		assertValuesInField("authors", authors, jsonDoc);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJournal() {
		Map<String, Object> journalJson = (Map<String, Object>) jsonDoc.get("journal");
		assertEquals("Environmental health perspectives", journalJson.get("title"));
		assertEquals("231-40", journalJson.get("pages"));
		assertEquals("2", journalJson.get("issue"));
		assertEquals("117", journalJson.get("volume"));
	}

	@Test
	public void testAffiliation() {
		Set<String> affiliations = new HashSet<>(
				Arrays.asList("Department of Biology, East Carolina University, Greenville, North Carolina 27858, USA. zhangb@ecu.edu"));
		assertValuesInField("affiliation", affiliations, jsonDoc);
	}


}
