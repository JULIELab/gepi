package de.julielab.jules.jpp;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.slf4j.LoggerFactory;

import de.julielab.jules.consumer.CasToXmiDBConsumer;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class ProcessingSubsetTableCreator extends AbstractJPPDBManager {

	public ProcessingSubsetTableCreator() {
		super(LoggerFactory.getLogger(ProcessingSubsetTableCreator.class),
				SortOfDBC.XMI);
	}

	@Override
	public void process() throws Exception {
		createMedlineXmlMirrorSubset();
		createXmiMirrorSubsets();
	}

	private void createXmiMirrorSubsets() {
		try {
			String documentTable = (String) Utilities
					.getCasProcessorParameterValue("cas.*xmi.*consumer",
							CasToXmiDBConsumer.PARAM_TABLE_DOCUMENT,
							cpeDescriptions, false);
			if (!documentTable.contains("."))
				documentTable = dbc.getActiveDataPGSchema() + "."
						+ documentTable;
			List<String> subsetTables = Utilities
					.getAllXmiSubsetTables(cpeDescriptions);
			if (!dbc.tableExists(documentTable)) {
				// We only create the default _data table for Medline XML when
				// we not set have an XMI document table. Because when we
				// already have it, we have perhaps directly created it and do
				// not need the Medline XML data table. For example, we import
				// PubmedCentral documents directly to XMI. Actually, this could
				// be done for Medline as well if we don't need the actual XML
				// later.
				if (!dbc.tableExists(Constants.DEFAULT_DATA_TABLE_NAME)) {
					dbc.createTable(
							Constants.DEFAULT_DATA_TABLE_NAME,
							"Created by "
									+ ProcessingSubsetTableCreator.class
											.getSimpleName()
									+ " in JPP-Application in order to be able to create the subset table schema. Creation time: "
									+ new Date());
				}
				log.info(
						"Creating document table \"{}\" for being able to create mirror subsets.",
						documentTable);
				dbc.createTable(
						documentTable,
						Constants.DEFAULT_DATA_TABLE_NAME,
						dbc.getActiveTableSchema(),
						"Created by JPP-Application in order to be able to create mirror subsets in advance. Creation time: "
								+ new Date());
			}
			for (String subsetTable : subsetTables) {
				if (!dbc.tableExists(subsetTable)) {
					log.info(
							"Creating mirror subset table \"{}\" on document table \"{}\" for preprocessing pipelines.",
							subsetTable, documentTable);
					try {
						dbc.defineMirrorSubset(subsetTable, documentTable,
								true, 1, "Created by JPP-Application on "
										+ new Date());
					} catch (SQLException e) {
						log.error(
								"Subset table \"{}\" could not be created. Error was: {}",
								subsetTable, e.getMessage());
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void createMedlineXmlMirrorSubset() {
		try {
			// We need a DBC configured for the Medline configuration here,
			// otherwise the mirror collection table will end up in the wrong PG
			// schema.
			InputStream medlineDbcConfiguration = Utilities
					.getMedlineDbcConfiguration(cpeDescriptions);
			if (null == medlineDbcConfiguration)
				return;
			DataBaseConnector dbc = new DataBaseConnector(
					medlineDbcConfiguration);

			String medlineXmlSubsetTable = Constants.DEFAULT_DATA_SCHEMA + "."
					+ Utilities.getMedlineXmlSubsetTable(cpeDescriptions);
			if (!dbc.tableExists(Constants.DEFAULT_DATA_TABLE_NAME)) {
				dbc.createTable(
						Constants.DEFAULT_DATA_TABLE_NAME,
						"Created by "
								+ ProcessingSubsetTableCreator.class
										.getSimpleName()
								+ " in JPP-Application in order to be able to create the subset table schema. Creation time: "
								+ new Date());
			}
			if (!dbc.tableExists(medlineXmlSubsetTable)) {
				log.info("Creating Medline XML mirror subset table \"{}\".",
						medlineXmlSubsetTable);
				dbc.defineMirrorSubset(medlineXmlSubsetTable,
						dbc.getActiveDataTable(), true,
						"Created by JPP-Application on " + new Date(),
						dbc.getActiveTableSchema());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
