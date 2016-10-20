package de.julielab.jules.jpp;

import java.util.List;


public abstract interface IDocumentDeleter {
	
	public abstract void deleteDocuments(List<String> docIds);
	
	
}
