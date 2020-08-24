package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;
public interface IGeneIdService {
	Future<IdConversionResult> convert(Stream<String> stream, IdType from, IdType to);

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
		 * @see <url>http://www.uniprot.org</url>
		 */
		UNIPROT_ACCESSION,
		/**
		 * UniProt / SwissProt mnemonics (e.g. IL2_HUMAN)
		 *
		 * @see <url>http://www.uniprot.org</url>
		 */
		UNIPROT_MNEMONIC,
		/**
		 * NCBI Gene ID
		 * 
		 * @see <url>https://www.ncbi.nlm.nih.gov/gene</url>
		 */
		GENE, GENE_NAME, UNKNOWN,
		/**
		 * The GePi-internal gene orthology aggregate IDs
		 */
		GEPI_AGGREGATE
	}

	Future<Stream<String>> convertUniprot2Gene(Stream<String> uniprotIds);

	Future<Stream<String>> convertGene2Gepi(Stream<String> geneIds);

	CompletableFuture<Multimap<String, String>> convertGeneNames2AggregateIds(Stream<String> geneNames);
	/**
	 * Tries to figure out which IDs are given.
	 *
	 * @param idStream
	 * @return
	 */
	IdType determineIdType(Stream<String> idStream);

	Future<Stream<String>> convert2Gepi(Stream<String> idStream);

	/**
	 * Upon given IDs or gene names converts the input into atids that resemble
	 * the top homology atid of the respective gene
	 *
	 * @param input
	 * @return
	 */
	CompletableFuture<Multimap<String, String>> convertGene2AggregateIds(Stream<String> input);


}
