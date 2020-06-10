package de.julielab.gepi.core.retrieval.services;

import com.google.common.collect.Sets;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.neo4j.driver.Values.parameters;

public class EventPostProcessingService implements IEventPostProcessingService {

	private Logger log;

	private Driver driver;

	public EventPostProcessingService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
		this.log = log;
		driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));

	}

	/*
	 * Currently checks against geneId rather than top homology (th) atid, as not
	 * all species nodes in our neo4j contain th atids. Thus, the cypher query is
	 * sensitive to a present/absent th node connection.
	 */
	@Override
	public List<Event> setPreferredNameFromConceptId(List<Event> ev) {

		log.trace("Number of events for post processing: {}", ev.size());
		// the following hashmap maps gene ids as they appear in the previous hashmaps
		// to their respective preferred name as it is written in the neo4j database
		// get preferred names from neo4j database
		Map<String, String> geneIdPrefNameMap = getGeneIdPrefNameMap(
				ev.stream().flatMap(e -> e.getArguments().stream().map(Argument::getConceptId)).collect(toSet()));

		ev.stream().flatMap(e -> e.getArguments().stream()).forEach(a -> {
			String preferredName = geneIdPrefNameMap.get(a.getConceptId());
			assert preferredName != null : "Could not find the preferred name for the concept ID " + a.getConceptId();
			a.setPreferredName(preferredName);
		});

		return ev;
	}

	@Override
	public void setArgumentGeneIds(List<Event> events) {
		Map<String, String> concept2gene = getGeneIds(
				events.stream().flatMap(e -> e.getArguments().stream().map(Argument::getConceptId)).collect(toSet()));
		
		events.stream().flatMap(e -> e.getArguments().stream()).forEach(a -> {
			String geneId = concept2gene.get(a.getConceptId());
			assert geneId != null : "Could not find the gene ID for the concept ID " + a.getConceptId();
			a.setGeneId(geneId);
		});
	}

	private Map<String, String> getGeneIds(Set<String> conceptIds) {
		Map<String, String> concept2gene = new HashMap<>();
		try (Session session = driver.session()) {
			session.readTransaction(tx -> {
				String statementTemplate = "MATCH (g:ID_MAP_NCBI_GENES) WHERE g.id IN {conceptIds} RETURN g.id AS CONCEPT_ID,g.originalId AS GENE_ID";
				Value parameters = parameters("conceptIds", conceptIds);
				log.trace("Cypher query to obtain preferred names: {} with parameters {}", statementTemplate,
						parameters);
				Result result = tx.run(statementTemplate, parameters);
				while (result.hasNext()) {
					Record record = result.next();
					String conceptId = record.get("CONCEPT_ID").asString().replaceAll("\"", "");
					String geneId = record.get("GENE_ID").asString().replaceAll("\"", "");
					concept2gene.put(conceptId, geneId);
				}
				return concept2gene;
			});
		}
		
		assert conceptIds.size() == concept2gene.size() : conceptIds.size()
				+ " concept IDs were given but only for " + concept2gene.size()
				+ ", their gene ID was fetched. Missing concept IDs: "
				+ Sets.difference(conceptIds, concept2gene.keySet());
		
		return concept2gene;
	}

	/**
	 * query neo4j for mapping of gene id to top homology preferred name if
	 * available if not available take preferred name of species identified by its
	 * current ncbi entrez gene id
	 * 
	 * @param conceptIds
	 */
	private Map<String, String> getGeneIdPrefNameMap(Set<String> conceptIds) {

		Map<String, String> geneIdPrefNameMap = new HashMap<>();

		try (Session session = driver.session()) {

			session.readTransaction(new TransactionWork<Map<String, String>>() {
				@Override
				public Map<String, String> execute(Transaction tx) {
					Record record;

					String statementTemplate = "MATCH (t:ID_MAP_NCBI_GENES) where t.id IN {entrezIds} " + "WITH t "
							+ "OPTIONAL MATCH (t)-[:HAS_ELEMENT*2]-(n:AGGREGATE_TOP_HOMOLOGY) "
							+ "return DISTINCT t.id AS ENTREZ_ID, "
							+ "COALESCE(n.preferredName, t.preferredName) AS PNAME";
					Value parameters = parameters("entrezIds", conceptIds);
					log.trace("Cypher query to obtain preferred names: {} with parameters {}", statementTemplate,
							parameters);
					Result result = tx.run(statementTemplate, parameters);
					int numReceived = 0;
					while (result.hasNext()) {
						record = result.next();

						geneIdPrefNameMap.put(record.get("ENTREZ_ID").toString().replaceAll("\"", ""),
								record.get("PNAME").toString().replaceAll("\"", ""));
						++numReceived;
					}
					log.trace("Received {} concept ID - preferred name mapping", numReceived);
					return geneIdPrefNameMap;
				}
			});
		}

		assert conceptIds.size() == geneIdPrefNameMap.size() : conceptIds.size()
				+ " concept IDs were given but only for " + geneIdPrefNameMap.size()
				+ ", their preferred name was fetched. Missing concept IDs: "
				+ Sets.difference(conceptIds, geneIdPrefNameMap.keySet());

		if (log.isTraceEnabled())
			geneIdPrefNameMap.entrySet().stream().map(Entry::toString).forEach(log::trace);

		return geneIdPrefNameMap;

	}

}