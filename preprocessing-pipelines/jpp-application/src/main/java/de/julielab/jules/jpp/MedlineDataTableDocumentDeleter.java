package de.julielab.jules.jpp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class MedlineDataTableDocumentDeleter implements IDocumentDeleter {

	private final  static Logger log = LoggerFactory.getLogger(MedlineDataTableDocumentDeleter.class);
	
	private final DataBaseConnector dbc;

	public MedlineDataTableDocumentDeleter(DataBaseConnector dbc) {
		this.dbc = dbc;
	}

	@Override
	public void deleteDocuments(List<String> docIds) {
		log.info(
				"Deleting {} documents marked for deletion in update file from table \"{}\".",
				docIds.size(), Constants.DEFAULT_DATA_TABLE_NAME);
		dbc.deleteFromTableSimplePK(Constants.DEFAULT_DATA_TABLE_NAME, docIds);
	}

}
