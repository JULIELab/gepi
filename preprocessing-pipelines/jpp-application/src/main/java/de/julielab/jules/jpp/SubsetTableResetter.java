package de.julielab.jules.jpp;

import java.io.InputStream;
import java.util.List;

import org.slf4j.LoggerFactory;

import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class SubsetTableResetter extends AbstractJPPDBManager {

	public SubsetTableResetter() {
		super(LoggerFactory.getLogger(ProcessingSubsetTableCreator.class), SortOfDBC.XMI);
	}

	@Override
	public void process() throws Exception {
		resetMedlineXMLTable();
		resetXMITables();
	}

	private void resetXMITables() {
		List<String> subsetTables = Utilities
				.getAllXmiSubsetTables(cpeDescriptions);
		for (String subsetTable : subsetTables) {
			if (dbc.tableExists(subsetTable)) {
				log.info("Resetting XMI subset table {}", subsetTable);
				dbc.resetSubset(subsetTable);
			} else {
				log.warn("The XMI subset table {} does not exist and thus is not reset.", subsetTable);
			}
		}
	}

	private void resetMedlineXMLTable() {
		// First get the medline XML table for reset, if it exists
		InputStream medlineDbcConfiguration = Utilities.getMedlineDbcConfiguration(cpeDescriptions);
		if (null == medlineDbcConfiguration)
			return;
		DataBaseConnector dbc = new DataBaseConnector(medlineDbcConfiguration);

		String medlineXmlSubsetTable = Constants.DEFAULT_DATA_SCHEMA + "."
				+ Utilities.getMedlineXmlSubsetTable(cpeDescriptions);
		if (dbc.tableExists(medlineXmlSubsetTable)) {
			log.info("Resetting Medline XML subset table {}", medlineXmlSubsetTable);
			dbc.resetSubset(medlineXmlSubsetTable);
		} else {
			log.warn("The Medline XML subset table {} does not exist and thus is not reset.", medlineXmlSubsetTable);
		}
	}

}
