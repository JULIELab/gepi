package de.julielab.gepi.core.services;

import de.julielab.gepi.core.GepiCoreSymbolConstants;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.neo4j.driver.Values.parameters;

public class GeneIdService implements IGeneIdService {


	private Logger log;
	private String boltUrl;

    public GeneIdService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
		this.log = log;

		this.boltUrl = boltUrl;
    }

	@Override
	public Future<Stream<String>> convertUniprot2Gene(Stream<String> uniprotIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Stream<String>> convertGene2Gepi(Stream<String> geneIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IdType recognizeIdType(Stream<String> idStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Stream<String>> convert2Gepi(Stream<String> idStream) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Future<Stream<String>> convertInput2Atid(String input) {
		return CompletableFuture.supplyAsync(() -> {
			Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));

			try (Session session = driver.session()) {

				return Stream.of(session.readTransaction(tx -> queryNeo4j(tx, input)));

			}
		});
	}

	private String[] queryNeo4j(Transaction tx, String input) {
		Record record;
		List<String> topAtids = new ArrayList<String>();
		
		String[] searchInput = input.split("\n");
		Result result = tx.run(
				"MATCH (n:CONCEPT) WHERE n:ID_MAP_NCBI_GENES AND n.originalId IN $originalIds " +
						"OPTIONAL MATCH (n)<-[:HAS_ELEMENT]-(a:AGGREGATE_GENEGROUP) " +
						"WITH a " +
						"OPTIONAL MATCH (a)<-[:HAS_ELEMENT]-(top:AGGREGATE_TOP_ORTHOLOGY) " +
						"RETURN DISTINCT COALESCE(top.id,a.id) AS SEARCH_ID",
				parameters("originalIds", searchInput));
		
		while (result.hasNext()) {
			record = result.next();
			topAtids.add(record.get("SEARCH_ID").asString());
		}
		return topAtids.toArray(new String[topAtids.size()]);

	}

}
