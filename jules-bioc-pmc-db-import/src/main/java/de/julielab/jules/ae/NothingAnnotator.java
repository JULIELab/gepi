package de.julielab.jules.ae;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

public class NothingAnnotator extends JCasAnnotator_ImplBase {

	
	private int counter = 0;
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		++counter;
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		System.out.println("Document count: " + counter);
	}
	

}
