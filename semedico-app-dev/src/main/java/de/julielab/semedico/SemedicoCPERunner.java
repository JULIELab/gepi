/**
 * SemedicoDBCPERunner.java
 *
 * Copyright (c) 2012, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 *
 * Author: faessler
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 12.10.2012
 **/

/**
 * 
 */
package de.julielab.semedico;

import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.util.XMLInputSource;

/**
 * @author faessler
 * 
 */
public class SemedicoCPERunner {

	public static void main(String[] args) throws Exception {
		String descriptorFile = args[0];
		CpeDescription cpeDescription =
				UIMAFramework.getXMLParser().parseCpeDescription(new XMLInputSource(descriptorFile));
		final CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDescription);
		cpe.process();
		cpe.addStatusCallbackListener(new StatusCallbackListener() {

			@Override
			public void resumed() {
				// TODO Auto-generated method stub

			}

			@Override
			public void paused() {
				// TODO Auto-generated method stub

			}

			@Override
			public void initializationComplete() {
				// TODO Auto-generated method stub

			}

			@Override
			public void collectionProcessComplete() {
				// TODO Auto-generated method stub

				System.out.println(cpe.getPerformanceReport().toString());
			}

			@Override
			public void batchProcessComplete() {
				// TODO Auto-generated method stub

			}

			@Override
			public void aborted() {
				// TODO Auto-generated method stub

			}

			@Override
			public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
				if (aStatus.isException()) {
					List<Exception> exceptions = aStatus.getExceptions();
					for (int i = 0; i < exceptions.size(); ++i) {
						Throwable e = exceptions.get(i);
						e.printStackTrace();
					}
					System.err.println("Encountered error, exiting program.");
					System.exit(1);
				}
			}
		});
	}

}
