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

public class SemedicoMedlineFieldsGeneratorTest1 extends AbstractSemedicoFieldsGeneratorTest {

	@BeforeClass
	public static void setup() throws Exception {
		documentPath = "src/test/resources/medline-fields-generator/10364429.xmi";
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
		// the test document does not have a structured abstracts
	}

	@Test
	public void testPubmedID() {
		String pubmedID = (String) jsonDoc.get("pubmedID");
		assertEquals("10364429", pubmedID);
	}

	@Test
	public void testKeywords() {
		// the test document has no keywords
	}

	@Test
	public void testMeshMinor() {
		// "meshminor":["tidmesh10","tidmesh8","tidmesh11","tidmesh3","tidmesh9","tidmesh5","tidmesh4","tidmesh12","tidmesh14","tidmesh6","tidmesh7","tidmesh13"],"meshmajor":["tidmesh1","tidmesh2"]
		Set<String> minorMeshTopics = new HashSet<>(Arrays.asList("tidmesh10", "tidmesh8", "tidmesh11", "tidmesh3", "tidmesh9", "tidmesh5", "tidmesh4",
				"tidmesh12", "tidmesh14", "tidmesh6", "tidmesh7", "tidmesh13"));
		assertValuesInField("meshminor", minorMeshTopics, jsonDoc);
	}

	@Test
	public void testMeshMajor() {
		Set<String> minorMajorTopics = new HashSet<>(Arrays.asList("tidmesh1", "tidmesh2"));
		assertValuesInField("meshmajor", minorMajorTopics, jsonDoc);
	}

	@Test
	public void testSubstances() {
		Set<String> substances = new HashSet<>(Arrays.asList("tidmesh1", "tidsubstance1", "tidmesh9", "tidmesh10", "tidmesh2", "tidmesh12"));
		assertValuesInField("substances", substances, jsonDoc);
	}

	@Test
	public void testAuthors() {
		Set<String> authors = new HashSet<>(Arrays.asList("Feinbaum, R", "Ambros, V"));
		assertValuesInField("authors", authors, jsonDoc);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJournal() {
		Map<String, Object> journalJson = (Map<String, Object>) jsonDoc.get("journal");
		assertEquals("Developmental biology", journalJson.get("title"));
		assertEquals("87-95", journalJson.get("pages"));
		assertEquals("1", journalJson.get("issue"));
		assertEquals("210", journalJson.get("volume"));
	}

	@Test
	public void testConceptList() {
		Set<String> concepts = new HashSet<>(Arrays.asList("tidmesh3", "tidmesh4", "tidmesh5", "tidmesh6", "tidmesh7", "tidmesh8", "tidmesh9", "tidmesh10",
				"tidmesh11", "tidmesh2", "tidmesh12", "tidmesh13", "tidmesh1", "tidmesh14", "tidsubstance1"));
		assertValuesInField("conceptlist", concepts, jsonDoc);
	}

	@Test
	public void testAffiliation() {
		Set<String> affiliations = new HashSet<>(
				Arrays.asList("Department of Molecular Biology, Massachusetts General Hospital, Boston, Massachusetts, 02114, USA."));
		assertValuesInField("affiliation", affiliations, jsonDoc);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEvents() {
		List<Map<String, Object>> events = (List<Map<String, Object>>) jsonDoc.get("events");
		assertEquals(3, events.size());
		for (Map<String, Object> event : events) {
			List<String> allarguments = (List<String>) event.get("allarguments");
			// in this particular document, the events are unary. So we get one
			// gene ID and the covered text of the gene
			assertEquals(1, allarguments.size());
			assertTrue(allarguments.contains("lin-14") || allarguments.contains("LIN-14 protein"));

			List<String> maineventtype = (List<String>) event.get("maineventtype");
			// specificType and covered text
			assertEquals(1, maineventtype.size());
			for (String token : maineventtype)
				assertFalse(StringUtils.isBlank(token));

			List<String> alleventtypes = (List<String>) event.get("alleventtypeids");
			assertNotNull(alleventtypes.size());
			assertEquals(1, alleventtypes.size());

			List<String> allargumentIds = (List<String>) event.get("allargumentids");
			// in this particular document, the events are unary. So we get one
			// gene ID and the covered text of the gene
			assertEquals(1, allargumentIds.size());
			assertTrue(allargumentIds.contains("181337"));

			List<String> maineventtypeId = (List<String>) event.get("maineventtypeid");
			// specificType and covered text
			assertEquals(1, maineventtypeId.size());
			for (String token : maineventtypeId)
				assertTrue(token.equals("Positive_regulation") || token.equals("Negative_regulation"));

			List<String> alleventtypeIds = (List<String>) event.get("alleventtypeids");
			assertNotNull(alleventtypeIds.size());
			assertEquals(1, alleventtypeIds.size());

			
			String likelihood = (String) event.get("likelihood");
			assertFalse(StringUtils.isBlank(likelihood));
		}
	}

}
