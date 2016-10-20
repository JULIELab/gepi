package de.julielab.jules.jpp;

import org.apache.uima.collection.CollectionProcessingEngine;

import de.julielab.jules.cpe.DBStatusCallbackListener;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class JPPDBStatusCallbackListener extends DBStatusCallbackListener {

	public JPPDBStatusCallbackListener(CollectionProcessingEngine cpe,
			DataBaseConnector dbc, String subset, Integer batchSize) {
		super(cpe, dbc, subset, batchSize);
	}

	@Override
	public synchronized void collectionProcessComplete() {
		super.collectionProcessComplete();
		// TODO tell the pipelinemanager we're finished.
	}
	
	

}
