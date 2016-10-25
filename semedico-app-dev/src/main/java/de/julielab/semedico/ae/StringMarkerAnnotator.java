package de.julielab.semedico.ae;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.DiscontinuousAnnotation;

public class StringMarkerAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Matcher m = Pattern.compile("Stimulation with anti").matcher(aJCas.getDocumentText());
		while (m.find()) {
			int begin = m.start();
			int end = m.end();
			new DiscontinuousAnnotation(aJCas, begin, end).addToIndexes();;
		}

	}

}
