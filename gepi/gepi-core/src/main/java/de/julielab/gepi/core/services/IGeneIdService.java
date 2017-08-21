package de.julielab.gepi.core.services;

import java.util.stream.Stream;

public interface IGeneIdService {
	/**
	 * This enumeration lists the kinds of IDs that GePi can work with.
	 * 
	 * @author faessler
	 *
	 */
	public enum IdType {
		/**
		 * UniProt / SwissProt accession IDs
		 * 
		 * @see http://www.uniprot.org
		 */
		UNIPROT_ACCESSION,
		/**
		 * NCBI Gene ID
		 * 
		 * @see https://www.ncbi.nlm.nih.gov/gene
		 */
		GENE, UNKNOWN
	}

	// TODO Think about whether we want to convert IDs online or if we'd rather
	// just store the mapped IDs directly in the Neo4j database and then simply
	// query directly
	Stream<String> convertUniprot2Gene(Stream<String> uniprotIds);

	Stream<String> convertGene2Gepi(Stream<String> geneIds);

	/**
	 * Tries to figure out which IDs are given.
	 * 
	 * @param idStream
	 * @return
	 */
	IdType recognizeIdType(Stream<String> idStream);

	Stream<String> convert2Gepi(Stream<String> idStream);
	
	/**
	 * Upon given IDs or gene names converts the input into atids that resemble 
	 * the top homology atid of the respective gene 
	 * 
	 * @param input
	 * @return
	 */
	String[] convertInput2Atid( String input);
	
}
