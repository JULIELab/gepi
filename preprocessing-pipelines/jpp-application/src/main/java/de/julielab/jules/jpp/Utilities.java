package de.julielab.jules.jpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.reader.DBCasXmiReader;
import de.julielab.jules.reader.DBReader;

public class Utilities {

	private final static Logger log = LoggerFactory.getLogger(Utilities.class);

	/**
	 * Returns the value of the <tt>parameterName</tt> parameter of the first CPE description which has a
	 * <tt>CpeCasProcessor</tt> which has a name where the regular expression <tt>regexp</tt> can be found and which
	 * reveals this parameter.
	 * 
	 * @param regexp
	 *            A regular expression applied to the lowercased names of <tt>CpeCasProcessors</tt> to identify the
	 *            correct processor.
	 * @param parameterName
	 *            The name of the parameter of which the parameter value is desired.
	 * @param cpeDescriptions
	 *            The CAS descriptions to search the parameter value in.
	 * @return The value of the parameter <tt>parameterName</tt> in the first <tt>CpeCasProcessor</tt> containing
	 *         <tt>regexp</tt> revealing this parameter.
	 * @throws IllegalArgumentException
	 *             If no non-null value can be found.
	 */
	@SuppressWarnings("unchecked")
	public static Object getCasProcessorParameterValue(String regexp, String parameterName,
			Collection<CpeDescription> cpeDescriptions, boolean returnAll) {
		Object parameterValues = null;
		List<String> matchingCpeDescUrls = new ArrayList<>();
		try {
			Pattern p = Pattern.compile(regexp);
			Matcher m = p.matcher("");
			for (CpeDescription cpeDesc : cpeDescriptions) {
				CpeCasProcessors cpeCasProcessors = cpeDesc.getCpeCasProcessors();
				for (CpeCasProcessor cp : cpeCasProcessors.getAllCpeCasProcessors()) {
					String name = cp.getName();
					m.reset(name.toLowerCase());
					if (m.find()) {
						matchingCpeDescUrls.add(cpeDesc.getSourceUrlString());
						Object currentParameterValue = cp.getConfigurationParameterSettings().getParameterValue(
								parameterName);
						// Nothing defined in the CPE descriptor? Check the
						// original descriptor, if the value is set there.
						if (null == currentParameterValue) {
							ConfigurationParameterSettings importedSettings = getImportedCpeCasProcessorParameterSettings(cp);
							currentParameterValue = importedSettings.getParameterValue(parameterName);

						}
						if (null != currentParameterValue) {
							log.debug(
									"Found value \"{}\" for parameter \"{}\" for CasProcessor \"{}\" in CPE at \"{}\".",
									new Object[] { currentParameterValue, parameterName, cp.getName(),
											cpeDesc.getSourceUrlString() });
							if (returnAll) {
								if (null == parameterValues)
									parameterValues = new ArrayList<String>();
								((List<Object>) parameterValues).add(currentParameterValue);
							} else {
								parameterValues = currentParameterValue;
								break;
							}
						}
					}
					if (null != parameterValues && !returnAll)
						break;
				}
				if (null != parameterValues && !returnAll)
					break;
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		}
		if (null == parameterValues) {
			List<String> cpeDescUrls = new ArrayList<>();
			for (CpeDescription cpeDesc : cpeDescriptions)
				cpeDescUrls.add(cpeDesc.getSourceUrlString());
			if (0 == matchingCpeDescUrls.size())
				log.warn("No CAS processor with a name containing the regular expression \"" + regexp
						+ "\" was found. Searched CPE descriptors were: " + StringUtils.join(cpeDescUrls, ", "));
			else
				log.warn("No non-null parameter value for \"" + parameterName
						+ "\" was found in the CPE descriptor(s) \"" + StringUtils.join(matchingCpeDescUrls, ",")
						+ "\". Searched CPE descriptors were: " + StringUtils.join(cpeDescUrls, ","));
		}
		return parameterValues;
	}

	private static ConfigurationParameterSettings getImportedCpeCasProcessorParameterSettings(CpeCasProcessor cp) {
		InputStream cpDescStream = null;
		CpeComponentDescriptor cpeComponentDesc = cp.getCpeComponentDescriptor();
		try {
			String name = cpeComponentDesc.getImport().getName();
			File cpeDescDir = new File(cp.getSourceUrl().toURI()).getParentFile();
			if (null != name)
				cpDescStream = Utilities.class.getResourceAsStream(name);
			else
				cpDescStream = new FileInputStream(cpeDescDir.getAbsolutePath() + "/"
						+ cpeComponentDesc.getImport().getLocation());
			AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
					new XMLInputSource(cpDescStream, new File(cp.getSourceUrl().toURI())));
			ConfigurationParameterSettings aeSettings = aeDesc.getMetaData().getConfigurationParameterSettings();
			return aeSettings;
		} catch (FileNotFoundException | InvalidXMLException | URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static ConfigurationParameterSettings getImportedCollectionReaderParameterSettings(CpeCollectionReader cr) {
		try {
			CollectionReaderDescription crDesc = getImportedCollectionReader(cr);
			ConfigurationParameterSettings aeSettings = crDesc.getMetaData().getConfigurationParameterSettings();
			return aeSettings;
		} catch (FileNotFoundException | InvalidXMLException | URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static CollectionReaderDescription getImportedCollectionReader(CpeCollectionReader cr)
			throws URISyntaxException, FileNotFoundException, InvalidXMLException {
		InputStream cpDescStream = null;
		CpeComponentDescriptor cpeComponentDesc = cr.getDescriptor();
		String name = cpeComponentDesc.getImport().getName();
		File cpeDescDir = new File(cr.getSourceUrl().toURI()).getParentFile();
		if (null != name)
			cpDescStream = Utilities.class.getResourceAsStream(name);
		else
			cpDescStream = new FileInputStream(cpeDescDir.getAbsolutePath() + "/"
					+ cpeComponentDesc.getImport().getLocation());
		CollectionReaderDescription crDesc = UIMAFramework.getXMLParser().parseCollectionReaderDescription(
				new XMLInputSource(cpDescStream, new File(cr.getSourceUrl().toURI())));
		return crDesc;
	}

	public static void setCasProcessorParameterValue(String regexp, String parameterName, Object parameterValue,
			CpeDescription cpeDesc) {
		try {
			Pattern p = Pattern.compile(regexp);
			Matcher m = p.matcher("");
			CpeCasProcessors cpeCasProcessors = cpeDesc.getCpeCasProcessors();
			for (CpeCasProcessor cp : cpeCasProcessors.getAllCpeCasProcessors()) {
				String name = cp.getName();
				m.reset(name.toLowerCase());
				if (m.find()) {
					CasProcessorConfigurationParameterSettings configurationParameterSettings = cp
							.getConfigurationParameterSettings();
					configurationParameterSettings.setParameterValue(parameterName, parameterValue);
					configurationParameterSettings.setParameterValue(parameterName, parameterValue);

					// Just to be safe, I believe I once had problems with
					// setting parameter values this way...
					// Oh yeah, we can't set non-overridden CPE parameters
					// because in the
					// CasProcessorConfigurationParameterSettingsImpl there is a
					// list of parameters and some array which has no further
					// function but to be asked for its length when returning
					// parameters; and the length is equal to the number of
					// parameter overrides, so new set parameters aren't seen.
					Object newParameterValue = configurationParameterSettings.getParameterValue(parameterName);
					if (!parameterValue.equals(newParameterValue))
						throw new IllegalStateException("Parameter \"" + parameterName + "\" of CpeProcessor \""
								+ cp.getName() + "\" in CPE at \"" + cpeDesc.getSourceUrlString()
								+ "\" was set, but the value did not actually change. This happens"
								+ " when the set parameter is not defined as a parameter override in the"
								+ " CPE descriptor. All parameters that should be able to be overriden"
								+ " within the Java application must have a parameter override in the CPE.");

					log.info("Set parameter \"{}\" of CpeProcessor \"{}\" in CPE at \"{}\" to \"{}\".", new Object[] {
							parameterName, cp.getName(), cpeDesc.getSourceUrlString(), parameterValue });
				}
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the value of the <tt>parameterName</tt> parameter of the first <tt>CpeCasProcessor</tt> which has a name
	 * where the regular expression <tt>regexp</tt> can be found and which reveals this parameter.
	 * 
	 * @param regexp
	 *            A regular expression applied to the lowercased names of <tt>CpeCasProcessors</tt> to identify the
	 *            correct processor.
	 * @param parameterName
	 *            The name of the parameter of which the parameter value is desired.
	 * @param cpeDesc
	 *            The CAS description to search the parameter value in.
	 * @return The value of the parameter <tt>parameterName</tt> in the first <tt>CpeCasProcessor</tt> containing
	 *         <tt>regexp</tt> revealing this parameter. May be <tt>null</tt> if the parameter is not found.
	 */
	public static Object getCasProcessorParameterValue(String regexp, String parameterName, CpeDescription cpeDesc) {
		Object parameterValue = null;

		try {
			Pattern p = Pattern.compile(regexp);
			Matcher m = p.matcher("");
			CpeCasProcessors cpeCasProcessors = cpeDesc.getCpeCasProcessors();
			for (CpeCasProcessor cp : cpeCasProcessors.getAllCpeCasProcessors()) {
				String name = cp.getName();
				m.reset(name.toLowerCase());
				if (m.find()) {
					parameterValue = cp.getConfigurationParameterSettings().getParameterValue(parameterName);
					if (null != parameterValue)
						break;
				}
				if (null != parameterValue)
					break;
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		}

		return parameterValue;
	}

	public static InputStream getMedlineDbcConfiguration(Collection<CpeDescription> cpeDescriptions) {
		CpeCollectionReader reader = getMedlineXmlCpeCollectionReader(cpeDescriptions);
		InputStream dbcConfiguration = getReaderDbcConfiguration(reader);
		return dbcConfiguration;
	}

	public static InputStream getXmiReaderDbcConfiguration(Collection<CpeDescription> cpeDescriptions) {
		CpeCollectionReader reader = getXmiCpeCollectionReader(cpeDescriptions);
		InputStream dbcConfiguration = getReaderDbcConfiguration(reader);
		return dbcConfiguration;
	}

	@SuppressWarnings("resource")
	public static InputStream getReaderDbcConfiguration(CpeCollectionReader reader) {
		if (null == reader)
			return null;
		// get dbc configuration parameter from the Medline Reader in the CPE;
		// the configuration is used to instantiate the Database Connector
		String configuration = null;
		CasProcessorConfigurationParameterSettings configurationParameterSettings = reader
				.getConfigurationParameterSettings();
		if (null != configurationParameterSettings)
			configuration = (String) configurationParameterSettings.getParameterValue(DBReader.PARAM_DBC_CONFIG_NAME);

		if (null == configuration) {
			ConfigurationParameterSettings importedConfigurationParameterSettings = getImportedCollectionReaderParameterSettings(reader);
			configuration = (String) importedConfigurationParameterSettings
					.getParameterValue(DBReader.PARAM_DBC_CONFIG_NAME);
		}
		if (null == configuration)
			throw new IllegalStateException(
					"DataBaseConnectior configuration is null. The parameter was taken from the reader of the CPE at: "
							+ reader.getSourceUrlString() + ".");

		InputStream is = null;
		try {
			is = new FileInputStream(configuration);
		} catch (FileNotFoundException e) {
			log.debug("DBC configuration not found as file at \"" + configuration + "\"; trying as resource...");
		}
		if (is == null)
			is = Utilities.class.getResourceAsStream(configuration);
		if (is == null)
			throw new IllegalStateException(
					"DataBaseConnector configuration could not be found as file or as classpath resource at \""
							+ configuration + "\". The parameter was taken from the reader of the CPE at: "
							+ reader.getSourceUrlString() + ".");
		log.info("DBCConfiguration taken from CollectionReader: " + configuration);
		return is;
	}

	/**
	 * <p>
	 * Returns the first <tt>CpeCollectionReader</tt> belonging to a {@link DBCasXmiReader} found in
	 * <tt>cpeDescriptions</tt>.
	 * </p>
	 * <p>
	 * The distinction whether a reader is a {@link DBMedlineReader} or a <tt>DBCasXmiReader</tt> is made by looking up
	 * whether there are additional tables defined (the medline reader doesn't, the XMI readers do; they don't have to,
	 * but they do in the JPP-usecase).
	 * </p>
	 * 
	 * @param cpeDescriptions
	 * @return
	 */
	public static CpeCollectionReader getXmiCpeCollectionReader(Collection<CpeDescription> cpeDescriptions) {
		CpeCollectionReader xmiReader = null;
		try {
			for (CpeDescription cpeDesc : cpeDescriptions) {
				CpeCollectionReader dbReader = cpeDesc.getAllCollectionCollectionReaders()[0];
				boolean isXmiReader = isXmiCollectionReader(dbReader);
				if (isXmiReader) {
					xmiReader = dbReader;
					break;
				}
				// CasProcessorConfigurationParameterSettings settings = dbReader.getConfigurationParameterSettings();
				// if (null == settings.getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)
				// || 0 == ((String[]) settings.getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)).length)
				// continue;
				// xmiReader = dbReader;
				// break;
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xmiReader;

	}

	private static boolean isXmiCollectionReader(CpeCollectionReader dbReader) throws URISyntaxException,
			FileNotFoundException, InvalidXMLException {
		CollectionReaderDescription cpReader = getImportedCollectionReader(dbReader);
		String implementationName = cpReader.getImplementationName();
		boolean isXmiReader = implementationName.endsWith("DBCasXmiReader");
		return isXmiReader;
	}

	/**
	 * <p>
	 * Returns the first <tt>CpeCollectionReader</tt> belonging to a {@link DBMedlineReader} found in
	 * <tt>cpeDescriptions</tt>.
	 * </p>
	 * <p>
	 * The distinction whether a reader is a <tt>DBMedlineReader</tt> or a {@link DBCasXmiReader} is made by looking up
	 * whether there are additional tables defined (the medline reader doesn't, the XMI readers do; they don't have to,
	 * but they do in the JPP-usecase).
	 * </p>
	 * 
	 * @param cpeDescriptions
	 * @return
	 */
	public static CpeCollectionReader getMedlineXmlCpeCollectionReader(Collection<CpeDescription> cpeDescriptions) {
		CpeCollectionReader medlineReader = null;
		try {
			for (CpeDescription cpeDesc : cpeDescriptions) {
				CpeCollectionReader dbReader = cpeDesc.getAllCollectionCollectionReaders()[0];
				CollectionReaderDescription cpReader = getImportedCollectionReader(dbReader);
				String implementationName = cpReader.getImplementationName();

				if (implementationName.endsWith("DBMedlineReader")) {
					medlineReader = dbReader;
					break;
				}
				// CasProcessorConfigurationParameterSettings settings = dbReader
				// .getConfigurationParameterSettings();
				// if (null == settings) {
				// ConfigurationParameterSettings importedSettings =
				// getImportedCollectionReaderParameterSettings(dbReader);
				// if (null == importedSettings
				// .getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)
				// || 0 == ((String[]) importedSettings
				// .getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)).length) {
				// medlineReader = dbReader;
				// break;
				// }
				// }
				// else if (null == settings
				// .getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)
				// || 0 == ((String[]) settings
				// .getParameterValue(DBReader.PARAM_ADDITIONAL_TABLES)).length) {
				// medlineReader = dbReader;
				// break;
				// }
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return medlineReader;

	}

	public static List<String> getAllXmiSubsetTables(Collection<CpeDescription> cpeDescriptions) {
		List<String> xmiSubsetTables = null;
		try {
			for (CpeDescription cpeDesc : cpeDescriptions) {
				CpeCollectionReader dbReader = cpeDesc.getAllCollectionCollectionReaders()[0];
				if (isXmiCollectionReader(dbReader)) {
					String xmiSubsetTable = null;
					CasProcessorConfigurationParameterSettings configurationParameterSettings = dbReader.getConfigurationParameterSettings();
					if (null != configurationParameterSettings)
						xmiSubsetTable= (String) configurationParameterSettings.getParameterValue(DBReader.PARAM_TABLE);
					else {
						ConfigurationParameterSettings importedSettings = getImportedCollectionReaderParameterSettings(dbReader);
						xmiSubsetTable = (String) importedSettings.getParameterValue(DBReader.PARAM_TABLE);
					}
					if (!StringUtils.isBlank(xmiSubsetTable) && null == xmiSubsetTables)
						xmiSubsetTables = new ArrayList<>();
						if (!StringUtils.isBlank(xmiSubsetTable))
							xmiSubsetTables.add(xmiSubsetTable);
				}
//				String[] additionalTables = (String[]) dbReader.getConfigurationParameterSettings().getParameterValue(
//						DBReader.PARAM_ADDITIONAL_TABLES);
//				if (null == additionalTables || additionalTables.length == 0)
//					// This is not an XMIReader but the Medline XML reader.
//					continue;
//				String xmiSubsetTable = (String) dbReader.getConfigurationParameterSettings().getParameterValue(
//						DBReader.PARAM_TABLE);
			}
		} catch (CpeDescriptorException e) {
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xmiSubsetTables;
	}

	public static String getMedlineXmlSubsetTable(Collection<CpeDescription> cpeDescriptions) {
		CpeCollectionReader medlineReader = getMedlineXmlCpeCollectionReader(cpeDescriptions);
		CasProcessorConfigurationParameterSettings configurationParameterSettings = medlineReader
				.getConfigurationParameterSettings();
		if (null != configurationParameterSettings)
			return (String) configurationParameterSettings.getParameterValue(DBReader.PARAM_TABLE);
		ConfigurationParameterSettings importedSettings = getImportedCollectionReaderParameterSettings(medlineReader);
		return (String) importedSettings.getParameterValue(DBReader.PARAM_TABLE);
		// try {
		// for (CpeDescription cpeDesc : cpeDescriptions) {
		// CpeCollectionReader dbReader = cpeDesc.getAllCollectionCollectionReaders()[0];
		// String[] additionalTables = (String[]) dbReader.getConfigurationParameterSettings().getParameterValue(
		// DBReader.PARAM_ADDITIONAL_TABLES);
		// if (null == additionalTables || additionalTables.length == 0) {
		// // This is not an XMIReader but the Medline XML reader.
		// String medlineXmlSubsetTable = (String) dbReader.getConfigurationParameterSettings()
		// .getParameterValue(DBReader.PARAM_TABLE);
		// return medlineXmlSubsetTable;
		// }
		// }
		// } catch (CpeDescriptorException e) {
		// e.printStackTrace();
		// }
		// return null;
	}
}
