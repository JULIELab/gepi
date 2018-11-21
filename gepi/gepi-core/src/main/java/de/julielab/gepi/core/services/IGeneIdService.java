package de.julielab.gepi.core.services;

import java.util.concurrent.Future;
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

	Future<Stream<String>> convertUniprot2Gene(Stream<String> uniprotIds);

	Future<Stream<String>> convertGene2Gepi(Stream<String> geneIds);

	/**
	 * Tries to figure out which IDs are given.
	 * 
	 * @param idStream
	 * @return
	 */
	IdType recognizeIdType(Stream<String> idStream);

	Future<Stream<String>> convert2Gepi(Stream<String> idStream);
	
	/**
	 * Upon given IDs or gene names converts the input into atids that resemble 
	 * the top homology atid of the respective gene 
	 * 
	 * @param input
	 * @return
	 */
	Future<Stream<String>> convertInput2Atid( String input);
	
}
