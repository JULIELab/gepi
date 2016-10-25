package de.julielab.semedico.consumer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractSectionHeading;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.utility.JCoReTools;

public class StructuredAbstractLikelihoodWriter extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(StructuredAbstractLikelihoodWriter.class);
	private Map<String, Map<String, List<Integer>>> batch;
	private Map<String, Integer> likelihoodValues;
	private File outputFile;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		likelihoodValues = new HashMap<>();
		likelihoodValues.put("negation", 1);
		likelihoodValues.put("low", 2);
		likelihoodValues.put("investigation", 3);
		likelihoodValues.put("moderate", 4);
		likelihoodValues.put("high", 5);
		likelihoodValues.put("assertion", 6);

		batch = new HashMap<>();

		File outputdir = new File("abstract-section-likelihoods");
		if (!outputdir.exists())
			outputdir.mkdir();

		try {
			// create an output file; other annotators could have already
			// created files, so we search for a free place
			int numFile = 0;
			outputFile = new File(outputdir.getAbsolutePath() + File.separator
					+ "abstractSectionLikelihoods" + numFile + ".tsv");
			while (!outputFile.createNewFile())
				outputFile = new File(outputdir.getAbsolutePath() + File.separator
						+ "abstractSectionLikelihoods" + numFile++ + ".tsv");
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		log.info("Writing likelihoods for abstract sections to {}", outputFile.getAbsolutePath());
		super.initialize(aContext);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(AbstractSection.type).iterator();
		if (!it.hasNext())
			return;
		String pmid = JCoReTools.getPubmedId(aJCas);
		Map<String, List<Integer>> likelihoodPerLabel = new HashMap<>();
		while (it.hasNext()) {
			AbstractSection section = (AbstractSection) it.next();
			AbstractSectionHeading abstractSectionHeading = (AbstractSectionHeading) section
					.getAbstractSectionHeading();
			String nlmCategory = abstractSectionHeading.getNlmCategory();
			List<LikelihoodIndicator> likelihoodsInSection = JCasUtil.selectCovered(
					LikelihoodIndicator.class, section);
			List<Integer> likelihoodOrdinals = new ArrayList<>();
			if (likelihoodsInSection.isEmpty())
				likelihoodOrdinals.add(likelihoodValues.get("assertion"));
			for (LikelihoodIndicator indicator : likelihoodsInSection)
				likelihoodOrdinals.add(likelihoodValues.get(indicator.getLikelihood()));
			likelihoodPerLabel.put(nlmCategory, likelihoodOrdinals);
		}
		batch.put(pmid, likelihoodPerLabel);
	}

	private void writeLikelihoods() throws AnalysisEngineProcessException {
		try {
			for (String pmid : batch.keySet()) {
				Map<String, List<Integer>> likelihoodPerLabel = batch.get(pmid);
				for (String nlmCategory : likelihoodPerLabel.keySet()) {
					List<Integer> likelihoodsInSection = likelihoodPerLabel.get(nlmCategory);
					for (Integer likelihood : likelihoodsInSection) {
						FileUtils.write(outputFile, pmid + "\t" + nlmCategory + "\t" + likelihood
								+ "\n", "UTF-8", true);
					}
				}
			}
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		batch.clear();
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		log.info("Writing batch of {} abstract-section-likelihood-mappings to {}", batch.size(), outputFile.getAbsolutePath());
		writeLikelihoods();
		super.batchProcessComplete();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		log.info("Writing batch of {} abstract-section-likelihood-mappings to {}", batch.size(), outputFile.getAbsolutePath());
		writeLikelihoods();
		super.collectionProcessComplete();
	}

}
