package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;

public class GeneIdService implements IGeneIdService {

	private String BASE_URL;

	public GeneIdService() {
		this.BASE_URL = "bolt://darwin:7687";
	}

	@Override
	public Stream<String> convertUniprot2Gene(Stream<String> uniprotIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<String> convertGene2Gepi(Stream<String> geneIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IdType recognizeIdType(Stream<String> idStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<String> convert2Gepi(Stream<String> idStream) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String[] convertInput2Atid(String input) {
		
		Config neo4jconf = Config.build().withoutEncryption().toConfig();
		Driver driver = GraphDatabase.driver(this.BASE_URL, AuthTokens.basic("neo4j", "julielab"), neo4jconf);

		try (Session session = driver.session()) {

			return session.readTransaction(new TransactionWork<String[]>() {
				@Override
				public String[] execute(Transaction tx) {
					return queryNeo4j(tx, input);
				}
			});

		}
	}

	// TODO: What if gene name is given and top_homology is ambiguous? Has to be
	// handled via, e.g. majority vote
	private String[] queryNeo4j(Transaction tx, String input) {
		Record record;
		List<String> topAtids = new ArrayList<String>();
		
		String[] searchInput = input.split("\n");
		StatementResult result = tx.run(
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
