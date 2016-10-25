package de.julielab.semedico.consumer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.utility.JCoReTools;

public class GeneIdExtractor extends JCasAnnotator_ImplBase {

//	public static final String OUTPUT_FILE = "OutputFile";
//	private String outputFile;
	private FileOutputStream os;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			os = new FileOutputStream("genesIdsInSemedicoTestDocs.lst");
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			FSIterator<Annotation> geneIt = aJCas.getAnnotationIndex(Gene.type).iterator();
			while (geneIt.hasNext()) {
				Gene gene = (Gene) geneIt.next();
				FSArray resourceEntryList = gene.getResourceEntryList();
				if (null != resourceEntryList) {
					for (int i = 0; i < resourceEntryList.size(); i++) {
						GeneResourceEntry resourceEntry = (GeneResourceEntry) resourceEntryList.get(i);
						if (null != resourceEntry) {
							String geneId = resourceEntry.getEntryId();
							String pmid = JCoReTools.getPubmedId(aJCas);
							List<String> record = Lists.newArrayList(pmid, geneId);
							IOUtils.write(StringUtils.join(record, "\t") + "\n", os, "UTF-8");
						}
					}
				}
			}
		} catch (CASRuntimeException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (FileNotFoundException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}

	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		try {
			os.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

}
