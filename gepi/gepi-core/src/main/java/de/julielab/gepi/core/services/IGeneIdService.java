package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.GepiGeneInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;
public interface IGeneIdService {
	Future<IdConversionResult> convert(Stream<String> stream, IdType from, IdType to, Collection<String> taxIds);

	/**
	 * This enumeration lists the kinds of IDs that GePi can work with.
	 * 
	 * @author faessler
	 *
	 */
	enum IdType {
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
		GO,
		ENSEMBL,
		/**
		 * NCBI Gene ID
		 * 
		 * @see <url>https://www.ncbi.nlm.nih.gov/gene</url>
		 */
		GENE, GENE_NAME, UNKNOWN,
		/**
		 * The GePi-internal gene orthology aggregate IDs
		 */
		GEPI_AGGREGATE,
		/**
		 * FamPlex protein complex/family
		 *
		 * @see <url>https://github.com/sorgerlab/famplex</url>
		 */
		FAMPLEX,
		HGNC,
		/**
		 * HGNC Gene Group
		 *
		 * @see <url>https://www.genenames.org/data/genegroup/#!/</url>
		 */
		HGNC_GROUP
	}

	Future<IdConversionResult> convert(Stream<String> stream, IdType to, Collection<String> taxIds);

	Future<Stream<String>> convertUniprot2Gene(Stream<String> uniprotIds);

	Future<Stream<String>> convertGene2Gepi(Stream<String> geneIds);

	CompletableFuture<Multimap<String, String>> convertConceptNames2AggregateIds(Stream<String> geneNames);
	/**
	 * Tries to figure out which IDs are given.
	 *
	 * @param idStream
	 * @return
	 */
	IdType determineIdType(Stream<String> idStream);


	Multimap<IdType, String> determineIdTypes(Stream<String> idStream);

	Future<Multimap<String, String>> convert2Gepi(Stream<String> input, String label, String originalId);

	/**
	 * Upon given IDs or gene names converts the input into atids that resemble
	 * the top homology atid of the respective gene
	 *
	 * @param input
	 * @param label
	 * @param originalId
	 * @return
	 */
	CompletableFuture<Multimap<String, String>> convertConcept2AggregateGepiIds(Stream<String> input, String label, String originalId);

	Map<String, GepiGeneInfo> getGeneInfo(Iterable<String> conceptIds);

}
