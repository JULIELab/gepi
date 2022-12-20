package de.julielab.gepi.core.services;

import org.apache.tapestry5.ioc.internal.services.ClasspathResourceSymbolProvider;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.slf4j.Logger;

import java.io.IOException;

public class ConfigurationSymbolProvider implements SymbolProvider {

	public static final String CONFIG_FILE_PROPERTY = "gepi.configuration";
	public static final String CONFIG_FILE_ENV_VARIABLE = "GEPI_CONFIGURATION";
	private SymbolProvider symbolProvider;

	public ConfigurationSymbolProvider(Logger log) {
		String configFileName = System.getProperty(CONFIG_FILE_PROPERTY);
		if (configFileName == null || configFileName.isBlank())
			configFileName = System.getenv(CONFIG_FILE_ENV_VARIABLE);
		try {
			if (null == configFileName || configFileName.isBlank()) {
				String username = System.getProperty("user.name");
				configFileName = "configuration.properties." + username;
				log.info(
						"System property {} for configuration location undefined. Looking for classpath resource {} for a configuration.",
						CONFIG_FILE_PROPERTY, configFileName);
			} else {
				log.info("Given configuration resource location: {}", configFileName);
				try {
					this.symbolProvider = new ClasspathResourceSymbolProvider(configFileName);
					log.info("Found resource {} on the classpath.");
				} catch (NullPointerException e) {
					try {
						this.symbolProvider = new FileSymbolProvider(configFileName);
					} catch (IOException e1) {
						throw new IllegalArgumentException("Could not read configuration file at " + configFileName, e1);
					}
				}
			}
			if (null == this.symbolProvider) {
				log.info("Loading configuration from {}", configFileName);
				this.symbolProvider = new ClasspathResourceSymbolProvider(configFileName);
			}
		} catch (NullPointerException e) {
			throw new IllegalStateException(
					"No configuration file found in the classpath. A configuration file as classpath resource must either be given by the "+CONFIG_FILE_PROPERTY+" system property or there must exist a file named configuration.properties.user.name where user.name is the system user name system property.");
		}
		String resourceType = symbolProvider instanceof ClasspathResourceSymbolProvider ? "classpath" : "file";
		log.info("Found configuration as {} resource at {}", resourceType, configFileName);
	}

	@Override
	public String valueForSymbol(String symbolName) {
		return symbolProvider.valueForSymbol(symbolName);
	}

}
