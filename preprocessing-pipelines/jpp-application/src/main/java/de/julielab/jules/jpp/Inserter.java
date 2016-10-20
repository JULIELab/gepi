package de.julielab.jules.jpp;

import java.sql.SQLException;
import java.util.Date;

import org.slf4j.LoggerFactory;

import de.julielab.xmlData.Constants;

public class Inserter extends AbstractJPPDBManager {
	private final String medlineFile;

	public Inserter(String medlineFile) {
		super(LoggerFactory.getLogger(Inserter.class), SortOfDBC.MEDLINE);
		this.medlineFile = medlineFile;
	}

	public void process() {
		try {
			log.info("Inserting Medline XML data from \"{}\".", medlineFile);
			if (!dbc.tableExists(Constants.DEFAULT_DATA_TABLE_NAME))
				dbc.createTable(Constants.DEFAULT_DATA_TABLE_NAME,
						"Medline document table created by JPP-Application on "
								+ new Date());
			dbc.importFromXMLFile(medlineFile, Constants.DEFAULT_DATA_TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	};
}
