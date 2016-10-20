package de.julielab.jules.jpp;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;

import org.apache.uima.collection.metadata.CpeDescription;
import org.slf4j.LoggerFactory;

import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class Updater extends AbstractJPPDBManager {

	/**
	 * Name of the table that keeps track of already imported and finished
	 * Medline update files. Value: {@value #UPDATE_TABLE}.
	 */
	public static final String UPDATE_TABLE = Constants.DEFAULT_DATA_SCHEMA
			+ "._medline_update_files";

	public static final String COLUMN_FILENAME = "update_file_name";
	public static final String COLUMN_IS_IMPORTED = "is_imported";
	public static final String COLUMN_TIMESTAMP = "timestamp_of_import";

	private final String medlineFile;

	public Updater(String medlineFile) {
		super(LoggerFactory.getLogger(Updater.class), SortOfDBC.MEDLINE);
		this.medlineFile = medlineFile;
	}

	public void process() throws FileNotFoundException {
		File[] updateFiles = getMedlineFiles(medlineFile);
		List<File> unprocessedMedlineUpdates = getUnprocessedMedlineUpdates(
				updateFiles, dbc);
		IDocumentDeleter[] documentDeleters = getDocumentDeleters(
				cpeDescriptions, dbc);
		for (File file : unprocessedMedlineUpdates) {
			log.info("Processing file {}.", file.getAbsoluteFile());
			dbc.updateFromXML(file.getAbsolutePath(),
					Constants.DEFAULT_DATA_TABLE_NAME);
			List<String> pmidsToDelete = getPmidsToDelete(file);
			for (IDocumentDeleter documentDeleter : documentDeleters)
				documentDeleter.deleteDocuments(pmidsToDelete);

			// As last thing, mark the current update file as finished.
			markFileAsImported(file, dbc);
		}

	}

	/**
	 * This method servers as a kind of configuration about in which resource
	 * the documents are deleted (Solr, additional database tables etc).
	 * 
	 * @param dbc
	 * @param cpeDescriptions
	 * 
	 * @return
	 */
	private IDocumentDeleter[] getDocumentDeleters(
			Collection<CpeDescription> cpeDescriptions, DataBaseConnector dbc) {
		return new IDocumentDeleter[] {
				new MedlineDataTableDocumentDeleter(dbc),
				new ElasticSearchDocumentDeleter("conf/elasticsearch.properties") };
	}

	private static List<File> getUnprocessedMedlineUpdates(File[] updateFiles,
			DataBaseConnector dbc) {
		Connection conn = dbc.getConn();

		List<File> unprocessedFiles = new ArrayList<>();

		Set<String> updateFileNameSet = new HashSet<String>();
		for (File f : updateFiles)
			updateFileNameSet.add(f.getName());

		try {
			Statement st = conn.createStatement();
			// Create the table listing the update file names, if it does not
			// exist.
			boolean exists = dbc.tableExists(UPDATE_TABLE);
			if (!exists) {

				String createUpdateTable = String.format("CREATE TABLE %s "
						+ "(%s TEXT PRIMARY KEY," + "%s BOOLEAN DEFAULT FALSE,"
						+ "%s TIMESTAMP WITHOUT TIME ZONE)", UPDATE_TABLE,
						COLUMN_FILENAME, COLUMN_IS_IMPORTED, COLUMN_TIMESTAMP);
				st.execute(createUpdateTable);
			}
			// Determine which update files are new.
			Set<String> filenamesInDBSet = new HashSet<String>();
			ResultSet rs = st.executeQuery(String.format("SELECT %s from %s",
					COLUMN_FILENAME, UPDATE_TABLE));

			while (rs.next()) {
				String filename = rs.getString(COLUMN_FILENAME);
				filenamesInDBSet.add(filename);
			}
			// From all update files we found in the update directory, remove
			// the files already residing in the database.
			updateFileNameSet.removeAll(filenamesInDBSet);

			// Now add all new filenames.
			conn.setAutoCommit(false);
			PreparedStatement ps = conn.prepareStatement(String.format(
					"INSERT INTO %s VALUES (?)", UPDATE_TABLE));
			for (String filename : updateFileNameSet) {
				ps.setString(1, filename);
				ps.addBatch();
			}
			ps.executeBatch();
			conn.commit();
			conn.setAutoCommit(true);

			// Retrieve all those filenames from the database which have not yet
			// been processed. This includes the new files we just entered into
			// the database table.
			String sql = String.format(
					"SELECT %s FROM %s WHERE %s = FALSE", COLUMN_FILENAME,
					UPDATE_TABLE, COLUMN_IS_IMPORTED);
			rs = st.executeQuery(sql);
			final Set<String> unprocessedFileSet = new HashSet<String>();
			while (rs.next()) {
				unprocessedFileSet.add(rs.getString(COLUMN_FILENAME));
			}
			// Create a list of files which will only contain all files
			// to be processed.
			for (File updateFile : updateFiles) {
				if (unprocessedFileSet.contains(updateFile.getName()))
					unprocessedFiles.add(updateFile);
			}
			// And now a last but very important step: Sort the list to be in
			// the correct update-sequence. If we don't, it can happen that we
			// process a newer update before an older which will then result in
			// deprecated documents kept in the database.
			Collections.sort(unprocessedFiles, new Comparator<File>() {

				@Override
				public int compare(File f1, File f2) {
					// The files are named like "medline13n0792.xml.zip", i.e.
					// their sequence number if part of the file name. We just
					// have to sort by string comparison.
					return f1.getName().compareTo(f2.getName());
				}
			});
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return unprocessedFiles;
	}

	private static List<String> getPmidsToDelete(File file) {
		List<String> pmidsToDelete = new ArrayList<String>();
		String forEachXpath = "/MedlineCitationSet/DeleteCitation/PMID";
		List<Map<String, String>> fields = new ArrayList<Map<String, String>>();
		Map<String, String> field = new HashMap<String, String>();
		field.put(JulieXMLConstants.NAME, Constants.PMID_FIELD_NAME);
		field.put(JulieXMLConstants.XPATH, ".");
		fields.add(field);

		int bufferSize = 1000;
		Iterator<Map<String, Object>> it = JulieXMLTools
				.constructRowIterator(file.getAbsolutePath(), bufferSize,
						forEachXpath, fields, false);

		while (it.hasNext()) {
			Map<String, Object> row = it.next();
			String pmid = (String) row.get(Constants.PMID_FIELD_NAME);
			pmidsToDelete.add(pmid);
		}
		return pmidsToDelete;
	}

	/**
	 * Marks the file <code>file</code> as being imported in the database table
	 * {@link #UPDATE_TABLE}.
	 * 
	 * @param file
	 * @param dbc
	 */
	private void markFileAsImported(File file, DataBaseConnector dbc) {
		Connection conn = dbc.getConn();
		String sql = null;
		try {
			sql = String.format("UPDATE %s SET %s = TRUE, %s = '"
					+ new Timestamp(System.currentTimeMillis())
					+ "' WHERE %s = '%s'", UPDATE_TABLE, COLUMN_IS_IMPORTED,
					COLUMN_TIMESTAMP, COLUMN_FILENAME, file.getName());
			conn.createStatement().execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("SQL command was: {}", sql);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
