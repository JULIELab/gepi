package de.julielab.gepi.core.retrieval.services;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tapestry5.annotations.Log;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.slf4j.Logger;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;

public class EventPostProcessingService implements IEventPostProcessingService {

	private Logger log;

	private String BASE_NEO4J_URL = "bolt://dawkins:7687";

	public EventPostProcessingService(Logger log) {
		this.log = log;
	}

	/*
	 * Currently checks against geneId rather than top homology (th) atid, as not
	 * all species nodes in our neo4j contain th atids. Thus, the cypher query is
	 * sensitive to a present/absent th node connection.
	 */
	@Log
	@Override
	public List<Event> setPreferredNameFromGeneId(List<Event> ev) {
		// the following hashmap maps gene ids as they appear in the previous hashmaps
		// to their respective preferred name as it is written in the neo4j database
		Map<String, String> geneIdPrefNameMap = new HashMap<>();

		Set<Argument> args = new HashSet<Argument>();
		Set<String> conceptIds = new HashSet<String>();
		for (Event e : ev)
			for (Argument a : e.getArguments()) {
				args.add(a);
				if (a.getConceptId() != null)
					conceptIds.add(a.getConceptId());
				else {
					log.warn("Event {} has a null geneId, check why!", e);
				}
			}

		// get preferred names from neo4j database
		geneIdPrefNameMap = getGeneIdPrefNameMap(geneIdPrefNameMap, conceptIds);

		for (Argument a : args) {
			String preferredName = geneIdPrefNameMap.get(a.getConceptId());
			if (preferredName != null)
			a.setPreferredName(preferredName);
			else 
				a.setPreferredName("<unknown>");
		}

		return ev;
	}

	/**
	 * query neo4j for mapping of gene id to top homology preferred name if
	 * available if not available take preferred name of species identified by its
	 * current ncbi entrez gene id
	 * 
	 * @param geneIdPrefNameMap
	 * @param conceptIds
	 */
	private Map<String, String> getGeneIdPrefNameMap(Map<String, String> geneIdPrefNameMap, Set<String> conceptIds) {

		Config neo4jconf = Config.build().withoutEncryption().toConfig();
		Driver driver = GraphDatabase.driver(this.BASE_NEO4J_URL, AuthTokens.basic("neo4j", "julielab"), neo4jconf);

		try (Session session = driver.session()) {

			session.readTransaction(new TransactionWork<Map<String, String>>() {
				@Override
				public Map<String, String> execute(Transaction tx) {
					Record record;
					String[] searchInput = new String[conceptIds.size()];
					searchInput = conceptIds.toArray(new String[conceptIds.size()]);

					StatementResult result = tx.run("MATCH (t:ID_MAP_NCBI_GENES) where t.id IN {entrezIds} "
							+ "WITH t "
							+ "OPTIONAL MATCH (t:ID_MAP_NCBI_GENES)-[:HAS_ELEMENT*2]-(n:AGGREGATE_TOP_HOMOLOGY) "
							+ "WHERE t.id IN {entrezIds} " + "return DISTINCT t.id AS ENTREZ_ID, "
							+ "COALESCE(n.preferredName, t.preferredName) AS PNAME",
							parameters("entrezIds", searchInput));

					while (result.hasNext()) {
						record = result.next();
						geneIdPrefNameMap.put(record.get("ENTREZ_ID").toString().replaceAll("\"", ""),
								record.get("PNAME").toString().replaceAll("\"", ""));
					}
					return geneIdPrefNameMap;
				}
			});
		}

		geneIdPrefNameMap.entrySet().stream().map(Entry::toString).forEach(log::trace);

		return geneIdPrefNameMap;

	}

}