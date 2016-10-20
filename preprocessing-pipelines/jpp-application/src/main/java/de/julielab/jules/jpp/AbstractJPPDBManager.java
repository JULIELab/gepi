package de.julielab.jules.jpp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;

import de.julielab.xmlData.dataBase.DataBaseConnector;

public abstract class AbstractJPPDBManager {
	protected enum SortOfDBC {
		XMI, MEDLINE
	}

	protected final Logger log;
	protected DataBaseConnector dbc;
	protected Collection<CpeDescription> cpeDescriptions;

	/**
	 * <p>
	 * The parameter <tt>dbcSort</tt> determines which database connector
	 * configuration to use; for work with Medline XML data (e.g. for
	 * importing/updating from Medline XML), {@link SortOfDBC#MEDLINE} is to be
	 * used. For work with XMI data, {@link SortOfDBC#XMI} will be required.
	 * </p>
	 * <p>
	 * The choice of the DBC configuration makes a difference when creating
	 * tables, inserting/updating into tables and retrieving data from tables.
	 * In all these use cases, the correct table schema has to be employed which
	 * is stored in the database configuration.
	 * </p>
	 * 
	 * @param log
	 * @param dbcSort
	 *            Which database connector configuration to use; one of
	 *            {@link SortOfDBC#MEDLINE} or {@link SortOfDBC#XMI}
	 * @param jppBasedir
	 */
	public AbstractJPPDBManager(Logger log, SortOfDBC dbcSort) {
		this.log = log;
		cpeDescriptions = getCPEDescriptions();
		InputStream dbcConfigStream = null;
		switch (dbcSort) {
		case XMI:
			dbcConfigStream = Utilities.getXmiReaderDbcConfiguration(cpeDescriptions);
			break;
		case MEDLINE:
			dbcConfigStream = Utilities.getMedlineDbcConfiguration(cpeDescriptions);
			break;
		}
		dbc = new DataBaseConnector(dbcConfigStream);
	}

	public abstract void process() throws Exception;

	protected File[] getMedlineFiles(String medlinePathString) throws FileNotFoundException {
		File medlinePath = new File(medlinePathString);
		if (!medlinePath.exists())
			throw new FileNotFoundException("File \"" + medlinePathString + "\" was not found.");
		if (!medlinePath.isDirectory())
			return new File[] { medlinePath };
		File[] medlineFiles = medlinePath.listFiles(new FileFilter() {
			public boolean accept(File file) {
				String filename = file.getName();
				return filename.endsWith("gz") || filename.endsWith("gzip") || filename.endsWith("zip");
			}
		});
		// Check whether anything has been read.
		if (medlineFiles == null || medlineFiles.length == 0) {
			log.info("No (g)zipped files found in directory {}. No update will be performed.", medlinePathString);
			System.exit(0);
		}
		return medlineFiles;
	}

	private Collection<CpeDescription> getCPEDescriptions() {
		Collection<CpeDescription> allCpeFiles = new ArrayList<>();

		try {
			// We just assume that the JPPApplication is a sibling directory to
			// the JPP subpipelines. Thus, when we go up one directory, we
			// should have reached the common root directory of the
			// preprocessing project and find the sub-pipelines there.
			File rootfile = new File("../");
			IOFileFilter falseFileFilter = FileFilterUtils.falseFileFilter();
			Collection<File> jppDirs = FileUtils.listFilesAndDirs(rootfile, falseFileFilter,
					FileFilterUtils.prefixFileFilter("jpp-"));
			// Check for 1 and not for 0 because seemingly the directory '.' is
			// always included.
			if (jppDirs.size() == 1)
				throw new IllegalStateException(
						"No pipelines subdirectories have been found. The current working directory is: \""
								+ rootfile.getAbsolutePath()
								+ "\". It should be the jules-preprocessing-pipelines/jpp-application diretory.");
			for (File jppDir : jppDirs) {
				File descDir = FileUtils.getFile(jppDir, "desc");
				if (jppDir.equals(rootfile))
					continue;
				if (!descDir.isDirectory())
					continue;
				// If we don't get the canonical file, there will be ugly
				// constructions like
				// ...jules-preprocessing-pipelines/./jpp-syntax/desc in the
				// path (which can actually be a problem later).
				descDir = descDir.getCanonicalFile();
				RegexFileFilter cpeFileFilter = new RegexFileFilter("CPE.*");
				Collection<File> cpeFiles = FileUtils.listFiles(descDir, cpeFileFilter, null);
				// if (cpeFiles.size() > 1) {
				// List<String> fileNameList = new ArrayList<>();
				// for (File cpeFile : cpeFiles)
				// fileNameList.add(cpeFile.getAbsolutePath());
				// throw new IllegalStateException(
				// "Found multiple CPE files: "
				// + StringUtils.join(cpeFiles, ","));
				// } else
				if (cpeFiles.size() == 0) {
					throw new IllegalStateException("No CPE file was found in: " + descDir.getAbsolutePath());
				}
				Iterator<File> it = cpeFiles.iterator();
				while (it.hasNext()) {
					File cpeFile = it.next();
					CpeDescription cpeDescription;
					try {
						cpeDescription = UIMAFramework.getXMLParser().parseCpeDescription(new XMLInputSource(cpeFile));
					} catch (Exception e) {
						throw new RuntimeException(
								"The CPE Descriptor \"" + cpeFile
										+ "\" could not be parsed. Program execution is terminated. Original error follows.",
								e);
					}
					allCpeFiles.add(cpeDescription);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allCpeFiles;
	}
}
