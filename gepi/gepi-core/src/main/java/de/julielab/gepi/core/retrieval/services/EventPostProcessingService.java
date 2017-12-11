package de.julielab.gepi.core.retrieval.services;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import de.julielab.gepi.core.retrieval.data.Event;

public class EventPostProcessingService implements IEventPostProcessingService {

	private Logger log;
	private String BASE_NEO4J_URL = "bolt://dawkins:7687";

	
	/*
	 * Currently checks against geneId rather than top homology (th)
	 * atid, as not all species nodes in our neo4j contain th atids. 
	 * Thus, the cypher query is sensitive to a present/absent th node
	 * connection.
	 */
	@Override
	public List<Event> setPreferredNameFromGeneId(List<Event> ev) {
		// the following hashmaps gather information of which gene is appearing 
		// in which event(s)
		// two maps are created to differentiate between first and second argument
		HashMap<String, List<Event>> geneIdEvtMap1stArg, geneIdEvtMap2ndArg;
		// the following hashmap maps gene ids as they appear in the previous hashmaps
		// to their respective preferred name as it is written in the neo4j database
		HashMap<String, String> geneIdPrefNameMap;
		
		geneIdEvtMap1stArg = new HashMap<String, List<Event>>();
		geneIdEvtMap2ndArg = new HashMap<String, List<Event>>();
		setGeneIdEvtMap(ev, geneIdEvtMap1stArg, geneIdEvtMap2ndArg);
		// get preferred names from neo4j database
		Set<String> geneIds = new HashSet<String>( geneIdEvtMap1stArg.size() + geneIdEvtMap2ndArg.size() );
		geneIds.addAll(geneIdEvtMap1stArg.keySet());
		geneIds.addAll(geneIdEvtMap2ndArg.keySet());
		geneIdPrefNameMap = new HashMap<String, String>();		
		getGeneIdPrefNameMap(geneIdPrefNameMap, geneIds);
		
		// associate the correct preferred name to the event
		String tmpPrefName = new String();
		// check 1st arguments
		for (String key : geneIdEvtMap1stArg.keySet()) {
			tmpPrefName = geneIdPrefNameMap.get(key);
			Iterator<Event> it = geneIdEvtMap1stArg.get(key).iterator();		
			while (it.hasNext()) {
				it.next().getFirstArgument().setPreferredName(tmpPrefName);
			}
		}
		// check 2nd arguments
		for (String key : geneIdEvtMap2ndArg.keySet()) {
			tmpPrefName = geneIdPrefNameMap.get(key);
			Iterator<Event> it = geneIdEvtMap2ndArg.get(key).iterator();		
			while (it.hasNext()) {
				it.next().getSecondArgument().setPreferredName(tmpPrefName);
			}
		}
		
		return ev;
	}
	
	
	/**
	 * query neo4j for mapping of gene id to top homology preferred name if available
	 * if not available take preferred name of species identified by its current ncbi entrez gene id
	 * @param geneIdPrefNameMap
	 * @param geneIds
	 */
	private void getGeneIdPrefNameMap(HashMap<String, String> geneIdPrefNameMap, Set<String> geneIds) {
		
		Config neo4jconf = Config.build().withoutEncryption().toConfig();
		Driver driver = GraphDatabase.driver(this.BASE_NEO4J_URL, 
				AuthTokens.basic("neo4j", "julielab"), neo4jconf);
		
		try (Session session = driver.session()) {

			session.readTransaction(new TransactionWork<HashMap<String, String>>() {
				@Override
				public HashMap<String, String> execute(Transaction tx) {
					Record record;
					String[] searchInput = new String[geneIds.size()];
					searchInput = geneIds.toArray(new String[geneIds.size()]);
					
					StatementResult result = tx.run(
							"MATCH (t:ID_MAP_NCBI_GENES) where t.originalId IN {entrezIds} "
							+ "WITH t "
							+ "OPTIONAL MATCH (t:ID_MAP_NCBI_GENES)-[:HAS_ELEMENT*2]-(n:AGGREGATE_TOP_HOMOLOGY) "
							+ "WHERE t.originalId IN {entrezIds} "
							+ "return DISTINCT t.originalId AS ENTREZ_ID, "
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
		
	}
	
	
	/**
	 * Create a map from entrez gene ids to to events.
	 * The reason to use gene ids is that of now not all genes in our database are associated to a 
	 * top homology atid.
	 * @param ev - List of events
	 */
	private void setGeneIdEvtMap(List<Event> ev, 
			HashMap<String, List<Event>> geneIdEvtMap1stArg, 
			HashMap<String, List<Event>> geneIdEvtMap2ndArg) {
		Event currentEvt;
		String tmpTHAtid;
				
		Iterator<Event> it = ev.iterator();
		while (it.hasNext()) {
			currentEvt = it.next();
			// first argument
			tmpTHAtid  = currentEvt.getFirstArgument().getGeneId();
			if ( geneIdEvtMap1stArg.containsKey( tmpTHAtid ) )
				geneIdEvtMap1stArg.get(tmpTHAtid).add(currentEvt);
			else {
				List<Event> tmpList = new ArrayList<Event>();
				tmpList.add(currentEvt);
				geneIdEvtMap1stArg.put(tmpTHAtid, tmpList);				
			}
			// 2nd argument
			tmpTHAtid  = currentEvt.getSecondArgument().getGeneId();
			if ( geneIdEvtMap2ndArg.containsKey( tmpTHAtid ) )
				geneIdEvtMap2ndArg.get(tmpTHAtid).add(currentEvt);
			else {
				List<Event> tmpList = new ArrayList<Event>();
				tmpList.add(currentEvt);
				geneIdEvtMap2ndArg.put(tmpTHAtid, tmpList);
			}
		}		
	}

}