package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;

public class GeneIdService implements IGeneIdService {

	private String BASE_URL;
	
	public GeneIdService () {
		this.BASE_URL = "dawkins://7676";
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

	// TODO: What if gene name is given and top_homology is ambiguous? Has to be handled via, e.g. majority vote
	@Override
	public Stream<String> convertInput2Atid(String input) {

		Record record;
		Driver driver = GraphDatabase.driver( this.BASE_URL );
		Session session = driver.session();

		String[] searchInput = {"196", "11622"};
		
		StatementResult result = session.run( "MATCH (t:ID_MAP_NCBI_GENES) <-[:HAS_ELEMENT*2]-(a:AGGREGATE_TOP_HOMOLOGY) "
				+ "WHERE t.originalId IN {originalIds} RETURN DISTINCT(a.id) AS ATID",
		        parameters( "originalIds", searchInput ) );

		StringBuilder outputAtids = new StringBuilder();
		
		while ( result.hasNext() )
		{
			record = result.next();
			outputAtids.append(record.get( "ATID" ).asString() + "\n");
		}

		session.close();
		driver.close();
		
		return Stream.of(outputAtids.toString().split("\n"));
	}

	
	
}
