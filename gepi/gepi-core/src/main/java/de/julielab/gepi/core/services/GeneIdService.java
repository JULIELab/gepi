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

	// TODO: What if gene name is given and top_homology is ambiguous? Has to be
	// handled via, e.g. majority vote
	private String[] queryNeo4j(Transaction tx, String input) {
		Record record;
		List<String> topAtids = new ArrayList<String>();
		
		String[] searchInput = input.split("\n");
		Result result = tx.run(
				"MATCH (n:ID_MAP_NCBI_GENES) WHERE n.originalId IN {originalIds} "
				+ "OPTIONAL MATCH (n)<-[:HAS_ELEMENT*2]-(a:AGGREGATE_TOP_HOMOLOGY) "
				+ "RETURN DISTINCT( COALESCE(a.id,n.id) ) AS SEARCH_ID",
				parameters("originalIds", searchInput));
		
		while (result.hasNext()) {
			record = result.next();
			topAtids.add(record.get("SEARCH_ID").asString());
		}
		return topAtids.toArray(new String[topAtids.size()]);

	}

}
