package de.julielab.semedico.consumer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.GeneralEventMention;
import de.julielab.jcore.types.ResourceEntry;

public class EventTypeExtractor extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(EventTypeExtractor.class);

	public static final String PARAM_OUTPUT_FILE = "OutputFile";
	private FileOutputStream fos;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		String outputFile = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_FILE);
		try {
			fos = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(GeneralEventMention.type).iterator();
		while (it.hasNext()) {
			GeneralEventMention gem = (GeneralEventMention) it.next();
			String specificType = gem.getSpecificType();
			FSArray arguments = gem.getArguments();
			List<String> argumentIds = new ArrayList<String>();
			for (int i = 0; i < arguments.size(); i++) {
				ArgumentMention arg = (ArgumentMention) arguments.get(i);
				ConceptMention ref = (ConceptMention) arg.getRef();
				if (null == ref) {
					log.warn("Referenced entity of ArgumentMention was null. Please check whether the respective reference type (Genes, miRNAs, ...) is loaded. No ID printed for this argument.");
					continue;
				}
				FSArray resourceEntryList = ref.getResourceEntryList();
				if (null != resourceEntryList && resourceEntryList.size() > 0) {
					ResourceEntry entry = (ResourceEntry) resourceEntryList.get(0);
					String entryId = entry.getEntryId();
					argumentIds.add(entryId);
				} else {
					log.warn("Entity has no entries for its ResourceEntryList, thus no ID can be determined. Type of the entity is "
							+ ref.getClass().getCanonicalName());
				}
			}
			try {
				IOUtils.write(specificType + "\t" + StringUtils.join(argumentIds, "-") + "\n", fos, "UTF-8");
			} catch (IOException e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		try {
			fos.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

}
